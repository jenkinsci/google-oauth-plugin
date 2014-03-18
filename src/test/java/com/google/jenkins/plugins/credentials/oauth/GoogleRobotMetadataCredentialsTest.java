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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_NOT_FOUND;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_OK;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.util.MetadataReader;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

/**
 * Tests for {@link GoogleRobotMetadataCredentials}.
 */
public class GoogleRobotMetadataCredentialsTest {

  // Allow for testing using JUnit4, instead of JUnit3.
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Mock
  private GoogleCredential credential;

  /**
   */
  public static class Module extends GoogleRobotMetadataCredentialsModule {
    @Override
    public MetadataReader getMetadataReader() {
      return reader;
    }

    public final MockHttpTransport transport = spy(new MockHttpTransport());
    public final MetadataReader reader = new MetadataReader.Default(
        transport.createRequestFactory());
    public final MockLowLevelHttpRequest request =
        spy(new MockLowLevelHttpRequest());

    public void stubRequest(String url, int statusCode,
        String responseContent) throws IOException {
      request.setResponse(new MockLowLevelHttpResponse()
          .setStatusCode(statusCode)
          .setContent(responseContent));
      doReturn(request).when(transport).buildRequest("GET", url);
    }

    private void verifyRequest(String url) throws IOException {
      verify(transport).buildRequest("GET", url);
      verify(request).execute();
      // TODO(mattmoor): When ComputeCredentials switches to the v1 endpoint
      // it will have to specify this header.
      // assertEquals("true", getOnlyElement(request.getHeaderValues(
      //     "X-Google-Metadata-Request")));
    }

    @Override
    public HttpTransport getHttpTransport() {
      return transport;
    }
  }

  /**
   */
  @Extension
  public static class MockDescriptor
      extends GoogleRobotMetadataCredentials.Descriptor {
    public MockDescriptor() {
      super(new Module());
    }
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    // Make sure that OUR @Extension creates the descriptor when
    // Jenkins starts up by disabling the factory method.
    GoogleRobotMetadataCredentials.Descriptor.disableForTesting = true;
  }

  @Test
  @WithoutJenkins
  public void basicRoundtripTest() throws Exception {
    final Module module = new Module();
    GoogleRobotMetadataCredentials newCreds =
        new GoogleRobotMetadataCredentials(PROJECT_ID, module);

    GoogleRobotMetadataCredentials updateCreds =
        new GoogleRobotMetadataCredentials(newCreds.getProjectId(),
            module);

    assertEquals(newCreds.getId(), updateCreds.getId());
    assertEquals(newCreds.getProjectId(), updateCreds.getProjectId());
  }

  @Test
  @WithoutJenkins
  public void accessTokenTest() throws Exception {
    final Module module = new Module();

    GoogleRobotMetadataCredentials newCreds =
        new GoogleRobotMetadataCredentials(PROJECT_ID, module);

    Credential cred = newCreds.getGoogleCredential(
        new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));

    module.stubRequest(METADATA_ENDPOINT, STATUS_CODE_OK,
        "{\"access_token\":\"" + ACCESS_TOKEN + "\","
        + "\"expires_in\":1234,"
        + "\"token_type\":\"Bearer\"}");

