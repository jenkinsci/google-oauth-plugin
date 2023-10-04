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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainRestrictedCredentials;
import com.google.api.client.googleapis.compute.ComputeCredential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.jenkins.plugins.util.ExecutorException;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An implementation of {@link GoogleRobotCredentials} that produces OAuth2 access tokens using the
 * metadata service attached to Google Compute instances. These Credentials are inherently limited
 * to the set of OAuth2 scopes that a Google Compute instance is bound to at startup.
 *
 * <p>NOTE: This plugin is only available to Jenkins masters running on a Google Compute Engine
 * virtual machine.
 *
 * @author Matt Moore
 */
@NameWith(value = GoogleRobotNameProvider.class, priority = 50)
public final class GoogleRobotMetadataCredentials extends GoogleRobotCredentials
    implements DomainRestrictedCredentials {

  /**
   * Construct a set of service account credentials with a specific id. It helps for updating
   * credentials, as well as for migrating old credentials that had no id and relied on the project
   * id.
   *
   * @param id the id to assign
   * @param projectId The Pantheon project id associated with this service account
   * @param module The module for instantiating dependent objects, or null.
   */
  @DataBoundConstructor
  public GoogleRobotMetadataCredentials(
      @CheckForNull CredentialsScope scope,
      String id,
      String projectId,
      @Nullable GoogleRobotMetadataCredentialsModule module)
      throws Exception {
    super(scope, id, projectId, module);
  }

  @SuppressFBWarnings(
      value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification =
          "For migration purposes: older credentials might not have had separate id or "
              + "scope fields. These fields might be null when deserializing. The readResolve "
              + "method sets defaults for null id and scope. Id defaults to getProjectId() and "
              + "scope defaults to CredentialsScope.GLOBAL if null.")
  private Object readResolve() throws Exception {
    return new GoogleRobotMetadataCredentials(
        getScope() == null ? CredentialsScope.GLOBAL : getScope(),
        getId() == null ? getProjectId() : getId(),
        getProjectId(),
        getModule());
  }

  /** {@inheritDoc} */
  @Override
  public GoogleRobotMetadataCredentialsModule getModule() {
    // down-cast to our required type
    return (GoogleRobotMetadataCredentialsModule) super.getModule();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized boolean matches(List<DomainRequirement> requirements) {
    if (metadataScopes == null) {
      metadataScopes =
          new Domain(
              "metadata",
              "",
              ImmutableList.of(
                  new GoogleOAuth2ScopeSpecification(getDescriptor().defaultScopes())));
    }
    return metadataScopes.test(requirements);
  }

  @Nullable private transient Domain metadataScopes;

  /** {@inheritDoc} */
  @Override
  public String getUsername() {
    try {
      return getModule().getMetadataReader().readMetadata(IDENTITY_PATH);
    } catch (ExecutorException | IOException e) {
      throw new IllegalStateException(
          Messages.GoogleRobotMetadataCredentials_DefaultIdentityError(), e);
    }
  }

  /**
   * The endpoint of the {@code METADATA_SERVER} for resolving the identity (email) of the service
   * account.
   */
  private static final String IDENTITY_PATH = "/instance/service-accounts/default/email";

  /** {@inheritDoc} */
  @Override
  public ComputeCredential getGoogleCredential(GoogleOAuth2ScopeRequirement requirement)
      throws GeneralSecurityException {
    // Ideally GCE would allow us to down-scope the metadata credentials we are
    // providing a given library.
    return new ComputeCredential(getModule().getHttpTransport(), getModule().getJsonFactory());
  }

  /** {@inheritDoc} */
  @Override
  public Descriptor getDescriptor() {
    return (Descriptor) super.getDescriptor();
  }

  /** Descriptor for our unlimited service account extension. */
  public static class Descriptor extends AbstractGoogleRobotCredentialsDescriptor {
    /**
     * This factory method determines whether the host machine has an associated metadata server,
     * and if so registers the metadata-based robot credential.
     */
    @Extension
    @Nullable
    public static Descriptor metadataDescriptor() throws IOException {
      if (disableForTesting) {
        // For unit testing, we want to provide custom startup logic, in
        // particular, we want to unconditionally instantiate it and do so with
        // a module where we can mock out the MetadataReader's HttpTransport
        // layer.
        return null;
      }

      GoogleRobotMetadataCredentialsModule defaultModule =
          new GoogleRobotMetadataCredentialsModule();
      if (defaultModule.getMetadataReader().hasMetadata()) {
        // Otherwise only instantiate the metadata credential if we are on a
        // machine with metadata.
        return new Descriptor(defaultModule);
      }
      return null;
    }

    /** Used by unit testing to take control of how the descriptor is instantiated by Jenkins. */
    @VisibleForTesting static boolean disableForTesting = false;

    @VisibleForTesting
    Descriptor(GoogleRobotMetadataCredentialsModule module) {
      super(GoogleRobotMetadataCredentials.class, module);
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
      return Messages.GoogleRobotMetadataCredentials_DisplayName();
    }

    /** {@inheritDoc} */
    @Override
    public GoogleRobotMetadataCredentialsModule getModule() {
      return (GoogleRobotMetadataCredentialsModule) super.getModule();
    }

    /**
     * When we are running on GCE, we should be able to pre-populate the {@code projectId} field
     * with the "right" project id.
     *
     * @return the project associated with this GCE instance, or null.
     */
    @Nullable
    public String defaultProject() {
      try {
        return getModule().getMetadataReader().readMetadata(PROJECT_ID_PATH);
      } catch (ExecutorException | IOException e) {
        return null;
      }
    }

    /**
     * When we are running on GCE, we should be able to pre-populate the {@code projectId} field
     * with the "right" project id.
     *
     * @return the project associated with this GCE instance, or null.
     */
    public List<String> defaultScopes() {
      try {
        String scopes = getModule().getMetadataReader().readMetadata(SCOPES_PATH);

        return Lists.newArrayList(Splitter.on('\n').trimResults().omitEmptyStrings().split(scopes));
      } catch (ExecutorException | IOException e) {
        return ImmutableList.of();
      }
    }

    /** This is the metadata endpoint for retrieving the project-id. */
    private static final String PROJECT_ID_PATH = "/project/project-id";

    /**
     * This is the metadata endpoint for retrieving the set of oauth scopes to which the host
     * instance is limited.
     */
    private static final String SCOPES_PATH = "/instance/service-accounts/default/scopes";
  }

  /** For {@link java.io.Serializable} */
  private static final long serialVersionUID = 1L;
}
