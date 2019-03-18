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

import org.kohsuke.stapler.QueryParameter;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.google.common.base.Strings;

import hudson.util.FormValidation;

/**
 * The descriptor for Google robot account credential extensions.
 *
 * @author Matt Moore
 */
public abstract class AbstractGoogleRobotCredentialsDescriptor
    extends CredentialsDescriptor {
  protected AbstractGoogleRobotCredentialsDescriptor(
      Class<? extends GoogleRobotCredentials> clazz,
      GoogleRobotCredentialsModule module) {
    super(clazz);
    this.module = checkNotNull(module);
  }

  protected AbstractGoogleRobotCredentialsDescriptor(
      Class<? extends GoogleRobotCredentials> clazz) {
    this(clazz, new GoogleRobotCredentialsModule());
  }

  /**
   * The module to use for instantiating depended upon resources
   */
  public GoogleRobotCredentialsModule getModule() {
    return module;
  }
  private final GoogleRobotCredentialsModule module;

  /**
   * Validate project-id entries
   */
  public FormValidation doCheckProjectId(@QueryParameter String projectId) {
    if (!Strings.isNullOrEmpty(projectId)) {
      return FormValidation.ok();
    } else {
      return FormValidation.error(
          Messages.GoogleRobotMetadataCredentials_ProjectIDError());
    }
  }

  /**
   * For {@link Serializable}
   */
  private static final long serialVersionUID = 1L;
}
