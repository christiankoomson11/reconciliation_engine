package com.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Main {
    public static void main (String[] args) {
        Transaction t = new Transaction("TXN1001", new BigDecimal("500.00"), LocalDate.parse("2026-06-01"));
        Break b1 = new Break ("TXN1002",Break.Type.VALUE_MISMATCH, "A: 500.00 vs B: 505.00");
        Break b2 = new Break ("TXN1003", Break.Type.MISSING_FROM_B, "Present in A, missing in B");

        /*
        System.out.println(t.getId());
        System.out.println(t.getAmount());
        System.out.println(t);
         */
        System.out.println(b1);
        System.out.println(b2);
    }
}
