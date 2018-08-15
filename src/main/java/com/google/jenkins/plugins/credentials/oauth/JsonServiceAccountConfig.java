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
import javax.annotation.Nonnull;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.SecretBytes;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.PemReader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import jenkins.model.Jenkins;

/**
 * Provides authentication mechanism for a service account by setting a .json
 * private key file. The .json file structure needs to be:
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
  private static final long serialVersionUID = 6818111194672325387L;
  private static final Logger LOGGER =
      Logger.getLogger(JsonServiceAccountConfig.class.getSimpleName());
  @Nonnull
  private final String filename;
  @Nonnull
  private final SecretBytes secretJsonKey;
  @Deprecated   // for migration purpose
  @CheckForNull
  private transient String jsonKeyFile;
  private transient JsonKey jsonKey;

  /**
   * @param jsonKeyFile uploaded json key file
   * @param filename previous json key file name. used if jsonKeyFile is not provided.
   * @param secretJsonKey previous json key file content. used if jsonKeyFile is not provided.
   * @since 0.7
   */
  @DataBoundConstructor
  public JsonServiceAccountConfig(FileItem jsonKeyFile,
      String filename, SecretBytes secretJsonKey) {
    if (jsonKeyFile != null && jsonKeyFile.getSize() > 0) {
      try {
        JsonKey jsonKey = JsonKey.load(new JacksonFactory(),
                jsonKeyFile.getInputStream());
        if (jsonKey.getClientEmail() == null ||
                jsonKey.getPrivateKey() == null) {
          throw new IllegalArgumentException("Invalid json key file");
        }
        this.filename = extractFilename(jsonKeyFile.getName());
        this.secretJsonKey = SecretBytes.fromBytes(jsonKeyFile.get());
      } catch (IOException e) {
          throw new IllegalArgumentException("Failed to read json key from file", e);
      }
    } else {
      if (filename == null || secretJsonKey == null) {
        throw new IllegalArgumentException("No content provided or resolved.");
      }
      this.filename = extractFilename(filename);
      this.secretJsonKey = secretJsonKey;
    }
  }

  @Deprecated
  public JsonServiceAccountConfig(FileItem jsonKeyFile,
      String prevJsonKeyFile) {
    this(null, prevJsonKeyFile, getSecretBytesFromFile(prevJsonKeyFile));
  }

  @Deprecated   // used only for compatibility purpose
  @CheckForNull
  private static SecretBytes getSecretBytesFromFile(@CheckForNull String filename) {
    if (filename == null || filename.isEmpty()) {
      return null;
    }
    try {
      return SecretBytes.fromBytes(FileUtils.readFileToByteArray(new File(filename)));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, String.format("Failed to read previous key from %s", filename), e);
      return null;
    }
  }

  private static String extractFilename(String path) {
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
   * @return Original uploaded file name
   * @since 0.7
   */
  @Nonnull
  public String getFilename() {
    return filename;
  }

  @Restricted(DoNotUse.class)   // for UI purpose only
  @Nonnull
  public SecretBytes getSecretJsonKey() {
    return secretJsonKey;
  }

  @Deprecated
  public String getJsonKeyFile() {
    return jsonKeyFile;
  }

  @Override
  public String getAccountId() {
    JsonKey jsonKey = getJsonKey();
    if (jsonKey != null) {
      return jsonKey.getClientEmail();
    }
    return null;
  }

  @Override
  public PrivateKey getPrivateKey() {
    JsonKey jsonKey = getJsonKey();
    if (jsonKey != null) {
      String privateKey = jsonKey.getPrivateKey();
      if (privateKey != null && !privateKey.isEmpty()) {
        PemReader pemReader = new PemReader(new StringReader(privateKey));
        try {
          PemReader.Section section = pemReader.readNextSection();
          PKCS8EncodedKeySpec keySpec =
              new PKCS8EncodedKeySpec(section.getBase64DecodedBytes());
          return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Failed to read private key", e);
        } catch (InvalidKeySpecException e) {
          LOGGER.log(Level.SEVERE, "Failed to read private key", e);
        } catch (NoSuchAlgorithmException e) {
          LOGGER.log(Level.SEVERE, "Failed to read private key", e);
        }
      }
    }
    return null;
  }

  private JsonKey getJsonKey() {
    if (jsonKey == null) {
      try {
        jsonKey = JsonKey.load(new JacksonFactory(),
            new ByteArrayInputStream(secretJsonKey.getPlainData()));
      } catch (IOException ignored) {
      }
    }
    return jsonKey;
  }

  /**
   * descriptor for .json service account authentication
   */
  @Extension
  public static final class DescriptorImpl extends Descriptor {
    @Override
    public String getDisplayName() {
      return Messages.JsonServiceAccountConfig_DisplayName();
    }
  }
}
