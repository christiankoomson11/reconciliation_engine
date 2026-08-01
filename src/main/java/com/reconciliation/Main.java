package com.reconciliation;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main (String[] args )throws IOException {
        ReadResult resultA= CSVReader.read("ledger_a.csv");
        ReadResult resultB = CSVReader.read("ledger_b.csv");

        List<Transaction> transactionA = resultA.getTransactions();
        List<Transaction> transactionB = resultB.getTransactions();


        ReconciliationEngine engine = new ReconciliationEngine();
        List<Break> breaks = engine.reconcile(transactionA, transactionB);

        System.out.println();
        System.out.println("Reconciliation complete.");
        System.out.println("Source A: " + transactionA.size() + " transactions");
        System.out.println("Source B: " + transactionB.size() + " transactions");
        System.out.println("Breaks found: " + breaks.size());
        System.out.println();

        for (Break b : breaks) {            System.out.println(" " + b);
        }
    }
}
