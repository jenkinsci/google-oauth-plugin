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
import java.security.GeneralSecurityException;
import java.util.Collection;

import javax.annotation.Nullable;

import org.apache.commons.fileupload.FileItem;
import org.kohsuke.stapler.DataBoundConstructor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import hudson.Extension;
import hudson.FilePath;
import jenkins.model.Jenkins;

/**
 * A set of Google service account credentials for a cloud project
 * to use for authenticating against Google service APIs.
 *
 * Example APIs: Google Cloud Storage, Google Compute Engine
 *
 * @author Matt Moore
 */
@NameWith(value = GoogleRobotNameProvider.class, priority = 50)
public final class GoogleRobotPrivateKeyCredentials
    extends GoogleRobotCredentials {
  /**
   * Construct a set of service account credentials.
   *
   * @param projectId The project id associated with this service account
   * @param prevSecretsFile The path to a previously uploaded secrets file
   * @param secretsFile A new secrets file for this service account
   * @param prevP12File The path to a previously uploaded private key file
   * @param p12File A new private key file for this service account
   * @param module The module for instantiating dependent objects, or null.
   */
  @DataBoundConstructor
  public GoogleRobotPrivateKeyCredentials(String projectId,
      String prevSecretsFile, FileItem secretsFile,
      String prevP12File, FileItem p12File,
      @Nullable GoogleRobotCredentialsModule module)
      throws Exception {
    super(projectId, module);

    FilePath home = checkNotNull(Jenkins.getInstance().getRootPath());
    // home is assumed to exist...

    FilePath secretsHome = home.child("gauth");
    secretsHome.mkdirs();

    // Use the hash code to escape the projectId.
    FilePath thisSecretDir = secretsHome.child("" + projectId.hashCode());
    thisSecretDir.mkdirs();

    final String theSecretsFile = writeIfNewFileOrReturnOld(
        prevSecretsFile, secretsFile, thisSecretDir, "client_secrets", "json");

    final String theP12File = writeIfNewFileOrReturnOld(
        prevP12File, p12File, thisSecretDir, "private", "p12");

    // TODO(mattmoor): we need form validation that something was
    // specified for each of these REQUIRED fields.

    this.secretsFile = theSecretsFile;
    this.p12File = theP12File;
  }

  /**
   * If a new file is passed, this writes it to a new file on disk  with the
   * {@code prefix} and {@code extension}.  Otherwise it returns the provided
   * old file.
   */
  private String writeIfNewFileOrReturnOld(String oldFile, FileItem newFile,
      FilePath containerDir, String prefix, String extension) throws Exception {
    if (checkNotNull(newFile).getSize() == 0) {
      return oldFile;
    }

    FilePath newFilePath =
        containerDir.createTempFile(prefix + ".", "." + extension);
    String theNewFile = newFilePath.toString();
    newFile.write(new java.io.File(theNewFile));

    return theNewFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUsername() {
    try {
      GoogleCredential credential = getGoogleCredential(
          new GoogleOAuth2ScopeRequirement() {
            @Override
            public Collection<String> getScopes() {
              return ImmutableList.of();
            }
          });

      // Retrieve the email to use as a username (the email of the service
      // account is used as the service account id).
      return credential.getServiceAccountId();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          Messages.GoogleRobotPrivateKeyCredentials_BadCredentials(), e);
    }
  }

  /**
   * Used for populating the help file on the {@code &lt;a:credentials .../&gt;}
   * tag.  For more details see {@code /lib/auth/credentials.jelly}.
   */
  public static String getHelpFile() {
    return Jenkins.getInstance()
        .getDescriptorOrDie(GoogleRobotPrivateKeyCredentials.class)
        .getHelpFile("credentials");
  }

  /**
   * Return the {@code client_secrets.json} path on the master.
   */
  public String getSecretsFile() {
    return secretsFile;
  }
  private final String secretsFile;

  /**
   * Retrieve the "client_secrets.json" file as a set of service-account
   * secrets.
   *
   * @throws IOException if there are issues interacting with the secrets file.
   */
  @VisibleForTesting
  GoogleRobotSecrets getRobotSecrets() throws IOException {
    return GoogleRobotSecrets.load(
        getModule().getJsonFactory(), new FileInputStream(getSecretsFile()));
  }

  /**
   * Return the {@code private....p12} path on the master.
   */
  public String getP12File() {
    return p12File;
  }
  private final String p12File;

  @Override
  public CredentialsScope getScope() {
    return CredentialsScope.GLOBAL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GoogleCredential getGoogleCredential(
      GoogleOAuth2ScopeRequirement requirement)
      throws GeneralSecurityException {
    try {
      GoogleRobotSecrets robotSecrets = getRobotSecrets();

      return new GoogleCredential.Builder()
          .setTransport(getModule().getHttpTransport())
          .setJsonFactory(getModule().getJsonFactory())
          .setServiceAccountScopes(requirement.getScopes())
          .setServiceAccountPrivateKeyFromP12File(new File(getP12File()))
          .setServiceAccountId(robotSecrets.getWeb().getClientEmail())
          .build();
    } catch (IOException e) {
      throw new GeneralSecurityException(
          Messages.GoogleRobotPrivateKeyCredentials_ExceptionString(), e);
    }
  }

  /**
   * Descriptor for our unlimited service account extension.
   */
  @Extension
  public static class Descriptor
      extends AbstractGoogleRobotCredentialsDescriptor {
    public Descriptor() {
      this(new GoogleRobotCredentialsModule());
    }

    @VisibleForTesting
    Descriptor(GoogleRobotCredentialsModule module) {
      super(GoogleRobotPrivateKeyCredentials.class, module);
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
      return Messages.GoogleRobotPrivateKeyCredentials_DisplayName();
    }

    // TODO(mattmoor): We should beef up our form validation, including:
    //  - validate secretsFile (prev or new should be specified)
    //  - validate p12File (prev or new should be specified)
    //  - some sort of test connection to validate the credentials?
  }
}
