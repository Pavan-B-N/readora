package com.readora.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** One line item on an order, with price/title frozen at purchase time. */
@Entity
@Table(name = "order_items", schema = "commerce")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "title_snapshot", nullable = false)
    private String titleSnapshot;

    @Column(name = "isbn_snapshot", length = 13)
    private String isbnSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public OrderItem(
            Order order, UUID bookId, String titleSnapshot, String isbnSnapshot,
            BigDecimal unitPriceSnapshot, int qty
    ) {
        this.order = order;
        this.bookId = bookId;
        this.titleSnapshot = titleSnapshot;
        this.isbnSnapshot = isbnSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.qty = qty;
        this.lineTotal = unitPriceSnapshot.multiply(BigDecimal.valueOf(qty));
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public String getIsbnSnapshot() {
        return isbnSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public int getQty() {
        return qty;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OrderItem orderItem)) return false;
        return id != null && Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
