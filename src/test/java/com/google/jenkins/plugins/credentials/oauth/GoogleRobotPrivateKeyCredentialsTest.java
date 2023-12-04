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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import hudson.util.FormValidation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyPair;
import jenkins.model.Jenkins;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for {@link GoogleRobotPrivateKeyCredentials}. */
public class GoogleRobotPrivateKeyCredentialsTest {
  private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS = "service@account.com";
  private static final String ACCESS_TOKEN = "ThE.ToKeN";
  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String FAKE_SCOPE = "my.fake.scope";
  private static final String CREDENTIAL_ID = "credential.id";
  private static final String DESCRIPTION = "credential.description";
  private static KeyPair keyPair;
  private static String jsonKeyPath;
  private static String p12KeyPath;
  private static String legacyJsonKeyPath;
  @Rule public JenkinsRule jenkins = new JenkinsRule();
  private MockHttpTransport transport;
  private MockLowLevelHttpRequest request;
  @Mock private FileItem mockFileItem;
  private GoogleRobotCredentialsModule module;

  @BeforeClass
  public static void preparePrivateKey() throws Exception {
    keyPair = P12ServiceAccountConfigTestUtil.generateKeyPair();
    jsonKeyPath =
        JsonServiceAccountConfigTestUtil.createTempJsonKeyFile(
            SERVICE_ACCOUNT_EMAIL_ADDRESS, keyPair.getPrivate());
    p12KeyPath = P12ServiceAccountConfigTestUtil.createTempP12KeyFile(keyPair);
    legacyJsonKeyPath =
        LegacyJsonServiceAccountConfigUtil.createTempLegacyJsonKeyFile(
            SERVICE_ACCOUNT_EMAIL_ADDRESS);
  }

  private static void setPrivateField(
      GoogleRobotPrivateKeyCredentials credentials, String fieldName, Object value)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = GoogleRobotPrivateKeyCredentials.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(credentials, value);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    transport = spy(new MockHttpTransport());
    request = spy(new MockLowLevelHttpRequest());

