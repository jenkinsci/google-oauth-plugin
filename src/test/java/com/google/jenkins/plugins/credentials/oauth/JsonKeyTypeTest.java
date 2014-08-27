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

import java.io.*;
import java.security.PrivateKey;

import static org.junit.Assert.*;
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
 * Tests for {@link JsonKeyType}.
 */
public class JsonKeyTypeTest {
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
    privateKey = JsonKeyUtil.generatePrivateKey();
    jsonKeyPath = JsonKeyUtil.createTempJsonKeyFile(
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
    JsonKeyType jsonKeyType = new JsonKeyType(mockFileItem, null);

    assertTrue(new File(jsonKeyType.getJsonKeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonKeyType.getAccountId());
    assertEquals(privateKey, jsonKeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateJsonKeyTypeWithNullParameters() throws Exception {
    JsonKeyType jsonKeyType = new JsonKeyType(null, null);

    assertNull(jsonKeyType.getJsonKeyFile());
    assertNull(jsonKeyType.getAccountId());
    assertNull(jsonKeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateJsonKeyTypeWithEmptyJsonKeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    JsonKeyType jsonKeyType = new JsonKeyType(mockFileItem, null);

    assertNull(jsonKeyType.getJsonKeyFile());
    assertNull(jsonKeyType.getAccountId());
    assertNull(jsonKeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateJsonKeyTypeWithInvalidJsonKeyFile() throws Exception {
    byte[] bytes = "invalidJsonKeyFile".getBytes();
    when(mockFileItem.getSize()).thenReturn((long) bytes.length);
    when(mockFileItem.getInputStream())
            .thenReturn(new ByteArrayInputStream(bytes));
    JsonKeyType jsonKeyType = new JsonKeyType(mockFileItem, null);

    assertNull(jsonKeyType.getJsonKeyFile());
    assertNull(jsonKeyType.getAccountId());
    assertNull(jsonKeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateJsonKeyTypeWithPrevJsonKeyFile() throws Exception {
    JsonKeyType jsonKeyType = new JsonKeyType(null, jsonKeyPath);

    assertTrue(new File(jsonKeyType.getJsonKeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, jsonKeyType.getAccountId());
    assertEquals(privateKey, jsonKeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateJsonKeyTypeWithEmptyPrevJsonKeyFile() throws Exception {
    JsonKeyType jsonKeyType = new JsonKeyType(null, "");

    assertNull(jsonKeyType.getJsonKeyFile());
    assertNull(jsonKeyType.getAccountId());
    assertNull(jsonKeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateJsonKeyTypeWithInvalidPrevJsonKeyFile()
          throws Exception {
    String invalidPrevJsonKeyFile = "invalidPrevJsonKeyFile.json";
    JsonKeyType jsonKeyType = new JsonKeyType(null, invalidPrevJsonKeyFile);

    assertEquals(invalidPrevJsonKeyFile, jsonKeyType.getJsonKeyFile());
    assertNull(jsonKeyType.getAccountId());
    assertNull(jsonKeyType.getPrivateKey());
  }

  @Test
  public void testSerialization() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
            .thenReturn(new FileInputStream(jsonKeyPath));
    JsonKeyType jsonKeyType = new JsonKeyType(mockFileItem, null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    serialize(jsonKeyType, out);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    JsonKeyType deserializedJsonKeyType = deserialize(in);

    assertTrue(new File(deserializedJsonKeyType.getJsonKeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            deserializedJsonKeyType.getAccountId());
    assertEquals(privateKey, deserializedJsonKeyType.getPrivateKey());
  }

  private void serialize(JsonKeyType jsonKeyType, OutputStream out)
          throws IOException {
    ObjectOutputStream objectOut = null;
    try {
      objectOut = new ObjectOutputStream(out);
      objectOut.writeObject(jsonKeyType);
    } finally {
      if (objectOut != null) {
        try {
          objectOut.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private JsonKeyType deserialize(InputStream in) throws IOException,
          ClassNotFoundException {
    ObjectInputStream objectIn = null;
    try {
      objectIn = new ObjectInputStream(in);
      return (JsonKeyType) objectIn.readObject();
    } finally {
      if (objectIn != null) {
        try {
          objectIn.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
