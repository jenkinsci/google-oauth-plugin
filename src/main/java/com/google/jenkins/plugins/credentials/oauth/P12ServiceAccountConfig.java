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

import java.io.ByteArrayInputStream;
import java.io.File;
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

import javax.annotation.CheckForNull;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.SecretBytes;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
  @CheckForNull
  private String filename;
  @CheckForNull
  private SecretBytes secretP12Key;
  @Deprecated   // for migration purpose
  @CheckForNull
  private transient String p12KeyFile;

  /**
   * @param emailAddress email address
   * @param p12KeyFile uploaded p12 key file
   * @param filename
   *     previous json key file name.
   *     used if p12KeyFile is not provided.
   * @param secretP12Key
   *     previous p12 key file content.
   *     used if p12KeyFile is not provided.
   * @since 0.7
   */
  @DataBoundConstructor
  public P12ServiceAccountConfig(String emailAddress, FileItem p12KeyFile,
                                 String filename, SecretBytes secretP12Key) {
    this.emailAddress = emailAddress;
    if (p12KeyFile != null && p12KeyFile.getSize() > 0) {
      this.filename = extractFilename(p12KeyFile.getName());
      this.secretP12Key = SecretBytes.fromBytes(p12KeyFile.get());
    } else {
      this.filename = extractFilename(filename);
      this.secretP12Key = secretP12Key;
    }
  }

  @Deprecated
  public P12ServiceAccountConfig(String emailAddress, FileItem p12KeyFile,
                                 String prevP12KeyFile) {
    this(emailAddress, p12KeyFile,
        prevP12KeyFile, getSecretBytesFromFile(prevP12KeyFile));
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
    if (secretP12Key == null) {
      // google-oauth-plugin < 0.7
      return new P12ServiceAccountConfig(
        getEmailAddress(),
        null,
        getP12KeyFile()
      );
    }
    return this;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance()
            .getDescriptorOrDie(P12ServiceAccountConfig.class);
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * @return Original uploaded file name
   * @since 0.7
   */
  @CheckForNull
  public String getFilename() {
    return filename;
  }

  @Restricted(DoNotUse.class)   // for UI purpose only
  @CheckForNull
  public SecretBytes getSecretP12Key() {
    return secretP12Key;
  }

  @Deprecated
  public String getP12KeyFile() {
    return p12KeyFile;
  }

  @Override
  public String getAccountId() {
    return getEmailAddress();
  }

  @Override
  public PrivateKey getPrivateKey() {
    try {
      KeyStore p12KeyStore = getP12KeyStore();
      if (p12KeyStore == null) {
        return null;
      }
      return (PrivateKey) p12KeyStore.getKey(DEFAULT_P12_ALIAS,
              DEFAULT_P12_SECRET.toCharArray());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to read private key", e);
    } catch (GeneralSecurityException e) {
      LOGGER.log(Level.SEVERE, "Failed to read private key", e);
    }
    return null;
  }

  @CheckForNull
  private KeyStore getP12KeyStore() throws KeyStoreException,
          IOException, CertificateException, NoSuchAlgorithmException {
    InputStream in = null;
    if (secretP12Key == null) {
      return null;
    }
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      in = new ByteArrayInputStream(secretP12Key.getPlainData());
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
