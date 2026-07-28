package com.reconciliation;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Main {
    public static void main (String[] args )throws IOException {
        ReadResult result = CSVReader.read("ledger.csv");

        System.out.println("Good transaction:");
        for (Transaction t : result.getTransactions()) {
            System.out.println("  " + t);
        }

        System.out.println("Bad rows:");
        for (String bad : result.getBadRows()) {
            System.out.println("  " + bad);
        }
    }
}
