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

import com.cloudbees.plugins.credentials.SecretBytes;
import com.google.api.client.util.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
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
import jenkins.model.Jenkins;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Provides authentication mechanism for a service account by setting a service account email
 * address and P12 private key file.
 */
public class P12ServiceAccountConfig extends ServiceAccountConfig {
  /*
   * TODO(jenkinsci/google-oauth-plugin#50): Dedupe shared functionality in
   *    google-auth-library.
   */

  private static final long serialVersionUID = 8706353638974721795L;
  private static final Logger LOGGER =
      Logger.getLogger(P12ServiceAccountConfig.class.getSimpleName());
  private static final String DEFAULT_P12_SECRET = "notasecret";
  private static final String DEFAULT_P12_ALIAS = "privatekey";
  private final String emailAddress;
  @CheckForNull private String filename;
  @CheckForNull private SecretBytes secretP12Key;

  @Deprecated // for migration purpose
  @CheckForNull
  private transient String p12KeyFile;

  /**
   * @param emailAddress The service account email address.
   * @since 0.8
   */
  @DataBoundConstructor
  public P12ServiceAccountConfig(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
   * For being able to load credentials created with versions < 0.8 and backwards compatibility with
   * external callers.
   *
   * @param emailAddress The service account email address.
   * @param p12KeyFileUpload The uploaded p12 key file.
   * @param prevP12KeyFile The path of the previous p12 key file.
   * @since 0.3
   */
  @Deprecated
  public P12ServiceAccountConfig(
      String emailAddress, FileItem p12KeyFileUpload, String prevP12KeyFile) {
    this(emailAddress);
    this.setP12KeyFileUpload(p12KeyFileUpload);
    if (filename == null && prevP12KeyFile != null) {
      this.setFilename(prevP12KeyFile);
      this.setSecretP12Key(getSecretBytesFromFile(prevP12KeyFile));
    }
  }

  /** @param p12KeyFile The uploaded p12 key file. */
  @Deprecated
  @DataBoundSetter // Called on form submit, only used when key file is uploaded
  public void setP12KeyFileUpload(FileItem p12KeyFile) {
    if (p12KeyFile != null && p12KeyFile.getSize() > 0) {
      this.filename = extractFilename(p12KeyFile.getName());
      this.secretP12Key = SecretBytes.fromBytes(p12KeyFile.get());
    }
  }

  /** @param filename The previous p12 key file name. */
  @DataBoundSetter
  public void setFilename(String filename) {
    if (!Strings.isNullOrEmpty(filename)) {
      this.filename = extractFilename(filename);
    }
  }

  /** @param secretP12Key The previous p12 key file content. */
  @DataBoundSetter
  public void setSecretP12Key(SecretBytes secretP12Key) {
    if (secretP12Key != null && secretP12Key.getPlainData().length > 0) {
      this.secretP12Key = secretP12Key;
    }
  }

  @Deprecated // used only for compatibility purpose
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
          null, // p12KeyFileUpload
          getP12KeyFile());
    }
    return this;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(P12ServiceAccountConfig.class);
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * @return Original uploaded file name.
   * @since 0.7
   */
  @CheckForNull
  public String getFilename() {
    return filename;
  }

  /**
   * Do not use, required for UI.
   *
   * @return The secret p12 key.
   */
  @Restricted(DoNotUse.class) // UI:  Required for stapler call of setter.
  @CheckForNull
  public SecretBytes getSecretP12Key() {
    return secretP12Key;
  }

  /** @return The path of the previous p12 key file. */
  @Deprecated
  public String getP12KeyFile() {
    return p12KeyFile;
  }

  /**
   * Do not use, required for UI.
   *
   * @return The uploaded p12 key file.
   */
  @Deprecated
  @Restricted(DoNotUse.class) // UI: Required for stapler call of setter.
  public FileItem getP12KeyFileUpload() {
    return null;
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
      return (PrivateKey) p12KeyStore.getKey(DEFAULT_P12_ALIAS, DEFAULT_P12_SECRET.toCharArray());
    } catch (IOException | GeneralSecurityException e) {
      LOGGER.log(Level.SEVERE, "Failed to read private key", e);
    }
    return null;
  }

  @CheckForNull
  private KeyStore getP12KeyStore()
      throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
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

  /** Descriptor for P12 service account authentication. */
  @Extension
  public static final class DescriptorImpl extends Descriptor {
    @Override
    public String getDisplayName() {
      return Messages.P12ServiceAccountConfig_DisplayName();
    }
  }
}
