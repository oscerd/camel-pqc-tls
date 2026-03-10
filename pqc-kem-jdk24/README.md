# Post-Quantum TLS Handshake with BouncyCastle JSSE on JDK 24

This example demonstrates a fully working Post-Quantum Cryptography (PQC) TLS 1.3 handshake using the **X25519MLKEM768** hybrid key exchange on **JDK 24**, powered by BouncyCastle's JSSE provider (BCJSSE). No JDK 27 required.

## What This Example Proves

At startup, the application performs a self-contained TLS handshake where:

1. An EC key pair and self-signed certificate are **generated at runtime** using BouncyCastle's API
2. An **SSLServerSocket** is created using BCJSSE with default named groups (which include `X25519MLKEM768`)
3. An **SSLSocket** client connects offering `X25519MLKEM768` for PQC key exchange and `secp256r1` for ECDSA authentication
4. If the TLS 1.3 handshake **succeeds**, PQC key exchange was definitively negotiated

No external keystore or certificate files are needed - everything is self-contained.

## JDK 24 and PQC

JDK 24 sits at an interesting point in the PQC timeline:

- **ML-KEM as a standalone primitive** - JDK 24 includes native ML-KEM support via the `javax.crypto.KEM` API ([JEP 496](https://openjdk.org/jeps/496)), meaning the KEM building blocks are available natively
- **No PQC in TLS** - JDK 24's SunJSSE does not support PQC named groups like `X25519MLKEM768` in TLS handshakes (that requires JDK 27 / [JEP 527](https://openjdk.org/jeps/527))
- **BCJSSE bridges the gap** - BouncyCastle's JSSE provider brings PQC TLS support to JDK 24, using its own ML-KEM implementation within the TLS stack

The `/api/verify-kem` endpoint directly exercises JDK 24's native `javax.crypto.KEM` API with ML-KEM-768, running encapsulate/decapsulate cycles with both the JDK native (SunJCE) and BouncyCastle providers, including cross-provider interoperability tests.

## How It Works

### X25519MLKEM768 Hybrid Key Exchange

`X25519MLKEM768` is a post-quantum hybrid key exchange that combines:

- **X25519** - classical elliptic curve Diffie-Hellman (well-proven security)
- **ML-KEM-768** - post-quantum lattice-based key encapsulation ([NIST FIPS 203](https://csrc.nist.gov/pubs/fips/203))

Both algorithms run together. Security is maintained even if one is broken.

### Named Groups in TLS 1.3

In TLS 1.3, the `supported_groups` extension serves two purposes:

- **Key exchange** - determines the key agreement mechanism (e.g., `X25519MLKEM768` for PQC hybrid)
- **ECDSA authentication** - determines which EC curves are supported for signature verification (e.g., `secp256r1`)

The client in this example offers both `X25519MLKEM768` (for PQC key exchange) and `secp256r1` (for ECDSA signature verification of the server's EC certificate). The key exchange will use X25519MLKEM768, proving PQC was negotiated.

### Application Bootstrap

The `PQCSSLContextApplication.java` bootstrap class performs two critical setup steps before starting the Camel context:

1. **Removes `ECDH` from `jdk.tls.disabledAlgorithms`** - BCJSSE interprets this broadly, preventing EC credentials from being used during TLS handshakes.

2. **Registers BouncyCastle providers** - BC is registered at low priority (end of provider list) so JDK's SUN/SunJCE remain preferred for standard operations, while BCJSSE is inserted at position 1 to handle all TLS operations.

### Credential Generation

Instead of loading certificates from external files, this example generates everything programmatically at runtime:

1. **EC key pair** via `KeyPairGenerator.getInstance("EC", bcProvider)` with `secp256r1`
2. **Self-signed X.509 certificate** via BouncyCastle's `JcaX509v3CertificateBuilder` signed with `SHA256withECDSA`
3. **In-memory JKS keystores** for both the keystore and truststore

## Prerequisites

- **JDK 24** or later
- **Maven 3.9+**
- **Apache Camel 4.19.0-SNAPSHOT** installed in local Maven repository

## Project Structure

```
pqc-kem-jdk24/
├── pom.xml
├── README.md
└── src/main/
    ├── java/org/apache/camel/example/pqc/
    │   └── PQCSSLContextApplication.java
    └── resources/
        ├── application.properties
        ├── log4j2.properties
        └── camel/
            └── pqc-ssl-context.camel.yaml
```

### Key Files

| File | Description |
|------|-------------|
| `PQCSSLContextApplication.java` | Bootstrap class. Removes ECDH from disabled algorithms, registers BC and BCJSSE providers, starts Camel. |
| `pqc-ssl-context.camel.yaml` | Camel routes (YAML DSL). Contains the PQC TLS handshake logic in Groovy scripts. |
| `application.properties` | Camel and HTTP server configuration (port 8080). |

### Routes

| Route | Endpoint | Description |
|-------|----------|-------------|
| `pqc-verify` | `timer://pqc-verify` | Runs once at startup. Generates EC credentials via BC, performs a PQC TLS 1.3 handshake with X25519MLKEM768, and logs the result. |
| `pqc-secure-server` | `GET /api/data` | Returns PQC status information as JSON. |
| `pqc-verify-endpoint` | `GET /api/verify-pqc` | Runs a fresh PQC TLS handshake on demand and returns the result. |
| `ml-kem-verify` | `GET /api/verify-kem` | Runs ML-KEM encapsulate/decapsulate using JDK 24 native KEM API and BouncyCastle, with cross-provider interoperability tests. |
| `pqc-ssl-info` | `GET /api/ssl-info` | Returns BouncyCastle provider details, native KEM support, available PQC named groups, enabled protocols and cipher suites. |

## Step-by-Step Guide

### Step 1: Select JDK 24

```sh
sdk use java 24.0.1-tem
java -version
```

### Step 2: Build and Run

```sh
mvn clean compile exec:exec
```

No certificate generation step is needed - credentials are generated programmatically at runtime.

### Step 3: Verify PQC in the Logs

On successful startup, you should see:

```
=== Post-Quantum TLS Handshake Verification (BouncyCastle JSSE on JDK 24) ===
PQC Verification Result:
{
    "pqcVerified": true,
    "tlsProvider": "BouncyCastle JSSE (BCJSSE)",
    "jdkVersion": "24.0.1",
    "server": {
        "protocol": "TLSv1.3",
        "cipherSuite": "TLS_CHACHA20_POLY1305_SHA256"
    },
    "client": {
        "protocol": "TLSv1.3",
        "cipherSuite": "TLS_CHACHA20_POLY1305_SHA256",
        "namedGroupsOffered": "X25519MLKEM768 (PQC key exchange) + secp256r1 (ECDSA auth)"
    },
    "message": "PQC VERIFIED: TLS 1.3 handshake with X25519MLKEM768 succeeded on JDK 24.0.1 using BouncyCastle JSSE"
}
```

### Step 4: Query the REST Endpoints

**PQC verification on demand:**

```sh
curl http://localhost:8080/api/verify-pqc
```

**ML-KEM key encapsulation using JDK 24 native KEM API (with cross-provider tests):**

```sh
curl http://localhost:8080/api/verify-kem
```

```json
{
    "kemVerified": true,
    "jdkVersion": "24.0.1",
    "algorithm": "ML-KEM-768 (FIPS 203)",
    "tests": [
        {
            "test": "JDK native -> JDK native",
            "provider": "SunJCE",
            "algorithm": "ML-KEM-768",
            "secretsMatch": true
        },
        {
            "test": "BouncyCastle -> BouncyCastle",
            "provider": "BC",
            "algorithm": "ML-KEM-768",
            "secretsMatch": true
        },
        {
            "test": "JDK keygen -> BC encapsulate/decapsulate",
            "secretsMatch": true
        },
        {
            "test": "BC keygen -> JDK encapsulate/decapsulate",
            "secretsMatch": true
        }
    ],
    "message": "ALL TESTS PASSED: ML-KEM works with both JDK native and BouncyCastle providers"
}
```

This endpoint exercises JDK 24's native `javax.crypto.KEM` API — the same ML-KEM-768 primitive that underpins the `X25519MLKEM768` TLS key exchange.

**SSL configuration info (providers, PQC named groups, native KEM support):**

```sh
curl http://localhost:8080/api/ssl-info
```

**Data endpoint:**

```sh
curl http://localhost:8080/api/data
```

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `bcprov-jdk18on` | 1.83 | BouncyCastle crypto provider (EC key generation, signing) |
| `bctls-jdk18on` | 1.83 | BouncyCastle TLS/JSSE provider (PQC TLS support) |
| `bcpkix-jdk18on` | 1.83 | BouncyCastle PKI support (X.509 certificate building) |
| `bcutil-jdk18on` | 1.83 | BouncyCastle utility classes |
| `camel-main` | 4.19.0-SNAPSHOT | Camel standalone runtime |
| `camel-yaml-dsl` | 4.19.0-SNAPSHOT | YAML route definitions |
| `camel-platform-http-main` | 4.19.0-SNAPSHOT | Embedded Vert.x HTTP server |
| `camel-groovy` | 4.19.0-SNAPSHOT | Groovy scripting in routes |
| `camel-timer` | 4.19.0-SNAPSHOT | Timer component for startup route |

## JDK Compatibility

| JDK Version | ML-KEM KEM API | PQC in TLS | Approach |
|-------------|---------------|-----------|----------|
| **21** | No | **Yes (via BouncyCastle)** | [`pqc-ssl-context-jdk21`](../pqc-ssl-context-jdk21/) - BCJSSE provides PQC TLS support |
| **24** | **Yes (JEP 496)** | **Yes (via BouncyCastle)** | **This example** - BCJSSE for TLS, JDK has native ML-KEM primitives |
| **27** | Yes | **Yes (native)** | [`pqc-ssl-context`](../pqc-ssl-context/) - JEP 527 adds PQC to SunJSSE |

## Related Examples

- [`pqc-ssl-context`](../pqc-ssl-context/) - PQC TLS using JDK 27's native support (JEP 527)
- [`pqc-ssl-context-jdk21`](../pqc-ssl-context-jdk21/) - PQC TLS using BouncyCastle JSSE on JDK 21

## Help and Contributions

If you hit any problem using Camel or have some feedback, then please
[let us know](https://camel.apache.org/community/support/).

We also love contributors, so
[get involved](https://camel.apache.org/community/contributing/) :-)

The Camel riders!
