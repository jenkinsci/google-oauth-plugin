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

import java.io.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.PemReader;
import hudson.Extension;
import jenkins.model.Jenkins;

/**
 * provides authentication mechanism for a service account by setting a a .json
 * private key file. the .json file
 * sturcture needs to be:
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
public class JsonKeyType extends KeyType {
  private static final long serialVersionUID = 6818111194672325387L;
  private static final Logger LOGGER =
          Logger.getLogger(JsonKeyType.class.getSimpleName());
  private String jsonKeyFile;
  private transient JsonKey jsonKey;

  @DataBoundConstructor
  public JsonKeyType(FileItem jsonKeyFile, String prevJsonKeyFile) {
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
    File jsonKeyFile = createJsonKeyFile();
    writeJsonKeyToFile(jsonKey, jsonKeyFile);
    return jsonKeyFile.getAbsolutePath();
  }

  private File createJsonKeyFile() throws IOException {
    File keyFolder = new File(Jenkins.getInstance().getRootDir(), "gauth");
    if (keyFolder.exists() || keyFolder.mkdirs()) {
      return File.createTempFile("key", ".json", keyFolder);
    } else {
      throw new IOException("Failed to create key folder");
    }
  }

  private void writeJsonKeyToFile(JsonKey jsonKey, File file)
          throws IOException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(file);
      IOUtils.write(jsonKey.toPrettyString(), out);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance()
            .getDescriptorOrDie(JsonKeyType.class);
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
      return "JSON key";
    }
  }
}
