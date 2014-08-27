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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

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
  private KeyType keyType;
  @Deprecated
  private String secretsFile = null;
  @Deprecated
  private String p12File = null;


  /**
   * Construct a set of service account credentials.
   *
   * @param projectId The project id associated with this service account
   * @param keyType The KeyType to use
   * @param module The module for instantiating dependent objects, or null.
   */
  @DataBoundConstructor
  public GoogleRobotPrivateKeyCredentials(String projectId, KeyType keyType,
          @Nullable GoogleRobotCredentialsModule module) throws Exception {
    super(projectId, module);
    this.keyType = keyType;
  }

  @SuppressWarnings("deprecation")
  public Object readResolve() {
    if (keyType == null && p12File != null && secretsFile != null) {
      try {
        JsonKeyLegacy jsonKeyLegacy = JsonKeyLegacy.load(
                getModule().getJsonFactory(), new FileInputStream(secretsFile));
        keyType = new P12KeyType(jsonKeyLegacy.getWeb().getClientEmail(), null,
                p12File);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to migrate keys", e);
      }
    }
    p12File = null;
    secretsFile = null;
    return this;
  }

  public static List<KeyType.Descriptor> getKeyTypeDescriptors() {
    Jenkins instance = Jenkins.getInstance();
    return Arrays.asList(
            (KeyType.Descriptor) instance.getDescriptorOrDie(JsonKeyType.class),
            (KeyType.Descriptor) instance.getDescriptorOrDie(P12KeyType.class));
  }

  @NonNull
  @Override
  public String getUsername() {
      GoogleCredential credential = getGoogleCredential(
          new GoogleOAuth2ScopeRequirement() {
            private static final long serialVersionUID = -8046870980553756366L;

            @Override
            public Collection<String> getScopes() {
              return ImmutableList.of();
            }
          });
    if (credential != null) {
      return credential.getServiceAccountId();
    }
    return "";
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
      GoogleOAuth2ScopeRequirement requirement) {
    if (keyType != null) {
      return new GoogleCredential.Builder()
          .setTransport(getModule().getHttpTransport())
          .setJsonFactory(getModule().getJsonFactory())
          .setServiceAccountScopes(requirement.getScopes())
          .setServiceAccountId(keyType.getAccountId())
          .setServiceAccountPrivateKey(keyType.getPrivateKey())
          .build();
    }
    return null;
  }

  public KeyType getKeyType() {
    return keyType;
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
