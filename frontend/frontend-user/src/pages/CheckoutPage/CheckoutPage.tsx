import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Check, Lock, Plus, QrCode, Wallet as WalletIcon } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchCart, cartCleared } from '@/redux/slices/cartSlice';
import { checkout } from '@/api/orderApi';
import { addAddress, getMe, listAddresses } from '@/api/userApi';
import { listStores } from '@/api/catalogApi';
import type { Address, AddressRecipientType, MeResponse } from '@/types/user';
import type { Store } from '@/types/catalog';
import type { PaymentMethod } from '@/types/order';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './CheckoutPage.module.css';

const FREE_SHIPPING_THRESHOLD = 499;
const FLAT_SHIPPING_FEE = 40;
const PACKAGING_FEE = 15;
const TAX_RATE = 0.09;

interface NewAddressForm {
  label: 'HOME' | 'WORK' | 'OTHER';
  recipientType: AddressRecipientType;
  recipientName: string;
  recipientPhone: string;
  line1: string;
  line2: string;
}

export function CheckoutPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { items, subtotal, currency, requiresShippingAddress } = useAppSelector((state) => state.cart);

  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const [addingAddress, setAddingAddress] = useState(false);
  const [store, setStore] = useState<Store | null>(null);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [newAddress, setNewAddress] = useState<NewAddressForm | null>(null);

  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('WALLET');
  const [upiId, setUpiId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    dispatch(fetchCart());
    getMe().then(setMe);
    listStores().then((stores) => setStore(stores[0] ?? null));
    listAddresses().then((list) => {
      setAddresses(list);
      const preferred = list.find((a) => a.isDefault) ?? list[0];
      if (preferred) setSelectedAddressId(preferred.id);
    });
  }, [dispatch]);

  const pricing = useMemo(() => {
    const sub = Number(subtotal);
    const shippingFee = !requiresShippingAddress ? 0 : sub >= FREE_SHIPPING_THRESHOLD ? 0 : FLAT_SHIPPING_FEE;
    const packagingFee = requiresShippingAddress ? PACKAGING_FEE : 0;
    const taxAmount = Math.round(sub * TAX_RATE * 100) / 100;
    const grandTotal = sub + shippingFee + packagingFee + taxAmount;
    return { subtotal: sub, shippingFee, packagingFee, taxAmount, grandTotal };
  }, [subtotal, requiresShippingAddress]);

  const walletBalance = me ? Number(me.wallet.balance) : 0;
  const walletShort = pricing.grandTotal - walletBalance;
  const walletSufficient = walletShort <= 0;

  const openNewAddressForm = () => {
    setNewAddress({
      label: 'HOME',
      recipientType: 'OWNER',
      recipientName: me?.displayName ?? '',
      recipientPhone: me?.phone ?? '',
      line1: '',
      line2: '',
    });
    setSelectedAddressId(null);
  };

  const onNewAddressRecipientType = (recipientType: AddressRecipientType) => {
    setNewAddress((f) =>
      f
        ? {
            ...f,
            recipientType,
            recipientName: recipientType === 'OWNER' ? (me?.displayName ?? '') : '',
            recipientPhone: recipientType === 'OWNER' ? (me?.phone ?? '') : '',
          }
        : f,
    );
  };

  const resolveShippingAddress = async (): Promise<Address | null> => {
    if (!requiresShippingAddress) return null;

    if (newAddress) {
      if (!newAddress.recipientName.trim() || !newAddress.recipientPhone.trim() || !newAddress.line1.trim()) {
        showToast('Fill in the recipient and address details', 'error');
        return null;
      }
      if (!store) {
        showToast('No store available yet — try again shortly', 'error');
        return null;
      }
      setAddingAddress(true);
      try {
        const saved = await addAddress({
          label: newAddress.label,
          recipientType: newAddress.recipientType,
          recipientName: newAddress.recipientName.trim(),
          recipientPhone: newAddress.recipientPhone.trim(),
          line1: newAddress.line1.trim(),
          line2: newAddress.line2.trim() || undefined,
          city: store.city,
          state: store.state,
          postalCode: store.postalCode,
          countryCode: store.countryCode,
          storeId: store.id,
          isDefault: addresses.length === 0,
        });
        return {
          id: saved.id,
          label: newAddress.label,
          recipientType: newAddress.recipientType,
          recipientName: newAddress.recipientName.trim(),
          recipientPhone: newAddress.recipientPhone.trim(),
          line1: newAddress.line1.trim(),
          line2: newAddress.line2.trim() || null,
          city: store.city,
          state: store.state,
          postalCode: store.postalCode,
          countryCode: store.countryCode,
          storeId: store.id,
          isDefault: saved.isDefault,
        };
      } finally {
        setAddingAddress(false);
      }
    }

    const selected = addresses.find((a) => a.id === selectedAddressId);
    if (!selected) {
      showToast('Select a delivery address', 'error');
      return null;
    }
    return selected;
  };

  const onSubmit = async () => {
    if (paymentMethod === 'UPI' && !upiId.trim()) {
      showToast('Enter your UPI ID', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const address = await resolveShippingAddress();
      if (requiresShippingAddress && !address) {
        setSubmitting(false);
        return;
      }

      const response = await checkout({
        shippingAddress: address
          ? {
              recipientName: address.recipientName,
              line1: address.line1,
              line2: address.line2 ?? undefined,
              city: address.city,
              state: address.state,
              postalCode: address.postalCode,
              countryCode: address.countryCode,
              phone: address.recipientPhone ?? undefined,
            }
          : null,
        paymentMethod,
        upiId: paymentMethod === 'UPI' ? upiId.trim() : undefined,
        items: items.map((item) => ({ bookId: item.bookId, qty: item.qty, deliveryType: item.deliveryType })),
      });
      dispatch(cartCleared());
      showToast(paymentMethod === 'UPI' ? 'Order placed — confirming your UPI payment…' : 'Order placed');
      navigate(ROUTES.orderDetail(response.orderId));
    } catch (error: unknown) {
      const errorCode = (error as { response?: { data?: { errorCode?: string; message?: string } } })?.response?.data
        ?.errorCode;
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;

      if (errorCode === 'INSUFFICIENT_WALLET_BALANCE') {
        showToast(message ?? 'Insufficient wallet balance', 'error');
      } else {
        showToast(message ?? 'Checkout failed', 'error');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (items.length === 0) {
    return <p style={{ color: 'var(--color-text-muted)' }}>Your cart is empty.</p>;
  }

  const canSubmit = paymentMethod === 'UPI' || walletSufficient;

  return (
    <div>
      <h1>Checkout</h1>
      <div className={styles.layout}>
        <div className={styles.form}>
          {requiresShippingAddress && (
            <Card>
              <CardHeader
                title="Shipping address"
                subtitle={store ? `Delivered from ${store.name}, ${store.city}` : undefined}
                actions={
                  !newAddress && (
                    <Button variant="secondary" size="sm" onClick={openNewAddressForm}>
                      <Plus size={14} />
                      New address
                    </Button>
                  )
                }
              />

              {newAddress ? (
                <div className={styles.form}>
                  <div className={styles.recipientTypeRow}>
                    <button
                      type="button"
                      className={[styles.recipientTypeButton, newAddress.recipientType === 'OWNER' && styles.recipientTypeActive]
                        .filter(Boolean)
                        .join(' ')}
                      onClick={() => onNewAddressRecipientType('OWNER')}
                    >
                      {newAddress.recipientType === 'OWNER' && <Check size={13} />}
                      For me
                    </button>
                    <button
                      type="button"
                      className={[styles.recipientTypeButton, newAddress.recipientType === 'GUEST' && styles.recipientTypeActive]
                        .filter(Boolean)
                        .join(' ')}
                      onClick={() => onNewAddressRecipientType('GUEST')}
                    >
                      {newAddress.recipientType === 'GUEST' && <Check size={13} />}
                      For someone else
                    </button>
                  </div>
                  <div className={styles.row2}>
                    <Input
                      label="Recipient name"
                      required
                      disabled={newAddress.recipientType === 'OWNER'}
                      value={newAddress.recipientName}
                      onChange={(e) => setNewAddress((f) => (f ? { ...f, recipientName: e.target.value } : f))}
                    />
                    <Input
                      label="Recipient phone"
                      required
                      disabled={newAddress.recipientType === 'OWNER'}
                      value={newAddress.recipientPhone}
                      onChange={(e) => setNewAddress((f) => (f ? { ...f, recipientPhone: e.target.value } : f))}
                    />
                  </div>
                  <Input
                    label="Street / door no."
                    required
                    placeholder="e.g. Flat 4B, 221 Residency Road"
                    value={newAddress.line1}
                    onChange={(e) => setNewAddress((f) => (f ? { ...f, line1: e.target.value } : f))}
                  />
                  <Input
                    label="Landmark"
                    placeholder="Optional"
                    value={newAddress.line2}
                    onChange={(e) => setNewAddress((f) => (f ? { ...f, line2: e.target.value } : f))}
                  />
                  <Button variant="ghost" size="sm" onClick={() => setNewAddress(null)}>
                    Use a saved address instead
                  </Button>
                </div>
              ) : addresses.length === 0 ? (
                <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
                  No saved addresses yet — add one above.
                </p>
              ) : (
                <div className={styles.addressList}>
                  {addresses.map((address) => (
                    <label
                      key={address.id}
                      className={[styles.addressOption, selectedAddressId === address.id && styles.addressOptionActive]
                        .filter(Boolean)
                        .join(' ')}
                    >
                      <input
                        type="radio"
                        name="shippingAddress"
                        checked={selectedAddressId === address.id}
                        onChange={() => setSelectedAddressId(address.id)}
                      />
                      <span className={styles.addressOptionText}>
                        <span className={styles.addressOptionLabel}>
                          {address.label.charAt(0) + address.label.slice(1).toLowerCase()} · {address.recipientName}
                          {address.isDefault && <span className={styles.defaultTag}>Default</span>}
                        </span>
                        <span className={styles.addressOptionDetail}>
                          {address.line1}
                          {address.line2 ? `, ${address.line2}` : ''}, {address.city}
                        </span>
                      </span>
                    </label>
                  ))}
                </div>
              )}
            </Card>
          )}

          <Card>
            <CardHeader title="Payment method" />
            <div className={styles.paymentTabs}>
              <button
                type="button"
                className={[styles.paymentTab, paymentMethod === 'WALLET' && styles.paymentTabActive].filter(Boolean).join(' ')}
                onClick={() => setPaymentMethod('WALLET')}
              >
                <WalletIcon size={16} />
                Wallet
              </button>
              <button
                type="button"
                className={[styles.paymentTab, paymentMethod === 'UPI' && styles.paymentTabActive].filter(Boolean).join(' ')}
                onClick={() => setPaymentMethod('UPI')}
              >
                <QrCode size={16} />
                UPI
              </button>
            </div>

            {paymentMethod === 'WALLET' && me && (
              <div className={[styles.walletStatus, !walletSufficient && styles.walletStatusShort].join(' ')}>
                <span>Wallet balance</span>
                <span className={styles.walletBalanceValue}>
                  ₹{me.wallet.balance}
                  {!walletSufficient && (
                    <span className={styles.walletShort}>
                      <AlertTriangle size={12} />
                      Short by ₹{walletShort.toFixed(2)}
                    </span>
                  )}
                </span>
                {!walletSufficient && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => navigate(`${ROUTES.wallet}?topup=1`)}
                    style={{ gridColumn: '1 / -1' }}
                  >
                    Top up wallet
                  </Button>
                )}
              </div>
            )}

            {paymentMethod === 'UPI' && (
              <div className={styles.form}>
                <Input
                  label="UPI ID"
                  required
                  placeholder="yourname@bank"
                  value={upiId}
                  onChange={(e) => setUpiId(e.target.value)}
                />
                <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
                  Demo UPI — we'll simulate your approval and confirm the payment in a few seconds.
                </p>
              </div>
            )}
          </Card>
        </div>

        <Card className={styles.summary}>
          <CardHeader title="Order summary" />
          {items.map((item) => (
            <div className={styles.summaryItem} key={`${item.bookId}:${item.deliveryType}`}>
              <span className={styles.summaryItemName}>
                {item.title} × {item.qty}
                <span className={styles.summaryItemType}>{item.deliveryType === 'VIRTUAL' ? 'Virtual' : 'Physical'}</span>
              </span>
              <span>₹{item.lineTotal}</span>
            </div>
          ))}
          <div className={styles.summaryDivider} />
          <div className={styles.summaryRow}>
            <span>Subtotal</span>
            <span>₹{pricing.subtotal.toFixed(2)}</span>
          </div>
          <div className={styles.summaryRow}>
            <span>Shipping</span>
            <span>{pricing.shippingFee === 0 ? 'Free' : `₹${pricing.shippingFee.toFixed(2)}`}</span>
          </div>
          {pricing.packagingFee > 0 && (
            <div className={styles.summaryRow}>
              <span>Packaging</span>
              <span>₹{pricing.packagingFee.toFixed(2)}</span>
            </div>
          )}
          <div className={styles.summaryRow}>
            <span>GST (9%)</span>
            <span>₹{pricing.taxAmount.toFixed(2)}</span>
          </div>
          <div className={styles.summaryTotal}>
            <span>Total</span>
            <span>
              ₹{pricing.grandTotal.toFixed(2)} <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 400 }}>{currency}</span>
            </span>
          </div>
          <Button onClick={onSubmit} disabled={submitting || addingAddress || !canSubmit} block>
            <Lock size={14} />
            {submitting || addingAddress ? 'Placing order…' : !canSubmit ? 'Top up wallet to continue' : 'Place order'}
          </Button>
        </Card>
      </div>
    </div>
  );
}
