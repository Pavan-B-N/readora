import { useEffect, useRef, useState } from 'react';
import { Send } from 'lucide-react';
import { getReturnMessages, postReturnMessage } from '@/api/orderApi';
import type { ReturnMessage } from '@/types/order';
import { useToast } from '@readora/shared-ui';
import styles from './ReturnChatPanel.module.css';

const POLL_INTERVAL_MS = 3000;

interface ReturnChatPanelProps {
  orderId: string;
  /** true once the return has been approved/rejected — no more messages accepted, polling stops. */
  locked: boolean;
}

/** The small back-and-forth with the customer while a return sits at RETURN_REQUESTED — admin side. */
export function ReturnChatPanel({ orderId, locked }: ReturnChatPanelProps) {
  const { showToast } = useToast();
  const [messages, setMessages] = useState<ReturnMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const reload = () => getReturnMessages(orderId).then(setMessages);

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId]);

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
      await postReturnMessage(orderId, content);
      setDraft('');
      reload();
    } catch {
      showToast('Could not send message', 'error');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className={styles.panel}>
      <div className={styles.messages} ref={listRef}>
        {messages.length === 0 ? (
          <p className={styles.empty}>No messages yet.</p>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={[styles.message, message.senderRole === 'ADMIN' && styles.messageAdmin].filter(Boolean).join(' ')}
            >
              <span className={styles.messageSender}>{message.senderRole === 'ADMIN' ? 'You' : 'Customer'}</span>
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
            placeholder="Reply to the customer…"
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
