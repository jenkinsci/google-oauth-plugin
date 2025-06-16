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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.security.GeneralSecurityException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link RemotableGoogleCredentials}. */
@ExtendWith(MockitoExtension.class)
class RemotableGoogleCredentialsTest {

    private static final long ERROR = 1; // 1 second error
    private static final long IMMINENT_EXPIRATION_SECONDS = 60;
    private static final long EXPIRATION_SECONDS = 1234;
    private static final String USERNAME = "theUserName";
    private static final String PROJECT_ID = "foo.com:bar-baz";
    private static final String THE_SCOPE = "my.scope";
    private static final String BAD_SCOPE = "NOT.my.scope";
    private static final String ACCESS_TOKEN = "ThE.ToKeN";

    private GoogleCredential fakeCredential;

    @Mock
    private GoogleRobotCredentials mockCredentials;

    private TestGoogleOAuth2DomainRequirement testConsumer;

    private GoogleRobotCredentialsModule module;

    @BeforeEach
    void setUp() throws Exception {
        // Freeze time
        DateTime now = new DateTime();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        this.module = new GoogleRobotCredentialsModule();

        this.testConsumer = new TestGoogleOAuth2DomainRequirement(THE_SCOPE);
        this.fakeCredential = new GoogleCredential();

        when(mockCredentials.getProjectId()).thenReturn(PROJECT_ID);
        when(mockCredentials.getGoogleCredential(testConsumer)).thenReturn(fakeCredential);
        when(mockCredentials.getUsername()).thenReturn(USERNAME);
    }

    @Test
    void testUsername() throws Exception {
        fakeCredential.setAccessToken(ACCESS_TOKEN);
        fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

        GoogleRobotCredentials credentials = new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
        Credential credential = credentials.getGoogleCredential(testConsumer);

        assertEquals(USERNAME, credentials.getUsername());
        assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    }

    @Test
    void testNullExpirationBadRefresh() {
        assertThrows(
                GeneralSecurityException.class,
                () -> new RemotableGoogleCredentials(mockCredentials, testConsumer, module));
    }

    @Test
    void testImminentExpirationBadRefresh() {
        fakeCredential.setExpiresInSeconds(IMMINENT_EXPIRATION_SECONDS);
        assertThrows(
                GeneralSecurityException.class,
                () -> new RemotableGoogleCredentials(mockCredentials, testConsumer, module));
    }

    @Test
    void testReasonableExpiration() throws Exception {
        fakeCredential.setAccessToken(ACCESS_TOKEN);
        fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

        GoogleRobotCredentials credentials = new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
        Credential credential = credentials.getGoogleCredential(testConsumer);

        assertEquals(ACCESS_TOKEN, credential.getAccessToken());
        assertThat(credential.getExpiresInSeconds().doubleValue(), closeTo(EXPIRATION_SECONDS, 2));
    }

    @Test
    void testName() throws Exception {
        fakeCredential.setAccessToken(ACCESS_TOKEN);
        fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

        GoogleRobotCredentials credentials = new RemotableGoogleCredentials(mockCredentials, testConsumer, module);

        assertThat(
                CredentialsNameProvider.name(credentials),
                matchesPattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    void testUnsupportedDescriptor() throws GeneralSecurityException {
        fakeCredential.setAccessToken(ACCESS_TOKEN);
        fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);
        GoogleRobotCredentials credentials = new RemotableGoogleCredentials(mockCredentials, testConsumer, module);
        assertThrows(UnsupportedOperationException.class, credentials::getDescriptor);
    }
}
