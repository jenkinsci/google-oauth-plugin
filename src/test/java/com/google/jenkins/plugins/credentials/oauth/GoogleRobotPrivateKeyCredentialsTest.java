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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyPair;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.commons.fileupload.FileItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

/**
 * Tests for {@link GoogleRobotPrivateKeyCredentials}.
 */
public class GoogleRobotPrivateKeyCredentialsTest {
  private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS =
          "service@account.com";
  private static final String ACCESS_TOKEN = "ThE.ToKeN";
  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String FAKE_SCOPE = "my.fake.scope";
  private static String jsonKeyPath;
  private static String p12KeyPath;
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();
  private MockHttpTransport transport;
  private MockLowLevelHttpRequest request;
  @Mock
  private FileItem mockFileItem;
  @Mock
  private GoogleCredential credential;
  private GoogleRobotCredentialsModule module;

  @BeforeClass
  public static void preparePrivateKey() throws Exception {
    KeyPair keyPair = P12KeyUtil.generateKeyPair();
    jsonKeyPath = JsonKeyUtil.createTempJsonKeyFile(
            SERVICE_ACCOUNT_EMAIL_ADDRESS, keyPair.getPrivate());
    p12KeyPath = P12KeyUtil.createTempP12KeyFile(keyPair);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    transport = spy(new MockHttpTransport());
    request = spy(new MockLowLevelHttpRequest());

    module = new GoogleRobotCredentialsModule() {
      @Override
      public HttpTransport getHttpTransport() {
        return transport;
      }
    };
  }

  @Test
  public void testCreatePrivateKeyCredentialsWithJsonKeyType()
          throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
            .thenReturn(new FileInputStream(jsonKeyPath));
    GoogleRobotPrivateKeyCredentials credentials =
            new GoogleRobotPrivateKeyCredentials(PROJECT_ID,
                    new JsonKeyType(mockFileItem, null), module);

    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, credentials.getUsername());

    GoogleCredential googleCredential = credentials.getGoogleCredential(
            new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertNotNull(googleCredential);

    stubRequest("https://accounts.google.com/o/oauth2/token",
            HttpStatusCodes.STATUS_CODE_OK,
            "{\"access_token\":\"" + ACCESS_TOKEN + "\","
                    + "\"expires_in\":1234,"
                    + "\"token_type\":\"Bearer\"}");

    try {
      assertTrue(googleCredential.refreshToken());
      assertEquals(ACCESS_TOKEN, googleCredential.getAccessToken());
    } finally {
      verifyRequest("https://accounts.google.com/o/oauth2/token");
    }
  }

  @Test
  public void testCreatePrivateKeyCredentialsWithP12KeyType() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
            .thenReturn(new FileInputStream(p12KeyPath));
    P12KeyType keyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);
    GoogleRobotPrivateKeyCredentials credentials =
            new GoogleRobotPrivateKeyCredentials(PROJECT_ID, keyType, module);

    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, credentials.getUsername());

    GoogleCredential googleCredential = credentials.getGoogleCredential(
            new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertNotNull(googleCredential);

    stubRequest("https://accounts.google.com/o/oauth2/token",
            HttpStatusCodes.STATUS_CODE_OK,
            "{\"access_token\":\"" + ACCESS_TOKEN + "\","
                    + "\"expires_in\":1234,"
                    + "\"token_type\":\"Bearer\"}");

    try {
      assertTrue(googleCredential.refreshToken());
      assertEquals(ACCESS_TOKEN, googleCredential.getAccessToken());
    } finally {
      verifyRequest("https://accounts.google.com/o/oauth2/token");
    }
  }

  private void stubRequest(String url, int statusCode, String responseContent)
          throws IOException {
    request.setResponse(new MockLowLevelHttpResponse()
            .setStatusCode(statusCode)
            .setContent(responseContent));
    doReturn(request).when(transport).buildRequest("POST", url);
  }

  private void verifyRequest(String url) throws IOException {
    verify(transport).buildRequest("POST", url);
    verify(request).execute();
  }

  @Test
  public void testCreatePrivateKeyCredentialsWithNullKeyType()
          throws Exception {
    GoogleRobotPrivateKeyCredentials credentials =
            new GoogleRobotPrivateKeyCredentials(PROJECT_ID, null, module);

    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    assertTrue(credentials.getUsername().isEmpty());

    GoogleCredential googleCredential = credentials.getGoogleCredential(
            new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertNull(googleCredential);
  }

  @Test
  public void testGetById() throws Exception {
    GoogleRobotPrivateKeyCredentials credentials =
            new GoogleRobotPrivateKeyCredentials(PROJECT_ID,
                    new JsonKeyType(mockFileItem, null), null);

    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    assertSame(credentials,
            GoogleRobotCredentials.getById(credentials.getId()));
  }

  // TODO(mattmoor): redundant with GoogleRobotMetadataCredentials since there
  // isn't a shared descriptor for validating common fields.
  @Test
  public void testProjectIdValidation() throws Exception {
    GoogleRobotPrivateKeyCredentials.Descriptor descriptor =
            (GoogleRobotPrivateKeyCredentials.Descriptor) Jenkins.getInstance()
                    .getDescriptorOrDie(GoogleRobotPrivateKeyCredentials.class);

    assertEquals(FormValidation.Kind.OK,
            descriptor.doCheckProjectId(PROJECT_ID).kind);
    assertEquals(FormValidation.Kind.ERROR,
            descriptor.doCheckProjectId(null).kind);
    assertEquals(FormValidation.Kind.ERROR,
            descriptor.doCheckProjectId("").kind);
  }

  // TODO(mattmoor): Figure out why this flakes out so much under testing
  // @Test
  // public void testName() throws Exception {
  //   GoogleRobotPrivateKeyCredentials credentials =
  //       new GoogleRobotPrivateKeyCredentials(PROJECT_ID, "", mockFileItem,
  //           "", mockFileItem, null /* module */);
  //   SystemCredentialsProvider.getInstance().getCredentials()
  //      .add(credentials);

  //   assertEquals(PROJECT_ID, CredentialsNameProvider.name(credentials));
  //   assertEquals(PROJECT_ID, new GoogleRobotNameProvider().getName(
  //       credentials));
  // }
}