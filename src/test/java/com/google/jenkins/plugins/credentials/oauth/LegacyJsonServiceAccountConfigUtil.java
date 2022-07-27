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
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Util class for {@link com.google.jenkins.plugins.credentials.oauth
 * .GoogleRobotPrivateKeyCredentials}.
 */
public class LegacyJsonServiceAccountConfigUtil {
  private static File tempFolder;

  public static String createTempLegacyJsonKeyFile(String clientEmail) throws IOException {
    final File tempLegacyJsonKey = File.createTempFile("temp-legacykey", ".json", getTempFolder());
    final JsonGenerator jsonGenerator =
        new JacksonFactory()
            .createJsonGenerator(new FileOutputStream(tempLegacyJsonKey), Charset.forName("UTF-8"));
    jsonGenerator.enablePrettyPrint();
    jsonGenerator.serialize(createLegacyJsonKey(clientEmail));
    jsonGenerator.close();
    return tempLegacyJsonKey.getAbsolutePath();
  }

  public static String createTempLegacyJsonKeyFileWithMissingWebObject() throws IOException {
    final File tempLegacyJsonKey = File.createTempFile("temp-legacykey", ".json", getTempFolder());
    final JsonGenerator jsonGenerator =
        new JacksonFactory()
            .createJsonGenerator(new FileOutputStream(tempLegacyJsonKey), Charset.forName("UTF-8"));
    jsonGenerator.enablePrettyPrint();
    jsonGenerator.serialize(createLegacyJsonKeyWithMissingWebObject());
    jsonGenerator.close();
    return tempLegacyJsonKey.getAbsolutePath();
  }

  public static String createTempLegacyJsonKeyFileWithMissingClientEmail() throws IOException {
    final File tempLegacyJsonKey = File.createTempFile("temp-legacykey", ".json", getTempFolder());
    JsonGenerator jsonGenerator = null;
    try {
      jsonGenerator =
          new JacksonFactory()
              .createJsonGenerator(
                  new FileOutputStream(tempLegacyJsonKey), Charset.forName("UTF-8"));
      jsonGenerator.enablePrettyPrint();
      jsonGenerator.serialize(createLegacyJsonKeyWithMissingClientEmail());
    } finally {
      if (jsonGenerator != null) {
        jsonGenerator.close();
      }
    }
    return tempLegacyJsonKey.getAbsolutePath();
  }

  public static String createTempInvalidLegacyJsonKeyFile() throws IOException {
    final File tempLegacyJsonKey = File.createTempFile("temp-legacykey", ".json", getTempFolder());
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(tempLegacyJsonKey);
      out.write("InvalidLegacyJsonKeyFile".getBytes());
      out.flush();
    } finally {
      if (out != null) {
        out.close();
      }
    }
    return tempLegacyJsonKey.getAbsolutePath();
  }

  private static File getTempFolder() throws IOException {
    if (tempFolder == null) {
      tempFolder = Files.createTempDirectory("temp" + Long.toString(System.nanoTime())).toFile();
      tempFolder.deleteOnExit();
    }
    return tempFolder;
  }

  @SuppressWarnings("deprecation")
  private static LegacyJsonKey createLegacyJsonKey(String clientEmail) throws IOException {
    final LegacyJsonKey legacyJsonKey = new LegacyJsonKey();
    LegacyJsonKey.Details web = new LegacyJsonKey.Details();
    web.setClientEmail(clientEmail);
    legacyJsonKey.setWeb(web);
    return legacyJsonKey;
  }

  @SuppressWarnings("deprecation")
  private static LegacyJsonKey createLegacyJsonKeyWithMissingWebObject() throws IOException {
    return new LegacyJsonKey();
  }

  @SuppressWarnings("deprecation")
  private static LegacyJsonKey createLegacyJsonKeyWithMissingClientEmail() throws IOException {
    final LegacyJsonKey legacyJsonKey = new LegacyJsonKey();
    legacyJsonKey.setWeb(new LegacyJsonKey.Details());
    return legacyJsonKey;
  }
}
