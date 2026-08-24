import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { Truck, Download, Info, Lock } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchCart, cartCleared } from '@/redux/slices/cartSlice';
import { checkout } from '@/api/orderApi';
import type { DeliveryType } from '@/types/order';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './CheckoutPage.module.css';

// All optional here — this form also submits for VIRTUAL delivery, where the address section
// isn't rendered at all. Required-ness for PHYSICAL is checked in onSubmit, where deliveryType
// is known.
const addressSchema = z.object({
  recipientName: z.string().optional(),
  line1: z.string().optional(),
  line2: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  postalCode: z.string().optional(),
  countryCode: z.string().optional(),
  phone: z.string().optional(),
});

type AddressFormValues = z.infer<typeof addressSchema>;

export function CheckoutPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { items, subtotal, currency } = useAppSelector((state) => state.cart);

  const [deliveryType, setDeliveryType] = useState<DeliveryType>('PHYSICAL');
  const [paymentMethod, setPaymentMethod] = useState('CARD');
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<AddressFormValues>({ resolver: zodResolver(addressSchema) });

  useEffect(() => {
    dispatch(fetchCart());
  }, [dispatch]);

  const onSubmit = async (address: AddressFormValues) => {
    if (deliveryType === 'PHYSICAL') {
      const required = ['recipientName', 'line1', 'city', 'state', 'postalCode', 'countryCode'] as const;
      let hasError = false;

      for (const field of required) {
        if (!address[field]?.trim()) {
          setError(field, { message: 'Required' });
          hasError = true;
        }
      }
      if (address.countryCode?.trim() && address.countryCode.trim().length !== 2) {
        setError('countryCode', { message: 'Use a 2-letter country code' });
        hasError = true;
      }

      if (hasError) {
        showToast('Fill in the full shipping address', 'error');
        return;
      }
    }

    setSubmitting(true);
    try {
      const response = await checkout({
        deliveryType,
        shippingAddress: deliveryType === 'PHYSICAL' ? (address as Required<AddressFormValues>) : null,
        paymentMethod,
        items: items.map((item) => ({ bookId: item.bookId, qty: item.qty })),
      });
      dispatch(cartCleared());
      showToast('Order placed');
      navigate(ROUTES.orderDetail(response.orderId));
    } catch {
      showToast('Checkout failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (items.length === 0) {
    return <p style={{ color: 'var(--color-text-muted)' }}>Your cart is empty.</p>;
  }

  return (
    <div>
      <h1>Checkout</h1>
      <form className={styles.layout} onSubmit={handleSubmit(onSubmit)}>
        <div className={styles.form}>
          <Card>
            <CardHeader title="Delivery" subtitle="An order is entirely physical or entirely virtual." />
            <div className={styles.deliveryOptions}>
              <label
                className={[styles.deliveryOption, deliveryType === 'PHYSICAL' && styles.deliveryOptionActive]
                  .filter(Boolean)
                  .join(' ')}
              >
                <input
                  type="radio"
                  name="deliveryType"
                  checked={deliveryType === 'PHYSICAL'}
                  onChange={() => setDeliveryType('PHYSICAL')}
                />
                <span className={styles.deliveryText}>
                  <span className={styles.deliveryLabel}>
                    <Truck size={15} />
                    Physical copy
                  </span>
                  <span className={styles.deliveryHint}>Shipped to your address</span>
                </span>
              </label>

              <label
                className={[styles.deliveryOption, deliveryType === 'VIRTUAL' && styles.deliveryOptionActive]
                  .filter(Boolean)
                  .join(' ')}
              >
                <input
                  type="radio"
                  name="deliveryType"
                  checked={deliveryType === 'VIRTUAL'}
                  onChange={() => setDeliveryType('VIRTUAL')}
                />
                <span className={styles.deliveryText}>
                  <span className={styles.deliveryLabel}>
                    <Download size={15} />
                    Virtual edition
                  </span>
                  <span className={styles.deliveryHint}>Available instantly</span>
                </span>
              </label>
            </div>
          </Card>

          {deliveryType === 'PHYSICAL' ? (
            <Card>
              <CardHeader title="Shipping address" />
              <div className={styles.form}>
                <Input
                  label="Recipient name"
                  required
                  error={errors.recipientName?.message}
                  {...register('recipientName')}
                />
                <Input label="Address line 1" required error={errors.line1?.message} {...register('line1')} />
                <Input label="Address line 2" {...register('line2')} />
                <div className={styles.row2}>
                  <Input label="City" required error={errors.city?.message} {...register('city')} />
                  <Input label="State" required error={errors.state?.message} {...register('state')} />
                </div>
                <div className={styles.row2}>
                  <Input
                    label="Postal code"
                    required
                    error={errors.postalCode?.message}
                    {...register('postalCode')}
                  />
                  <Input
                    label="Country code"
                    required
                    hint="2 letters, e.g. IN"
                    error={errors.countryCode?.message}
                    {...register('countryCode')}
                  />
                </div>
                <Input label="Phone" {...register('phone')} />
              </div>
            </Card>
          ) : (
            <Card>
              <div className={styles.virtualNote}>
                <Info size={15} style={{ flexShrink: 0, marginTop: 1 }} />
                <span>
                  No shipping address needed. Every book in your cart must have a virtual edition
                  available — checkout will tell you if one doesn't. Virtual orders are priced at the
                  digital edition's own price and are delivered as soon as payment clears.
                </span>
              </div>
            </Card>
          )}

          <Card>
            <CardHeader title="Payment method" />
            <Select label="Method" value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
              <option value="CARD">Card</option>
              <option value="UPI">UPI</option>
              <option value="NETBANKING">Net banking</option>
              <option value="WALLET">Wallet</option>
            </Select>
          </Card>
        </div>

        <Card>
          <CardHeader title="Order summary" />
          {items.map((item) => (
            <div className={styles.summaryItem} key={item.bookId}>
              <span className={styles.summaryItemName}>
                {item.title} × {item.qty}
              </span>
              <span>₹{item.lineTotal}</span>
            </div>
          ))}
          <div className={styles.summaryTotal}>
            <span>Subtotal</span>
            <span>₹{subtotal}</span>
          </div>
          <Button type="submit" disabled={submitting} block>
            <Lock size={14} />
            {submitting ? 'Placing order…' : 'Place order'}
          </Button>
          <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)', marginTop: 'var(--space-3)', textAlign: 'center' }}>
            Tax is added at the final step. Currency: {currency}
          </p>
        </Card>
      </form>
    </div>
  );
}
