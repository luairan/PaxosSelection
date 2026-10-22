# Lease election: configuration and verification

This patch changes the protocol messages and node identity handling. Upgrade all
members together, after stopping the previous cluster and waiting at least the
maximum lease interval. Do not mix old and new binaries.

## Run a three-node cluster

Build with JDK 8 and Maven: `mvn clean verify`. Run each command in a separate
terminal; the list of acceptors must be identical on every node:

```sh
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar --server.port=8081 --paxos.node-id=1 --paxos.data-dir=./data/node1 --paxos.peers=ws://localhost:8081/paxos,ws://localhost:8082/paxos,ws://localhost:8083/paxos
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar --server.port=8082 --paxos.node-id=2 --paxos.data-dir=./data/node2 --paxos.peers=ws://localhost:8081/paxos,ws://localhost:8082/paxos,ws://localhost:8083/paxos
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar --server.port=8083 --paxos.node-id=3 --paxos.data-dir=./data/node3 --paxos.peers=ws://localhost:8081/paxos,ws://localhost:8082/paxos,ws://localhost:8083/paxos
```

Start each candidate with `curl http://localhost:8081/show/start` (and ports
8082/8083). Inspect `http://localhost:8081/show/showMater`. Starting twice is
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

`mvn verify` runs:

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
identities and inconsistent membership are outside this guarantee. Use an
isolated trusted network: the demo endpoints have no peer authentication.

Applications must check `State.isLeaseOwner()` before each operation. A boolean
check cannot stop an operation already paused or in flight: production writes
also need fencing enforced by the destination system. This patch does not add
storage fencing or turn the old Spring Boot demo into a production-ready service.
The existing dependency versions are retained to keep the change focused; build
with Java 8. The Maven wrapper JAR is absent from the original repository, so use
an installed Maven (CI installs Java and uses the runner's Maven).
