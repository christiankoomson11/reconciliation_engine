package com.reconciliation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReconciliationEngine {
    public List<Break> reconcile(List<Transaction> sourceA, List<Transaction> sourceB) {

        Map<String, Transaction> mapA = new HashMap<>();
        for (Transaction a : sourceA) {
            mapA.put(a.getId(), a);
        }

        Map<String, Transaction> mapB = new HashMap<>();
        for (Transaction b : sourceB) {
            mapB.put(b.getId(), b);
        }
        List<Break> breaks = new ArrayList<>();

        for (Transaction a : sourceA) {
            Transaction b = mapB.get(a.getId());
            if (b == null) {
                breaks.add(new Break(a.getId(), Break.Type.MISSING_FROM_B, "Present in A, missing in B"));
            } else if (a.getAmount().compareTo(b.getAmount()) != 0 || !a.getDate().equals(b.getDate())) {
                breaks.add(new Break(a.getId(), Break.Type.VALUE_MISMATCH, "A: " + a.getAmount() + " on " + a.getDate() + " vs B: " + b.getAmount() + " on " + b.getDate()));
            }
        }

        for (Transaction b : sourceB) {
            if (!mapA.containsKey(b.getId())) {
                breaks.add(new Break(b.getId(), Break.Type.MISSING_FROM_A, "Present in B, missing in A"));
            }
        }
        return breaks;
    }
}
