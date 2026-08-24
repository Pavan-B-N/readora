import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { Truck, Download } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchCart, cartCleared } from '@/redux/slices/cartSlice';
import { checkout } from '@/api/orderApi';
import type { DeliveryType } from '@/types/order';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './CheckoutPage.module.css';

const addressSchema = z.object({
  recipientName: z.string().min(1, 'Required'),
  line1: z.string().min(1, 'Required'),
  line2: z.string().optional(),
  city: z.string().min(1, 'Required'),
  state: z.string().min(1, 'Required'),
  postalCode: z.string().min(1, 'Required'),
  countryCode: z.string().length(2, 'Use a 2-letter country code'),
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
    formState: { errors },
  } = useForm<AddressFormValues>({ resolver: zodResolver(addressSchema) });

  useEffect(() => {
    dispatch(fetchCart());
  }, [dispatch]);

  const onSubmit = async (address: AddressFormValues) => {
    setSubmitting(true);
    try {
      const response = await checkout({
        deliveryType,
        shippingAddress: deliveryType === 'PHYSICAL' ? address : null,
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
    return <p>Your cart is empty.</p>;
  }

  return (
    <div>
      <h1>Checkout</h1>
      <form className={styles.layout} onSubmit={handleSubmit(onSubmit)}>
        <div className={styles.form}>
          <Card>
            <h2>Delivery</h2>
            <div className={styles.deliveryOptions}>
              <label className={[styles.deliveryOption, deliveryType === 'PHYSICAL' && styles.deliveryOptionActive].filter(Boolean).join(' ')}>
                <input type="radio" checked={deliveryType === 'PHYSICAL'} onChange={() => setDeliveryType('PHYSICAL')} />
                <Truck size={18} />
                Physical copy — shipped
              </label>
              <label className={[styles.deliveryOption, deliveryType === 'VIRTUAL' && styles.deliveryOptionActive].filter(Boolean).join(' ')}>
                <input type="radio" checked={deliveryType === 'VIRTUAL'} onChange={() => setDeliveryType('VIRTUAL')} />
                <Download size={18} />
                Virtual edition — instant
              </label>
            </div>
          </Card>

          {deliveryType === 'PHYSICAL' && (
            <Card>
              <h2>Shipping address</h2>
              <div className={styles.form}>
                <Input label="Recipient name" error={errors.recipientName?.message} {...register('recipientName')} />
                <Input label="Address line 1" error={errors.line1?.message} {...register('line1')} />
                <Input label="Address line 2" {...register('line2')} />
                <div className={styles.row}>
                  <Input label="City" error={errors.city?.message} {...register('city')} />
                  <Input label="State" error={errors.state?.message} {...register('state')} />
                </div>
                <div className={styles.row}>
                  <Input label="Postal code" error={errors.postalCode?.message} {...register('postalCode')} />
                  <Input label="Country code (e.g. US)" error={errors.countryCode?.message} {...register('countryCode')} />
                </div>
                <Input label="Phone" {...register('phone')} />
              </div>
            </Card>
          )}

          <Card>
            <h2>Payment method</h2>
            <Select label="Method" value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
              <option value="CARD">Card</option>
              <option value="UPI">UPI</option>
              <option value="NETBANKING">Net banking</option>
              <option value="WALLET">Wallet</option>
            </Select>
          </Card>
        </div>

        <Card>
          <h2>Order summary</h2>
          {items.map((item) => (
            <div className={styles.summaryRow} key={item.bookId}>
              <span>
                {item.title} × {item.qty}
              </span>
              <span>
                {item.lineTotal} {currency}
              </span>
            </div>
          ))}
          <div className={styles.summaryTotal}>
            <span>Subtotal</span>
            <span>
              {subtotal} {currency}
            </span>
          </div>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Placing order…' : 'Place order'}
          </Button>
        </Card>
      </form>
    </div>
  );
}
