# Post-Quantum SSLContext with Apache Camel

This example demonstrates a fully working Post-Quantum Cryptography (PQC) TLS handshake using Apache Camel's SSLContext configuration with the **X25519MLKEM768** hybrid key exchange.

## What This Example Proves

At startup, the application performs a self-contained TLS handshake where:

1. An **SSLServerSocket** is created using Camel's SSLContext, configured with `X25519MLKEM768` and `x25519` named groups
2. An **SSLSocket** client connects offering **only** `X25519MLKEM768` (no classical fallback)
3. If the TLS 1.3 handshake **succeeds**, PQC key exchange was definitively negotiated

This is a conclusive proof: the client offers only the PQC group, so a successful handshake means PQC was used.

## How It Works

### X25519MLKEM768 Hybrid Key Exchange

`X25519MLKEM768` is a post-quantum hybrid key exchange defined in [JEP 527](https://openjdk.org/jeps/527) that combines:

- **X25519** - classical elliptic curve Diffie-Hellman (well-proven security)
- **ML-KEM-768** - post-quantum lattice-based key encapsulation ([NIST FIPS 203](https://csrc.nist.gov/pubs/fips/203))

Both algorithms run together. Security is maintained even if one is broken.

### Camel SSL Configuration

PQC is configured via standard `camel.ssl.*` properties in `application.properties`:

```properties
camel.ssl.enabled=true
camel.ssl.secureSocketProtocol=TLSv1.3
camel.ssl.namedGroups=X25519MLKEM768,x25519
camel.ssl.cipherSuites=TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256
```

## Prerequisites

- **JDK 27 EA** (Early Access) - `X25519MLKEM768` as a TLS named group is only available starting from JDK 27 ([JEP 527](https://openjdk.org/jeps/527))
- **Maven 3.9+**
- **Apache Camel 4.19.0-SNAPSHOT** installed in local Maven repository

### Install JDK 27 EA

Using [SDKMAN](https://sdkman.io/):

```sh
sdk install java 27.ea.11-open
```

## Project Structure

```
pqc-ssl-context/
├── pom.xml
├── generate-certs.sh
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

### Routes

| Route | Description |
|-------|-------------|
| `pqc-verify` | Runs once at startup. Performs a full PQC TLS handshake and logs the result. |
| `pqc-secure-server` | REST endpoint at `/api/data` served over HTTPS. |
| `pqc-verify-endpoint` | REST endpoint at `/api/verify-pqc` to run PQC verification on demand. |
| `pqc-ssl-info` | REST endpoint at `/api/ssl-info` showing SSLContext configuration. |

## Step-by-Step Guide

### Step 1: Generate Certificates

Generate a self-signed keystore and truststore for TLS:

```sh
cd pqc-ssl-context
./generate-certs.sh
```

Expected output:

```
Generating self-signed certificate...
Keystore created: keystore.p12
Certificate exported: server.crt
Truststore created: truststore.p12

Certificate generation complete.
  Keystore:   keystore.p12
  Truststore: truststore.p12
  Password:   changeit
```

This creates two files in the project directory:
- `keystore.p12` - server identity (EC key with secp256r1)
- `truststore.p12` - trusted certificates

### Step 2: Select JDK 27

Activate JDK 27 in your current shell:

```sh
sdk use java 27.ea.11-open
```

Verify:

```sh
java -version
```

Expected:

```
openjdk version "27-ea" 2026-09-15
OpenJDK Runtime Environment (build 27-ea+11-968)
OpenJDK 64-Bit Server VM (build 27-ea+11-968, mixed mode, sharing)
```

### Step 3: Build and Run

```sh
mvn clean compile exec:exec
```

> **Note:** The `pom.xml` uses `exec:exec` (not `exec:java`) to fork a separate JVM process.
> The `java27.home` property in `pom.xml` points to the JDK 27 installation.
> Adjust it if your JDK 27 is installed in a different location.

### Step 4: Verify PQC in the Logs

On successful startup, you should see in the logs:

```
=== Post-Quantum TLS Handshake Verification ===
PQC Verification Result:
{
    "pqcVerified": true,
    "server": {
        "protocol": "TLSv1.3",
        "cipherSuite": "TLS_AES_256_GCM_SHA384",
        "namedGroupsConfigured": [
            "X25519MLKEM768",
            "x25519"
        ]
    },
    "client": {
        "protocol": "TLSv1.3",
        "cipherSuite": "TLS_AES_256_GCM_SHA384",
        "namedGroupOffered": "X25519MLKEM768 (only PQC group, no classical fallback)"
    },
    "message": "PQC VERIFIED: TLS 1.3 handshake succeeded with X25519MLKEM768 as the only client-offered group"
}
```

The key line is `"pqcVerified": true` - this proves the TLS handshake completed using only the `X25519MLKEM768` post-quantum hybrid group.

### Step 5: Query the REST Endpoints

With the application running, you can use these endpoints:

**PQC verification on demand:**

```sh
curl -k https://localhost:8443/api/verify-pqc
```

**SSL configuration info:**

```sh
curl -k https://localhost:8443/api/ssl-info
```

**Data endpoint (served over PQC-enabled HTTPS):**

```sh
curl -k https://localhost:8443/api/data
```

### Step 6: Verify with OpenSSL (Optional)

If your OpenSSL version supports ML-KEM (OpenSSL 3.5+), you can verify the server's PQC key exchange externally:

```sh
echo | openssl s_client -connect localhost:8443 -groups X25519MLKEM768:x25519 2>&1 | grep "Server Temp Key"
```

Expected:

```
Server Temp Key: X25519MLKEM768
```

## Configuration Reference

### Named Groups

| Named Group | Type | Description |
|-------------|------|-------------|
| `X25519MLKEM768` | PQC hybrid | X25519 + ML-KEM-768 (recommended) |
| `x25519` | Classical | X25519 ECDH (fallback) |
| `secp256r1` | Classical | NIST P-256 curve |
| `secp384r1` | Classical | NIST P-384 curve |

### SSL Properties

| Property | Description |
|----------|-------------|
| `camel.ssl.enabled` | Enable global SSL configuration |
| `camel.ssl.secureSocketProtocol` | TLS protocol version (`TLSv1.3` required for PQC) |
| `camel.ssl.namedGroups` | Comma-separated list of TLS named groups |
| `camel.ssl.cipherSuites` | Comma-separated list of cipher suites |
| `camel.ssl.keyStore` | Path to keystore |
| `camel.ssl.keystorePassword` | Keystore password |
| `camel.ssl.trustStore` | Path to truststore |
| `camel.ssl.trustStorePassword` | Truststore password |

### Filter-Based Configuration

Instead of explicit lists, you can use include/exclude filters:

```properties
camel.ssl.namedGroupsInclude=.*MLKEM.*,x25519
camel.ssl.namedGroupsExclude=secp521r1
```

## JDK Compatibility

| JDK Version | PQC in TLS | Notes |
|-------------|-----------|-------|
| 21 | No | `SSLParameters.setNamedGroups()` API exists, but no PQC groups |
| 24 | No | `ML-KEM` available as standalone KEM (`java.security.KEM`), but not in TLS |
| **27** | **Yes** | `X25519MLKEM768` in TLS via [JEP 527](https://openjdk.org/jeps/527) |

## Help and Contributions

If you hit any problem using Camel or have some feedback, then please
[let us know](https://camel.apache.org/community/support/).

We also love contributors, so
[get involved](https://camel.apache.org/community/contributing/) :-)

The Camel riders!
