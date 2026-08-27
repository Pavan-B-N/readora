package com.readora.payment.entity;

public enum PaymentMethod {
    CARD,
    UPI,
    NETBANKING,
    WALLET,
    /** Cash on Delivery — no upfront charge; captured immediately anyway, same as WALLET, since this dummy provider models "payment secured," not "cash physically in hand" (that happens for real at delivery, outside this service). */
    COD
}
