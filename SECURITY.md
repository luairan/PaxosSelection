# Authentication and dependency migration

## Credentials (required)

The application refuses to start without two distinct non-whitespace secrets of
at least 32 visible ASCII characters. There are no fallback/demo credentials. Generate strong
random values, for example:

```sh
export PAXOS_NODE_TOKEN="$(openssl rand -hex 32)"
export PAXOS_ADMIN_TOKEN="$(openssl rand -hex 32)"
```

Provide the **same node token** to all fixed cluster members through your secret
manager or environment. Keep the admin token separate; it may differ per node.
Do not commit tokens or put them in URLs, logs, command-line application arguments,
or shell startup files. The length check is not an entropy guarantee: use random
secrets, not sentences. Authorization compares bytes using constant-time equality.

| Endpoint | Method | Credential |
| --- | --- | --- |
| `/paxos` WebSocket upgrade | GET upgrade | Node bearer token |
| `/show/showMater` | GET | Admin bearer token |
| `/show/start` | POST | Admin bearer token |
| `/show/stop` | POST | Admin bearer token |
| `/business/entries/{key}` (optional) | GET / PUT | Admin bearer token |

Missing/invalid credentials return 401; valid credentials for the wrong role
return 403. Node credentials cannot manage elections; admin credentials cannot
participate in the protocol. Other routes are denied. The old mutating GET is
removed (authenticated callers receive 405). Status exposes a small explicit DTO,
not internal State, file handles or credentials.

The Java WebSocket client adds `Authorization: Bearer ...` to the handshake header.
Tokens in query parameters or cookies are not supported. Spring Security sessions,
form login and HTTP Basic are not used. CSRF protection is disabled for this
header-only stateless API: browsers do not automatically attach bearer headers.
WebSocket cross-origin requests are rejected by the default same-origin policy.
This shared node credential assumes trusted members; it does not implement
per-node cryptographic identities or Byzantine fault tolerance.

## Transport and lifecycle

The default HTTP server binds only to 127.0.0.1 for the local demonstration.
Plain `ws://` peer URLs are allowed only for literal loopback addresses or
`localhost`; other configured peers must use `wss://`. For remote deployment,
configure HTTPS certificates and a bind address using Spring Boot's standard
`server.ssl.*`/`server.address` settings. Use trusted certificates; never disable
TLS verification. Management callers must also use HTTPS on remote networks.
Client WebSocket redirects are disabled, including same-host redirects. A 3xx
response fails the connection instead of replaying the Authorization header to a
new address or downgrading TLS. Configure each peer's final WebSocket URL directly.
There is no redirect-based authentication and secrets are not sent in URLs.

Tokens are loaded at startup. Rotate by stopping all members, waiting for old
leases to expire, updating secrets, then restarting the cluster. Existing
WebSocket sessions are authenticated at upgrade; changing environment variables
does not revoke an established connection. This release does not offer live
rotation, fine-grained admin accounts, rate limiting or mutual TLS.

## Dependency/runtime migration

- Java 8 -> Java 17; Spring Boot 1.3.7 -> 3.5.16 (a stable 3.5 release, not a
  claim to use the newest major release). Spring-managed versions align Tomcat,
  Jackson, Spring Framework and Spring Security.
- `javax.annotation`/`javax.websocket` -> Jakarta APIs.
- Fastjson is replaced with Jackson serialization into explicit DTO classes;
  default/polymorphic typing is not enabled. Unknown protocol properties are
  rejected, including `@type`.
- Remove unused Velocity, SOAP starter, Java-WebSocket and dom4j dependencies,
  plus the unreferenced XML/native-object deserialization helper classes.
- Refresh Maven Wrapper to script-only 3.3.4 with Maven 3.9.9; no missing wrapper JAR.
- Preserve the JUnit 4 protocol regressions through the Vintage engine and add
  JUnit 5 security tests. CI builds and runs all tests with Java 17.

Upgrade all members together. Old processes do not supply authentication headers
and cannot connect to new nodes. Follow the lease waiting period in DEVELOPMENT.md.
This upgrade is not a guarantee that all dependencies are vulnerability-free;
continue updating managed patch versions and monitoring security advisories.

## Security review regression

Tomcat's default redirect handling reuses the endpoint configurator. Before the
explicit zero-redirect limit, a configured peer returning 301/302/303/307 could
cause the node token to be sent to another destination. Four real HTTP-server
regressions reproduce the leak before the fix and verify that the redirect target
receives **no request at all** afterward. The original configured peer still
receives its expected Authorization header. These tests exercise the packaged
Tomcat client so that future dependency changes cannot silently undo the policy.

The optional shared JDBC business API uses the same admin role. Its database
credentials are separate from node/admin tokens; see FENCING.md for deployment
and storage authorization requirements.
