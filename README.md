# Post-Quantum TLS with Apache Camel

This repository contains three examples demonstrating **Post-Quantum Cryptography (PQC) TLS 1.3 handshakes** with Apache Camel, using the **X25519MLKEM768** hybrid key exchange.

`X25519MLKEM768` combines classical **X25519** elliptic curve Diffie-Hellman with **ML-KEM-768**, a post-quantum lattice-based key encapsulation mechanism ([NIST FIPS 203](https://csrc.nist.gov/pubs/fips/203)). Both algorithms run together, so security is maintained even if one is broken.

## Examples

| Example | JDK | TLS Provider | Approach |
|---------|-----|-------------|----------|
| [`pqc-ssl-context`](pqc-ssl-context/) | **JDK 27** | SunJSSE (JDK native) | JEP 527 adds PQC named groups to the JDK's built-in TLS stack |
| [`pqc-kem-jdk24`](pqc-kem-jdk24/) | **JDK 24** | BouncyCastle JSSE 1.83 | BCJSSE for PQC TLS, JDK has native ML-KEM primitives via JEP 496 |
| [`pqc-ssl-context-jdk21`](pqc-ssl-context-jdk21/) | **JDK 21** | BouncyCastle JSSE 1.83 | BCJSSE replaces SunJSSE to provide PQC TLS support today |

All three examples perform the same fundamental operation: a self-contained TLS 1.3 handshake using `X25519MLKEM768` for post-quantum key exchange, verified at startup and available on demand via REST endpoints.

## How They Differ

### JDK 27 Native (`pqc-ssl-context`)

Uses the JDK's built-in SunJSSE provider, which gains PQC TLS support through [JEP 527](https://openjdk.org/jeps/527) in JDK 27.

- PQC configured via standard Camel SSL properties (`camel.ssl.namedGroups=X25519MLKEM768,x25519`)
- Certificates pre-generated via `keytool` and a shell script (`generate-certs.sh`)
- REST endpoints served over **HTTPS** on port 8443
- Named groups set through `SSLParameters.setNamedGroups()` (standard JDK API)

### JDK 24 with BouncyCastle (`pqc-kem-jdk24`)

Uses BouncyCastle's JSSE provider (BCJSSE) to bring PQC TLS support to JDK 24. While JDK 24 includes native ML-KEM as a standalone crypto primitive ([JEP 496](https://openjdk.org/jeps/496)), it does not expose PQC named groups in its TLS stack. BCJSSE bridges that gap.

- PQC configured via BouncyCastle's `BCSSLParameters.setNamedGroups()` API
- Certificates **generated programmatically at runtime** using BouncyCastle's crypto API
- REST endpoints served over **HTTP** on port 8080
- The `/api/ssl-info` endpoint also reports JDK 24's native KEM provider registrations

### JDK 21 with BouncyCastle (`pqc-ssl-context-jdk21`)

Uses BouncyCastle's JSSE provider (BCJSSE) to bring PQC TLS support to JDK 21, without waiting for JDK 27.

- PQC configured via BouncyCastle's `BCSSLParameters.setNamedGroups()` API
- Certificates **generated programmatically at runtime** using BouncyCastle's crypto API (no external files needed)
- REST endpoints served over **HTTP** on port 8080 (the PQC handshake is a self-contained internal test)
- Requires a bootstrap class (`PQCSSLContextApplication.java`) to remove `ECDH` from `jdk.tls.disabledAlgorithms` and register BouncyCastle providers

### Side-by-Side Comparison

| Aspect | JDK 27 Native | JDK 24 + BouncyCastle | JDK 21 + BouncyCastle |
|--------|--------------|----------------------|----------------------|
| JDK requirement | JDK 27 EA | JDK 24+ | JDK 21+ |
| TLS provider | SunJSSE | BCJSSE 1.83 | BCJSSE 1.83 |
| PQC mechanism | JEP 527 (built-in) | BouncyCastle TLS library | BouncyCastle TLS library |
| Native ML-KEM (KEM API) | Yes | Yes (JEP 496) | No |
| Configuration | `camel.ssl.*` properties | `BCSSLParameters` in Groovy scripts | `BCSSLParameters` in Groovy scripts |
| Certificate management | `keytool` + shell script | Programmatic (BC API) | Programmatic (BC API) |
| Named groups API | `SSLParameters.setNamedGroups()` | `BCSSLParameters.setNamedGroups()` | `BCSSLParameters.setNamedGroups()` |
| REST port | 8443 (HTTPS) | 8080 (HTTP) | 8080 (HTTP) |
| Extra dependencies | None (JDK native) | `bcprov`, `bctls`, `bcpkix`, `bcutil` | `bcprov`, `bctls`, `bcpkix`, `bcutil` |

## Prerequisites

- **Maven 3.9+**
- **Apache Camel 4.19.0-SNAPSHOT** installed in local Maven repository
- **JDK 27 EA** for the native example, **JDK 24** for the JDK 24 example, or **JDK 21+** for the JDK 21 example

## Quick Start

### JDK 27 example

```sh
cd pqc-ssl-context
./generate-certs.sh
sdk use java 27.ea.11-open
mvn clean compile exec:exec
curl -k https://localhost:8443/api/verify-pqc
```

### JDK 24 example

```sh
cd pqc-kem-jdk24
sdk use java 24.0.1-tem
mvn clean compile exec:exec
curl http://localhost:8080/api/verify-pqc
```

### JDK 21 example

```sh
cd pqc-ssl-context-jdk21
sdk use java 21.0.10-tem
mvn clean compile exec:exec
curl http://localhost:8080/api/verify-pqc
```

All three will return `"pqcVerified": true` when the PQC TLS handshake succeeds.

## JDK Compatibility

| JDK Version | ML-KEM KEM API | PQC in TLS | Approach |
|-------------|---------------|-----------|----------|
| **21** | No | Yes (via BouncyCastle) | `pqc-ssl-context-jdk21` - BCJSSE provides PQC TLS support |
| **24** | Yes (JEP 496) | Yes (via BouncyCastle) | `pqc-kem-jdk24` - BCJSSE for TLS, JDK has native ML-KEM primitives |
| **27** | Yes | Yes (native) | `pqc-ssl-context` - JEP 527 adds PQC to SunJSSE |

## Help and Contributions

If you hit any problem using Camel or have some feedback, then please
[let us know](https://camel.apache.org/community/support/).

We also love contributors, so
[get involved](https://camel.apache.org/community/contributing/) :-)

The Camel riders!
