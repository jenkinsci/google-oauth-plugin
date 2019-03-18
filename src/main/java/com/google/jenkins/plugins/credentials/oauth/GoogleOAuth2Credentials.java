/*
 * Copyright 2019 Google LLC
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

import java.security.GeneralSecurityException;

import com.google.api.client.auth.oauth2.Credential;

/**
 * Google-specific username / access token combination.
 *
 * <p>Implementations surface an API for obtaining the Google-standard
 * {@link Credential} object for interacting with OAuth2 APIs.
 */
public interface GoogleOAuth2Credentials
    extends StandardUsernameOAuth2Credentials<GoogleOAuth2ScopeRequirement> {
  /**
   * Fetches a Credential for the set of OAuth 2.0 scopes required.
   *
   * @param requirement The set of required OAuth 2.0 scopes
   * @return The Credential authorizing usage of the API scopes
   * @throws GeneralSecurityException when the authentication fails
   */
  Credential getGoogleCredential(GoogleOAuth2ScopeRequirement requirement)
      throws GeneralSecurityException;
}
