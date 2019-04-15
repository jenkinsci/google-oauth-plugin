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
package com.google.jenkins.plugins.util;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.common.base.Predicates;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Verifier;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for the {@link MockExecutor}. */
public class MockExecutorTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Mock private AbstractGoogleJsonClientRequest<String> mockRequest;

  private static class FakeRequest extends AbstractGoogleJsonClientRequest<String> {
    private FakeRequest(AbstractGoogleJsonClient client, String s, String t, Object o) {
      super(client, s, t, o, String.class);
    }
  };

  @Mock private FakeRequest otherMockRequest;

  private static final String theString = "tHe StRiNg!";
  private static final String theOtherString = "tHe OtHeR sTrInG!";

  private MockExecutor executor = new MockExecutor();

  @Rule
  public Verifier verifySawAll =
      new Verifier() {
        @Override
        public void verify() {
          assertTrue(executor.sawAll());
        }
      };

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testEmpty() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(containsString("Unexpected request"));

    try {
      executor.execute(mockRequest);
    } finally {
      assertTrue(executor.sawUnexpected());
    }
  }

  @Test
  public void testFailingPredicate() throws Exception {
    // Make sure that when a false predicate occurs, we throw
    // an exception
    executor.when(
        mockRequest.getClass(),
        theOtherString,
        Predicates.<AbstractGoogleJsonClientRequest<String>>alwaysFalse());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(containsString("User predicate"));

    try {
      executor.execute(mockRequest);
    } finally {
      assertTrue(executor.sawUnexpected());
    }
  }

  @Test
  public void testWhen() throws Exception {
    executor.when(mockRequest.getClass(), theOtherString);

    assertEquals(theOtherString, executor.execute(mockRequest));
    assertFalse(executor.sawUnexpected());
  }

  @Test
  public void testOutOfOrder() throws Exception {
    executor.when(otherMockRequest.getClass(), theOtherString);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(containsString("out of order"));

    executor.execute(mockRequest);
    assertFalse(executor.sawUnexpected());
  }

  private static final class MyException extends IOException {
    public MyException(String message) {
      super(message);
    }
  }

  @Test
  public void testThrowWhen() throws Exception {
    executor.throwWhen(mockRequest.getClass(), new MyException(theString));

    thrown.expect(MyException.class);
    thrown.expectMessage(theString);

    try {
      executor.execute(mockRequest);
    } finally {
      assertFalse(executor.sawUnexpected());
    }
  }

  @Test
  public void testPassThruWhen() throws Exception {
    when(mockRequest.getJsonContent()).thenReturn(theString);
    executor.passThruWhen(mockRequest.getClass());

    try {
      assertEquals(theString, executor.execute(mockRequest));
    } finally {
      assertFalse(executor.sawUnexpected());
    }
  }
}
