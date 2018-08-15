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

import java.security.GeneralSecurityException;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

/**
 * Tests for {@link RemotableGoogleCredentials}.
 */
public class RemotableGoogleCredentialsTest {

  private GoogleCredential fakeCredential;

  @Mock
  private GoogleRobotCredentials mockCredentials;

  private TestGoogleOAuth2DomainRequirement testConsumer;

  private GoogleRobotCredentialsModule module;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    // Freeze time
    DateTime now = new DateTime();
    DateTimeUtils.setCurrentMillisFixed(now.getMillis());

    this.module = new GoogleRobotCredentialsModule();

    this.testConsumer = new TestGoogleOAuth2DomainRequirement(THE_SCOPE);
    this.fakeCredential = new GoogleCredential();

    when(mockCredentials.getProjectId()).thenReturn(PROJECT_ID);
    when(mockCredentials.getGoogleCredential(testConsumer))
        .thenReturn(fakeCredential);
    when(mockCredentials.getUsername()).thenReturn(USERNAME);
  }

  @Test
  public void testUsername() throws Exception {
    fakeCredential.setAccessToken(ACCESS_TOKEN);
    fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

    GoogleRobotCredentials credentials =
        new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
    Credential credential = credentials.getGoogleCredential(testConsumer);

    assertEquals(USERNAME, credentials.getUsername());
    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
  }

  @Test(expected = GeneralSecurityException.class)
  public void testNullExpirationBadRefresh() throws Exception {
    new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
  }

  @Test(expected = GeneralSecurityException.class)
  public void testImminentExpirationBadRefresh() throws Exception {
    fakeCredential.setExpiresInSeconds(IMMINENT_EXPIRATION_SECONDS);
    new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
  }

  @Test
  public void testReasonableExpiration() throws Exception {
    fakeCredential.setAccessToken(ACCESS_TOKEN);
    fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

    GoogleRobotCredentials credentials =
        new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
    Credential credential = credentials.getGoogleCredential(testConsumer);

    assertEquals(ACCESS_TOKEN, credential.getAccessToken());
    assertThat(credential.getExpiresInSeconds().doubleValue(),
        closeTo(EXPIRATION_SECONDS, 2));
  }

  public void testName() throws Exception {
    fakeCredential.setAccessToken(ACCESS_TOKEN);
    fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

    GoogleRobotCredentials credentials =
        new RemotableGoogleCredentials(mockCredentials, testConsumer, module);

    assertEquals("RemotableGoogleCredentials",
        CredentialsNameProvider.name(credentials));
  }


  @Test(expected = UnsupportedOperationException.class)
  public void testUnsupportedDescriptor() throws Exception {
    fakeCredential.setAccessToken(ACCESS_TOKEN);
    fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

    GoogleRobotCredentials credentials =
        new RemotableGoogleCredentials(mockCredentials, testConsumer, module);

    credentials.getDescriptor();
  }

  private static final long ERROR = 1;  // 1 second error
  private static final long IMMINENT_EXPIRATION_SECONDS = 60;
  private static final long EXPIRATION_SECONDS = 1234;
  private static final String USERNAME = "theUserName";
  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String THE_SCOPE = "my.scope";
  private static final String BAD_SCOPE = "NOT.my.scope";
  private static final String ACCESS_TOKEN = "ThE.ToKeN";
}