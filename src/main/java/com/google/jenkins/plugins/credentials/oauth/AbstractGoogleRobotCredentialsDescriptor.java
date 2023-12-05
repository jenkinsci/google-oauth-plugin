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

import com.cloudbees.plugins.credentials.ContextInPath;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.User;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;

/**
 * The descriptor for Google robot account credential extensions.
 *
 * @author Matt Moore
 */
public abstract class AbstractGoogleRobotCredentialsDescriptor extends CredentialsDescriptor {
  protected AbstractGoogleRobotCredentialsDescriptor(
      Class<? extends GoogleRobotCredentials> clazz, GoogleRobotCredentialsModule module) {
    super(clazz);
    this.module = checkNotNull(module);
  }

  protected AbstractGoogleRobotCredentialsDescriptor(
      Class<? extends GoogleRobotCredentials> clazz) {
    this(clazz, new GoogleRobotCredentialsModule());
  }

  /** The module to use for instantiating depended upon resources */
  public GoogleRobotCredentialsModule getModule() {
    return module;
  }

  private final GoogleRobotCredentialsModule module;

  /** Validate project-id entries */
  public FormValidation doCheckProjectId(@QueryParameter String projectId) {
    if (!Strings.isNullOrEmpty(projectId)) {
      return FormValidation.ok();
    } else {
      return FormValidation.error(Messages.GoogleRobotMetadataCredentials_ProjectIDError());
    }
  }

  @CheckForNull
  private static FormValidation checkForDuplicates(
      String value, ModelObject context, ModelObject object) {
    CredentialsMatcher withId = CredentialsMatchers.withId(value);
    for (CredentialsStore store : CredentialsProvider.lookupStores(object)) {
      if (!store.hasPermission(CredentialsProvider.VIEW)) {
        continue;
      }
      ModelObject storeContext = store.getContext();
      for (Domain domain : store.getDomains()) {
        for (Credentials match : CredentialsMatchers.filter(store.getCredentials(domain), withId)) {
          if (storeContext == context) {
            return FormValidation.error("This ID is already in use");
          } else {
            CredentialsScope scope = match.getScope();
            if (scope != null && !scope.isVisible(context)) {
              // scope is not exported to child contexts
              continue;
            }
            return FormValidation.warning(
                "The ID ‘%s’ is already in use in %s",
                value,
                storeContext instanceof Item
                    ? ((Item) storeContext).getFullDisplayName()
                    : storeContext.getDisplayName());
          }
        }
      }
    }
    return null;
  }

  public final FormValidation doCheckId(
      @ContextInPath ModelObject context, @QueryParameter String value) {
    if (value.isEmpty()) {
      return FormValidation.ok();
    }
    if (!value.matches("[a-zA-Z0-9_.-]+")) { // anything else considered kosher?
      return FormValidation.error("Unacceptable characters");
    }
    FormValidation problem = checkForDuplicates(value, context, context);
    if (problem != null) {
      return problem;
    }
    if (!(context instanceof User)) {
      User me = User.current();
      if (me != null) {
        problem = checkForDuplicates(value, context, me);
        if (problem != null) {
          return problem;
        }
      }
    }
    if (!(context instanceof Jenkins)) {
      // CredentialsProvider.lookupStores(User) does not return SystemCredentialsProvider.
      Jenkins j = Jenkins.get();
      problem = checkForDuplicates(value, context, j);
      if (problem != null) {
        return problem;
      }
    }
    return FormValidation.ok();
  }

  /** For {@link java.io.Serializable} */
  private static final long serialVersionUID = 1L;
}
