import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ArrowDownLeft, ArrowUpRight, Gift, Plus, Wallet } from 'lucide-react';
import { getWallet, redeemCoupon, topUpWallet } from '@/api/userApi';
import type { WalletResponse } from '@/types/user';
import { useToast } from '@readora/shared-ui';
import { Card, CardHeader } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Input } from '@readora/shared-ui';
import { Modal } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
import styles from './WalletPage.module.css';

const QUICK_AMOUNTS = ['200', '500', '1000'];

function prettyType(type: string) {
  return type
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

export function WalletPage() {
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [topUpOpen, setTopUpOpen] = useState(searchParams.get('topup') === '1');
  const [amount, setAmount] = useState('500');
  const [submitting, setSubmitting] = useState(false);
  const [couponOpen, setCouponOpen] = useState(false);
  const [couponCode, setCouponCode] = useState('');
  const [redeeming, setRedeeming] = useState(false);

  const reload = () => getWallet(0, 20).then(setWallet);

  useEffect(() => {
    reload();
  }, []);

  useEffect(() => {
    if (searchParams.get('topup') === '1') {
      setTopUpOpen(true);
      searchParams.delete('topup');
      setSearchParams(searchParams, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onTopUp = async () => {
    const value = Number(amount);
    if (!amount.trim() || Number.isNaN(value) || value <= 0) {
      showToast('Enter a valid amount', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await topUpWallet(amount.trim());
      showToast(`₹${amount} added to your wallet`);
      setTopUpOpen(false);
      setAmount('500');
      reload();
    } catch {
      showToast('Top-up failed — please try again', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const onRedeemCoupon = async () => {
    if (!couponCode.trim()) {
      showToast('Enter a coupon code', 'error');
      return;
    }
    setRedeeming(true);
    try {
      const result = await redeemCoupon(couponCode.trim());
      showToast(`₹${result.creditedAmount} credited to your wallet`);
      setCouponOpen(false);
      setCouponCode('');
      reload();
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      showToast(message ?? 'Could not redeem that code', 'error');
    } finally {
      setRedeeming(false);
    }
  };

  if (!wallet) {
    return (
      <div>
        <h1>Wallet</h1>
        <Card className={styles.balanceCard}>
          <div className="skeletonPulse" style={{ width: 120, height: 16, marginBottom: 12, borderRadius: 4 }} />
          <div className="skeletonPulse" style={{ width: 200, height: 48, marginBottom: 24, borderRadius: 8 }} />
          <div style={{ display: 'flex', gap: 12 }}>
            <div className="skeletonPulse" style={{ width: 100, height: 36, borderRadius: 'var(--radius-pill)' }} />
            <div className="skeletonPulse" style={{ width: 140, height: 36, borderRadius: 'var(--radius-pill)' }} />
          </div>
        </Card>
        <Card>
          <div className="skeletonPulse" style={{ width: 200, height: 24, marginBottom: 8, borderRadius: 4 }} />
          <div className="skeletonPulse" style={{ width: 300, height: 16, marginBottom: 24, borderRadius: 4 }} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <div className="skeletonPulse" style={{ width: 140, height: 16, marginBottom: 6, borderRadius: 4 }} />
                  <div className="skeletonPulse" style={{ width: 100, height: 12, borderRadius: 4 }} />
                </div>
                <div className="skeletonPulse" style={{ width: 60, height: 20, borderRadius: 4 }} />
              </div>
            ))}
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <h1>Wallet</h1>

      <Card className={styles.balanceCard}>
        <span className={styles.balanceLabel}>Current balance</span>
        <div className={styles.balance}>
          ₹{wallet.balance} <span className={styles.balanceCurrency}>{wallet.currency}</span>
        </div>
        <div className={styles.balanceActions}>
          <Button onClick={() => setTopUpOpen(true)}>
            <Plus size={15} />
            Top up
          </Button>
          <Button variant="secondary" onClick={() => setCouponOpen(true)}>
            <Gift size={15} />
            Redeem coupon
          </Button>
        </div>
      </Card>

      <Card>
        <CardHeader title="Transaction history" subtitle="Every change to your balance, newest first." />

        {wallet.items.length === 0 ? (
          <EmptyState
            icon={Wallet}
            title="No transactions yet"
            description="Signup bonuses, refunds, top-ups, and wallet payments will appear here."
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

      <Modal open={topUpOpen} onClose={() => setTopUpOpen(false)} title="Top up your wallet" width={380}>
        <div className={styles.topUpForm}>
          <div className={styles.quickAmounts}>
            {QUICK_AMOUNTS.map((value) => (
              <button
                type="button"
                key={value}
                className={[styles.quickAmount, amount === value && styles.quickAmountActive].filter(Boolean).join(' ')}
                onClick={() => setAmount(value)}
              >
                ₹{value}
              </button>
            ))}
          </div>
          <Input
            label="Amount"
            hint="₹1 – ₹50,000"
            value={amount}
            onChange={(e) => setAmount(e.target.value.replace(/[^0-9.]/g, ''))}
          />
          <Button onClick={onTopUp} disabled={submitting} block>
            {submitting ? 'Adding…' : `Add ₹${amount || '0'} to wallet`}
          </Button>
          <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)', textAlign: 'center' }}>
            Demo top-up — no real payment is taken.
          </p>
        </div>
      </Modal>

      <Modal open={couponOpen} onClose={() => setCouponOpen(false)} title="Redeem a coupon" width={380}>
        <div className={styles.topUpForm}>
          <Input
            label="Coupon code"
            placeholder="e.g. WELCOME50"
            value={couponCode}
            onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
          />
          <Button onClick={onRedeemCoupon} disabled={redeeming} block>
            {redeeming ? 'Redeeming…' : 'Redeem'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
