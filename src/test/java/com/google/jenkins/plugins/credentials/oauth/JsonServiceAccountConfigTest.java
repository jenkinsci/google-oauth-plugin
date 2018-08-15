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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.SecretBytes;

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
  @Rule
  public final ExpectedException exception = ExpectedException.none();
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
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.get())
        .thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonKeyType =
        new JsonServiceAccountConfig(mockFileItem, null, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonKeyType.getAccountId());
    assertEquals(privateKey, jsonKeyType.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithNullParameters() throws Exception {
    exception.expect(IllegalArgumentException.class);
    new JsonServiceAccountConfig(null, null, null);
  }

  @Test
  public void testCreateJsonKeyTypeWithEmptyJsonKeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    exception.expect(IllegalArgumentException.class);
    new JsonServiceAccountConfig(mockFileItem, null, null);
  }

  @Test
  public void testCreateJsonKeyTypeWithInvalidJsonKeyFile() throws Exception {
    byte[] bytes = "invalidJsonKeyFile".getBytes();
    when(mockFileItem.getSize()).thenReturn((long) bytes.length);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream())
        .thenReturn(new ByteArrayInputStream(bytes));
    when(mockFileItem.get())
        .thenReturn(bytes);
    exception.expect(IllegalArgumentException.class);
    new JsonServiceAccountConfig(mockFileItem, null, null);
  }

  @Test
  public void testCreateJsonKeyTypeWithPrevJsonKeyFileForCompatibility() throws Exception {
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, jsonKeyPath);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        jsonServiceAccountConfig.getAccountId());
    assertEquals(privateKey, jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithPrevJsonKeyFile() throws Exception {
    SecretBytes prev = SecretBytes.fromBytes(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, jsonKeyPath, prev);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        jsonServiceAccountConfig.getAccountId());
    assertEquals(privateKey, jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithEmptyPrevJsonKeyFile() throws Exception {
    SecretBytes prev = SecretBytes.fromString("");
    exception.expect(IllegalArgumentException.class);
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(null, "", prev);

    assertNull(jsonServiceAccountConfig.getAccountId());
    assertNull(jsonServiceAccountConfig.getPrivateKey());
  }

  @Test
  public void testCreateJsonKeyTypeWithInvalidPrevJsonKeyFile()
      throws Exception {
    exception.expect(IllegalArgumentException.class);
    String invalidPrevJsonKeyFile = "invalidPrevJsonKeyFile.json";
    new JsonServiceAccountConfig(null, invalidPrevJsonKeyFile, null);
  }

  @Test
  public void testSerialization() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getName()).thenReturn(jsonKeyPath);
    when(mockFileItem.getInputStream())
        .thenReturn(new FileInputStream(jsonKeyPath));
    when(mockFileItem.get())
        .thenReturn(FileUtils.readFileToByteArray(new File(jsonKeyPath)));
    JsonServiceAccountConfig jsonServiceAccountConfig =
        new JsonServiceAccountConfig(mockFileItem, null, null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SerializationUtil.serialize(jsonServiceAccountConfig, out);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    JsonServiceAccountConfig deserializedJsonKeyType =
        SerializationUtil.deserialize(JsonServiceAccountConfig.class, in);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
        deserializedJsonKeyType.getAccountId());
    assertEquals(privateKey, deserializedJsonKeyType.getPrivateKey());
  }
}
