/*
 * Copyright 2013 Google LLC
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

import org.codehaus.jackson.JsonParseException;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
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
  private static final long serialVersionUID = -6768343254941345944L;
  private static final Logger LOGGER = Logger.getLogger(
          GoogleRobotPrivateKeyCredentials.class.getSimpleName());
  private ServiceAccountConfig serviceAccountConfig;
  @Deprecated
  private transient String secretsFile;
  @Deprecated
  private transient String p12File;

  /**
   * Construct a set of service account credentials.
   *
   * @param projectId The project id associated with this service account
   * @param serviceAccountConfig The ServiceAccountConfig to use
   * @param module The module for instantiating dependent objects, or null.
   */
  @DataBoundConstructor
  public GoogleRobotPrivateKeyCredentials(String projectId,
          ServiceAccountConfig serviceAccountConfig,
          @Nullable GoogleRobotCredentialsModule module) throws Exception {
    super(projectId, module);
    this.serviceAccountConfig = serviceAccountConfig;
  }

  @SuppressWarnings("deprecation")
  public Object readResolve() {
    if (serviceAccountConfig == null) {
      String clientEmail = getClientEmailFromSecretsFileAndLogErrors();
      serviceAccountConfig =
          new P12ServiceAccountConfig(clientEmail, null, p12File);
    }
    return this;
  }

  private String getClientEmailFromSecretsFileAndLogErrors() {
    try {
      return getClientEmailFromSecretsFile();
    } catch (MissingSecretsFileException e) {
      LOGGER.log(Level.WARNING, String.format("SecretsFile is not set. " +
              "Failed to set Service Account E-Mail Address on upgraded " +
              "Credentials with Project Id '%s'.", getProjectId()));
    } catch (SecretsFileNotFoundException e) {
      LOGGER.log(Level.WARNING, String.format("SecretsFile could not be " +
              "found. Failed to set Service Account E-Mail Address on " +
              "upgraded Credentials with Project Id '%s'.", getProjectId()));
    } catch (InvalidSecretsFileException e) {
      LOGGER.log(Level.WARNING, String.format("Invalid SecretsFile format. " +
              "Failed to set Service Account E-Mail Address on upgraded " +
              "Credentials with Project Id '%s'.", getProjectId()));
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  private String getClientEmailFromSecretsFile()
          throws MissingSecretsFileException, SecretsFileNotFoundException,
          InvalidSecretsFileException {
    if (secretsFile == null) {
      throw new MissingSecretsFileException();
    }
    LegacyJsonKey legacyJsonKey;
    try {
      legacyJsonKey = LegacyJsonKey.load(getModule().getJsonFactory(),
              new FileInputStream(secretsFile));
    } catch (JsonParseException e) {
      throw new InvalidSecretsFileException(e);
    } catch (IOException e) {
      throw new SecretsFileNotFoundException(e);
    }
    LegacyJsonKey.Details web = legacyJsonKey.getWeb();
    if (web == null) {
      throw new InvalidSecretsFileException();
    }
    String clientEmail = web.getClientEmail();
    if (clientEmail == null) {
      throw new InvalidSecretsFileException();
    }
    return clientEmail;
  }

  /**
   * Used for populating the configuration for each {@link ServiceAccountConfig}
   *
   * @return list of possible {@link ServiceAccountConfig}s
   */
  public static List<ServiceAccountConfig.Descriptor>
  getServiceAccountConfigDescriptors() {
    Jenkins instance = Jenkins.getInstance();
    return ImmutableList.of(
            (ServiceAccountConfig.Descriptor) instance
                    .getDescriptorOrDie(JsonServiceAccountConfig.class),
            (ServiceAccountConfig.Descriptor) instance
                    .getDescriptorOrDie(P12ServiceAccountConfig.class));
  }

  @NonNull
  @Override
  public String getUsername() throws KeyTypeNotSetException,
          AccountIdNotSetException {
    if (serviceAccountConfig == null) {
      throw new KeyTypeNotSetException();
    }
    String accountId = serviceAccountConfig.getAccountId();
    if (accountId == null) {
      throw new AccountIdNotSetException();
    }
    return accountId;
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

  @Override
  public CredentialsScope getScope() {
    return CredentialsScope.GLOBAL;
  }

  @Override
  public GoogleCredential getGoogleCredential(
          GoogleOAuth2ScopeRequirement requirement)
          throws KeyTypeNotSetException, AccountIdNotSetException,
          PrivateKeyNotSetException {
    if (serviceAccountConfig == null) {
      throw new KeyTypeNotSetException();
    }
    if (serviceAccountConfig.getAccountId() == null) {
      throw new AccountIdNotSetException();
    }
    if (serviceAccountConfig.getPrivateKey() == null) {
      throw new PrivateKeyNotSetException();
    }
    return new GoogleCredential.Builder()
        .setTransport(getModule().getHttpTransport())
        .setJsonFactory(getModule().getJsonFactory())
        .setServiceAccountScopes(requirement.getScopes())
        .setServiceAccountId(serviceAccountConfig.getAccountId())
        .setServiceAccountPrivateKey(serviceAccountConfig.getPrivateKey())
        .build();
  }

  public ServiceAccountConfig getServiceAccountConfig() {
    return serviceAccountConfig;
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

  /**
   * Exception that gets thrown if SecretsFile is not set while upgrading
   * legacy {@link GoogleRobotPrivateKeyCredentials}
   */
  public static class MissingSecretsFileException extends RuntimeException {
  }

  /**
   * Exception that gets thrown if an invalid SecretsFile is set while upgrading
   * legacy {@link GoogleRobotPrivateKeyCredentials}
   */
  public static class InvalidSecretsFileException extends RuntimeException {
    public InvalidSecretsFileException() {
    }

    public InvalidSecretsFileException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Exception that gets thrown if SecretsFile could not be found while
   * upgrading legacy {@link GoogleRobotPrivateKeyCredentials}
   */
  public static class SecretsFileNotFoundException extends RuntimeException {
    public SecretsFileNotFoundException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Exception that gets thrown if ServiceAccountConfig is not set.
   */
  public static class KeyTypeNotSetException extends RuntimeException {
  }

  /**
   * Exception that gets thrown if AccountId is not set.
   */
  public static class AccountIdNotSetException extends RuntimeException {
  }

  /**
   * Exception that gets thrown if PrivateKey is not set.
   */
  public static class PrivateKeyNotSetException extends RuntimeException {
  }
}
