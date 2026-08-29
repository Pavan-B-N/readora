ALTER TABLE users.wallet_transactions DROP CONSTRAINT chk_wallet_transaction_type;

ALTER TABLE users.wallet_transactions ADD CONSTRAINT chk_wallet_transaction_type
    CHECK (((type)::text = ANY (ARRAY[
        ('SIGNUP_BONUS'::character varying)::text,
        ('REFERRAL_BONUS'::character varying)::text,
        ('REDEEMED'::character varying)::text,
        ('REVERSED'::character varying)::text,
        ('TOPUP'::character varying)::text,
        ('COUPON_REDEEMED'::character varying)::text,
        ('CASHBACK'::character varying)::text
    ])));
