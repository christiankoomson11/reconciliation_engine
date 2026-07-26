# Design Decisions 

A record of the engineering choices in this project and the reasoning behind them.
Written as i buld, so the rationale and understanding is captured.

## Problem

Two parties indepenently record the same set of financial transactions - for
example an internal ledger and a counterparty statement. Bouth should tell the same story.
Where they do not. the discrepancy is called a **break** and must be
investigated. This engine automates that comparison: it ingests two sources,
matches them, and reports the breaks.

Why this problem: reconciliation is core to custody and asset-servicing
operations. The interesting engineering is not the storage but the matching
logic and the handling of imperfect real-world data.

## Domain model

### Transaction Class

Represents a single transaction as recorded in one source. The atomic unit the
rest of the system operates on.

**Why a class rather than parallel arrays**
A transaction is one thing with three inseparable attributes. Parallel arrays
have nothing enforcing that index 5 in each refers to the same record; one
sorting bug silently corrupts the data. Bundling the fields means the language
guarantees they travel together.

**Why private fields with public getters (encapsulation)**
External code can read a transaction but cannot alter it. If validation or a
computed field is needed later, it changes in one place and no caller is
affected — the public interface stays stable while internals stay free to change.

**Why final fields (immutability)**
A transaction is a historical fact: it occurred, for an amount, on a date. It is
never legitimately edited, and `final` makes the compiler enforce that. This
matters here specifically: the premise of the tool is comparing two records *as
they are*. If comparison logic could mutate what it compares, results are
meaningless. Immutable objects are also inherently thread-safe, leaving the door
open to parallel processing.

**Why BigDecimal for money rather than double**
Binary floating point cannot exactly represent most decimal fractions —
`0.1 + 0.2` evaluates to `0.30000000000000004`. In a tool whose entire job is
detecting when two amounts disagree, that is fatal: it would report phantom
breaks caused by its own arithmetic. Two related decisions:
- Construct from a String (`new BigDecimal("500.00")`); constructing from a
  double reintroduces the imprecision being avoided.
- Compare with `.compareTo(other) == 0`, not `.equals()`. `.equals()` also
  compares scale, so `"500.0"` and `"500.00"` are unequal despite being the same
  value. Two sources formatting the same figure differently is routine, so
  scale-insensitive comparison is the correct behaviour.

**Why LocalDate rather than String**
Text dates cannot be meaningfully compared or ordered — `"01/06/2026"` and
`"2026-06-01"` are the same day but different strings. `LocalDate` supports real
comparison (`isBefore`, `isAfter`), needed for timing breaks where the same trade
settles a day apart on each side. It also rejects malformed input at parse time
rather than silently accepting nonsense: in a financial pipeline, failing loudly
on bad data beats processing it wrongly.

**Why toString()**
Readable output in logs and reports; without it, printing yields
`Transaction@4f3f5b24`. The `@Override` annotation makes the compiler verify the
method genuinely overrides the inherited one, catching typos.

## Known limitations / future work

- v1 matches on exact transaction ID only. Tolerance-based matching (small
  amount or date differences) and one-to-many matching are the natural next steps.

## Break Class

**What a break represents** Represents a single discrepancy found during reconciliation — one finding that a
human will investigate, and one line in the final report.

**Why it is not modelled on Transaction's fields**
The instinct is to give a Break the same id/amount/date as a Transaction, but a
Break is a different kind of thing: a *finding about* a transaction, not a
transaction. Modelling it around the transaction's fields breaks down in two
places. For a value mismatch, the whole point is that A and B disagree, so
storing a single amount throws away half the information the investigator needs —
they need both sides. For a missing transaction, the record only exists on one
side, so there is no second value to store at all. So the class is modelled
around what an investigator needs to act on the finding, not around the shape of
a transaction.

**Why the three fields (id, type, detail)**
- `transactionId` — *what* is broken; without it the record can't be located.
- `type` — *how* it is broken (missing from A, missing from B, or value
  mismatch). The investigator handles each case completely differently, so this
  drives the response.
- `detail` — the *evidence*: a human-readable description of the specifics. It is
  a free-form String so it can hold both sides for a mismatch
  ("A: 500.00 vs B: 505.00") or a single side for a missing record — which solves
  the problem that different break types need different information.

**Why an enum for the type**
There are exactly three kinds of break and no others — a closed, known set. An
enum makes the compiler enforce that: an invalid or misspelled type won't
compile, whereas a String literal like "MISSNG_FROM_B" would compile and then
misbehave silently at runtime. The enum turns a potential runtime bug into a
compile-time error, and documents the complete set of cases in one place.

**Why immutable**
A break is a recorded finding. Once produced it should not change, for the same
reasons as Transaction: safety and predictability. Final fields, set once in the
constructor.