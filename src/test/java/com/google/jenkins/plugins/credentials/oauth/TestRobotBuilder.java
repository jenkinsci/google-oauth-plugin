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

import com.google.jenkins.plugins.credentials.domains.RequiresDomain;

import hudson.model.Descriptor;
import hudson.tasks.Builder;

/**
 * This is a trivial implementation of a {@link Builder} that
 * consumes {@link GoogleRobotCredentials}.
 */
@RequiresDomain(value = TestGoogleOAuth2DomainRequirement.class)
public class TestRobotBuilder extends Builder {
  public TestRobotBuilder() {
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return DESCRIPTOR;
  }

  /**
   * Descriptor for our trivial builder
   */
  public static final class DescriptorImpl
      extends Descriptor<Builder> {
    @Override
    public String getDisplayName() {
      return "Test Robot Builder";
    }
  }

  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
}