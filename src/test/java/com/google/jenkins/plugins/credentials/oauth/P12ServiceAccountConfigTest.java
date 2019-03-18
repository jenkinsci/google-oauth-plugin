/*
 * Copyright 2019 Google LLC
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyPair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.commons.fileupload.FileItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link P12ServiceAccountConfig}.
 */
public class P12ServiceAccountConfigTest {
  private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS =
      "service@account.com";
  private static KeyPair keyPair;
  private static String p12KeyPath;
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  @Mock
  private FileItem mockFileItem;

  @BeforeClass
  public static void preparePrivateKey() throws Exception {
    keyPair = P12ServiceAccountConfigTestUtil.generateKeyPair();
    p12KeyPath = P12ServiceAccountConfigTestUtil.createTempP12KeyFile(keyPair);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateWithNewP12KeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
        .thenReturn(new FileInputStream(p12KeyPath));
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12ServiceAccountConfig
        .getAccountId());
    assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithNullAccountId() throws Exception {
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(null, null, p12KeyPath);

    assertNull(p12ServiceAccountConfig.getAccountId());
    assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithNullP12KeyFile() throws Exception {
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        p12ServiceAccountConfig.getAccountId());
    assertNull(p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithEmptyP12KeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            p12ServiceAccountConfig.getAccountId());
    assertNull(p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateWithInvalidP12KeyFile() throws Exception {
    byte[] bytes = "invalidP12KeyFile".getBytes();
    when(mockFileItem.getSize()).thenReturn((long) bytes.length);
    when(mockFileItem.getInputStream())
        .thenReturn(new ByteArrayInputStream(bytes));
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        p12ServiceAccountConfig.getAccountId());
    assertNull(p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithPrevP12KeyFile() throws Exception {
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            p12KeyPath);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        p12ServiceAccountConfig.getAccountId());
    assertEquals(keyPair.getPrivate(), p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithEmptyPrevP12KeyFile() throws Exception {
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            "");

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        p12ServiceAccountConfig.getAccountId());
    assertNull(p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithInvalidPrevP12KeyFile() throws Exception {
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            "invalidPrevP12KeyFile.p12");

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        p12ServiceAccountConfig.getAccountId());
    assertNull(p12ServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testSerialization() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
        .thenReturn(new FileInputStream(p12KeyPath));
    P12ServiceAccountConfig p12ServiceAccountConfig =
        new P12ServiceAccountConfig(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SerializationUtil.serialize(p12ServiceAccountConfig, out);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    P12ServiceAccountConfig deserializedP12KeyType =
        SerializationUtil.deserialize(P12ServiceAccountConfig.class, in);

    assertTrue(new File(deserializedP12KeyType.getP12KeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        deserializedP12KeyType.getAccountId());
    assertEquals(keyPair.getPrivate(), deserializedP12KeyType.getPrivateKey());
  }
}
