/*
 * Copyright 2014 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.credentials.oauth;

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;

/** Util class for {@link JsonServiceAccountConfigTest}. */
public class JsonServiceAccountConfigTestUtil {
  private static File tempFolder;

  public static PrivateKey generatePrivateKey()
      throws NoSuchProviderException, NoSuchAlgorithmException {
    Security.addProvider(new BouncyCastleProvider());
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
    keyPairGenerator.initialize(1024);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return keyPair.getPrivate();
  }

  public static String createTempJsonKeyFile(String clientEmail, PrivateKey privateKey)
      throws IOException {
    final File tempJsonKey = File.createTempFile("temp-key", ".json", getTempFolder());
    JsonGenerator jsonGenerator = null;
    try {
      jsonGenerator =
          new JacksonFactory()
              .createJsonGenerator(new FileOutputStream(tempJsonKey), Charset.forName("UTF-8"));
      jsonGenerator.enablePrettyPrint();
      jsonGenerator.serialize(createJsonKey(clientEmail, privateKey));
    } finally {
      if (jsonGenerator != null) {
        jsonGenerator.close();
      }
    }
    return tempJsonKey.getAbsolutePath();
  }

  private static File getTempFolder() throws IOException {
    if (tempFolder == null) {
      tempFolder = Files.createTempDirectory("temp" + Long.toString(System.nanoTime())).toFile();
      tempFolder.deleteOnExit();
    }
    return tempFolder;
  }

  private static JsonKey createJsonKey(String clientEmail, PrivateKey privateKey)
      throws IOException {
    final JsonKey jsonKey = new JsonKey();
    jsonKey.setClientEmail(clientEmail);
    jsonKey.setPrivateKey(getInPemFormat(privateKey));
    return jsonKey;
  }

  private static String getInPemFormat(PrivateKey privateKey) throws IOException {
    final StringWriter stringWriter = new StringWriter();
    final PEMWriter pemWriter = new PEMWriter(stringWriter);
    pemWriter.writeObject(privateKey);
    pemWriter.flush();
    pemWriter.close();
    return stringWriter.toString();
  }
}
