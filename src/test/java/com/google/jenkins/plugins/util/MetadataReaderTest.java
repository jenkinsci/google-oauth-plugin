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

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_NOT_FOUND;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_OK;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_UNAUTHORIZED;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link MetadataReader}. */
@ExtendWith(MockitoExtension.class)
class MetadataReaderTest {

    private static final String METADATA_ENDPOINT = "http://metadata/computeMetadata/v1";
    private static final String MY_KEY = "/my/metadata/path";
    private static final String MY_VALUE = "RaNdOm value";

    private MockHttpTransport transport;
    private MockLowLevelHttpRequest request;

    private MetadataReader underTest;

    @BeforeEach
    void setUp() {
        transport = spy(new MockHttpTransport());
        request = spy(new MockLowLevelHttpRequest());

        underTest = new MetadataReader.Default(transport.createRequestFactory());
    }

    @Test
    void testHasMetadata() throws Exception {
        stubRequest(METADATA_ENDPOINT, STATUS_CODE_OK, "hi");

        assertTrue(underTest.hasMetadata());
        verifyRequest("");
    }

    @Test
    void testHasNoMetadata() throws Exception {
        stubRequest(METADATA_ENDPOINT, STATUS_CODE_NOT_FOUND, "hi");

        assertFalse(underTest.hasMetadata());
        verifyRequest("");
    }

    @Test
    void testHasNoMetadata2() throws Exception {
        stubRequest(METADATA_ENDPOINT, 409, "hi");

        assertFalse(underTest.hasMetadata());
        verifyRequest("");
    }

    @Test
    void testReadMetadata() throws Exception {
        stubRequest(METADATA_ENDPOINT + MY_KEY, STATUS_CODE_OK, MY_VALUE);

        assertEquals(MY_VALUE, underTest.readMetadata(MY_KEY));
        verifyRequest(MY_KEY);
    }

    @Test
    void testReadMissingMetadata() throws Exception {
        stubRequest(METADATA_ENDPOINT + MY_KEY, STATUS_CODE_NOT_FOUND, MY_VALUE);
        assertThrows(NotFoundException.class, () -> {
            try {
                underTest.readMetadata(MY_KEY);
            } finally {
                verifyRequest(MY_KEY);
            }
        });
    }

    @Test
    void testReadUnauthorizedMetadata() throws Exception {
        stubRequest(METADATA_ENDPOINT + MY_KEY, STATUS_CODE_UNAUTHORIZED, MY_VALUE);
        assertThrows(ForbiddenException.class, () -> {
            try {
                underTest.readMetadata(MY_KEY);
            } finally {
                verifyRequest(MY_KEY);
            }
        });
    }

    @Test
    void testReadUnrecognizedMetadataException() throws Exception {
        stubRequest(METADATA_ENDPOINT + MY_KEY, 409, MY_VALUE);
        assertThrows(IOException.class, () -> {
            try {
                underTest.readMetadata(MY_KEY);
            } finally {
                verifyRequest(MY_KEY);
            }
        });
    }

    private void stubRequest(String url, int statusCode, String responseContent) throws IOException {
        request.setResponse(
                new MockLowLevelHttpResponse().setStatusCode(statusCode).setContent(responseContent));
        doReturn(request).when(transport).buildRequest("GET", url);
    }

    private void verifyRequest(String key) throws IOException {
        verify(transport).buildRequest("GET", METADATA_ENDPOINT + key);
        verify(request).execute();
        assertEquals("Google", getOnlyElement(request.getHeaderValues("Metadata-Flavor")));
    }
}
