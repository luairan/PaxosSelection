# Safety review of the lease implementation

Scope: a fixed, trusted, non-Byzantine cluster, unique stable node IDs, durable
ballot sequences, and monotonic clocks advancing within +/-1% of real time.
Addresses must identify the same acceptors on all members. Arbitrary clock
suspension, dynamic membership and duplicated live node identities are excluded.
This is a code-level argument and regression suite, not a formal verification.

## Why two different live owners must not overlap

1. A proposer needs distinct responses from a majority of the configured peers.
   Two such majorities intersect. A duplicate reply from a peer cannot add votes.
2. Acceptor request checks and state updates run under the same State monitor.
   An acceptor refuses a different owner's request while its accepted lease is
   unexpired, even if the request has a higher ballot. Preparing a higher ballot
   never releases the existing reservation. Same-owner renewal cannot replace it
   with a different owner.
3. Ownership starts from the first phase-two construction/send-queue timestamp,
   before any acceptor receives the request. Reconstructing the request keeps
   this timestamp. A delayed response cannot restart the timer. Old rounds and
   cancelled rounds are ignored. Queuing and network delay only shorten usable
   ownership, never extend it.
4. At worst, a slow owner's 29-second deadline lasts 29/0.99 = 29.293 real
   seconds; a fast acceptor's 30-second reservation lasts 30/1.01 = 29.703 real
   seconds. Thus the owner expires before the intersecting acceptor may grant a
   different owner. Clocks may have unrelated offsets.
5. After memory loss, an acceptor grants nothing for 30 local seconds. Even a
   fast restarted acceptor waits at least 29.703 real seconds from restart,
   exceeding any former owner's remaining 29.293 real seconds. Delayed requests
   can reserve space for an already-expired attempt but cannot resurrect its
   proposer: its immutable deadline and ballot still apply.

## Follow-up review fixes

- Phase-two construction previously reset the start time on every call. Although
  the current automatic path calls it only once, its public API allowed an old
  quorum to be applied to a newer deadline. It is now idempotent with respect to
  time. A regression delivers old quorum replies after a successor has won.
- Synchronous network I/O previously ran under the election monitor, and the
  transport serialized all peers under one monitor. A blocked minority could
  prevent processing a healthy majority. Each peer now has one ordered worker
  and a bounded one-element pending queue; network I/O holds only that peer's
  transport lock. Superseded queued work is discarded, and stale ballots cannot
  grant ownership. Application shutdown interrupts all workers.
- Renewal was scheduled ten seconds after confirmation, which can be later than
  expiry when confirmation is delayed. It now targets ten seconds after the
  immutable phase-two timestamp; overdue renewals run on the next scheduler tick.

## Evidence and remaining limits

The tests cover opposite clock rates and unrelated offsets, recovery with a
slow former owner and fast restarted acceptors, immutable phase-two deadlines,
blocked minority communication, slow confirmations, and the existing partition,
restart, reordered/delayed/duplicate reply, contention and real-socket scenarios.

The safety argument depends on the assumptions above. There is no guarantee of
continuous leadership during arbitrary message loss or sustained contention.
Lease checks alone cannot stop an operation paused before a write. The optional
shared JDBC business store now implements resource-side fencing (see FENCING.md);
other downstream systems must enforce their own fencing. Endpoint authentication and dependency modernization are implemented separately
from the lease protocol; see SECURITY.md. Dynamic reconfiguration remains separate work.

## Ballot file ownership and initialization

`BallotSequenceStore` owns the node's file descriptor, exclusive OS lock and
counter. It preserves the existing `node-{id}.ballot` filename and eight-byte
counter format. A new ballot is returned only after the counter is synced.
Invalid lengths/values fail closed without resetting the file. Failed opens
close their descriptor, and `State` publishes the store only after initialization
succeeds. Failed/closed state cannot participate as a ready acceptor; a successful
retry starts the full recovery wait.

A process-local registry rejects duplicate opens before creating another file
descriptor, including aliases of the same file. This matters on systems where
closing any descriptor releases the process's OS locks for that file. Other
processes are excluded by the OS lock. All application access to ballot files
must use this store; do not modify, replace or open live ballot files through
unrelated code. The tests use child JVMs to verify lock exclusion after a rejected
duplicate open and retry after an external lock holder exits.

The counter now has an append-only `node-{id}.ballot.journal` sidecar. Each record
contains a strictly increasing reserved sequence and its bitwise complement.
The store syncs that reservation before overwriting/syncing the legacy eight-byte
counter. On open, it validates the entire journal and uses the higher of the
counter and last reservation, so a torn counter overwrite cannot silently lower
a previously reserved generation. Incomplete or invalid journal records fail
closed; they are not truncated automatically. Any persistence I/O failure poisons
the open store, stops further ballot issuance and makes State unready; State also
revokes its local ownership. Explicit close/reopen is required to validate again.

Existing counter-only directories are adopted on first open. Once adopted, keep
the journal and counter together in the same stable durable data directory;
backups must preserve both. Do not delete the journal, switch to a hard-link alias
directory, or downgrade to a binary that ignores it. The sidecar is append-only
(16 bytes per attempted generation); compaction is not implemented. Its checksum
checks record structure, not adversarial tampering or arbitrary storage corruption.
Directory sync is required; filesystems/platforms that cannot provide it fail
startup. The durable-directory and filesystem fsync guarantees remain assumptions.

## Explicit proposer phases

Each attempt transitions PREPARING -> PREPARED on a distinct prepare quorum,
then PREPARED -> ACCEPTING when phase two is first constructed. ACCEPTING becomes
SUCCEEDED only on a matching accept quorum within the immutable deadline, or
EXPIRED when a matching reply arrives at/after that deadline. Active phases can
be CANCELLED. SUCCEEDED, CANCELLED and EXPIRED are terminal: no response can reopen
them. SUCCEEDED records historical confirmation, not a claim that its lease is
still live; State remains the authority for current lease validity.

PREPARED does not count accept replies until phase two has actually started.
Repeated construction while ACCEPTING preserves the original timestamp and ballot;
construction in a terminal phase is rejected. Cancellation of an attempt never
revokes State ownership on its own, since that ownership may belong to a newer
attempt. The service's stop operation explicitly revokes current ownership.
