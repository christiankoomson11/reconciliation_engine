package com.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
public class Main {
    public static void main (String[] args) {
        Transaction t = new Transaction("TXN1001", new BigDecimal("500.00"), LocalDate.parse("2026-06-01"));
        System.out.println(t.getId());
        System.out.println(t.getAmount());
        System.out.println(t);
    }
}
