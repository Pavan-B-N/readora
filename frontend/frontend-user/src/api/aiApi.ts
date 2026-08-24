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

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * Streams an assistant reply token-by-token.
 *
 * Uses fetch rather than EventSource because the endpoint is a POST with a JSON body and needs
 * an Authorization header — EventSource supports neither.
 *
 * @param onToken called for each chunk of text as it arrives
 * @param signal  lets the caller abort a stream in flight (e.g. the user closes the widget)
 */
export async function streamChat(
  message: string,
  conversationId: string | null,
  onToken: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const { accessToken } = store.getState().auth;

  const response = await fetch(`${BASE_URL}/api/v1/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      'X-Correlation-Id': crypto.randomUUID(),
    },
    body: JSON.stringify({ message, conversationId }),
    signal,
  });

  if (!response.ok || !response.body) {
    throw new Error(`Chat failed with status ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // SSE frames are separated by a blank line; each "data:" line carries one chunk.
    const frames = buffer.split('\n\n');
    buffer = frames.pop() ?? '';

    for (const frame of frames) {
      for (const line of frame.split('\n')) {
        if (line.startsWith('data:')) {
          // Take everything after "data:" verbatim. Do NOT strip a leading space here: Spring
          // writes the payload with no separator space, so a token that is itself a space
          // arrives as "data: " — stripping would silently delete every space in the reply.
          onToken(line.slice(5));
        }
      }
    }
  }
}
