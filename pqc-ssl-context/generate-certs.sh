#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Generate self-signed certificates for the PQC SSLContext example
# Requires: Java 21+ (keytool)

set -e

KEYSTORE="keystore.p12"
TRUSTSTORE="truststore.p12"
PASSWORD="changeit"
ALIAS="pqc-example"
DNAME="CN=localhost,OU=Camel,O=Apache,L=Localhost,ST=Test,C=US"
VALIDITY=365

echo "Generating self-signed certificate..."

# Generate keystore with a self-signed certificate
keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg EC \
    -groupname secp256r1 \
    -sigalg SHA256withECDSA \
    -dname "$DNAME" \
    -keystore "$KEYSTORE" \
    -storetype PKCS12 \
    -storepass "$PASSWORD" \
    -validity "$VALIDITY" \
    -ext "SAN=dns:localhost,ip:127.0.0.1"

echo "Keystore created: $KEYSTORE"

# Export the certificate
keytool -exportcert \
    -alias "$ALIAS" \
    -keystore "$KEYSTORE" \
    -storepass "$PASSWORD" \
    -file server.crt \
    -rfc

echo "Certificate exported: server.crt"

# Import the certificate into the truststore
keytool -importcert \
    -alias "$ALIAS" \
    -file server.crt \
    -keystore "$TRUSTSTORE" \
    -storetype PKCS12 \
    -storepass "$PASSWORD" \
    -noprompt

echo "Truststore created: $TRUSTSTORE"

# Clean up
rm -f server.crt

echo ""
echo "Certificate generation complete."
echo "  Keystore:   $KEYSTORE"
echo "  Truststore: $TRUSTSTORE"
echo "  Password:   $PASSWORD"
echo ""
echo "The TLS connection will use PQC hybrid key exchange (X25519MLKEM768)"
echo "as configured in application.properties via camel.ssl.namedGroups."
