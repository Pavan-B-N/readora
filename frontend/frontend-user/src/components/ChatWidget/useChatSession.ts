import { useEffect, useRef, useState } from 'react';
import { getConversationMessages, semanticSearch, streamChat } from '@/api/aiApi';
import { getBookDetail } from '@/api/catalogApi';
import { useAppSelector } from '@/redux/hooks';
import type { BookDetail } from '@/types/catalog';

export interface ChatMessage {
  role: 'user' | 'assistant' | 'error';
  content: string;
  books?: BookDetail[];
}

// First tunable knob to revisit once there's real usage data: raise it if the carousel shows up
// for clearly unrelated chit-chat, lower it if it's missing for genuine book questions.
const BOOK_RELEVANCE_THRESHOLD = 0.5;
const MAX_CAROUSEL_BOOKS = 5;

/**
 * The send/stream/history logic shared by the chat widget and the full-page assistant — both are
 * just different shells around the same conversation session.
 */
export function useChatSession(enabled: boolean) {
  const storeId = useAppSelector((state) => state.store.selectedId);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [pickerBook, setPickerBook] = useState<BookDetail | null>(null);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const conversationIdRef = useRef<string | null>(null);

  // Abandon an in-flight stream on unmount, so it doesn't keep writing to dead state.
  useEffect(() => () => abortRef.current?.abort(), []);

  const loadConversation = async (id: string | null) => {
    // Guards against the caller (AssistantPage's URL-sync effect) re-firing for a conversation
    // this session already has loaded — e.g. right after send() mints a new id and the URL
    // catches up to it. conversationIdRef is authoritative and updates synchronously (unlike the
    // conversationId *state*, which the caller's effect can still read as stale for a render or
    // two), so checking against it here is race-proof regardless of how the effect above behaves.
    // Without this, a redundant reload replaces the freshly-streamed messages — complete with any
    // attached book carousels — with server history, which never carries carousel data at all.
    if (id === conversationIdRef.current) return;

    abortRef.current?.abort();
    conversationIdRef.current = id;
    setConversationId(id);
    setStreaming(false);
    if (!id) {
      setMessages([]);
      return;
    }
    const history = await getConversationMessages(id);
    setMessages(history.map((m) => ({ role: m.role === 'USER' ? 'user' : 'assistant', content: m.content })));
  };

  const attachBookResults = async (assistantIndex: number, forQuery: string) => {
    try {
      const { items } = await semanticSearch(forQuery, MAX_CAROUSEL_BOOKS);
      const relevant = items.filter((i) => i.score >= BOOK_RELEVANCE_THRESHOLD).slice(0, MAX_CAROUSEL_BOOKS);
      if (relevant.length === 0) return;

      const books = await Promise.all(
        relevant.map((i) => getBookDetail(i.bookId, storeId ?? undefined).catch(() => null)),
      );
      const found = books.filter((b): b is BookDetail => b !== null);
      if (found.length === 0) return;

      setMessages((m) => {
        const next = [...m];
        if (next[assistantIndex]) next[assistantIndex] = { ...next[assistantIndex], books: found };
        return next;
      });
    } catch {
      // Best-effort — a failed lookup just means no carousel for this turn, not a chat error.
    }
  };

  const send = async (text: string) => {
    if (!enabled) return;
    const message = text.trim();
    if (!message || streaming) return;

    // The assistant reply's eventual index — known up front since it's always the 2nd of the two
    // messages pushed below. Computed here rather than via a setMessages-updater trick, because
    // that call site needs to stay pure: React 18 StrictMode invokes updaters twice to check for
    // exactly that, and a call with real side effects (attachBookResults' network requests) inside
    // one fires twice in dev as a result.
    const assistantIndex = messages.length + 1;

    setDraft('');
    setMessages((m) => [...m, { role: 'user', content: message }, { role: 'assistant', content: '' }]);
    setStreaming(true);

    const controller = new AbortController();
    abortRef.current = controller;
    // Tracked independently of React state — a `let` mutated inside a setState updater isn't
    // reliably readable right after the call, since the updater's execution isn't guaranteed to
    // run synchronously with the dispatch (depends on whether other updates are already queued).
    let accumulated = '';

    try {
      const resolvedConversationId = await streamChat(
        message,
        conversationIdRef.current,
        (chunk) => {
          accumulated += chunk;
          setMessages((m) => {
            const next = [...m];
            const last = next[next.length - 1];
            if (last?.role === 'assistant') {
              next[next.length - 1] = { ...last, content: last.content + chunk };
            }
            return next;
          });
        },
        controller.signal,
      );
      conversationIdRef.current = resolvedConversationId;
      setConversationId(resolvedConversationId);

      // An empty reply means the model produced nothing — surface it rather than leaving a blank bubble.
      if (!accumulated.trim()) {
        setMessages((m) => {
          const next = [...m];
          const last = next[next.length - 1];
          if (last?.role === 'assistant' && !last.content.trim()) {
            next[next.length - 1] = {
              role: 'error',
              content: "The assistant didn't return a reply. It may not be configured yet.",
            };
          }
          return next;
        });
      } else {
        attachBookResults(assistantIndex, message);
      }
    } catch (error) {
      if ((error as Error).name === 'AbortError') return;
      setMessages((m) => {
        const next = [...m];
        if (next[next.length - 1]?.role === 'assistant' && !next[next.length - 1].content) next.pop();
        return [
          ...next,
          { role: 'error', content: "Couldn't reach the assistant. Please try again in a moment." },
        ];
      });
    } finally {
      setStreaming(false);
      abortRef.current = null;
    }
  };

  const reset = () => {
    abortRef.current?.abort();
    conversationIdRef.current = null;
    setConversationId(null);
    setMessages([]);
    setStreaming(false);
  };

  return {
    messages,
    draft,
    setDraft,
    streaming,
    pickerBook,
    setPickerBook,
    conversationId,
    send,
    reset,
    loadConversation,
  };
}
