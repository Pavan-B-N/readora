package com.readora.commerce.entity;

/** How an order's grand total is settled. WALLET is charged in full at checkout; UPI is authorized separately by payment-service. */
public enum PaymentMethod {
    WALLET,
    UPI
}
