# Post-Quantum TLS with Apache Camel

This repository contains two examples demonstrating **Post-Quantum Cryptography (PQC) TLS 1.3 handshakes** with Apache Camel, using the **X25519MLKEM768** hybrid key exchange.

`X25519MLKEM768` combines classical **X25519** elliptic curve Diffie-Hellman with **ML-KEM-768**, a post-quantum lattice-based key encapsulation mechanism ([NIST FIPS 203](https://csrc.nist.gov/pubs/fips/203)). Both algorithms run together, so security is maintained even if one is broken.

## Examples

| Example | JDK | TLS Provider | Approach |
|---------|-----|-------------|----------|
| [`pqc-ssl-context`](pqc-ssl-context/) | **JDK 27** | SunJSSE (JDK native) | JEP 527 adds PQC named groups to the JDK's built-in TLS stack |
| [`pqc-ssl-context-jdk21`](pqc-ssl-context-jdk21/) | **JDK 21** | BouncyCastle JSSE 1.83 | BCJSSE replaces SunJSSE to provide PQC TLS support today |

Both examples perform the same fundamental operation: a self-contained TLS 1.3 handshake using `X25519MLKEM768` for post-quantum key exchange, verified at startup and available on demand via REST endpoints.

## How They Differ

### JDK 27 Native (`pqc-ssl-context`)

Uses the JDK's built-in SunJSSE provider, which gains PQC TLS support through [JEP 527](https://openjdk.org/jeps/527) in JDK 27.

- PQC configured via standard Camel SSL properties (`camel.ssl.namedGroups=X25519MLKEM768,x25519`)
- Certificates pre-generated via `keytool` and a shell script (`generate-certs.sh`)
- REST endpoints served over **HTTPS** on port 8443
- Named groups set through `SSLParameters.setNamedGroups()` (standard JDK API)

### JDK 21 with BouncyCastle (`pqc-ssl-context-jdk21`)

Uses BouncyCastle's JSSE provider (BCJSSE) to bring PQC TLS support to JDK 21, without waiting for JDK 27.

- PQC configured via BouncyCastle's `BCSSLParameters.setNamedGroups()` API
- Certificates **generated programmatically at runtime** using BouncyCastle's crypto API (no external files needed)
- REST endpoints served over **HTTP** on port 8080 (the PQC handshake is a self-contained internal test)
- Requires a bootstrap class (`PQCSSLContextApplication.java`) to remove `ECDH` from `jdk.tls.disabledAlgorithms` and register BouncyCastle providers

### Side-by-Side Comparison

| Aspect | JDK 27 Native | JDK 21 + BouncyCastle |
|--------|--------------|----------------------|
| JDK requirement | JDK 27 EA | JDK 21+ |
| TLS provider | SunJSSE | BCJSSE 1.83 |
| PQC mechanism | JEP 527 (built-in) | BouncyCastle TLS library |
| Configuration | `camel.ssl.*` properties | `BCSSLParameters` in Groovy scripts |
| Certificate management | `keytool` + shell script | Programmatic (BC API, in-memory keystores) |
| Named groups API | `SSLParameters.setNamedGroups()` | `BCSSLParameters.setNamedGroups()` |
| Available PQC groups | `X25519MLKEM768` | `X25519MLKEM768`, `SecP256r1MLKEM768`, `SecP384r1MLKEM1024`, `MLKEM512`, `MLKEM768`, `MLKEM1024` |
| REST port | 8443 (HTTPS) | 8080 (HTTP) |
| Extra dependencies | None (JDK native) | `bcprov`, `bctls`, `bcpkix`, `bcutil` |
| Provider setup | None | Remove ECDH from disabled algorithms, register BC/BCJSSE |

## Prerequisites

- **Maven 3.9+**
- **Apache Camel 4.19.0-SNAPSHOT** installed in local Maven repository
- **JDK 27 EA** for the native example, or **JDK 21+** for the BouncyCastle example

## Quick Start

### JDK 27 example

```sh
cd pqc-ssl-context
./generate-certs.sh
sdk use java 27.ea.11-open
mvn clean compile exec:exec
curl -k https://localhost:8443/api/verify-pqc
```

### JDK 21 example

```sh
cd pqc-ssl-context-jdk21
sdk use java 21.0.10-tem
mvn clean compile exec:exec
curl http://localhost:8080/api/verify-pqc
```

Both will return `"pqcVerified": true` when the PQC TLS handshake succeeds.

## JDK Compatibility

| JDK Version | PQC in TLS | Approach |
|-------------|-----------|----------|
| **21** | Yes (via BouncyCastle) | `pqc-ssl-context-jdk21` - BCJSSE provides PQC TLS support |
| 24 | No (native) | `ML-KEM` available as standalone KEM, but not in TLS |
| **27** | Yes (native) | `pqc-ssl-context` - JEP 527 adds PQC to SunJSSE |

## Help and Contributions

If you hit any problem using Camel or have some feedback, then please
[let us know](https://camel.apache.org/community/support/).

We also love contributors, so
[get involved](https://camel.apache.org/community/contributing/) :-)

The Camel riders!
