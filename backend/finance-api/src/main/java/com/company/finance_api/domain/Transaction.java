package com.company.finance_api.domain;

import com.company.finance_api.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant createdAt;

    protected Transaction() {}

    private Transaction(
            User user,
            Instrument instrument,
            TransactionType type,
            BigDecimal price,
            BigDecimal quantity
    ) {
        this.user = user;
        this.instrument = instrument;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = price.multiply(quantity);
        this.createdAt = Instant.now();
    }

    public static Transaction buy(
            User user,
            Instrument instrument,
            BigDecimal price,
            BigDecimal quantity
    ) {
        return new Transaction(user, instrument, TransactionType.BUY, price, quantity);
    }

    public static Transaction sell(
            User user,
            Instrument instrument,
            BigDecimal price,
            BigDecimal quantity
    ) {
        return new Transaction(user, instrument, TransactionType.SELL, price, quantity);
    }
}