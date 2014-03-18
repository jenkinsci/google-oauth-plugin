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

import java.util.Collection;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

/**
 * A Google-specific implementation of the {@link OAuth2ScopeSpecification}
 * that limits its application to Google-specific {@link OAuth2ScopeRequirement}
 */
public class GoogleOAuth2ScopeSpecification
    extends OAuth2ScopeSpecification<GoogleOAuth2ScopeRequirement> {
  @DataBoundConstructor
  public GoogleOAuth2ScopeSpecification(Collection<String> specifiedScopes) {
    super(specifiedScopes);
  }

  /**
   * Denoted this class is a {@code DomainSpecification} plugin, in particular
   * for {@link OAuth2ScopeSpecification}
   */
  @Extension
  public static class DescriptorImpl
      extends OAuth2ScopeSpecification.Descriptor {
    public DescriptorImpl() {
      super(GoogleOAuth2ScopeRequirement.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.GoogleOAuth2ScopeSpecification_DisplayName();
    }
  }
}