    module =
        new GoogleRobotCredentialsModule() {
          @Override
          public HttpTransport getHttpTransport() {
            return transport;
          }
        };
  }

  @Test
  public void testCreatePrivateKeyCredentialsWithJsonKeyType() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
    jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(
            CredentialsScope.GLOBAL, "", PROJECT_ID, "", jsonServiceAccountConfig, module);

    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, credentials.getUsername());

    GoogleCredential googleCredential =
        credentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertNotNull(googleCredential);

    stubRequest(
        "https://oauth2.googleapis.com/token",
        HttpStatusCodes.STATUS_CODE_OK,
        "{\"access_token\":\""
            + ACCESS_TOKEN
            + "\","
            + "\"expires_in\":1234,"
            + "\"token_type\":\"Bearer\"}");

    try {
      assertTrue(googleCredential.refreshToken());
      assertEquals(ACCESS_TOKEN, googleCredential.getAccessToken());
    } finally {
      verifyRequest("https://oauth2.googleapis.com/token");
    }
  }

  @Test
  public void testCreatePrivateKeyCredentialsWithP12KeyType() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(p12KeyPath);
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(p12KeyPath)));
    P12ServiceAccountConfig keyType = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
    keyType.setP12KeyFileUpload(mockFileItem);
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(
            CredentialsScope.GLOBAL, "", PROJECT_ID, "", keyType, module);

    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, credentials.getUsername());

    GoogleCredential googleCredential =
        credentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertNotNull(googleCredential);

    stubRequest(
        "https://oauth2.googleapis.com/token",
        HttpStatusCodes.STATUS_CODE_OK,
        "{\"access_token\":\""
            + ACCESS_TOKEN
            + "\","
            + "\"expires_in\":1234,"
            + "\"token_type\":\"Bearer\"}");

    try {
      assertTrue(googleCredential.refreshToken());
      assertEquals(ACCESS_TOKEN, googleCredential.getAccessToken());
    } finally {
      verifyRequest("https://oauth2.googleapis.com/token");
    }
  }

  private void stubRequest(String url, int statusCode, String responseContent) throws IOException {
    request.setResponse(
        new MockLowLevelHttpResponse().setStatusCode(statusCode).setContent(responseContent));
    doReturn(request).when(transport).buildRequest("POST", url);
  }

  private void verifyRequest(String url) throws IOException {
    verify(transport).buildRequest("POST", url);
    verify(request).execute();
  }

  @Test
  public void testCreatePrivateKeyCredentialsWithNullKeyType() throws Exception {
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, module);

    try {
      credentials.getUsername();
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.KeyTypeNotSetException ignored) {
    }

    try {
      credentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.KeyTypeNotSetException ignored) {
    }
  }

  @Test
  public void testUpgradeLegacyCredentials() throws Exception {
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "secretsFile", legacyJsonKeyPath);
    setPrivateField(legacyCredentials, "p12File", p12KeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, upgradedCredentials.getUsername());
    GoogleCredential googleCredential =
        upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
    assertEquals(keyPair.getPrivate(), googleCredential.getServiceAccountPrivateKey());
  }

  @Test
  public void testUpgradeLegacyCredentialsWithoutSecretsFile() throws Exception {
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "p12File", p12KeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    try {
      upgradedCredentials.getUsername();
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
    try {
      upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
  }

  @Test
  public void testUpgradeLegacyCredentialsWithMissingWebObject() throws Exception {
    String legacyJsonKeyFileWithMissingWebObject =
        LegacyJsonServiceAccountConfigUtil.createTempLegacyJsonKeyFileWithMissingWebObject();
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "secretsFile", legacyJsonKeyFileWithMissingWebObject);
    setPrivateField(legacyCredentials, "p12File", p12KeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    try {
      upgradedCredentials.getUsername();
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
    try {
      upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
  }

  @Test
  public void testUpgradeLegacyCredentialsWithMissingClientEmail() throws Exception {
    String legacyJsonKeyFileWithMissingClientEmail =
        LegacyJsonServiceAccountConfigUtil.createTempLegacyJsonKeyFileWithMissingClientEmail();
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "secretsFile", legacyJsonKeyFileWithMissingClientEmail);
    setPrivateField(legacyCredentials, "p12File", p12KeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    try {
      upgradedCredentials.getUsername();
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
    try {
      upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
  }

  @Test
  public void testUpgradeLegacyCredentialsWithInvalidSecretsFile() throws Exception {
    String invalidLegacyJsonKeyFile =
        LegacyJsonServiceAccountConfigUtil.createTempInvalidLegacyJsonKeyFile();
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "secretsFile", invalidLegacyJsonKeyFile);
    setPrivateField(legacyCredentials, "p12File", p12KeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    try {
      upgradedCredentials.getUsername();
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
    try {
      upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
  }

  @Test
  public void testUpgradeLegacyCredentialsWithNotExistendSecretsFile() throws Exception {
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "secretsFile", "/notExistendSecretsFile");
    setPrivateField(legacyCredentials, "p12File", p12KeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    try {
      upgradedCredentials.getUsername();
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
    try {
      upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.AccountIdNotSetException ignored) {
    }
  }

  @Test
  public void testUpgradeLegacyCredentialsWithoutP12File() throws Exception {
    GoogleRobotPrivateKeyCredentials legacyCredentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, null);
    setPrivateField(legacyCredentials, "secretsFile", legacyJsonKeyPath);
    GoogleRobotPrivateKeyCredentials upgradedCredentials =
        (GoogleRobotPrivateKeyCredentials) legacyCredentials.readResolve();

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, upgradedCredentials.getUsername());
    try {
      upgradedCredentials.getGoogleCredential(new TestGoogleOAuth2DomainRequirement(FAKE_SCOPE));
      fail();
    } catch (GoogleRobotPrivateKeyCredentials.PrivateKeyNotSetException ignored) {
    }
  }

  @Test
  public void testGetById() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
    jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(
            CredentialsScope.GLOBAL, "", PROJECT_ID, "", jsonServiceAccountConfig, null);

    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    assertSame(credentials, GoogleRobotCredentials.getById(credentials.getId()));
  }

  // TODO(mattmoor): redundant with GoogleRobotMetadataCredentials since there
  // isn't a shared descriptor for validating common fields.
  @Test
  public void testProjectIdValidation() throws Exception {
    GoogleRobotPrivateKeyCredentials.Descriptor descriptor =
        (GoogleRobotPrivateKeyCredentials.Descriptor)
            Jenkins.getInstance().getDescriptorOrDie(GoogleRobotPrivateKeyCredentials.class);

    assertEquals(FormValidation.Kind.OK, descriptor.doCheckProjectId(PROJECT_ID).kind);
    assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckProjectId(null).kind);
    assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckProjectId("").kind);
  }

  @Test
  public void testName() throws Exception {
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(CredentialsScope.GLOBAL, "", PROJECT_ID, "", null, module);
    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    assertEquals(PROJECT_ID, CredentialsNameProvider.name(credentials));
    assertEquals(PROJECT_ID, new GoogleRobotNameProvider().getName(credentials));
  }

  @Test
  public void testCredentialCreationWithNonEmptyIdAndDescriptionAndJsonKey() throws Exception {
    // GIVEN: Setup the mock and configuration for JSON key
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
    jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);

    // WHEN: creating credential with defined id and description
    GoogleRobotPrivateKeyCredentials credentials =
            new GoogleRobotPrivateKeyCredentials(
                    CredentialsScope.SYSTEM, CREDENTIAL_ID, PROJECT_ID,  DESCRIPTION, jsonServiceAccountConfig, module);

    // THEN: resulting credential should have our defined id and description
    assertEquals(CREDENTIAL_ID, credentials.getId());
    assertEquals(DESCRIPTION, credentials.getDescription());
  }

  @Test
  public void testCredentialCreationWithNonEmptyIdAndDescriptionAndP12() throws Exception {
    // GIVEN: Setup the mock and configuration for P12 key
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(p12KeyPath);
    when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(p12KeyPath));
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(p12KeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
    jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);

    // WHEN: creating credential with defined id and description
    GoogleRobotPrivateKeyCredentials credentials =
            new GoogleRobotPrivateKeyCredentials(
                    CredentialsScope.SYSTEM, CREDENTIAL_ID, PROJECT_ID,  DESCRIPTION, jsonServiceAccountConfig, module);

    // THEN: resulting credential should have our defined id and description
    assertEquals(CREDENTIAL_ID, credentials.getId());
    assertEquals(DESCRIPTION, credentials.getDescription());
  }

  @Test
  public void testCredentialCreationWithSystemScope() throws Exception {
    // GIVEN: Setup the mock and configuration
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
    jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);

    // WHEN: creating a credential with SYSTEM scope
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(
            CredentialsScope.SYSTEM, "", PROJECT_ID, "", jsonServiceAccountConfig, module);

    // THEN: the resulting credential should have SYSTEM scope
    assertEquals(CredentialsScope.SYSTEM, credentials.getScope());
  }

  @Test
  public void testCredentialCreationWithGlobalScope() throws Exception {
    // GIVEN: Setup the mock and configuration
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
    when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
    jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);

    // WHEN: creating a credential with GLOBAL scope
    GoogleRobotPrivateKeyCredentials credentials =
        new GoogleRobotPrivateKeyCredentials(
            CredentialsScope.GLOBAL, "", PROJECT_ID, "", jsonServiceAccountConfig, module);

    // THEN: the resulting credential should have GLOBAL scope
    assertEquals(CredentialsScope.GLOBAL, credentials.getScope());
  }
}
