package com.readora.commerce.entity;

/** Whether an entire order is fulfilled physically (shipped) or virtually (instant digital delivery). Chosen once per order — an order can't mix both. */
public enum DeliveryType {
    PHYSICAL,
    VIRTUAL
}
