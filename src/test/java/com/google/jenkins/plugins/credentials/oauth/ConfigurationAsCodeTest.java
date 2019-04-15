/*
 * Copyright 2019 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SecretBytes;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

/** Tests that the credentials are correctly processed by the Configuration as Code plugin. */
public class ConfigurationAsCodeTest {

  @Rule public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

  @Test
  @ConfiguredWithCode("json-service-account-config.yml")
  public void supportsConfigurationWithJsonServiceAccountConfig() throws IOException {
    List<GoogleRobotPrivateKeyCredentials> credentialsList =
        CredentialsProvider.lookupCredentials(GoogleRobotPrivateKeyCredentials.class);
    assertNotNull(credentialsList);
    assertEquals("No credentials created", 1, credentialsList.size());
    GoogleRobotPrivateKeyCredentials credentials = credentialsList.get(0);
    assertNotNull(credentials);
    JsonServiceAccountConfig config =
        (JsonServiceAccountConfig) credentials.getServiceAccountConfig();
    assertNotNull(config);
    assertNull(config.getFilename());
    assertNull(config.getJsonKeyFile());
    assertNull(config.getJsonKeyFileUpload());
    assertNull(config.getPrivateKey()); // Because private_key is not valid.
    SecretBytes bytes = config.getSecretJsonKey();
    assertEquals("test-account@test-project.iam.gserviceaccount.com", config.getAccountId());
    String actualBytes = new String(bytes.getPlainData(), StandardCharsets.UTF_8);
    String expectedBytes = IOUtils.toString(this.getClass().getResourceAsStream("test-key.json"));
    assertEquals("Failed to configure secretJsonKey correctly.", expectedBytes, actualBytes);
  }

  @Test
  @ConfiguredWithCode("p12-service-account-config.yml")
  public void supportsConfigurationWithP12ServiceAccountConfig() {
    List<GoogleRobotPrivateKeyCredentials> credentialsList =
        CredentialsProvider.lookupCredentials(GoogleRobotPrivateKeyCredentials.class);
    assertNotNull(credentialsList);
    assertEquals("No credentials created", 1, credentialsList.size());
    GoogleRobotPrivateKeyCredentials credentials = credentialsList.get(0);
    assertNotNull(credentials);
    P12ServiceAccountConfig config =
        (P12ServiceAccountConfig) credentials.getServiceAccountConfig();
    assertNotNull(config);
    assertNull(config.getFilename());
    assertNull(config.getP12KeyFile());
    assertNull(config.getP12KeyFileUpload());
    // Because the bytes do not form a valid p12 key file.
    assertNull(config.getPrivateKey());
    assertEquals("test-account@test-project.iam.gserviceaccount.com", config.getEmailAddress());
    assertEquals(config.getEmailAddress(), config.getAccountId());
    SecretBytes bytes = config.getSecretP12Key();
    String actualBytes = new String(bytes.getPlainData(), StandardCharsets.UTF_8);
    String expectedBytes = "test-p12-key";
    assertEquals("Failed to configure secretP12Key correctly", expectedBytes, actualBytes);
  }
}
