package com.reconciliation;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReconciliationEngineTest {

    @Test
    void twoIdenticalSourcesProduceNoBreaks() {
        List<Transaction> a = List.of(
                new Transaction("TXN1", new BigDecimal("100.00"), LocalDate.parse("2026-01-01")));
        List<Transaction> b = List.of(
                new Transaction("TXN1", new BigDecimal("100.00"), LocalDate.parse("2026-01-01")));

        List<Break> breaks = new ReconciliationEngine().reconcile(a,b);

        assertEquals(0, breaks.size());
    }

    @Test
    void valueMismatchIsDetected () {
        List<Transaction> a = List.of(
                new Transaction("TXN1", new BigDecimal("100.00"), LocalDate.parse("2026-06-06")));
        List<Transaction> b = List.of(
                new Transaction("TXN1", new BigDecimal("105.00"), LocalDate.parse("2026-06-06")));
        List<Break> breaks = new ReconciliationEngine().reconcile(a, b);

        assertEquals(1, breaks.size());
        assertEquals(Break.Type.VALUE_MISMATCH, breaks.get(0).getType());
    }

    @Test
    void transactionMissingFromBIsDetected() {
        List<Transaction> a = List.of(
                new Transaction("TXN1", new BigDecimal("100.00"), LocalDate.parse("2026-06-06")));
        List<Transaction> b = List.of();

        List<Break> breaks = new ReconciliationEngine().reconcile(a, b);

        assertEquals(1, breaks.size());
        assertEquals(Break.Type.MISSING_FROM_B, breaks.get(0).getType());
    }

    @Test
    void transactionMissingFromAIsDetected() {
        List<Transaction> a = List.of();

        List<Transaction> b =  List.of(
                new Transaction("TXN1", new BigDecimal("100.00"), LocalDate.parse("2026-06-06")));
        List<Break> breaks = new ReconciliationEngine().reconcile(a, b);

        assertEquals(1, breaks.size());
        assertEquals(Break.Type.MISSING_FROM_A, breaks.get(0).getType());
    }
}

