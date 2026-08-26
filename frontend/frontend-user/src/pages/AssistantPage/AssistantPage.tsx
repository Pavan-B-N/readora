import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MessageSquarePlus, Send, Sparkles } from 'lucide-react';
import { listConversations, type ConversationSummary } from '@/api/aiApi';
import { RichText } from '@/components/ChatWidget/RichText';
import { ChatBookCarousel } from '@/components/ChatWidget/ChatBookCarousel';
import { ChatBookPicker } from '@/components/ChatWidget/ChatBookPicker';
import { useChatSession } from '@/components/ChatWidget/useChatSession';
import { Spinner } from '@/components/Spinner';
import { ROUTES } from '@/constants/routes';
import styles from './AssistantPage.module.css';

const SUGGESTIONS = [
  'Recommend a book about focus and productivity',
  'What sci-fi do you have under ₹500?',
  'Compare Clean Code and Clean Architecture',
];

function relativeTime(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

export function AssistantPage() {
  const { conversationId: routeConversationId } = useParams<{ conversationId?: string }>();
  const navigate = useNavigate();
  const scrollRef = useRef<HTMLDivElement>(null);

  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [loadingList, setLoadingList] = useState(true);

  const { messages, draft, setDraft, streaming, pickerBook, setPickerBook, conversationId, send, loadConversation } =
    useChatSession(true);

  const refreshList = () => listConversations(50).then(setConversations);

  useEffect(() => {
    refreshList().finally(() => setLoadingList(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // The URL is the source of truth for which conversation is open — switching it (sidebar click,
  // browser back/forward) re-syncs the session to match.
  useEffect(() => {
    loadConversation(routeConversationId ?? null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeConversationId]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, streaming]);

  // Once a brand-new chat gets its first real conversationId back from the server, reflect it in
  // the URL and refresh the sidebar so it shows up there too.
  useEffect(() => {
    if (conversationId && conversationId !== routeConversationId) {
      navigate(ROUTES.assistantConversation(conversationId), { replace: true });
      refreshList();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversationId]);

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send(draft);
    }
  };

  return (
    <div className={styles.page}>
      <aside className={styles.sidebar}>
        <button type="button" className={styles.newChatButton} onClick={() => navigate(ROUTES.assistant)}>
          <MessageSquarePlus size={15} />
          New chat
        </button>

        <div className={styles.historyList}>
          {loadingList ? (
            <Spinner />
          ) : conversations.length === 0 ? (
            <p className={styles.emptyHistory}>Your past conversations will show up here.</p>
          ) : (
            conversations.map((c) => (
              <button
                type="button"
                key={c.conversationId}
                className={[styles.historyItem, c.conversationId === conversationId && styles.historyItemActive]
                  .filter(Boolean)
                  .join(' ')}
                onClick={() => navigate(ROUTES.assistantConversation(c.conversationId))}
              >
                <span className={styles.historyTitle}>{c.title || 'New conversation'}</span>
                <span className={styles.historyMeta}>
                  {c.messageCount} message{c.messageCount === 1 ? '' : 's'} · {relativeTime(c.updatedAt)}
                </span>
              </button>
            ))
          )}
        </div>
      </aside>

      <div className={styles.chatPane}>
        <div className={styles.messages} ref={scrollRef}>
          {messages.length === 0 ? (
            <div className={styles.welcome}>
              <span className={styles.welcomeIcon}>
                <Sparkles size={22} />
              </span>
              <span className={styles.welcomeTitle}>How can I help?</span>
              <span className={styles.welcomeText}>I can search the catalogue by meaning, not just keywords.</span>
              <div className={styles.suggestions}>
                {SUGGESTIONS.map((suggestion) => (
                  <button type="button" key={suggestion} className={styles.suggestion} onClick={() => send(suggestion)}>
                    {suggestion}
                  </button>
                ))}
              </div>
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
      </div>

      <ChatBookPicker book={pickerBook} onClose={() => setPickerBook(null)} />
    </div>
  );
}