    try {
      assertTrue(cred.refreshToken());
      assertEquals(ACCESS_TOKEN, cred.getAccessToken());
    } finally {
      module.verifyRequest(METADATA_ENDPOINT);
    }
  }

  @Test
  @WithoutJenkins
  public void getUsernameTest() throws Exception {
    final Module module = new Module();
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials(PROJECT_ID, module);

    module.stubRequest("http://metadata/computeMetadata/v1/instance/" +
        "service-accounts/default/email", STATUS_CODE_OK, USERNAME);
    assertEquals(USERNAME, credentials.getUsername());
    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
  }

  @Test(expected = IllegalStateException.class)
  @WithoutJenkins
  public void getUsernameWithNotFoundExceptionTest() throws Exception {
    final Module module = new Module();
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials(PROJECT_ID, module);

    module.stubRequest("http://metadata/computeMetadata/v1/instance/" +
        "service-accounts/default/email", STATUS_CODE_NOT_FOUND, USERNAME);

    // Expected to throw
    credentials.getUsername();
  }

  @Test(expected = IllegalStateException.class)
  @WithoutJenkins
  public void getUsernameWithUnknownIOExceptionTest() throws Exception {
    final Module module = new Module();
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials(PROJECT_ID, module);

    module.stubRequest("http://metadata/computeMetadata/v1/instance/" +
        "service-accounts/default/email", 409, USERNAME);

    // Expected to throw
    credentials.getUsername();
  }

  @Test
  public void defaultProjectTest() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials("doesn't matter", null /* module */);

    final GoogleRobotMetadataCredentials.Descriptor descriptor =
        credentials.getDescriptor();

    final Module module = (Module) descriptor.getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/project/project-id",
        STATUS_CODE_OK, PROJECT_ID);
    assertEquals(PROJECT_ID, descriptor.defaultProject());
  }

  @Test
  public void defaultProjectNotFoundTest() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials("doesn't matter", null /* module */);

    final GoogleRobotMetadataCredentials.Descriptor descriptor =
        credentials.getDescriptor();

    final Module module = (Module) descriptor.getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/project/project-id",
        STATUS_CODE_NOT_FOUND, PROJECT_ID);

    assertNull(descriptor.defaultProject());
  }

  @Test
  public void defaultProjectUnknownIOExceptionTest() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials("doesn't matter", null /* module */);

    final GoogleRobotMetadataCredentials.Descriptor descriptor =
        credentials.getDescriptor();

    final Module module = (Module) descriptor.getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/project/project-id",
        409, PROJECT_ID);

    assertNull(descriptor.defaultProject());
  }

  @Test
  public void defaultScopesTest() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials("doesn't matter", null /* module */);

    final GoogleRobotMetadataCredentials.Descriptor descriptor =
        credentials.getDescriptor();

    final Module module = (Module) descriptor.getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/instance/"
        + "service-accounts/default/scopes", STATUS_CODE_OK,
        Joiner.on("\n").join(SCOPES));
    assertEquals(SCOPES, descriptor.defaultScopes());
  }

  @Test
  public void defaultScopesNotFoundTest() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials("doesn't matter", null /* module */);

    final GoogleRobotMetadataCredentials.Descriptor descriptor =
        credentials.getDescriptor();

    final Module module = (Module) descriptor.getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/instance/"
        + "service-accounts/default/scopes", STATUS_CODE_NOT_FOUND,
        Joiner.on("\n").join(SCOPES));
    assertEquals(0, descriptor.defaultScopes().size());
  }

  @Test
  public void defaultScopesUnknownIOExceptionTest() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials("doesn't matter", null /* module */);

    final GoogleRobotMetadataCredentials.Descriptor descriptor =
        credentials.getDescriptor();

    final Module module = (Module) descriptor.getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/instance/"
        + "service-accounts/default/scopes", 409,
        Joiner.on("\n").join(SCOPES));
    assertEquals(0, descriptor.defaultScopes().size());
  }

  @Test
  public void testGetById() throws Exception {
    GoogleRobotMetadataCredentials credentials =
        new GoogleRobotMetadataCredentials(PROJECT_ID, null /* module */);
    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
    Module module = (Module) credentials.getDescriptor().getModule();

    module.stubRequest("http://metadata/computeMetadata/v1/instance/"
        + "service-accounts/default/scopes", STATUS_CODE_OK, "does.not.Matter");

    assertSame(credentials, GoogleRobotCredentials.getById(
        credentials.getId()));
  }

  // TODO(mattmoor): Figure out why this flakes out so much under testing
  // @Test
  // public void testName() throws Exception {
  //   GoogleRobotMetadataCredentials credentials =
  //       new GoogleRobotMetadataCredentials(PROJECT_ID, null /* module */);
  //   SystemCredentialsProvider.getInstance().getCredentials()
  //      .add(credentials);

  //   assertEquals(PROJECT_ID, CredentialsNameProvider.name(credentials));
  //   assertEquals(PROJECT_ID, new GoogleRobotNameProvider().getName(
  //       credentials));
  // }

  @Test
  public void testProjectIdValidation() throws Exception {
    GoogleRobotMetadataCredentials.Descriptor descriptor =
        (GoogleRobotMetadataCredentials.Descriptor) Jenkins.getInstance()
        .getDescriptorOrDie(GoogleRobotMetadataCredentials.class);

    assertEquals(FormValidation.Kind.OK,
        descriptor.doCheckProjectId(PROJECT_ID).kind);
    assertEquals(FormValidation.Kind.ERROR,
        descriptor.doCheckProjectId(null).kind);
    assertEquals(FormValidation.Kind.ERROR,
        descriptor.doCheckProjectId("").kind);
  }

  private static String METADATA_ENDPOINT =
      "http://metadata/computeMetadata/v1beta1/"
      + "instance/service-accounts/default/token";
  private static final String USERNAME = "bazinga";
  private static final long EXPIRATION_SECONDS = 1234;
  private static final String ACCESS_TOKEN = "ThE.ToKeN";
  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String FAKE_SCOPE = "my.fake.scope";
  private static final List<String> SCOPES =
      ImmutableList.of("scope1", "scope2", "scope3");
}