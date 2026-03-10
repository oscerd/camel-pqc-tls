/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.pqc;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import org.apache.camel.main.Main;

public class PQCSSLContextApplication {

    public static void main(String[] args) throws Exception {
        // Remove ECDH from jdk.tls.disabledAlgorithms.
        // JDK 21 disables raw "ECDH" which BCJSSE interprets broadly,
        // preventing EC credentials from being used in TLS handshakes.
        String disabled = Security.getProperty("jdk.tls.disabledAlgorithms");
        if (disabled != null) {
            disabled = disabled.replaceAll(",\\s*ECDH\\b", "");
            Security.setProperty("jdk.tls.disabledAlgorithms", disabled);
        }

        // Register BC at the end (low priority) so BCJSSE can use it
        // for key conversion, while JDK's SUN/SunJCE remain the preferred
        // providers for PKCS12 KeyStore and PBE algorithms.
        Security.addProvider(new BouncyCastleProvider());

        // Register BCJSSE at position 1 for TLS.
        // BCJSSE will find BC from the global provider list.
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        Main main = new Main();
        main.run(args);
    }
}
