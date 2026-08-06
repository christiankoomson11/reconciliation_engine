# Reconciliation Engine

A Java tool that compares two sets of financial transactions and reports the discrepancies ("breaks") between them.

## The Problem

In finance, the same set of transactions is often recorded independently by two parties — for example an internal ledger and a counterparty statement. Both should tell the same story. Where they disagree, the discrepancy is called a **break** and must be investigated. This is core, daily work in custody and asset-servicing operations. This engine automates that comparison: it ingests two transaction sources, matches them by ID, and reports every break.

## What It Does
- Exposes reconciliation as a REST API — returns breaks as JSON over HTTP (Spring Boot)
- Reads transactions from two CSV files
- Matches them by transaction ID using a hash-based index (O(n), not O(n²))
- Detects three kinds of break:
    - **Value mismatch** — same ID on both sides, but the amount or date differs
    - **Missing from B** — a transaction in source A with no counterpart in B
    - **Missing from A** — a transaction in source B with no counterpart in A
- Handles malformed CSV rows gracefully — bad rows are collected and reported, not crashed on
- Uses `BigDecimal` for exact monetary values and `LocalDate` for dates
- Covered by JUnit unit tests for every break type

## How to Run

Requires Java 21+.

Clone the repo and open it in IntelliJ IDEA (or any IDE with Maven support). Let Maven import the dependencies, then:

- **Console version:** run `Main.java`
- **REST API:** run `ReconciliationApiApplication.java`, then visit `http://localhost:8080/reconcile` in a browser

Run the tests from the IDE, or via the Maven panel → Lifecycle → `test`.

## Running the API

The engine is also exposed as a REST endpoint via Spring Boot.

Start the application:

```bash
mvn spring-boot:run
```
Or open the project in IntelliJ and run ReconciliationApiApplication (for the API)
or Main (for the console version)
Then call the reconcile endpoint:

GET https://localhost:8080/reconcile

It reads `ledger_a.csv` and `ledger_b.csv` from the project root, runs the reconciliation, and returns the breaks as JSON:

```json
[
  { "transactionId": "TXN1002", "type": "VALUE_MISMATCH", "detail": "A: 120.50 on 2026-06-02 vs B: 125.50 on 2026-06-02" },
  { "transactionId": "TXN1003", "type": "MISSING_FROM_B", "detail": "Present in A, missing in B" },
  { "transactionId": "TXN1004", "type": "MISSING_FROM_A", "detail": "Present in B, missing in A" }
]
```

Run the tests with:

```bash
mvn test
```

## Design Highlights

- **Exact money:** amounts use `BigDecimal`, compared with `compareTo`, so formatting differences like `500.0` vs `500.00` don't produce phantom breaks.
- **Efficient matching:** each source is indexed in a `HashMap` by ID, making lookups O(1) and the whole reconciliation O(n) rather than the O(n²) of nested loops.
- **Immutable domain model:** `Transaction` and `Break` are immutable, reflecting that they are historical records that should never change after creation.
- **Resilient input handling:** the reader isolates malformed rows and reports them rather than aborting the run.

Full reasoning for every decision is in [DESIGN.md](DESIGN.md).

## Tech Stack

Java 21+, Maven, JUnit 5, Spring Boot

## Future Work

v1 matches on exact transaction ID only. Planned next steps:
- **Tolerance matching** — treat amounts within a small threshold (e.g. rounding differences) as matches
- **Timing breaks** — handle the same trade settling a day apart on each side
- **One-to-many matching** — one payment on one side corresponding to several line items on the other
After v2
- Since the endpoint currently reads fixed files, the next step is to add file uploads.