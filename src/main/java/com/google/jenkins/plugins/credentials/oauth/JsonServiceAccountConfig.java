/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.credentials.oauth;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.SecretBytes;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.PemReader;
import com.google.api.client.util.Strings;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import jenkins.model.Jenkins;

/**
 * Provides authentication mechanism for a service account by setting a JSON
 * private key file. The JSON file structure needs to be:
 * <p>
 * <code>
 *     {
 *       "private_key":"-----BEGIN PRIVATE KEY-----\n
 *                      ...
 *                      \n-----END PRIVATE KEY-----\n",
 *       "client_email":"...@developer.gserviceaccount.com",
 *       ...
 *     }
 * </code>
 */
public class JsonServiceAccountConfig extends ServiceAccountConfig {
  /*
   * TODO(jenkinsci/google-oauth-plugin#50): Dedupe shared functionality in
   *    google-auth-library.
   */

  private static final long serialVersionUID = 6818111194672325387L;
  private static final Logger LOGGER =
      Logger.getLogger(JsonServiceAccountConfig.class.getSimpleName());
  @CheckForNull
  private String filename;
  @CheckForNull
  private SecretBytes secretJsonKey;
  @Deprecated   // for migration purpose
  @CheckForNull
  private transient String jsonKeyFile;
  private transient JsonKey jsonKey;

  /** @since 0.8 */
  @DataBoundConstructor
  public JsonServiceAccountConfig() {}

  /**
   * For being able to load credentials created with versions < 0.8
   * and backwards compatibility with external callers.
   *
   * @param jsonKeyFile The uploaded JSON key file.
   * @param prevJsonKeyFile The path of the previous JSON key file.
   * @since 0.3
   */
  @Deprecated
  public JsonServiceAccountConfig(
      FileItem jsonKeyFile, String prevJsonKeyFile) {
    this.setJsonKeyFileUpload(jsonKeyFile);
    if (filename == null && prevJsonKeyFile != null) {
      this.filename = extractFilename(prevJsonKeyFile);
      this.secretJsonKey = getSecretBytesFromFile(prevJsonKeyFile);
    }
  }

  /** @param jsonKeyFileUpload The uploaded JSON key file. */
  @DataBoundSetter // Called on form submit, only used when key file is uploaded
  public void setJsonKeyFileUpload(FileItem jsonKeyFileUpload) {
    if (jsonKeyFileUpload != null && jsonKeyFileUpload.getSize() > 0) {
      try {
        JsonKey jsonKey = JsonKey.load(new JacksonFactory(),
            jsonKeyFileUpload.getInputStream());
        if (jsonKey.getClientEmail() != null &&
            jsonKey.getPrivateKey() != null) {
          this.filename = extractFilename(jsonKeyFileUpload.getName());
          this.secretJsonKey = SecretBytes.fromBytes(jsonKeyFileUpload.get());
        }
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to read JSON key from file", e);
      }
    }
  }

  /** @param filename The JSON key file name. */
  @DataBoundSetter
  public void setFilename(String filename) {
    String newFilename = extractFilename(filename);
    if (!Strings.isNullOrEmpty(newFilename)) {
      this.filename = newFilename;
    }
  }

  /** @param secretJsonKey The JSON key file content. */
  @DataBoundSetter
  public void setSecretJsonKey(SecretBytes secretJsonKey) {
    if (secretJsonKey != null && secretJsonKey.getPlainData().length > 0) {
      this.secretJsonKey = secretJsonKey;
    }
  }

  @Deprecated   // used only for compatibility purpose
  @CheckForNull
  private static SecretBytes getSecretBytesFromFile(
      @CheckForNull String filename) {
    if (filename == null || filename.isEmpty()) {
      return null;
    }
    try {
      return SecretBytes.fromBytes(
          FileUtils.readFileToByteArray(new File(filename)));
    } catch (IOException e) {
      LOGGER.log(
          Level.SEVERE,
          String.format("Failed to read previous key from %s", filename),
          e);
      return null;
    }
  }

  @CheckForNull
  private static String extractFilename(@CheckForNull String path) {
    if (path == null) {
      return null;
    }
    return path.replaceFirst("^.+[/\\\\]", "");
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  private Object readResolve() {
    if (secretJsonKey == null) {
      // google-oauth-plugin < 0.7
      return new JsonServiceAccountConfig(
          null,
        getJsonKeyFile()
      );
    }
    return this;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance()
        .getDescriptorOrDie(JsonServiceAccountConfig.class);
  }

  /**
   * @return Original uploaded file name.
   * @since 0.7
   */
  @CheckForNull
  public String getFilename() {
    return filename;
  }

  @Restricted(DoNotUse.class) // UI: Required for stapler call of setter.
  @CheckForNull
  public SecretBytes getSecretJsonKey() {
    return secretJsonKey;
  }

  @Deprecated
  public String getJsonKeyFile() {
    return jsonKeyFile;
  }

  /**
   * For use in UI, do not use.
   * @return The uploaded JSON key file.
   */
  @Deprecated
  @Restricted(DoNotUse.class) // UI: Required for stapler call of setter.
  public FileItem getJsonKeyFileUpload() {
    return null;
  }

  /**
   * In this context the service account id is represented by the email address
   * for that service account, which should be contained in the JSON key.
   *
   * @return The service account identifier. Null if no JSON key has been
   *    provided.
   */
  @Override
  public String getAccountId() {
    JsonKey jsonKey = getJsonKey();
    if (jsonKey != null) {
      return jsonKey.getClientEmail();
    }
    return null;
  }

  /**
   * @return The {@link PrivateKey} that comes from the secret JSON key. Null if
   *    this service account config contains no key or if the key is malformed.
   */
  @Override
  public PrivateKey getPrivateKey() {
    JsonKey jsonKey = getJsonKey();
    if (jsonKey != null) {
      String privateKey = jsonKey.getPrivateKey();
      if (privateKey != null && !privateKey.isEmpty()) {
        PemReader pemReader = new PemReader(new StringReader(privateKey));
        try {
          PemReader.Section section = pemReader.readNextSection();
          if (section != null) {
            PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(section.getBase64DecodedBytes());
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
          } else {
            LOGGER.severe("The provided private key is malformed.");
          }
        } catch (IOException
            | InvalidKeySpecException
            | NoSuchAlgorithmException e) {
          LOGGER.log(Level.SEVERE, "Failed to read private key", e);
        }
      }
    }
    return null;
  }

  private JsonKey getJsonKey() {
    if (jsonKey == null && secretJsonKey != null
        && secretJsonKey.getPlainData().length > 0) {
      try {
        jsonKey = JsonKey.load(new JacksonFactory(),
            new ByteArrayInputStream(secretJsonKey.getPlainData()));
      } catch (IOException ignored) {
      }
    }
    return jsonKey;
  }

  /**
   * Descriptor for JSON service account authentication.
   */
  @Extension
  public static final class DescriptorImpl extends Descriptor {
    @Override
    public String getDisplayName() {
      return Messages.JsonServiceAccountConfig_DisplayName();
    }
  }
}
