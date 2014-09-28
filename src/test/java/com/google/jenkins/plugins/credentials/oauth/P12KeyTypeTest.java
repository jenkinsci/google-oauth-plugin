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
import java.security.KeyPair;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
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
 * Tests for {@link P12KeyType}.
 */
public class P12KeyTypeTest {
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
    keyPair = P12KeyUtil.generateKeyPair();
    p12KeyPath = P12KeyUtil.createTempP12KeyFile(keyPair);
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
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertEquals(keyPair.getPrivate(), p12KeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithNullAccountId() throws Exception {
    P12KeyType p12KeyType = new P12KeyType(null, null, p12KeyPath);

    assertNull(p12KeyType.getAccountId());
    assertEquals(keyPair.getPrivate(), p12KeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithNullP12KeyFile() throws Exception {
    P12KeyType p12KeyType =
            new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS, null, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertNull(p12KeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithEmptyP12KeyFile() throws Exception {
    when(mockFileItem.getSize()).thenReturn(0L);
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertNull(p12KeyType.getPrivateKey());
  }

  @Test
  public void testCreateWithInvalidP12KeyFile() throws Exception {
    byte[] bytes = "invalidP12KeyFile".getBytes();
    when(mockFileItem.getSize()).thenReturn((long) bytes.length);
    when(mockFileItem.getInputStream())
            .thenReturn(new ByteArrayInputStream(bytes));
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertNull(p12KeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithPrevP12KeyFile() throws Exception {
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            p12KeyPath);

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertEquals(keyPair.getPrivate(), p12KeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithEmptyPrevP12KeyFile() throws Exception {
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            "");

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertNull(p12KeyType.getPrivateKey());
  }

  @Test
  @WithoutJenkins
  public void testCreateWithInvalidPrevP12KeyFile() throws Exception {
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS, null,
            "invalidPrevP12KeyFile.p12");

    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS, p12KeyType.getAccountId());
    assertNull(p12KeyType.getPrivateKey());
  }

  @Test
  public void testSerialization() throws Exception {
    when(mockFileItem.getSize()).thenReturn(1L);
    when(mockFileItem.getInputStream())
            .thenReturn(new FileInputStream(p12KeyPath));
    P12KeyType p12KeyType = new P12KeyType(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            mockFileItem, null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SerializationUtil.serialize(p12KeyType, out);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    P12KeyType deserializedP12KeyType =
            SerializationUtil.deserialize(P12KeyType.class, in);

    assertTrue(new File(deserializedP12KeyType.getP12KeyFile()).exists());
    assertEquals(SERVICE_ACCOUNT_EMAIL_ADDRESS,
            deserializedP12KeyType.getAccountId());
    assertEquals(keyPair.getPrivate(), deserializedP12KeyType.getPrivateKey());
  }
}
