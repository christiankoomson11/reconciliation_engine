package com.reconciliation;

import java.util.List;

public class ReadResult {
    private final List<Transaction> transactions;
    private final List<String> badRows;

    public ReadResult(List<Transaction> transactions, List<String> badRows) {
        this.transactions = transactions;
        this.badRows = badRows;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<String> getBadRows() {
        return badRows;
    }
}


