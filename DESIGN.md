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
- `transactionId` — *what* uis broken; without it the record can't be located.
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

### CsvReader

Reads a CSV file and turns it into a list of parsed `Transaction` objects plus a
list of rows it could not parse. Its single job is reading and parsing — nothing
about matching.

**Why reading is its own class (single responsibility)**
Reading and matching are distinct jobs that change for distinct reasons: a change
to the CSV format touches only the reader, while a change to the matching rules
touches only the engine. Keeping them in separate classes means each can be
tested in isolation — the reader against messy files, the engine against
in-memory lists with no files involved — and either can be reused or changed
without disturbing the other.

**Why bad rows are collected and returned, not printed**
When the reader hits a malformed row it does not print it or crash — it adds the
raw line to a `badRows` list and carries on. Printing would force a policy: it
would decide, on everyone's behalf, that a bad row goes to the console. But a
caller might instead want to count bad rows, write them to a log, fold them into
the reconciliation report, or ignore them silently. A low-level class should
surface information and let the caller decide what to do with it. So `read`
returns both lists and leaves the policy to whoever called it.

**Why store the raw line rather than the row's id**
The instinct is to store the bad row's id, but a row is "bad" precisely because it
failed to parse — the id may be the very field that is missing or malformed, so
it can't be relied upon. The one thing always available is the raw line text
(plus its line number), which is also exactly what an investigator needs to see
what went wrong. So a bad row is stored as its raw text with a line number.

**Why return a ReadResult rather than two bare lists**
A method returns one value, but reading produces two related results — the good
transactions and the bad rows. Rather than abuse a Map with magic string keys,
they are bundled in a small `ReadResult` object with two typed, named getters.
This is self-documenting (a reader clearly produces transactions and bad rows),
compiler-checked, and extensible if more fields are needed later.

**War story: why this resilience matters**
While wiring up the reader I accidentally saved the program's own output into
`ledger.csv` instead of the transaction data, so the file contained lines like
"Good transactions:" and "Bad rows:". The engine did not crash. It tried to parse
each line, quarantined the ones it couldn't into `badRows`, and carried on — which
is exactly the intended behaviour. Real ledger files are messy, and a
reconciliation tool that falls over on one malformed line is useless. The bug also
reinforced a debugging lesson: when output looks wrong, suspect the input as
readily as the code — here every class was correct and the fault was entirely in
the data file.

### ReconciliationEngine

The core of the system. Takes two lists of transactions (source A and source B)
and produces a list of breaks.

**Approach: index both sides, then two passes**
Both sources are first indexed into HashMaps keyed by transaction id, so any id
can be looked up in one step. Then:
- Pass 1 walks source A. For each transaction it looks up the same id in B's map.
  Not present -> MISSING_FROM_B. Present but a value differs -> VALUE_MISMATCH.
  Present and equal -> a clean match, no break.
- Pass 2 walks source B and looks up each id in A's map. Any id absent from A is
  a MISSING_FROM_A break.

**Why a HashMap index rather than nested loops (complexity)**
The naive approach compares every transaction in A against every transaction in
B — O(n^2). That does not scale: on large inputs it becomes unusably slow. I
experienced this directly when a brute-force solution to LeetCode's Contains
Duplicate hit Time Limit Exceeded. Indexing each source in a HashMap makes every
lookup O(1), so the whole reconciliation runs in O(n). The engine never scans one
list looking through the other; it looks each id up directly.

**Why pass 2 only checks for absence**
Every id that exists in both sources is already fully examined in pass 1 (matched
or value-mismatch). So pass 2's only remaining job is to find ids that exist in B
but never appeared in A. It deliberately does not re-compare values — doing so
would report every mismatch twice, once from each side.

**Why compareTo for amounts but equals for dates**
Amounts are BigDecimal and are compared with compareTo(...) == 0, because
BigDecimal.equals is scale-sensitive: "500.0" and "500.00" are equal in value but
not equal by equals, which would produce phantom breaks when two sources format
the same figure differently. Dates are LocalDate and compared with equals, which
is safe because dates have no equivalent scale trap. A mismatch is flagged if the
amount OR the date differs.

**Known limitations / future work**
v1 matches on exact transaction id only. It cannot yet handle real-world cases
such as amounts differing by a rounding penny, the same
trade settling a day apart on each side (timing breaks), or one payment on one
side corresponding to several line items on the other (one-to-many matching).
These are the natural next steps.

## Testing

The reconciliation logic is covered by unit tests using JUnit
(`ReconciliationEngineTest`).

**What is tested**
Four cases, one per branch of the engine's logic:
- Two identical sources produce no breaks (the clean-match path).
- A shared id with a differing amount is detected as a VALUE_MISMATCH.
- An id present only in source A is detected as MISSING_FROM_B.
- An id present only in source B is detected as MISSING_FROM_A.

**Why these cases**
Together they exercise every path through `reconcile`: the match path and all
three break types. Each test isolates a single behaviour and uses small,
hand-built inputs where the correct output is known in advance, so a failure
points directly at what broke. Each test follows Arrange–Act–Assert: build known
inputs, run the engine, assert both the number of breaks and the type of the
break produced.

**Why it matters**
Automated tests replace eyeballing console output by hand. They let the engine be
changed or refactored with confidence — if a change breaks any of the four
behaviours, the relevant test fails immediately and names the discrepancy
(e.g. "expected 1 but was 0"), instead of the bug going unnoticed until it
reaches real data.

**Future work**
As the engine gains tolerance matching, timing-break handling, and one-to-many
matching, each new behaviour should get its own test. Edge cases worth adding:
empty inputs on both sides, duplicate ids within one source, and malformed-row
handling end to end through `CsvReader`.