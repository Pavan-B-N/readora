import { useEffect, useRef, useState } from 'react';
import { getConversationMessages, semanticSearch, streamChat } from '@/api/aiApi';
import { getBookDetail } from '@/api/catalogApi';
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

      const books = await Promise.all(relevant.map((i) => getBookDetail(i.bookId).catch(() => null)));
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

    setDraft('');
    setMessages((m) => [...m, { role: 'user', content: message }, { role: 'assistant', content: '' }]);
    setStreaming(true);

    const controller = new AbortController();
    abortRef.current = controller;

    try {
      const resolvedConversationId = await streamChat(
        message,
        conversationIdRef.current,
        (chunk) => {
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
      let hadContent = false;
      setMessages((m) => {
        const next = [...m];
        const last = next[next.length - 1];
        if (last?.role === 'assistant' && !last.content.trim()) {
          next[next.length - 1] = {
            role: 'error',
            content: "The assistant didn't return a reply. It may not be configured yet.",
          };
        } else {
          hadContent = true;
        }
        return next;
      });

      if (hadContent) {
        setMessages((current) => {
          attachBookResults(current.length - 1, message);
          return current;
        });
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
