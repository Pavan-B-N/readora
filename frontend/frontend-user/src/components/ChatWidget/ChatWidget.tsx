import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import { MessageCircle, X, Send, Sparkles, RotateCcw } from 'lucide-react';
import { streamChat } from '@/api/aiApi';
import { useAppSelector } from '@/redux/hooks';
import { Tooltip } from '@/components/Tooltip';
import { RichText } from './RichText';
import { ROUTES } from '@/constants/routes';
import styles from './ChatWidget.module.css';

interface ChatMessage {
  role: 'user' | 'assistant' | 'error';
  content: string;
}

const SUGGESTIONS = [
  'Recommend a book about focus and productivity',
  'What sci-fi do you have under ₹500?',
  'Compare Clean Code and Clean Architecture',
];

export function ChatWidget() {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [streaming, setStreaming] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, streaming]);

  // Abandon an in-flight stream if the widget unmounts, so it doesn't keep writing to dead state.
  useEffect(() => () => abortRef.current?.abort(), []);

  const send = async (text: string) => {
    const message = text.trim();
    if (!message || streaming) return;

    setDraft('');
    setMessages((m) => [...m, { role: 'user', content: message }, { role: 'assistant', content: '' }]);
    setStreaming(true);

    const controller = new AbortController();
    abortRef.current = controller;

    try {
      await streamChat(
        message,
        null,
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

      // An empty reply means the model produced nothing — surface it rather than leaving a blank bubble.
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
    <div className={styles.panel} role="dialog" aria-label="Book assistant">
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
              <div
                key={index}
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
    </div>
  );
}
