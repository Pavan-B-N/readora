import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import { MessageCircle, X, Send, Sparkles, RotateCcw, Maximize2, Minimize2 } from 'lucide-react';
import { getConversationMessages, listConversations, semanticSearch, streamChat } from '@/api/aiApi';
import { getBookDetail } from '@/api/catalogApi';
import type { BookDetail } from '@/types/catalog';
import { useAppSelector } from '@/redux/hooks';
import { Tooltip } from '@/components/Tooltip';
import { RichText } from './RichText';
import { ChatBookCarousel } from './ChatBookCarousel';
import { ChatBookPicker } from './ChatBookPicker';
import { ROUTES } from '@/constants/routes';
import styles from './ChatWidget.module.css';

interface ChatMessage {
  role: 'user' | 'assistant' | 'error';
  content: string;
  books?: BookDetail[];
}

// First tunable knob to revisit once there's real usage data: raise it if the carousel shows up
// for clearly unrelated chit-chat, lower it if it's missing for genuine book questions.
const BOOK_RELEVANCE_THRESHOLD = 0.5;
const MAX_CAROUSEL_BOOKS = 5;

const SUGGESTIONS = [
  'Recommend a book about focus and productivity',
  'What sci-fi do you have under ₹500?',
  'Compare Clean Code and Clean Architecture',
];

export function ChatWidget() {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [open, setOpen] = useState(false);
  const [fullScreen, setFullScreen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [pickerBook, setPickerBook] = useState<BookDetail | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const conversationIdRef = useRef<string | null>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, streaming]);

  // Abandon an in-flight stream if the widget unmounts, so it doesn't keep writing to dead state.
  useEffect(() => () => abortRef.current?.abort(), []);

  // Resume the most recent conversation on load, instead of always starting fresh.
  useEffect(() => {
    if (!accessToken) {
      conversationIdRef.current = null;
      setMessages([]);
      return;
    }

    listConversations(1).then(([latest]) => {
      if (!latest) return;
      conversationIdRef.current = latest.conversationId;
      getConversationMessages(latest.conversationId).then((history) => {
        setMessages(history.map((m) => ({ role: m.role === 'USER' ? 'user' : 'assistant', content: m.content })));
      });
    });
  }, [accessToken]);

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
        const assistantIndex = messages.length + 1;
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

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send(draft);
    }
  };

  const reset = () => {
    abortRef.current?.abort();
    conversationIdRef.current = null;
    setMessages([]);
    setStreaming(false);
  };

  if (!open) {
    return (
      <Tooltip label="Ask the book assistant" placement="left">
        <button
          type="button"
          className={styles.launcher}
          onClick={() => setOpen(true)}
          aria-label="Open the book assistant"
        >
          <MessageCircle size={22} />
        </button>
      </Tooltip>
    );
  }

  return (
    <div className={[styles.panel, fullScreen && styles.panelFullScreen].filter(Boolean).join(' ')} role="dialog" aria-label="Book assistant">
      <div className={styles.header}>
        <span className={styles.headerIcon}>
          <Sparkles size={16} />
        </span>
        <span className={styles.headerText}>
          <div className={styles.headerTitle}>Book assistant</div>
          <div className={styles.headerStatus}>
            {streaming ? 'Thinking…' : 'Ask about anything in the catalogue'}
          </div>
        </span>
        <span className={styles.headerActions}>
          {messages.length > 0 && (
            <Tooltip label="New conversation">
              <button type="button" className={styles.iconButton} onClick={reset} aria-label="New conversation">
                <RotateCcw size={15} />
              </button>
            </Tooltip>
          )}
          <Tooltip label={fullScreen ? 'Exit full screen' : 'Full screen'}>
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => setFullScreen((f) => !f)}
              aria-label={fullScreen ? 'Exit full screen' : 'Open full screen'}
            >
              {fullScreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            </button>
          </Tooltip>
          <Tooltip label="Close">
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => setOpen(false)}
              aria-label="Close the assistant"
            >
              <X size={16} />
            </button>
          </Tooltip>
        </span>
      </div>

      <div className={styles.messages} ref={scrollRef}>
        {messages.length === 0 ? (
          <div className={styles.welcome}>
            <span className={styles.welcomeIcon}>
              <Sparkles size={20} />
            </span>
            <span className={styles.welcomeTitle}>How can I help?</span>
            <span className={styles.welcomeText}>
              I can search the catalogue by meaning, not just keywords.
            </span>
            {accessToken && (
              <div className={styles.suggestions}>
                {SUGGESTIONS.map((suggestion) => (
                  <button
                    type="button"
                    key={suggestion}
                    className={styles.suggestion}
                    onClick={() => send(suggestion)}
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            )}
          </div>
        ) : (
          messages.map((message, index) => {
            const isEmptyStreaming = message.role === 'assistant' && !message.content && streaming;
            return (
              <div key={index} className={[styles.turn, message.role === 'user' && styles.turnUser].filter(Boolean).join(' ')}>
                <div
                  className={[
                    styles.bubble,
                    message.role === 'user' && styles.user,
                    message.role === 'assistant' && styles.assistant,
                    message.role === 'error' && styles.errorBubble,
                  ]
                    .filter(Boolean)
                    .join(' ')}
                >
                  {isEmptyStreaming ? (
                    <span className={styles.typing}>
                      <span className={styles.dot} />
                      <span className={styles.dot} />
                      <span className={styles.dot} />
                    </span>
                  ) : message.role === 'assistant' ? (
                    <RichText text={message.content} />
                  ) : (
                    message.content
                  )}
                </div>
                {message.books && <ChatBookCarousel books={message.books} onSelect={setPickerBook} />}
              </div>
            );
          })
        )}
      </div>

      {accessToken ? (
        <div className={styles.composer}>
          <textarea
            className={styles.composerInput}
            rows={1}
            placeholder="Ask about a book…"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onKeyDown}
            disabled={streaming}
          />
          <button
            type="button"
            className={styles.sendButton}
            onClick={() => send(draft)}
            disabled={!draft.trim() || streaming}
            aria-label="Send message"
          >
            <Send size={15} />
          </button>
        </div>
      ) : (
        <div className={styles.signInPrompt}>
          <Link to={ROUTES.login}>Sign in</Link> to chat with the assistant.
        </div>
      )}

      <ChatBookPicker book={pickerBook} onClose={() => setPickerBook(null)} />
    </div>
  );
}
