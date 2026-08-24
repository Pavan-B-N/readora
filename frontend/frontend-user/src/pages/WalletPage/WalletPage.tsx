import { useEffect, useState } from 'react';
import { getWallet } from '@/api/userApi';
import type { WalletResponse } from '@/types/user';
import { Card } from '@/components/Card';
import styles from './WalletPage.module.css';

export function WalletPage() {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);

  useEffect(() => {
    getWallet(0, 20).then(setWallet);
  }, []);

  if (!wallet) {
    return <p>Loading…</p>;
  }

  return (
    <div>
      <h1>Wallet</h1>
      <div className={styles.balance}>
        {wallet.balance} {wallet.currency}
        <div className={styles.balanceLabel}>Current balance</div>
      </div>

      <Card>
        <h2>Transaction history</h2>
        {wallet.items.length === 0 ? (
          <p>No transactions yet.</p>
        ) : (
          wallet.items.map((item) => (
            <div className={styles.ledgerItem} key={item.id}>
              <span>{item.type}</span>
              <span className={Number(item.amount) >= 0 ? styles.credit : styles.debit}>
                {item.amount} {wallet.currency}
              </span>
              <span className={styles.date}>{new Date(item.createdAt).toLocaleDateString()}</span>
            </div>
          ))
        )}
      </Card>
    </div>
  );
}
