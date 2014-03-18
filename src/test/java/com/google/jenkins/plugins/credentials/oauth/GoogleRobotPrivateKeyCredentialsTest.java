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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.fileupload.FileItem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_OK;
import static com.google.common.io.ByteStreams.copy;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.common.io.Files;

import hudson.util.FormValidation;
import jenkins.model.Jenkins;

/**
 * Tests for {@link GoogleRobotPrivateKeyCredentials}.
 */
public class GoogleRobotPrivateKeyCredentialsTest {
  private MockHttpTransport transport;
  private MockLowLevelHttpRequest request;

  private void stubRequest(String url, int statusCode,
      String responseContent) throws IOException {
    request.setResponse(new MockLowLevelHttpResponse()
        .setStatusCode(statusCode)
        .setContent(responseContent));
    doReturn(request).when(transport).buildRequest("POST", url);
  }

  private void verifyRequest(String url) throws IOException {
    verify(transport).buildRequest("POST", url);
    verify(request).execute();
  }

  // Allow for testing using JUnit4, instead of JUnit3.
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Mock
  private FileItem mockFileItem;

  private String secretsFileData;

  private String secretsPath;
  private String privateKeyPath;
  private GoogleRobotCredentialsModule module;

  @Mock
  private GoogleCredential credential;

  @Mock
  private java.security.PrivateKey privateKey;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    transport = spy(new MockHttpTransport());
    request = spy(new MockLowLevelHttpRequest());

    this.secretsFileData = "{\"web\":{" +
        "\"auth_uri\":\"" + AUTH_URI + "\"," +
        "\"client_secret\":\"" + CLIENT_SECRET + "\"," +
        "\"token_uri\":\"" + TOKEN_URI + "\"," +
        "\"client_email\":\"" + CLIENT_EMAIL + "\"," +
        "\"redirect_uris\":[\"" + REDIRECT_URI + "\"]," +
        "\"client_x509_cert_url\":\"" + "\"," +
        "\"client_id\":\"" + CLIENT_ID + "\"," +
        "\"auth_provider_x509_cert_url\":\"" + "\"" +
        "}}";

    File temp = File.createTempFile("temp-file-name", ".tmp");
    // Emit the JSON content we want...
    {
      FileWriter writer = new FileWriter(temp);
      writer.write(secretsFileData);
      writer.close();
    }

    this.secretsPath = temp.getAbsolutePath();


    File tempP12 = File.createTempFile("temp-file-name", ".p12");

    // Emit the JSON content we want...
    {
      InputStream in = getClass().getClassLoader().getResourceAsStream(
          "com/google/jenkins/plugins/credentials/oauth/sample-privatekey.p12");
      FileOutputStream writer = new FileOutputStream(tempP12);
      copy(in, writer);
      writer.close();
    }

    this.privateKeyPath = tempP12.getAbsolutePath();

    module = new GoogleRobotCredentialsModule() {
        @Override
        public HttpTransport getHttpTransport() {
          return transport;
        }
      };
  }

  @Test
  public void basicRoundtripTest() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    GoogleRobotPrivateKeyCredentials newCreds =
        new GoogleRobotPrivateKeyCredentials(PROJECT_ID, "",
            mockFileItem, "", mockFileItem, null /* module */);

    when(mockFileItem.getSize()).thenReturn(0L);
    GoogleRobotPrivateKeyCredentials updateCreds =
        new GoogleRobotPrivateKeyCredentials(newCreds.getProjectId(),
            newCreds.getSecretsFile(), mockFileItem, newCreds.getP12File(),
            mockFileItem, null /* module */);

    assertEquals(newCreds.getId(), updateCreds.getId());
    assertEquals(newCreds.getProjectId(), updateCreds.getProjectId());
    assertEquals(newCreds.getP12File(), updateCreds.getP12File());
    assertEquals(newCreds.getSecretsFile(), updateCreds.getSecretsFile());
  }

  @Test
  public void explicitCredentialTest() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    GoogleRobotPrivateKeyCredentials newCreds =
        new GoogleRobotPrivateKeyCredentials(PROJECT_ID, this.secretsPath,
            mockFileItem, this.privateKeyPath, mockFileItem, module);

    GoogleCredential credential = newCreds.getGoogleCredential(
        new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertNotNull(credential);

    stubRequest("https://accounts.google.com/o/oauth2/token", STATUS_CODE_OK,
        "{\"access_token\":\"" + ACCESS_TOKEN + "\","
        + "\"expires_in\":1234,"
        + "\"token_type\":\"Bearer\"}");

    try {
      assertTrue(credential.refreshToken());
      assertEquals(ACCESS_TOKEN, credential.getAccessToken());
    } finally {
      verifyRequest("https://accounts.google.com/o/oauth2/token");
    }

    assertEquals(CredentialsScope.GLOBAL, newCreds.getScope());
  }

  @Test
  public void testGetUsername() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    GoogleRobotPrivateKeyCredentials newCreds =
        new GoogleRobotPrivateKeyCredentials(PROJECT_ID, this.secretsPath,
            mockFileItem, this.privateKeyPath, mockFileItem, module);

    assertEquals(CLIENT_EMAIL, newCreds.getUsername());
  }

  @Test
  public void testNewClientSecretsFile() throws Exception {
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(PROJECT_ID, "", mockFileItem,
            "", mockFileItem, null /* module */);

    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    HtmlForm form = jenkins.createWebClient().goTo("credentials")
        .getFormByName("config");

    // Set the value of the secrets file to something new:
    {
      HtmlFileInput secretsFileInput = form.getInputByName("secretsFile");
      secretsFileInput.setValueAttribute("client_secrets.json");
      secretsFileInput.setContentType("application/json");
      secretsFileInput.setData(secretsFileData.getBytes());
    }

    jenkins.submit(form);

    for (Credentials creds : SystemCredentialsProvider.getInstance()
             .getCredentials()) {
      if (creds instanceof GoogleRobotPrivateKeyCredentials) {
        GoogleRobotPrivateKeyCredentials grc =
            (GoogleRobotPrivateKeyCredentials) creds;

        // Everything should be the same, except for...
        assertEquals(credentials.getId(), grc.getId());
        assertEquals(credentials.getProjectId(), grc.getProjectId());
        assertEquals(credentials.getP12File(), grc.getP12File());

        // The secrets file, which should have a new path...
        assertTrue(!credentials.getSecretsFile().equals(grc.getSecretsFile()));

        // And that the new secrets file matches what we uploaded...
        String contents = Files.toString(
            new File(grc.getSecretsFile()), Charset.defaultCharset());
        assertEquals(contents, secretsFileData);
      }
    }
  }

  @Test
  public void testGetById() throws Exception {
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(PROJECT_ID, "", mockFileItem,
            "", mockFileItem, null /* module */);

    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    assertSame(credentials, GoogleRobotCredentials.getById(
        credentials.getId()));
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

  private static final long EXPIRATION_SECONDS = 1234;
  private static final String ACCESS_TOKEN = "ThE.ToKeN";
  private static final String AUTH_URI = "my auth URI";
  private static final String CLIENT_EMAIL = "jenkins@my.host.name";
  private static final String CLIENT_ID = "app_jenkins_foo";
  private static final String CLIENT_SECRET = "deadbeef";
  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String REDIRECT_URI =
      "http://my.host.name:8080/jenkins/securityRealm/login";
  private static final String TOKEN_URI = "my token uri";
  private static final String FAKE_SCOPE = "my.fake.scope";
}