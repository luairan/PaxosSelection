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
There is no storage fencing: an operation paused after checking ownership may
still run after its lease ends. Downstream systems must enforce fencing before
using this demonstration for protected production writes. Endpoint authentication,
dependency modernization and dynamic reconfiguration remain separate work.
