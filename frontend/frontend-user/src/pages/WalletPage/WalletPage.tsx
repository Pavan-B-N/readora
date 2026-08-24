import { useEffect, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, Wallet } from 'lucide-react';
import { getWallet } from '@/api/userApi';
import type { WalletResponse } from '@/types/user';
import { Card, CardHeader } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import styles from './WalletPage.module.css';

function prettyType(type: string) {
  return type
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

export function WalletPage() {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);

  useEffect(() => {
    getWallet(0, 20).then(setWallet);
  }, []);

  if (!wallet) return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;

  return (
    <div>
      <h1>Wallet</h1>

      <Card className={styles.balanceCard}>
        <span className={styles.balanceLabel}>Current balance</span>
        <div className={styles.balance}>
          ₹{wallet.balance} <span className={styles.balanceCurrency}>{wallet.currency}</span>
        </div>
      </Card>

      <Card>
        <CardHeader title="Transaction history" subtitle="Every change to your balance, newest first." />

        {wallet.items.length === 0 ? (
          <EmptyState
            icon={Wallet}
            title="No transactions yet"
            description="Signup bonuses, refunds, and wallet payments will appear here."
          />
        ) : (
          wallet.items.map((item) => {
            const isCredit = Number(item.amount) >= 0;
            return (
              <div className={styles.item} key={item.id}>
                <span className={[styles.itemIcon, isCredit ? styles.creditIcon : styles.debitIcon].join(' ')}>
                  {isCredit ? <ArrowDownLeft size={15} /> : <ArrowUpRight size={15} />}
                </span>
                <span className={styles.itemInfo}>
                  <div className={styles.itemType}>{prettyType(item.type)}</div>
                  <div className={styles.itemDate}>{new Date(item.createdAt).toLocaleString()}</div>
                </span>
                <span className={[styles.amount, isCredit ? styles.credit : styles.debit].join(' ')}>
                  {isCredit ? '+' : ''}
                  ₹{item.amount}
                </span>
                <span className={styles.balanceAfter}>bal ₹{item.balanceAfter}</span>
              </div>
            );
          })
        )}
      </Card>
    </div>
  );
}
