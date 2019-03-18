/*
 * Copyright 2013-2019 Google LLC
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
package com.google.jenkins.plugins.util;

import java.net.SocketTimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_FORBIDDEN;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_NOT_FOUND;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_SERVER_ERROR;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.api.client.http.HttpResponseException;

/**
 * Tests for {@link Executor}.
 */
public class ExecutorTest {

  private HttpResponseException notFoundJsonException;
  private HttpResponseException conflictJsonException;
  private HttpResponseException forbiddenJsonException;
  private HttpResponseException errorJsonException;
  private SocketTimeoutException timeoutException;

  @Mock
  private com.google.api.client.http.HttpHeaders headers;

  @Mock
  private AbstractGoogleJsonClientRequest<Void> mockRequest;

  private Executor underTest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    notFoundJsonException = new HttpResponseException.Builder(
        STATUS_CODE_NOT_FOUND, STATUS_MESSAGE, headers).build();
    conflictJsonException = new HttpResponseException.Builder(
        409 /* STATUS_CODE_CONFLICT */, STATUS_MESSAGE, headers).build();
    forbiddenJsonException = new HttpResponseException.Builder(
        STATUS_CODE_FORBIDDEN, STATUS_MESSAGE, headers).build();
    errorJsonException = new HttpResponseException.Builder(
        STATUS_CODE_SERVER_ERROR, STATUS_MESSAGE, headers).build();

    timeoutException = new SocketTimeoutException(STATUS_MESSAGE);

    underTest = new Executor.Default() {
        @Override
        public void sleep() {
          // Don't really sleep...
        }
      };
  }

  @Test
  public void testVanillaNewExecutor() throws Exception {
    assertNotNull(underTest);
    when(mockRequest.execute()).thenReturn((Void) null);

    underTest.execute(mockRequest);
  }

  @Test(expected = NotFoundException.class)
  public void testNewExecutorWithNotFound() throws Exception {
    assertNotNull(underTest);
    when(mockRequest.execute()).thenThrow(notFoundJsonException);

    underTest.execute(mockRequest);
  }

  @Test(expected = ConflictException.class)
  public void testNewExecutorWithConflict() throws Exception {
    assertNotNull(underTest);
    when(mockRequest.execute()).thenThrow(conflictJsonException);

    underTest.execute(mockRequest);
  }

  @Test(expected = ForbiddenException.class)
  public void testNewExecutorWithForbidden() throws Exception {
    assertNotNull(underTest);
    when(mockRequest.execute()).thenThrow(forbiddenJsonException);

    underTest.execute(mockRequest);
  }

  @Test(expected = HttpResponseException.class)
  public void testNewExecutorWithAllErrors() throws Exception {
    assertNotNull(underTest);
    when(mockRequest.execute()).thenThrow(errorJsonException);

    underTest.execute(mockRequest);
  }

  @Test
  public void testNewExecutorWithErrorsThenSuccess() throws Exception {
    assertNotNull(underTest);
    when(mockRequest.execute())
        .thenThrow(errorJsonException)
        .thenThrow(timeoutException)
        .thenReturn((Void) null);

    underTest.execute(mockRequest);
  }

  private static final String STATUS_MESSAGE = "doesn't matter";
}
