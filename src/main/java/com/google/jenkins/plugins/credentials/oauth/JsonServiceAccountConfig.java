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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.PemReader;
import hudson.Extension;
import jenkins.model.Jenkins;

/**
 * Provides authentication mechanism for a service account by setting a .json
 * private key file. the .json file
 * structure needs to be:
 * <p/>
 * <code>
 * <pre>
 *     {
 *       "private_key":"-----BEGIN PRIVATE KEY-----\n
 *                      ...
 *                      \n-----END PRIVATE KEY-----\n",
 *       "client_email":"...@developer.gserviceaccount.com",
 *       ...
 *     }
 * </pre>
 * </code>
 */
public class JsonServiceAccountConfig extends ServiceAccountConfig {
  private static final long serialVersionUID = 6818111194672325387L;
  private static final Logger LOGGER =
      Logger.getLogger(JsonServiceAccountConfig.class.getSimpleName());
  private String jsonKeyFile;
  private transient JsonKey jsonKey;

  @DataBoundConstructor
  public JsonServiceAccountConfig(FileItem jsonKeyFile,
      String prevJsonKeyFile) {
    if (jsonKeyFile != null && jsonKeyFile.getSize() > 0) {
      try {
        JsonKey jsonKey = JsonKey.load(new JacksonFactory(),
                jsonKeyFile.getInputStream());
        if (jsonKey.getClientEmail() != null &&
                jsonKey.getPrivateKey() != null) {
          try {
            this.jsonKeyFile = writeJsonKeyToFile(jsonKey);
          } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write json key to file", e);
          }
        }
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to read json key from file", e);
      }
    } else if (prevJsonKeyFile != null && !prevJsonKeyFile.isEmpty()) {
      this.jsonKeyFile = prevJsonKeyFile;
    }
  }

  private String writeJsonKeyToFile(JsonKey jsonKey) throws IOException {
    File jsonKeyFile = KeyUtils.createKeyFile("key", ".json");
    KeyUtils.writeKeyToFileEncoded(jsonKey.toPrettyString(), jsonKeyFile);
    return jsonKeyFile.getAbsolutePath();
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance()
        .getDescriptorOrDie(JsonServiceAccountConfig.class);
  }

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
    if (jsonKey == null && jsonKeyFile != null && !jsonKeyFile.isEmpty()) {
      try {
        jsonKey = JsonKey.load(new JacksonFactory(),
            new FileInputStream(jsonKeyFile));
        File jsonKeyFileObject = new File(jsonKeyFile);
        KeyUtils.updatePermissions(jsonKeyFileObject);
        KeyUtils.writeKeyToFileEncoded(jsonKey.toPrettyString(),
            jsonKeyFileObject);
        return jsonKey;
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
