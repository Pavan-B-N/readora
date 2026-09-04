import { useEffect, useRef, useState } from 'react';
import { Send } from 'lucide-react';
import { useToast } from '../Toast';
import styles from './ReturnChatPanel.module.css';

const POLL_INTERVAL_MS = 3000;

export interface ReturnMessage {
  id: string;
  senderUserId: string;
  senderRole: 'CUSTOMER' | 'ADMIN';
  content: string;
  createdAt: string;
}

interface ReturnChatPanelProps {
  orderId: string;
  /** true once the return has been approved/rejected — no more messages accepted, polling stops. */
  locked: boolean;
  /** Which side of the conversation is viewing — drives labels and the composer placeholder. */
  viewerRole: 'CUSTOMER' | 'ADMIN';
  fetchMessages: (orderId: string) => Promise<ReturnMessage[]>;
  sendMessage: (orderId: string, content: string) => Promise<ReturnMessage>;
}

/** The small back-and-forth between a customer and admin while a return sits at RETURN_REQUESTED. */
export function ReturnChatPanel({ orderId, locked, viewerRole, fetchMessages, sendMessage }: ReturnChatPanelProps) {
  const { showToast } = useToast();
  const [messages, setMessages] = useState<ReturnMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const reload = () => fetchMessages(orderId).then(setMessages);

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId]);

  // Only worth polling while the conversation is still active — once decided, nothing new will arrive.
  useEffect(() => {
    if (locked) return;
    const interval = window.setInterval(reload, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId, locked]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight });
  }, [messages]);

  const onSend = async () => {
    const content = draft.trim();
    if (!content) return;
    setSending(true);
    try {
      await sendMessage(orderId, content);
      setDraft('');
      reload();
    } catch {
      showToast('Could not send message', 'error');
    } finally {
      setSending(false);
    }
  };

  const senderLabel = (senderRole: ReturnMessage['senderRole']) => {
    if (senderRole === viewerRole) return 'You';
    return viewerRole === 'ADMIN' ? 'Customer' : 'Support';
  };

  return (
    <div className={styles.panel}>
      <div className={styles.messages} ref={listRef}>
        {messages.length === 0 ? (
          <p className={styles.empty}>
            {viewerRole === 'CUSTOMER'
              ? 'No messages yet — describe the issue below and an admin will respond here.'
              : 'No messages yet.'}
          </p>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={[styles.message, message.senderRole === 'ADMIN' && styles.messageAdmin].filter(Boolean).join(' ')}
            >
              <span className={styles.messageSender}>{senderLabel(message.senderRole)}</span>
              <p className={styles.messageContent}>{message.content}</p>
              <span className={styles.messageTime}>{new Date(message.createdAt).toLocaleString()}</span>
            </div>
          ))
        )}
      </div>

      {locked ? (
        <p className={styles.lockedNote}>This conversation is closed — the return has already been decided.</p>
      ) : (
        <div className={styles.composer}>
          <textarea
            className={styles.composerInput}
            rows={2}
            placeholder={viewerRole === 'CUSTOMER' ? 'Describe the issue…' : 'Reply to the customer…'}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                onSend();
              }
            }}
          />
          <button
            type="button"
            className={styles.sendButton}
            onClick={onSend}
            disabled={sending || !draft.trim()}
            aria-label="Send message"
          >
            <Send size={15} />
          </button>
        </div>
      )}
    </div>
  );
}
