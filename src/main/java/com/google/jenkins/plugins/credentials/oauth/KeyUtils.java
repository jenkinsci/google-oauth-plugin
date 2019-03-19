/*
 * Copyright 2015 Google LLC
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import hudson.util.Secret;
import jenkins.model.Jenkins;

/**
 * Utility methods for handling key files.
 */
public class KeyUtils {
  /**
   * Utility class should never be instantiated.
   */
  private KeyUtils() {}

  /**
   * Creates a file with the given prefix/suffix in a standard Google auth
   * directory, and sets the permissions of the file to owner-only read/write.
   *
   * @throws IOException if filesystem interaction fails.
   */
  public static File createKeyFile(String prefix, String suffix)
      throws IOException {
    File keyFolder = new File(Jenkins.getInstance().getRootDir(), "gauth");
    if (keyFolder.exists() || keyFolder.mkdirs()) {
      File result = File.createTempFile(prefix, suffix, keyFolder);
      if (result == null) {
        throw new IOException("Failed to create key file");
      }
      updatePermissions(result);
      return result;
    } else {
      throw new IOException("Failed to create key folder");
    }
  }

  /**
   * Sets the permissions of the file to owner-only read/write.
   *
   * @throws IOException if filesystem interaction fails.
   */
  public static void updatePermissions(File file) throws IOException {
    if (file == null || !file.exists()) {
      return;
    }
    // Set world read/write permissions to false.
    // Set owner read/write permissions to true.
    if (!file.setReadable(false, false)
        || !file.setWritable(false, false)
        || !file.setReadable(true, true)
        || !file.setWritable(true, true)) {
      throw new IOException("Failed to update key file permissions");
    }
  }

  /**
   * Writes the given key to the given keyfile, passing it through
   * {@link Secret} to encode the string. Note that, per the documentation of
   * {@link Secret}, this does not protect against an attacker who has full
   * access to the local file system, but reduces the chance of accidental
   * exposure.
   */
  public static void writeKeyToFileEncoded(String key, File file)
      throws IOException {
    if (key == null || file == null) {
      return;
    }
    Secret encoded = Secret.fromString(key);
    writeKeyToFile(IOUtils.toInputStream(encoded.getEncryptedValue(),
        StandardCharsets.UTF_8), file);
  }

  /**
   * Writes the key contained in the given {@link InputStream} to the given
   * keyfile. Does not close the input stream.
   */
  public static void writeKeyToFile(InputStream keyStream, File file)
      throws IOException {
    if (keyStream == null || file == null) {
      return;
    }
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(file);
      IOUtils.copy(keyStream, out);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }
}
