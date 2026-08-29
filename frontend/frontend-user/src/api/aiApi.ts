import { apiClient } from './client';
import { store } from '@/redux/store';

export interface AiSearchItem {
  bookId: string;
  title: string;
  score: number;
}

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

export interface ConversationSummary {
  conversationId: string;
  title: string | null;
  messageCount: number;
  updatedAt: string;
}

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

export async function getConversationMessages(conversationId: string): Promise<StoredMessage[]> {
  const response = await apiClient.get<StoredMessage[]>(`/api/v1/ai/conversations/${conversationId}/messages`);
  return response.data;
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/** The final SSE frame of a reply carries this prefix instead of reply text — see ChatService.BOOK_IDS_FRAME_PREFIX. */
const BOOK_IDS_FRAME_PREFIX = '@@RDX_BOOK_IDS@@:';

export interface StreamChatResult {
  conversationId: string | null;
  bookIds: string[];
}

/**
 * Streams an assistant reply token-by-token.
 *
 * Uses fetch rather than EventSource because the endpoint is a POST with a JSON body and needs
 * an Authorization header — EventSource supports neither.
 *
 * @param onToken called for each chunk of text as it arrives
 * @param signal  lets the caller abort a stream in flight (e.g. the user closes the widget)
 * @returns the conversation id (the one passed in, or a newly created one when it was null) and
 *          the book ids the backend found relevant to this reply
 */
export async function streamChat(
  message: string,
  conversationId: string | null,
  onToken: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<StreamChatResult> {
  const { accessToken } = store.getState().auth;
  // The book-search tools filter every candidate to what's actually purchasable at this store
  // (see ChatClientConfig's system prompt) — without it, the assistant can only recommend from
  // the whole catalogue, including books with no stock here and no virtual edition.
  const { selectedId: storeId } = store.getState().store;

  const response = await fetch(`${BASE_URL}/api/v1/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'X-Correlation-Id': crypto.randomUUID(),
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

    // SSE frames are separated by a blank line. A single frame carries one "message", but when
    // that message's payload contains its own newlines, the spec requires the sender to split it
    // across multiple "data:" lines within the same frame — the receiver is expected to rejoin
    // them with "\n" to recover the original text. A chunk like "\n\n- Core Java Syntax" (a very
    // normal thing for a model to emit while writing a markdown list) arrives as three separate
    // "data:" lines; calling onToken() once per line rather than once per frame would silently
    // drop those newlines; instead of concatenating chunk text directly, so bullet points and
    // paragraph breaks would vanish from the live-streamed text (while a reload rendered fine —
    // the persisted message is the complete string with newlines intact, never round-tripped
    // through this per-line reconstruction).
    const frames = buffer.split('\n\n');
    buffer = frames.pop() ?? '';

    for (const frame of frames) {
      const dataLines = frame.split('\n').filter((line) => line.startsWith('data:'));
      if (dataLines.length === 0) continue;

      // Do NOT strip a leading space here: Spring writes the payload with no separator space, so
      // a token that is itself a space arrives as "data: " — stripping would silently delete
      // every space in the reply.
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

// ---------------------------------------------------------------------------
// Reader assistant — a focused RAG Q&A over one purchased book's own content,
// entirely separate from the shopping assistant above (no tools, no book
// recommendations, no cross-book knowledge).
// ---------------------------------------------------------------------------

export type ReaderIndexStatus = 'PENDING' | 'READY' | 'FAILED' | null;

export interface ReaderMessage {
  role: 'USER' | 'ASSISTANT';
  content: string;
}

export async function getReaderStatus(bookId: string): Promise<ReaderIndexStatus> {
  const response = await apiClient.get<{ status: ReaderIndexStatus }>(`/api/v1/ai/books/${bookId}/reader/status`);
  return response.data.status;
}

/** Embeds the book's content for the assistant — one-time, shared by every owner. */
export async function initializeReader(bookId: string): Promise<ReaderIndexStatus> {
  const response = await apiClient.post<{ status: ReaderIndexStatus }>(`/api/v1/ai/books/${bookId}/reader/initialize`);
  return response.data.status;
}

export async function getReaderHistory(bookId: string): Promise<ReaderMessage[]> {
  const response = await apiClient.get<ReaderMessage[]>(`/api/v1/ai/books/${bookId}/reader/messages`);
  return response.data;
}

export async function sendReaderMessage(bookId: string, message: string): Promise<string> {
  const response = await apiClient.post<{ reply: string }>(`/api/v1/ai/books/${bookId}/reader/chat`, { message });
  return response.data.reply;
}
