/*
 * Copyright 2014 Google LLC
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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.SecretBytes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for {@link JsonServiceAccountConfig}. */
public class JsonServiceAccountConfigTest {
    private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS = "service@account.com";
    private static PrivateKey privateKey;
    private static String jsonKeyPath;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Mock
    private FileItem mockFileItem;

    @BeforeClass
    public static void preparePrivateKey() throws Exception {
        privateKey = JsonServiceAccountConfigTestUtil.generatePrivateKey();
        jsonKeyPath = JsonServiceAccountConfigTestUtil.createTempJsonKeyFile(SERVICE_ACCOUNT_EMAIL_ADDRESS, privateKey);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateJsonKeyTypeWithNewJsonKeyFile() throws Exception {
        when(mockFileItem.getSize()).thenReturn(1L);
        when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
        when(mockFileItem.getName()).thenReturn(jsonKeyPath);
        when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
        JsonServiceAccountConfig jsonKeyType = new JsonServiceAccountConfig();
        jsonKeyType.setJsonKeyFileUpload(mockFileItem);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonKeyType.getAccountId());
        assertEquals(privateKey, jsonKeyType.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithNullParameters() {
        JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();

        assertNull(jsonServiceAccountConfig.getAccountId());
        assertNull(jsonServiceAccountConfig.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithEmptyJsonKeyFile() throws Exception {
        when(mockFileItem.getSize()).thenReturn(0L);
        JsonServiceAccountConfig jsonKeyType = new JsonServiceAccountConfig();
        jsonKeyType.setJsonKeyFileUpload(mockFileItem);

        assertNull(jsonKeyType.getJsonKeyFile());
        assertNull(jsonKeyType.getAccountId());
        assertNull(jsonKeyType.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithInvalidJsonKeyFile() throws Exception {
        byte[] bytes = "invalidJsonKeyFile".getBytes();
        when(mockFileItem.getSize()).thenReturn((long) bytes.length);
        when(mockFileItem.getInputStream()).thenReturn(new ByteArrayInputStream(bytes));
        JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
        jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);

        assertNull(jsonServiceAccountConfig.getAccountId());
        assertNull(jsonServiceAccountConfig.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithPrevJsonKeyFileForCompatibility() {
        JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig(null, jsonKeyPath);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonServiceAccountConfig.getAccountId());
        assertEquals(privateKey, jsonServiceAccountConfig.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithPrevJsonKeyFile() throws Exception {
        SecretBytes prev = SecretBytes.fromBytes(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
        JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
        jsonServiceAccountConfig.setFilename(jsonKeyPath);
        jsonServiceAccountConfig.setSecretJsonKey(prev);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonServiceAccountConfig.getAccountId());
        assertEquals(privateKey, jsonServiceAccountConfig.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithEmptyPrevJsonKeyFile() {
        SecretBytes prev = SecretBytes.fromString("");
        JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
        jsonServiceAccountConfig.setFilename("");
        jsonServiceAccountConfig.setSecretJsonKey(prev);

        assertNull(jsonServiceAccountConfig.getAccountId());
        assertNull(jsonServiceAccountConfig.getPrivateKey());
    }

    @Test
    public void testCreateJsonKeyTypeWithInvalidPrevJsonKeyFile() {
        JsonServiceAccountConfig jsonServiceAccountConfig =
                new JsonServiceAccountConfig(null, "invalidPrevJsonKeyFile.json");

        assertNull(jsonServiceAccountConfig.getAccountId());
        assertNull(jsonServiceAccountConfig.getPrivateKey());
    }

    @Test
    public void testSerialization() throws Exception {
        when(mockFileItem.getSize()).thenReturn(1L);
        when(mockFileItem.getName()).thenReturn(jsonKeyPath);
        when(mockFileItem.getInputStream()).thenReturn(new FileInputStream(jsonKeyPath));
        when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
        JsonServiceAccountConfig jsonServiceAccountConfig = new JsonServiceAccountConfig();
        jsonServiceAccountConfig.setJsonKeyFileUpload(mockFileItem);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializationUtil.serialize(jsonServiceAccountConfig, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        JsonServiceAccountConfig deserializedJsonKeyType =
                SerializationUtil.deserialize(JsonServiceAccountConfig.class, in);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, deserializedJsonKeyType.getAccountId());
        assertEquals(privateKey, deserializedJsonKeyType.getPrivateKey());
    }
}
