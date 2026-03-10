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

/**
 * Bootstrap class for the PQC TLS handshake example on JDK 24.
 *
 * Performs two critical setup steps before starting the Camel context:
 *
 * 1. Removes ECDH from jdk.tls.disabledAlgorithms - JDK 24 may include
 *    raw ECDH in the disabled algorithms list, which BCJSSE interprets
 *    broadly, preventing EC credentials from being used during TLS handshakes.
 *
 * 2. Registers BouncyCastle providers - BC is registered at low priority
 *    (end of provider list) so JDK's SUN/SunJCE remain preferred for
 *    standard operations, while BCJSSE is inserted at position 1 to
 *    handle all TLS operations.
 */
public class PQCSSLContextApplication {

    public static void main(String[] args) throws Exception {
        String disabled = Security.getProperty("jdk.tls.disabledAlgorithms");
        if (disabled != null) {
            disabled = disabled.replaceAll(",\\s*ECDH\\b", "");
            Security.setProperty("jdk.tls.disabledAlgorithms", disabled);
        }

        Security.addProvider(new BouncyCastleProvider());
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        Main main = new Main();
        main.run(args);
    }
}
