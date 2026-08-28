import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MessageCircle, X, Send, Sparkles, RotateCcw, Maximize2 } from 'lucide-react';
import { listConversations } from '@/api/aiApi';
import { useAppSelector } from '@/redux/hooks';
import { Tooltip } from '@/components/Tooltip';
import { RichText } from './RichText';
import { ChatBookCarousel } from './ChatBookCarousel';
import { useChatSession } from './useChatSession';
import { ROUTES } from '@/constants/routes';
import styles from './ChatWidget.module.css';

const SUGGESTIONS = [
  'Recommend a book about focus and productivity',
  'What sci-fi do you have under ₹500?',
  'Compare Clean Code and Clean Architecture',
];

export function ChatWidget() {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  const { messages, draft, setDraft, streaming, send, reset, loadConversation } = useChatSession(Boolean(accessToken));

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, streaming]);

  // Resume the most recent conversation on load, instead of always starting fresh.
  useEffect(() => {
    if (!accessToken) {
      loadConversation(null);
      return;
    }
    listConversations(1).then(([latest]) => loadConversation(latest?.conversationId ?? null));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send(draft);
    }
  };

  if (location.pathname.startsWith(ROUTES.assistant)) {
    return null;
  }

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
            <Tooltip label="New conversation" placement="bottom">
              <button type="button" className={styles.iconButton} onClick={reset} aria-label="New conversation">
                <RotateCcw size={15} />
              </button>
            </Tooltip>
          )}
          <Tooltip label="Open full page" placement="bottom">
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => {
                setOpen(false);
                navigate(ROUTES.assistant);
              }}
              aria-label="Open the assistant in a full page"
            >
              <Maximize2 size={15} />
            </button>
          </Tooltip>
          <Tooltip label="Close" placement="bottom">
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
                {message.books && <ChatBookCarousel books={message.books} />}
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
