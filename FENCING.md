# Fenced business writes

The original project had no business persistence layer. This feature adds an
**optional shared JDBC key/value example**, with PostgreSQL as the deployment
target. It does not claim to fence arbitrary files, emails, HTTP calls or other
systems that do not participate in the same storage protocol.

## Responsibility boundaries

- `LeaseGrant`: immutable identity of a **confirmed** successful lease. State uses
  this one object as the source of truth for ownership and its deadline; an
  unconfirmed next-election ballot is never exposed as a write generation.
- `FencedWriteService`: captures the lease, activates its generation in storage,
  rechecks the exact lease after possibly blocking I/O, then performs the write.
- `FencedStore`: the resource contract. `JdbcFencedStore` implements activation and
  mutation using database transactions; the HTTP controller does no locking or
  token comparison and never accepts a caller-supplied fencing token.

## Storage invariant and paused writers

Each namespace has a durable `business_fence` row containing the highest activated
winning ballot and its owner. Activation locks that row with `SELECT ... FOR
UPDATE`; it may raise the fence or repeat the same token/owner, never lower it or
reassign an equal token to another owner. A write locks the **same row**, requires
an exact namespace/token/owner match, and mutates business data before committing.
The lock is held through the business commit, not just through validation.
The store owns REQUIRES_NEW transactions: activation is committed before a permit
is returned and cannot be undone by an unrelated caller transaction rollback.
This single-entry API does not compose atomically with a surrounding transaction.

Therefore an old operation has two possible orderings relative to takeover:

1. It already holds the guard lock. Its mutation commits (or rolls back) before
   the next generation can activate. The next generation can then write.
2. The successor has activated first. The old operation sees the newer guard and
   fails without changing data, even if it passed a local lease check long ago.

The guarantee starts at **storage activation**, not merely at a Paxos election.
Activation happens before the new generation's first business write. Before that
activation an old in-flight operation may finish. Once the new activation commits,
no older permit can commit through this store. A fencing token is an ordering
credential, not a secret or an authentication mechanism.

A slow, expired activation is checked again before its permit is exposed. It may
raise the durable fence and temporarily prevent another writer from progressing,
but cannot expose a valid stale write permit. If recovered acceptors elect a
ballot below the persisted storage high-water mark, writes fail closed and the
observed mark feeds the next election counter; the store is never reset to make
an old ballot work. Renewal also creates a new generation once activated.

## Enable with one shared PostgreSQL database

Business persistence is disabled by default, so existing election-only processes
need no database. All cluster members must use the **same database and namespace**.
Separate per-node databases provide no cross-node fencing.

Apply `src/main/resources/db/fencing.sql` once using a migration/admin account.
Then supply credentials using a secret manager or environment:

```sh
export BUSINESS_JDBC_URL='jdbc:postgresql://db.example:5432/business?sslmode=verify-full'
export BUSINESS_DB_USER='paxos_writer'
# Supply BUSINESS_DB_PASSWORD securely; do not commit it or place it in the JDBC URL.
java -jar target/paxos-selection-0.0.1-SNAPSHOT.jar \
  --paxos.business.enabled=true --paxos.business.namespace=my-cluster
```

The existing node/admin tokens and HTTPS/WSS requirements still apply. The
runtime database account needs SELECT/INSERT/UPDATE on these two tables; do not
grant DELETE, TRUNCATE or DDL privileges. Guard updates must go through this store.
SQL permissions alone do not enforce monotonic UPDATE: direct SQL writers or an
attacker holding database credentials can bypass this adapter and are outside
the guarantee. All cooperative writers must use the guarded transaction protocol.
For a disposable demo only, `--paxos.business.initialize-schema=true` creates the
tables automatically. Production should use explicit migrations and leave this
option false. Enabling the feature with no reachable/schema-ready database fails
startup instead of falling back to memory. The JDBC driver is Boot-managed;
H2 is test-only and is not packaged as a deployment database.

```sh
# Send these to the current leader; use HTTPS remotely.
curl -X PUT -H "Authorization: Bearer $PAXOS_ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -d '{"value":"paid"}' \
  http://localhost:8081/business/entries/order-1
curl -H "Authorization: Bearer $PAXOS_ADMIN_TOKEN" \
  http://localhost:8081/business/entries/order-1
```

Successful writes return 204. Reads return 200/404 and do not require leadership.
Missing credentials return 401; node credentials return 403. No current lease,
a superseded fence or leadership change during preparation returns 409; retry on
the leader after a new election. Invalid entries return 400. Storage failures
return 503 without exposing SQL or credentials. Keys are limited to 128 characters
and values to 4096 characters. Operations replace one value; they do not provide
exactly-once processing or external-side-effect idempotency.

## Durability and operational limits

Keep the namespace stable and never reset the guard while old writers exist.
Database recovery/failover must preserve committed high-water marks and data; a
backup restore that rolls fencing state backward invalidates this guarantee.
The single guard row intentionally serializes writes in a namespace. For higher
throughput, design separate independently fenced resources or batching; do not
remove the transaction lock. Lease assumptions in SAFETY.md still apply.

## Tests

Local `./mvnw clean verify` runs shared-connection H2 regressions, a durable file
restart test, a three-node election/paused-old-writer test and real HTTP/JDBC
authentication tests. A race test stops a write exactly between fence validation
and mutation: activation must remain blocked until the old transaction commits.
Mutation failure is checked for rollback and lock release.

CI additionally starts a **real PostgreSQL 17** service and executes the same race
contract plus a persistent high-water/restarted-repository test. Locally these two
tests are skipped unless `FENCING_TEST_JDBC_URL`, `FENCING_TEST_DB_USER`, and
`FENCING_TEST_DB_PASSWORD` identify a disposable PostgreSQL test database. They
create tables and test namespaces, so never point them at production.
