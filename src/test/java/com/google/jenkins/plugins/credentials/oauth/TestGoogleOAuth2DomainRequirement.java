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

import java.util.Collection;
import java.util.Collections;

/** This is a trivial implementation of a {@link GoogleOAuth2ScopeRequirement}. */
public class TestGoogleOAuth2DomainRequirement extends GoogleOAuth2ScopeRequirement {
  private static final long serialVersionUID = 2234181311205118742L;

  public TestGoogleOAuth2DomainRequirement(String scope) {
    this.scope = scope;
  }

  @Override
  public Collection<String> getScopes() {
    return Collections.singletonList(scope);
  }

  private final String scope;
}
