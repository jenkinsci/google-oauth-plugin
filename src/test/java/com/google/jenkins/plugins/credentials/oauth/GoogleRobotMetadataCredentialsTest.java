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

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_NOT_FOUND;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_OK;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.OAuth2Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.common.base.Joiner;
import com.google.jenkins.plugins.util.MetadataReader;
import hudson.Extension;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests for {@link GoogleRobotMetadataCredentials}. */
@WithJenkins
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleRobotMetadataCredentialsTest {

    private static final String METADATA_ENDPOINT =
            OAuth2Utils.getMetadataServerUrl() + "/computeMetadata/v1/" + "instance/service-accounts/default/token";
    private static final String USERNAME = "bazinga";
    private static final String ACCESS_TOKEN = "ThE.ToKeN";
    private static final String PROJECT_ID = "foo.com:bar-baz";
    private static final String FAKE_SCOPE = "my.fake.scope";
    private static final String CREDENTIAL_ID = "credential.id";
    private static final String DESCRIPTION = "credential.description";
    private static final List<String> SCOPES = List.of("scope1", "scope2", "scope3");

    @Mock
    private GoogleCredential credential;

    @BeforeEach
    void setUp() {
        // Make sure that OUR @Extension creates the descriptor when
        // Jenkins starts up by disabling the factory method.
        GoogleRobotMetadataCredentials.Descriptor.disableForTesting = true;
    }

    @Test
    @WithoutJenkins
    void accessTokenTest() throws Exception {
        final Module module = new Module();

        GoogleRobotMetadataCredentials newCreds =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", module);

        Credential cred = newCreds.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));

        module.stubRequest(
                METADATA_ENDPOINT,
                STATUS_CODE_OK,
                "{\"access_token\":\"" + ACCESS_TOKEN + "\"," + "\"expires_in\":1234," + "\"token_type\":\"Bearer\"}");

        try {
            assertTrue(cred.refreshToken());
            assertEquals(ACCESS_TOKEN, cred.getAccessToken());
        } finally {
            module.verifyRequest(METADATA_ENDPOINT);
        }
    }

    @Test
    @WithoutJenkins
    void getUsernameTest() throws Exception {
        final Module module = new Module();
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", module);

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/email",
                STATUS_CODE_OK,
                USERNAME);
        assertEquals(USERNAME, credentials.getUsername());
        assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    }

    @Test
    @WithoutJenkins
    void getUsernameWithNotFoundExceptionTest() throws Exception {
        final Module module = new Module();
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", module);
        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/email",
                STATUS_CODE_NOT_FOUND,
                USERNAME);
        // Expected to throw
        assertThrows(IllegalStateException.class, credentials::getUsername);
    }

    @Test
    @WithoutJenkins
    void getUsernameWithUnknownIOExceptionTest() throws Exception {
        final Module module = new Module();
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", module);
        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/email", 409, USERNAME);
        // Expected to throw
        assertThrows(IllegalStateException.class, credentials::getUsername);
    }

    @Test
    void defaultProjectTest(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.GLOBAL, "", "doesn't matter", "", null /* module */);

        final GoogleRobotMetadataCredentials.Descriptor descriptor = credentials.getDescriptor();

        final Module module = (Module) descriptor.getModule();

        module.stubRequest("http://metadata/computeMetadata/v1/project/project-id", STATUS_CODE_OK, PROJECT_ID);
        assertEquals(PROJECT_ID, descriptor.defaultProject());
    }

    @Test
    void defaultProjectNotFoundTest(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.GLOBAL, "", "doesn't matter", "", null /* module */);

        final GoogleRobotMetadataCredentials.Descriptor descriptor = credentials.getDescriptor();

        final Module module = (Module) descriptor.getModule();

        module.stubRequest("http://metadata/computeMetadata/v1/project/project-id", STATUS_CODE_NOT_FOUND, PROJECT_ID);

        assertNull(descriptor.defaultProject());
    }

    @Test
    void defaultProjectUnknownIOExceptionTest(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.GLOBAL, "", "doesn't matter", "", null /* module */);

        final GoogleRobotMetadataCredentials.Descriptor descriptor = credentials.getDescriptor();

        final Module module = (Module) descriptor.getModule();

        module.stubRequest("http://metadata/computeMetadata/v1/project/project-id", 409, PROJECT_ID);

        assertNull(descriptor.defaultProject());
    }

    @Test
    void defaultScopesTest(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.GLOBAL, "", "doesn't matter", "", null /* module */);

        final GoogleRobotMetadataCredentials.Descriptor descriptor = credentials.getDescriptor();

        final Module module = (Module) descriptor.getModule();

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/scopes",
                STATUS_CODE_OK,
                Joiner.on("\n").join(SCOPES));
        assertEquals(SCOPES, descriptor.defaultScopes());
    }

    @Test
    void defaultScopesNotFoundTest(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.GLOBAL, "", "doesn't matter", "", null /* module */);

        final GoogleRobotMetadataCredentials.Descriptor descriptor = credentials.getDescriptor();

        final Module module = (Module) descriptor.getModule();

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/scopes",
                STATUS_CODE_NOT_FOUND,
                Joiner.on("\n").join(SCOPES));
        assertEquals(0, descriptor.defaultScopes().size());
    }

    @Test
    void defaultScopesUnknownIOExceptionTest(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.GLOBAL, "", "doesn't matter", "", null /* module */);

        final GoogleRobotMetadataCredentials.Descriptor descriptor = credentials.getDescriptor();

        final Module module = (Module) descriptor.getModule();

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/scopes",
                409,
                Joiner.on("\n").join(SCOPES));
        assertEquals(0, descriptor.defaultScopes().size());
    }

    @Test
    void testGetById(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null /* module */);
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
        Module module = (Module) credentials.getDescriptor().getModule();

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/scopes",
                STATUS_CODE_OK,
                "does.not.Matter");

        assertSame(credentials, GoogleRobotCredentials.getById(credentials.getId()));
    }

    // TODO(mattmoor): Figure out why this flakes out so much under testing
    @Test
    void testName(JenkinsRule jenkins) throws Exception {
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null /* module */);
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

        assertEquals(PROJECT_ID, CredentialsNameProvider.name(credentials));
        assertEquals(PROJECT_ID, new GoogleRobotNameProvider().getName(credentials));
    }

    @Test
    void testProjectIdValidation(JenkinsRule jenkins) {
        GoogleRobotMetadataCredentials.Descriptor descriptor = (GoogleRobotMetadataCredentials.Descriptor)
                Jenkins.getInstance().getDescriptorOrDie(GoogleRobotMetadataCredentials.class);

        assertEquals(FormValidation.Kind.OK, descriptor.doCheckProjectId(PROJECT_ID).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckProjectId(null).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckProjectId("").kind);
    }

    @Test
    void testCredentialCreationWithNonEmptyIdAndDescription(JenkinsRule jenkins) throws Exception {
        final Module module = new Module();

        // WHEN: creating credential with defined id and description
        GoogleRobotMetadataCredentials credentials = new GoogleRobotMetadataCredentials(
                CredentialsScope.SYSTEM, CREDENTIAL_ID, PROJECT_ID, DESCRIPTION, module);

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/email",
                STATUS_CODE_OK,
                USERNAME);

        // THEN: resulting credential should have our defined id and description
        assertEquals(CREDENTIAL_ID, credentials.getId());
        assertEquals(DESCRIPTION, credentials.getDescription());
    }

    @Test
    void testCredentialCreationWithSystemScope(JenkinsRule jenkins) throws Exception {
        final Module module = new Module();

        // WHEN: creating a credential with SYSTEM scope
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.SYSTEM, "", PROJECT_ID, "", module);

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/email",
                STATUS_CODE_OK,
                USERNAME);

        // THEN: the resulting credential should have SYSTEM scope
        assertEquals(CredentialsScope.SYSTEM, credentials.getScope());
        assertEquals(USERNAME, credentials.getUsername());
    }

    @Test
    void testCredentialCreationWithGlobalScope(JenkinsRule jenkins) throws Exception {
        final Module module = new Module();

        // WHEN: creating a credential with GLOBAL scope
        GoogleRobotMetadataCredentials credentials =
                new GoogleRobotMetadataCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", module);

        module.stubRequest(
                "http://metadata/computeMetadata/v1/instance/" + "service-accounts/default/email",
                STATUS_CODE_OK,
                USERNAME);

        // THEN: the resulting credential should have GLOBAL scope
        assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
        assertEquals(USERNAME, credentials.getUsername());
    }

    /** */
    public static class Module extends GoogleRobotMetadataCredentialsModule {
        @Override
        public MetadataReader getMetadataReader() {
            return reader;
        }

        public final MockHttpTransport transport = spy(new MockHttpTransport());
        public final MetadataReader reader = new MetadataReader.Default(transport.createRequestFactory());
        public final MockLowLevelHttpRequest request = spy(new MockLowLevelHttpRequest());

        public void stubRequest(String url, int statusCode, String responseContent) throws IOException {
            request.setResponse(
                    new MockLowLevelHttpResponse().setStatusCode(statusCode).setContent(responseContent));
            doReturn(request).when(transport).buildRequest("GET", url);
        }

        private void verifyRequest(String url) throws IOException {
            verify(transport).buildRequest("GET", url);
            verify(request).execute();
            assertEquals("Google", getOnlyElement(request.getHeaderValues("Metadata-Flavor")));
        }

        @Override
        public HttpTransport getHttpTransport() {
            return transport;
        }
    }

    /** */
    @Extension
    public static class MockDescriptor extends GoogleRobotMetadataCredentials.Descriptor {
        public MockDescriptor() {
            super(new Module());
        }
    }
}
