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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.common.base.Predicates;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for the {@link MockExecutor}. */
@ExtendWith(MockitoExtension.class)
class MockExecutorTest {

    private static final String theString = "tHe StRiNg!";
    private static final String theOtherString = "tHe OtHeR sTrInG!";

    @Mock
    private AbstractGoogleJsonClientRequest<String> mockRequest;

    @Mock
    private FakeRequest otherMockRequest;

    private final MockExecutor executor = new MockExecutor();

    @AfterEach
    void tearDown() {
        assertTrue(executor.sawAll());
    }

    @Test
    void testEmpty() {
        Throwable exception = assertThrows(IllegalStateException.class, () -> {
            try {
                executor.execute(mockRequest);
            } finally {
                assertTrue(executor.sawUnexpected());
            }
        });
        assertThat(exception.getMessage(), containsString("Unexpected request"));
    }

    @Test
    void testFailingPredicate() {
        Throwable exception = assertThrows(IllegalStateException.class, () -> {
            // Make sure that when a false predicate occurs, we throw
            // an exception
            executor.when(mockRequest.getClass(), theOtherString, Predicates.alwaysFalse());

            try {
                executor.execute(mockRequest);
            } finally {
                assertTrue(executor.sawUnexpected());
            }
        });
        assertThat(exception.getMessage(), containsString("User predicate"));
    }

    @Test
    void testWhen() throws Exception {
        executor.when(mockRequest.getClass(), theOtherString);

        assertEquals(theOtherString, executor.execute(mockRequest));
        assertFalse(executor.sawUnexpected());
    }

    @Test
    void testOutOfOrder() {
        Throwable exception = assertThrows(IllegalStateException.class, () -> {
            executor.when(otherMockRequest.getClass(), theOtherString);

            executor.execute(mockRequest);
            assertFalse(executor.sawUnexpected());
        });
        assertThat(exception.getMessage(), containsString("out of order"));
    }

    private static final class MyException extends IOException {
        public MyException(String message) {
            super(message);
        }
    }

    @Test
    void testThrowWhen() {
        Throwable exception = assertThrows(MyException.class, () -> {
            executor.throwWhen(mockRequest.getClass(), new MyException(theString));

            try {
                executor.execute(mockRequest);
            } finally {
                assertFalse(executor.sawUnexpected());
            }
        });
        assertTrue(exception.getMessage().contains(theString));
    }

    @Test
    void testPassThruWhen() throws Exception {
        when(mockRequest.getJsonContent()).thenReturn(theString);
        executor.passThruWhen(mockRequest.getClass());

        try {
            assertEquals(theString, executor.execute(mockRequest));
        } finally {
            assertFalse(executor.sawUnexpected());
        }
    }

    private static class FakeRequest extends AbstractGoogleJsonClientRequest<String> {
        private FakeRequest(AbstractGoogleJsonClient client, String s, String t, Object o) {
            super(client, s, t, o, String.class);
        }
    }
}
