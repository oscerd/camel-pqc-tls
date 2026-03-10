# Post-Quantum TLS Handshake with BouncyCastle JSSE on JDK 21

This example demonstrates a fully working Post-Quantum Cryptography (PQC) TLS 1.3 handshake using the **X25519MLKEM768** hybrid key exchange on **JDK 21**, powered by BouncyCastle's JSSE provider (BCJSSE). No JDK 27 required.

## What This Example Proves

At startup, the application performs a self-contained TLS handshake where:

1. An EC key pair and self-signed certificate are **generated at runtime** using BouncyCastle's API
2. An **SSLServerSocket** is created using BCJSSE with default named groups (which include `X25519MLKEM768`)
3. An **SSLSocket** client connects offering `X25519MLKEM768` for PQC key exchange and `secp256r1` for ECDSA authentication
4. If the TLS 1.3 handshake **succeeds**, PQC key exchange was definitively negotiated

No external keystore or certificate files are needed - everything is self-contained.

## How It Works

### X25519MLKEM768 Hybrid Key Exchange

`X25519MLKEM768` is a post-quantum hybrid key exchange that combines:

- **X25519** - classical elliptic curve Diffie-Hellman (well-proven security)
- **ML-KEM-768** - post-quantum lattice-based key encapsulation ([NIST FIPS 203](https://csrc.nist.gov/pubs/fips/203))

Both algorithms run together. Security is maintained even if one is broken.

### BouncyCastle JSSE vs JDK Native TLS

| Aspect | This Example (BCJSSE) | pqc-ssl-context (JDK Native) |
|--------|----------------------|----------------------------|
| JDK Requirement | **JDK 21** | JDK 27 |
| TLS Provider | BouncyCastle JSSE 1.83 | SunJSSE (JEP 527) |
| PQC Named Groups | `X25519MLKEM768`, `SecP256r1MLKEM768`, `MLKEM768`, ... | `X25519MLKEM768` |
| Configuration | `BCSSLParameters.setNamedGroups()` | `SSLParameters.setNamedGroups()` |
| Certificates | Generated at runtime via BC | Pre-generated via `keytool` |

### Named Groups in TLS 1.3

In TLS 1.3, the `supported_groups` extension serves two purposes:

- **Key exchange** - determines the key agreement mechanism (e.g., `X25519MLKEM768` for PQC hybrid)
- **ECDSA authentication** - determines which EC curves are supported for signature verification (e.g., `secp256r1`)

The client in this example offers both `X25519MLKEM768` (for PQC key exchange) and `secp256r1` (for ECDSA signature verification of the server's EC certificate). The key exchange will use X25519MLKEM768, proving PQC was negotiated.

### Application Bootstrap

The `PQCSSLContextApplication.java` bootstrap class performs two critical setup steps before starting the Camel context:

1. **Removes `ECDH` from `jdk.tls.disabledAlgorithms`** - JDK 21 includes raw `ECDH` in the disabled algorithms list, which BCJSSE interprets broadly, preventing EC credentials from being used during TLS handshakes.

2. **Registers BouncyCastle providers** - BC is registered at low priority (end of provider list) so JDK's SUN/SunJCE remain preferred for standard operations (PKCS12, PBE), while BCJSSE is inserted at position 1 to handle all TLS operations.

### Credential Generation

Instead of loading certificates from external files, this example generates everything programmatically at runtime:

1. **EC key pair** via `KeyPairGenerator.getInstance("EC", bcProvider)` with `secp256r1`
2. **Self-signed X.509 certificate** via BouncyCastle's `JcaX509v3CertificateBuilder` signed with `SHA256withECDSA`
3. **In-memory JKS keystores** for both the keystore and truststore

This approach avoids external file dependencies and ensures all cryptographic material is created through BouncyCastle's provider.

## Prerequisites

- **JDK 21** or later
- **Maven 3.9+**
- **Apache Camel 4.19.0-SNAPSHOT** installed in local Maven repository

## Project Structure

```
pqc-ssl-context-jdk21/
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
| `pqc-ssl-info` | `GET /api/ssl-info` | Returns BouncyCastle provider details, available PQC named groups, enabled protocols and cipher suites. |

## Step-by-Step Guide

### Step 1: Select JDK 21

```sh
sdk use java 21.0.10-tem
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
=== Post-Quantum TLS Handshake Verification (BouncyCastle JSSE) ===
PQC Verification Result:
{
    "pqcVerified": true,
    "tlsProvider": "BouncyCastle JSSE (BCJSSE)",
    "jdkVersion": "21.0.10",
    "server": {
        "protocol": "TLSv1.3",
        "cipherSuite": "TLS_CHACHA20_POLY1305_SHA256"
    },
    "client": {
        "protocol": "TLSv1.3",
        "cipherSuite": "TLS_CHACHA20_POLY1305_SHA256",
        "namedGroupsOffered": "X25519MLKEM768 (PQC key exchange) + secp256r1 (ECDSA auth)"
    },
    "message": "PQC VERIFIED: TLS 1.3 handshake with X25519MLKEM768 succeeded on JDK 21.0.10 using BouncyCastle JSSE"
}
```

`"pqcVerified": true` proves the TLS 1.3 handshake completed using the `X25519MLKEM768` post-quantum hybrid group for key exchange on JDK 21 via BouncyCastle.

### Step 4: Query the REST Endpoints

**PQC verification on demand:**

```sh
curl http://localhost:8080/api/verify-pqc
```

```json
{
    "pqcVerified": true,
    "tlsProvider": "BouncyCastle JSSE (BCJSSE)",
    "jdkVersion": "21.0.10",
    "protocol": "TLSv1.3",
    "cipherSuite": "TLS_CHACHA20_POLY1305_SHA256",
    "namedGroup": "X25519MLKEM768",
    "message": "PQC VERIFIED: TLS handshake succeeded with X25519MLKEM768 key exchange"
}
```

**SSL configuration info (providers, PQC named groups):**

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

## PQC Named Groups Available in BouncyCastle JSSE 1.83

| Named Group | Type | Description |
|-------------|------|-------------|
| `X25519MLKEM768` | PQC hybrid | X25519 + ML-KEM-768 (recommended) |
| `SecP256r1MLKEM768` | PQC hybrid | NIST P-256 + ML-KEM-768 |
| `SecP384r1MLKEM1024` | PQC hybrid | NIST P-384 + ML-KEM-1024 |
| `MLKEM512` | Pure PQC | ML-KEM-512 |
| `MLKEM768` | Pure PQC | ML-KEM-768 |
| `MLKEM1024` | Pure PQC | ML-KEM-1024 |

## JDK Compatibility

| JDK Version | PQC in TLS | Approach |
|-------------|-----------|----------|
| **21** | **Yes (via BouncyCastle)** | This example - BCJSSE provides PQC TLS support |
| 24 | No (native) | `ML-KEM` available as standalone KEM, but not in TLS |
| **27** | **Yes (native)** | [`pqc-ssl-context`](../pqc-ssl-context/) - JEP 527 adds PQC to SunJSSE |

## Related Examples

- [`pqc-ssl-context`](../pqc-ssl-context/) - PQC TLS using JDK 27's native support (JEP 527)

## Help and Contributions

If you hit any problem using Camel or have some feedback, then please
[let us know](https://camel.apache.org/community/support/).

We also love contributors, so
[get involved](https://camel.apache.org/community/contributing/) :-)

The Camel riders!
