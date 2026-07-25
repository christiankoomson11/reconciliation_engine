package com.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private final String id;
    private final BigDecimal amount;
    private final LocalDate date;

    public Transaction(String id, BigDecimal amount, LocalDate date) {
        this.id = id;
        this.amount = amount;
        this.date = date;
    }

    public String getId() { return id; }
    public BigDecimal getAmount() { return amount;}
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return id + " | " + amount + " | " + date;
    }
}
