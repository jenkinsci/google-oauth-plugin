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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.SecretBytes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.KeyPair;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link P12ServiceAccountConfig}. */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class P12ServiceAccountConfigTest {
    private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS = "service@account.com";
    private static KeyPair keyPair;
    private static String p12KeyPath;

    @Mock
    private FileItem mockFileItem;

    @BeforeAll
    static void preparePrivateKey() throws Exception {
        keyPair = P12ServiceAccountConfigTestUtil.generateKeyPair();
        p12KeyPath = P12ServiceAccountConfigTestUtil.createTempP12KeyFile(keyPair);
    }

    @Test
    void testCreateWithNewP12KeyFile(JenkinsRule jenkinsRule) throws Exception {
        when(mockFileItem.getSize()).thenReturn(1L);
        when(mockFileItem.getName()).thenReturn(p12KeyPath);
        when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(p12KeyPath)));
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setP12KeyFileUpload(mockFileItem);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    void testCreateWithNullAccountId(JenkinsRule jenkinsRule) throws Exception {
        SecretBytes prev = SecretBytes.fromBytes(FileUtils.readFileToByteArray(new File(p12KeyPath)));
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(null);
        p12ServiceAccountConfig.setFilename(p12KeyPath);
        p12ServiceAccountConfig.setSecretP12Key(prev);

        assertNull(p12ServiceAccountConfig.getAccountId());
        assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    @WithoutJenkins
    void testCreateWithNullP12KeyFile(JenkinsRule jenkinsRule) {
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertNull(p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    @WithoutJenkins
    void testCreateWithEmptyP12KeyFile(JenkinsRule jenkinsRule) {
        when(mockFileItem.getSize()).thenReturn(0L);
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setP12KeyFileUpload(mockFileItem);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertNull(p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    void testCreateWithInvalidP12KeyFile(JenkinsRule jenkinsRule) {
        byte[] bytes = "invalidP12KeyFile".getBytes();
        when(mockFileItem.getSize()).thenReturn((long) bytes.length);
        when(mockFileItem.getName()).thenReturn("invalidP12KeyFile");
        when(mockFileItem.get()).thenReturn(bytes);
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setP12KeyFileUpload(mockFileItem);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertNull(p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    void testCreateWithPrevP12KeyFileForCompatibility(JenkinsRule jenkinsRule) {
        P12ServiceAccountConfig p12ServiceAccountConfig =
                new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS, null, p12KeyPath);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    void testCreateWithPrevP12KeyFile(JenkinsRule jenkinsRule) throws Exception {
        SecretBytes prev = SecretBytes.fromBytes(FileUtils.readFileToByteArray(new File(p12KeyPath)));
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setFilename(p12KeyPath);
        p12ServiceAccountConfig.setSecretP12Key(prev);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    void testCreateWithEmptyPrevP12KeyFile(JenkinsRule jenkinsRule) {
        SecretBytes prev = SecretBytes.fromString("");
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setFilename("");
        p12ServiceAccountConfig.setSecretP12Key(prev);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertNull(p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    @WithoutJenkins
    void testCreateWithInvalidPrevP12KeyFile(JenkinsRule jenkinsRule) {
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setFilename("invalidPrevP12KeyFile.p12");

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig.getAccountId());
        assertNull(p12ServiceAccountConfig.getPrivateKey());
    }

    @Test
    void testSerialization(JenkinsRule jenkinsRule) throws Exception {
        when(mockFileItem.getSize()).thenReturn(1L);
        when(mockFileItem.getName()).thenReturn(p12KeyPath);
        when(mockFileItem.get()).thenReturn(FileUtils.readFileToByteArray(new File(p12KeyPath)));
        P12ServiceAccountConfig p12ServiceAccountConfig = new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS);
        p12ServiceAccountConfig.setP12KeyFileUpload(mockFileItem);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializationUtil.serialize(p12ServiceAccountConfig, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        P12ServiceAccountConfig deserializedP12KeyType =
                SerializationUtil.deserialize(P12ServiceAccountConfig.class, in);

        assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, deserializedP12KeyType.getAccountId());
        assertEquals(keyPair.getPrivate(), deserializedP12KeyType.getPrivateKey());
    }
}
