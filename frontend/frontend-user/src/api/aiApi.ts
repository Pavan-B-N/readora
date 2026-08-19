import { apiClient, correlationId } from './client';
import { store } from '@/redux/store';

/** One semantic-search hit — a book id/title paired with its similarity score. */
export interface AiSearchItem {
  bookId: string;
  title: string;
  score: number;
}

/** Response payload for a semantic search request. */
export interface AiSearchResponse {
  query: string;
  items: AiSearchItem[];
}

/** Semantic (vector) search — public, no auth required. */
export async function semanticSearch(query: string, limit = 8): Promise<AiSearchResponse> {
  const response = await apiClient.get<AiSearchResponse>('/api/v1/ai/search', {
    params: { q: query, limit },
  });
  return response.data;
}

/** Summary of one saved shopping-assistant conversation, as shown in a history list. */
export interface ConversationSummary {
  conversationId: string;
  title: string | null;
  messageCount: number;
  updatedAt: string;
}

/** One persisted turn of a shopping-assistant conversation. */
export interface StoredMessage {
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
  bookIds: string[];
}

/** Most recent conversation first — used to find the one to resume on chat reopen. */
export async function listConversations(size = 1): Promise<ConversationSummary[]> {
  const response = await apiClient.get<{ content: ConversationSummary[] }>('/api/v1/ai/conversations', {
    params: { page: 0, size },
  });
  return response.data.content;
}

/** Fetches the full message history for one saved conversation. */
export async function getConversationMessages(conversationId: string): Promise<StoredMessage[]> {
  const response = await apiClient.get<StoredMessage[]>(`/api/v1/ai/conversations/${conversationId}/messages`);
  return response.data;
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/** The final SSE frame of a reply carries this prefix instead of reply text — see ChatService.BOOK_IDS_FRAME_PREFIX. */
const BOOK_IDS_FRAME_PREFIX = '@@RDX_BOOK_IDS@@:';

/** Result of a completed (or aborted) streamChat call: the resolved conversation id and any book ids the backend surfaced. */
export interface StreamChatResult {
  conversationId: string | null;
  bookIds: string[];
}

/** Streams an assistant reply token-by-token via fetch rather than EventSource (this is a POST with a JSON body and an Authorization header, which EventSource can't send) — resolves to the conversation id (created if none was passed) and the book ids the backend found relevant to this reply. */
export async function streamChat(
  message: string,
  conversationId: string | null,
  onToken: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<StreamChatResult> {
  const { accessToken } = store.getState().auth;
  // The book-search tools filter every candidate to what's actually purchasable at this store (see ChatClientConfig's system prompt) — without it, the assistant can only recommend from the whole catalogue, including books with no stock here and no virtual edition.
  const { selectedId: storeId } = store.getState().store;

  const response = await fetch(`${BASE_URL}/api/v1/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'X-Correlation-Id': correlationId(),
    },
    body: JSON.stringify({ message, conversationId, storeId }),
    signal,
  });

  if (!response.ok || !response.body) {
    throw new Error(`Chat failed with status ${response.status}`);
  }

  const resolvedConversationId = response.headers.get('X-Conversation-Id') ?? conversationId;

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let bookIds: string[] = [];

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // SSE frames are separated by a blank line; a single frame's payload can itself contain newlines, which the spec requires the sender to split across multiple "data:" lines within that frame — the receiver rejoins them with "\n", so calling onToken() once per line instead of once per frame would silently drop those newlines (bullet points and paragraph breaks would vanish from the live-streamed text, even though a reload renders fine since the persisted message is never round-tripped through this per-line reconstruction).
    const frames = buffer.split('\n\n');
    buffer = frames.pop() ?? '';

    for (const frame of frames) {
      const dataLines = frame.split('\n').filter((line) => line.startsWith('data:'));
      if (dataLines.length === 0) continue;

      // Do NOT strip a leading space here: Spring writes the payload with no separator space, so a token that is itself a space arrives as "data: " — stripping would silently delete every space in the reply.
      const payload = dataLines.map((line) => line.slice(5)).join('\n');

      if (payload.startsWith(BOOK_IDS_FRAME_PREFIX)) {
        try {
          bookIds = JSON.parse(payload.slice(BOOK_IDS_FRAME_PREFIX.length));
        } catch {
          // Best-effort — a malformed frame just means no carousel for this turn.
        }
      } else {
        onToken(payload);
      }
    }
  }

  return { conversationId: resolvedConversationId, bookIds };
}

// Reader assistant — a focused RAG Q&A over one purchased book's own content, entirely separate from the shopping assistant above (no tools, no book recommendations, no cross-book knowledge).

/** Indexing state of a book's reader assistant: null/PENDING before ready, READY once queryable, FAILED if indexing errored. */
export type ReaderIndexStatus = 'PENDING' | 'READY' | 'FAILED' | null;

/** One turn of a per-book reader-assistant Q&A. */
export interface ReaderMessage {
  role: 'USER' | 'ASSISTANT';
  content: string;
}

/** Checks whether a book's content has been indexed for the reader assistant yet. */
export async function getReaderStatus(bookId: string): Promise<ReaderIndexStatus> {
  const response = await apiClient.get<{ status: ReaderIndexStatus }>(`/api/v1/ai/books/${bookId}/reader/status`);
  return response.data.status;
}

/** Embeds the book's content for the assistant — one-time, shared by every owner. */
export async function initializeReader(bookId: string): Promise<ReaderIndexStatus> {
  const response = await apiClient.post<{ status: ReaderIndexStatus }>(`/api/v1/ai/books/${bookId}/reader/initialize`);
  return response.data.status;
}

/** Fetches the caller's prior Q&A turns with this book's reader assistant. */
export async function getReaderHistory(bookId: string): Promise<ReaderMessage[]> {
  const response = await apiClient.get<ReaderMessage[]>(`/api/v1/ai/books/${bookId}/reader/messages`);
  return response.data;
}

/** Sends a question to the book's reader assistant and returns its reply. */
export async function sendReaderMessage(bookId: string, message: string): Promise<string> {
  const response = await apiClient.post<{ reply: string }>(`/api/v1/ai/books/${bookId}/reader/chat`, { message });
  return response.data.reply;
}
