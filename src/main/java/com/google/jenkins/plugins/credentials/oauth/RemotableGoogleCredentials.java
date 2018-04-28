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

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Ordering;

/**
 * As some implementations of {@link GoogleRobotCredentials} are bound to the
 * master, this ephemeral credential is remoted in place of those.  The use
 * case is basically that when a plugin needs to remote credential C, with some
 * requirement R, it would instead remote {@code C.forRemote(R)} to
 * instantiate one of these.
 *
 * TODO(mattmoor): Consider ways to use channels to remove the time limitation
 * that this has (access token expires).
 *
 * @author Matt Moore
 */
final class RemotableGoogleCredentials extends GoogleRobotCredentials {
  /**
   * Construct a remotable credential.  This should never be used directly,
   * which is why this class is {@code package-private}.  This should only
   * be called from {@link GoogleRobotCredentials#forRemote}.
   */
  public RemotableGoogleCredentials(
      GoogleRobotCredentials credentials,
      GoogleOAuth2ScopeRequirement requirement,
      GoogleRobotCredentialsModule module) throws GeneralSecurityException {
    super(checkNotNull(credentials).getProjectId(), checkNotNull(module));

    this.username = credentials.getUsername();

    // Eagerly create the access token we will use on the remote machine.
    Credential credential = credentials.getGoogleCredential(
        checkNotNull(requirement));
    try {
      Long rawExpiration = credential.getExpiresInSeconds();

      if (Ordering.natural().nullsFirst().compare(
          rawExpiration, MINIMUM_DURATION_SECONDS) < 0) {
        if (!credential.refreshToken()) {
          throw new GeneralSecurityException(
              Messages.RemotableGoogleCredentials_NoAccessToken());
        }
      }
    } catch (IOException e) {
      throw new GeneralSecurityException(
          Messages.RemotableGoogleCredentials_NoAccessToken(), e);
    }
    this.accessToken = checkNotNull(credential.getAccessToken());
    this.expiration = new DateTime().plusSeconds(
        checkNotNull(credential.getExpiresInSeconds()).intValue()).
            getMillis();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CredentialsScope getScope() {
    return CredentialsScope.GLOBAL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AbstractGoogleRobotCredentialsDescriptor getDescriptor() {
    throw new UnsupportedOperationException(
        Messages.RemotableGoogleCredentials_BadGetDescriptor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUsername() {
    return username;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Credential getGoogleCredential(
      GoogleOAuth2ScopeRequirement requirement)
      throws GeneralSecurityException {
    // Return a credential synthesized from our stored access token
    // and expiration.
    //
    // TODO(mattmoor): Consider throwing an exception if the access token
    // has expired.
    long lifetimeSeconds =
        (expiration - new DateTime().getMillis()) / 1000;

    return new GoogleCredential.Builder()
        .setTransport(getModule().getHttpTransport())
        .setJsonFactory(getModule().getJsonFactory())
        .build()
        .setAccessToken(accessToken)
        .setExpiresInSeconds(lifetimeSeconds);
  }

  /**
   * The identity of the credential.
   */
  private final String username;

  /**
   * The access token eagerly retrieved from the original credential.
   */
  private final String accessToken;

  /**
   * The time at which the accessToken will expire.
   */
  private final long expiration;

  /**
   * The minimum duration to allow for an access token before attempting to
   * refresh it.
   */
  private static final Long MINIMUM_DURATION_SECONDS =
      new Long(5 * 60 /* 5 minutes */);
}
