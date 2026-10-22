# Lease election: configuration and verification

This patch changes the protocol messages and node identity handling. Upgrade all
members together, after stopping the previous cluster and waiting at least the
maximum lease interval. Do not mix old and new binaries.

## Run a three-node cluster

Build with JDK 17 and Maven 3.6.3+: `./mvnw clean verify`. Run each command in a separate
terminal; the list of acceptors must be identical on every node:

```sh
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar --server.port=8081 --paxos.node-id=1 --paxos.data-dir=./data/node1 --paxos.peers=ws://localhost:8081/paxos,ws://localhost:8082/paxos,ws://localhost:8083/paxos
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar --server.port=8082 --paxos.node-id=2 --paxos.data-dir=./data/node2 --paxos.peers=ws://localhost:8081/paxos,ws://localhost:8082/paxos,ws://localhost:8083/paxos
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar --server.port=8083 --paxos.node-id=3 --paxos.data-dir=./data/node3 --paxos.peers=ws://localhost:8081/paxos,ws://localhost:8082/paxos,ws://localhost:8083/paxos
```

Before starting, generate credentials as shown in [SECURITY.md](SECURITY.md).
All nodes need the same node token; admin and node tokens must differ.
Start each candidate with `curl -X POST -H "Authorization: Bearer $PAXOS_ADMIN_TOKEN" http://localhost:8081/show/start`
(and ports 8082/8083). Read status with an authenticated GET to `/show/showMater`. Starting twice is
idempotent. Recovery intentionally prevents grants during the first 30 seconds.
The default configuration remains a one-node local demo.

- Assign a unique numeric node ID in 1..65535 to each process. Keep its data
  directory stable across restarts. Ballots combine a persisted, fsynced counter
  with the node ID. An exclusive file lock rejects duplicate local identities.
  Node IDs must also be unique across hosts; the application cannot enforce this.
- Membership is fixed. Include every acceptor exactly once, including the local
  node. Do not use aliases for the same server or change membership while running.
- Acceptors grant at most 30 seconds. Proposers own at most 29 seconds from the
  phase-two **send** time, and attempt renewal every 10 seconds. Time is checked
  lazily using `System.nanoTime`; delayed timer execution does not prolong a lease.
- An acceptor never replaces an unexpired lease with another owner's lease,
  even for a higher ballot. The current owner can renew. This deliberately
  conservative rule can delay failover until lease expiry.
- Expired, cancelled, duplicate, nonmember and prior-ballot replies cannot grant
  ownership. Responses are attributed to configured connections, not claimed IDs.
- All election attempts share one scheduler. Stop and application shutdown revoke
  local ownership and release sockets, scheduler threads and the ballot file lock.

## Tests

`./mvnw verify` runs:

- Three-node continuous renewal beyond multiple lease intervals.
- Five-node majority/minority partition and takeover after expiry.
- Majority restart while the former leader's lease is still valid.
- Delayed phase-two responses after a successor has won.
- Duplicate, stale and nonmember votes, and idempotent Prepare retries.
- Durable unique ballots, jumping over observed promises, concurrent Prepare
  processing, rejection of stale Accepts and conflicting live leases.
- Three embedded Spring Boot/Tomcat servers over real WebSocket connections:
  election, repeated renewal with forced disconnections, reconnect and takeover.

Protocol tests inject monotonic time; socket tests also control lease time but use
real loopback sockets and bounded waits for asynchronous message delivery. They
are regression tests, not an exhaustive model check or a multi-machine soak test.

## Assumptions and limits

The cluster is trusted, non-Byzantine and has fixed membership. Nodes must have
monotonic clocks whose rates stay within +/-1% of elapsed real time; the 1-second
owner margin covers this relative drift bound. Clock suspension, duplicate node
identities and inconsistent membership are outside this guarantee. Node WebSocket upgrades require a node bearer token; management endpoints require
a separate admin bearer token. See SECURITY.md for TLS and rotation requirements.

Applications must check `State.isLeaseOwner()` before each operation. A boolean
check cannot stop an operation already paused or in flight: production writes
also need fencing enforced by the destination system. The optional JDBC reference store adds fencing for its own business writes; see
FENCING.md. Other destinations need their own fencing. This is not a general
production-readiness guarantee.
The application uses Java 17, Spring Boot 3.5.16, Spring Security and the Boot-managed
Jackson serializer; Fastjson and unused legacy dependencies have been removed. The refreshed script-only Maven Wrapper downloads Maven 3.9.9 without a wrapper JAR;
CI uses the same wrapper. An installed Maven 3.6.3+ is also supported.

## Follow-up safety review

See [SAFETY.md](SAFETY.md) for the quorum-intersection argument, the clock-drift
inequalities and the distinction between lease ownership and fenced writes.
Additional regressions cover opposite clock rates, recovery under drift,
phase-two retransmission deadlines, blocked minority sends and slow quorum
confirmations. Networking uses one bounded ordered worker per peer; renewal is
scheduled relative to the original phase-two timestamp rather than confirmation.

Optional shared JDBC business persistence and its PostgreSQL CI tests are
documented in [FENCING.md](FENCING.md). It is disabled by default.
