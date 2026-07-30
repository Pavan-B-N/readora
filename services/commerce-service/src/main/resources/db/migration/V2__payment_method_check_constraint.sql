ALTER TABLE commerce.orders
    ADD CONSTRAINT chk_orders_payment_method
    CHECK ((payment_method)::text = ANY (ARRAY[('WALLET'::character varying)::text, ('UPI'::character varying)::text]));
