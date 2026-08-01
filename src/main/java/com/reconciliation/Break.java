package com.reconciliation;

public class Break {
    public enum Type {
        MISSING_FROM_A,
        MISSING_FROM_B,
        VALUE_MISMATCH
    }

    private final String transactionId;
    private final Type type;
    private final String detail;

    public Break (String transactionId, Type type, String detail) {
        this.transactionId = transactionId;
        this.type = type;
        this.detail = detail;
    }

    public String getTransactionId() { return transactionId;}
    public Type getType() { return type;}
    public String getDetail() { return detail;}

    @Override
    public String toString() {
        return "[ " + type + " ]" + transactionId + " -> " + detail;
    }
}
