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

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.domains.DomainRequirementProvider;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Objects;
import jenkins.model.Jenkins;

/**
 * The base implementation of service account (aka robot) credentials using OAuth2. These robot
 * credentials can be used to access Google APIs as the robot user.
 *
 * @author Matt Moore
 */
public abstract class GoogleRobotCredentials implements GoogleOAuth2Credentials {
  /**
   * Base constructor for populating the name and id for Google credentials.
   *
   * @param projectId The project id with which this credential is associated.
   * @param module The module to use for instantiating the dependencies of credentials.
   */
  protected GoogleRobotCredentials(String projectId, GoogleRobotCredentialsModule module) {
    this.id = IdCredentials.Helpers.fixEmptyId("");
    this.projectId = checkNotNull(projectId);

    if (module != null) {
      this.module = module;
    } else {
      this.module = getDescriptor().getModule();
    }
  }

  /** Fetch the module used for instantiating the dependencies of credentials */
  public GoogleRobotCredentialsModule getModule() {
    return module;
  }

  private String id;

  private final GoogleRobotCredentialsModule module;

  /** Retrieve a unique identifier that should be used to link to this object. */
  public String getId() {
    if (id == null) {
      id = projectId; // for migrating old credentials which had no id other than project id
    }
    return id;
  }

  /** {@inheritDoc} */
  @Override
  public String getDescription() {
    return Messages.GoogleRobotCredentials_Description();
  }

  /** {@inheritDoc} */
  @Override
  public AbstractGoogleRobotCredentialsDescriptor getDescriptor() {
    return (AbstractGoogleRobotCredentialsDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
  }

  /** {@inheritDoc} */
  @Override
  public Secret getAccessToken(GoogleOAuth2ScopeRequirement requirement) {
    try {
      Credential credential = getGoogleCredential(requirement);

      Long rawExpiration = credential.getExpiresInSeconds();
      if ((rawExpiration == null) || (rawExpiration < MINIMUM_DURATION_SECONDS)) {
        // Access token expired or is near expiration.
        if (!credential.refreshToken()) {
          return null;
        }
      }

      return Secret.fromString(credential.getAccessToken());
    } catch (IOException | GeneralSecurityException e) {
      return null;
    }
  }

  /* 3 minutes*/
  /** The minimum duration to allow for an access token before attempting to refresh it. */
  private static final Long MINIMUM_DURATION_SECONDS = 180L;

  /**
   * A trivial tuple for wrapping the list box of matched credentials with the requirements that
   * were used to filter them.
   */
  public static class CredentialsListBoxModel extends ListBoxModel {
    public CredentialsListBoxModel(GoogleOAuth2ScopeRequirement requirement) {
      this.requirement = requirement;
    }

    /** Retrieve the set of scopes for our requirement. */
    public Iterable<String> getScopes() {
      return requirement.getScopes();
    }

    private final GoogleOAuth2ScopeRequirement requirement;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      CredentialsListBoxModel options = (CredentialsListBoxModel) o;
      return Objects.equals(requirement, options.requirement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), requirement);
    }
  }

  /**
   * Helper utility for populating a jelly list box with matching {@link GoogleRobotCredentials} to
   * avoid listing credentials that avoids surfacing those with insufficient permissions.
   *
   * <p>Modeled after: http://developer-blog.cloudbees.com/2012/10/using-ssh-from-jenkins.html
   *
   * @param clazz The class annotated with @RequiresDomain indicating its scope requirements.
   * @return a list box populated solely with credentials compatible for the extension being
   *     configured.
   */
  public static CredentialsListBoxModel getCredentialsListBox(Class<?> clazz) {
    GoogleOAuth2ScopeRequirement requirement =
        DomainRequirementProvider.of(clazz, GoogleOAuth2ScopeRequirement.class);

    if (requirement == null) {
      throw new IllegalArgumentException(
          Messages.GoogleRobotCredentials_NoAnnotation(clazz.getSimpleName()));
    }

    CredentialsListBoxModel listBox = new CredentialsListBoxModel(requirement);
    Iterable<GoogleRobotCredentials> allGoogleCredentials =
        CredentialsProvider.lookupCredentials(
            GoogleRobotCredentials.class, Jenkins.get(), ACL.SYSTEM, ImmutableList.of(requirement));

    for (GoogleRobotCredentials credentials : allGoogleCredentials) {
      String name = CredentialsNameProvider.name(credentials);
      listBox.add(name, credentials.getId());
    }
    return listBox;
  }

  /** Retrieves the {@link GoogleRobotCredentials} identified by {@code id}. */
  public static GoogleRobotCredentials getById(String id) {
    Iterable<GoogleRobotCredentials> allGoogleCredentials =
        CredentialsProvider.lookupCredentials(
            GoogleRobotCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());

    for (GoogleRobotCredentials credentials : allGoogleCredentials) {
      if (credentials.getId().equals(id)) {
        return credentials;
      }
    }
    return null;
  }

  /** Retrieve a version of the credential that can be used on a remote machine. */
  public GoogleRobotCredentials forRemote(GoogleOAuth2ScopeRequirement requirement)
      throws GeneralSecurityException {
    if (this instanceof RemotableGoogleCredentials) {
      return this;
    } else {
      return new RemotableGoogleCredentials(this, requirement, getModule());
    }
  }

  /** Retrieve the project id for this credential */
  public String getProjectId() {
    return projectId;
  }

  private final String projectId;
}
