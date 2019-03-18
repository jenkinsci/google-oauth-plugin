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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;

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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link JsonServiceAccountConfig}.
 */
public class JsonServiceAccountConfigTest {
  private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS =
      "service@account.com";
  private static PrivateKey privateKey;
  private static String jsonKeyPath;
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  @Mock
  private FileItem mockFileItem;

  @BeforeClass
  public static void preparePrivateKey() throws Exception {
    privateKey = JsonServiceAccountConfigTestUtil.generatePrivateKey();
    jsonKeyPath = JsonServiceAccountConfigTestUtil.createTempJsonKeyFile(
        SERVICE_ACCOUNT_EMAIL_ADDRESS, privateKey);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateJsonKeyTypeWithNewJsonKeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
        .thenReturn(new FileInputStream(jsonKeyPath));
    JsonServiceAccountConfig jsonKeyType =
        new JsonServiceAccountConfig(mockFileItem, null);

    assertTrue(new File(jsonKeyType.getJsonKeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonKeyType.getAccountId());
    assertEquals(privateKey, jsonKeyType.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithNullParameters() throws Exception {
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, null);

    assertNull(jsonServiceAccountConfig.getJsonKeyFile());
    assertNull(jsonServiceAccountConfig.getAccountId());
    assertNull(jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithEmptyJsonKeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    JsonServiceAccountConfig jsonKeyType = new JsonServiceAccountConfig
        (mockFileItem, null);

    assertNull(jsonKeyType.getJsonKeyFile());
    assertNull(jsonKeyType.getAccountId());
    assertNull(jsonKeyType.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithInvalidJsonKeyFile() throws Exception {
    byte[] bytes = "invalidJsonKeyFile".getBytes();
    when(mockFileItem.getSize()).thenReturn((long) bytes.length);
    when(mockFileItem.getInputStream())
        .thenReturn(new ByteArrayInputStream(bytes));
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(mockFileItem, null);

    assertNull(jsonServiceAccountConfig.getJsonKeyFile());
    assertNull(jsonServiceAccountConfig.getAccountId());
    assertNull(jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithPrevJsonKeyFile() throws Exception {
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, jsonKeyPath);

    assertTrue(new File(jsonServiceAccountConfig.getJsonKeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        jsonServiceAccountConfig.getAccountId());
    assertEquals(privateKey, jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithEmptyPrevJsonKeyFile() throws Exception {
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, "");

    assertNull(jsonServiceAccountConfig.getJsonKeyFile());
    assertNull(jsonServiceAccountConfig.getAccountId());
    assertNull(jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithInvalidPrevJsonKeyFile()
      throws Exception {
    String invalidPrevJsonKeyFile = "invalidPrevJsonKeyFile.json";
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, invalidPrevJsonKeyFile);

    assertEquals(invalidPrevJsonKeyFile,
        jsonServiceAccountConfig.getJsonKeyFile());
    assertNull(jsonServiceAccountConfig.getAccountId());
    assertNull(jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testSerialization() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
        .thenReturn(new FileInputStream(jsonKeyPath));
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(mockFileItem, null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SerializationUtil.serialize(jsonServiceAccountConfig, out);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    JsonServiceAccountConfig deserializedJsonKeyType =
        SerializationUtil.deserialize(JsonServiceAccountConfig.class, in);

    assertTrue(new File(deserializedJsonKeyType.getJsonKeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        deserializedJsonKeyType.getAccountId());
    assertEquals(privateKey, deserializedJsonKeyType.getPrivateKey());
  }
}
