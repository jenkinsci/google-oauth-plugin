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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import jenkins.model.Jenkins;

/**
 * provides authentication mechanism for a service account by setting a service
 * account email address and .p12 private key file
 */
public class P12ServiceAccountConfig extends ServiceAccountConfig {
  private static final long serialVersionUID = 8706353638974721795L;
  private static final Logger LOGGER =
          Logger.getLogger(P12ServiceAccountConfig.class.getSimpleName());
  private static final String DEFAULT_P12_SECRET = "notasecret";
  private static final String DEFAULT_P12_ALIAS = "privatekey";
  private final String emailAddress;
  private String p12KeyFile;

  @DataBoundConstructor
  public P12ServiceAccountConfig(String emailAddress, FileItem p12KeyFile,
                                 String prevP12KeyFile) {
    this.emailAddress = emailAddress;
    if (p12KeyFile != null && p12KeyFile.getSize() > 0) {
      try {
        this.p12KeyFile = writeP12KeyToFile(p12KeyFile);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to write json key to file", e);
      }
    } else if (prevP12KeyFile != null && !prevP12KeyFile.isEmpty()) {
      this.p12KeyFile = prevP12KeyFile;
    }
  }

  private String writeP12KeyToFile(FileItem p12KeyFileItem) throws IOException {
    File p12KeyFile = createP12KeyFile();
    writeP12KeyToFile(p12KeyFileItem, p12KeyFile);
    return p12KeyFile.toString();
  }

  private void writeP12KeyToFile(FileItem p12KeyFileItem, File p12KeyFile)
          throws IOException {
    InputStream in = null;
    FileOutputStream out = null;
    try {
      in = p12KeyFileItem.getInputStream();
      out = new FileOutputStream(p12KeyFile);
      IOUtils.copy(in, out);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  private File createP12KeyFile() throws IOException {
    File keyFolder = new File(Jenkins.getInstance().getRootDir(), "gauth");
    if (keyFolder.exists() || keyFolder.mkdirs()) {
      return File.createTempFile("key", ".p12", keyFolder);
    } else {
      throw new IOException("Failed to create key folder");
    }
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance()
            .getDescriptorOrDie(P12ServiceAccountConfig.class);
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getP12KeyFile() {
    return p12KeyFile;
  }

  @Override
  public String getAccountId() {
    return getEmailAddress();
  }

  @Override
  public PrivateKey getPrivateKey() {
    if (p12KeyFile != null) {
      try {
        KeyStore p12KeyStore = getP12KeyStore();
        return (PrivateKey) p12KeyStore.getKey(DEFAULT_P12_ALIAS,
                DEFAULT_P12_SECRET.toCharArray());
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to read private key", e);
      } catch (GeneralSecurityException e) {
        LOGGER.log(Level.SEVERE, "Failed to read private key", e);
      }
    }
    return null;
  }

  private KeyStore getP12KeyStore() throws KeyStoreException,
          IOException, CertificateException, NoSuchAlgorithmException {
    FileInputStream in = null;
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      in = new FileInputStream(p12KeyFile);
      keyStore.load(in, DEFAULT_P12_SECRET.toCharArray());
      return keyStore;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * descriptor for .p12 service account authentication
   */
  @Extension
  public static final class DescriptorImpl extends Descriptor {
    @Override
    public String getDisplayName() {
      return Messages.P12ServiceAccountConfig_DisplayName();
    }
  }
}
