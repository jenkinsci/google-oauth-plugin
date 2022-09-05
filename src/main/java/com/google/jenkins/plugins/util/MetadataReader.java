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

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_FORBIDDEN;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_NOT_FOUND;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_UNAUTHORIZED;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;

/**
 * This helper utility is used for reading values out of a Google Compute Engine instance's attached
 * metadata service.
 *
 * @author Matt Moore
 */
public interface MetadataReader {
  /** Are we on a Google Compute Engine instance? */
  boolean hasMetadata() throws IOException;

  /**
   * Reads the specified sub-element out of the Google Compute Engine instance's metadata. These
   * relative paths are expected to start with:
   *
   * <ul>
   *   <li>/instance/...
   *   <li>/project/...
   * </ul>
   */
  String readMetadata(String metadataPath) throws IOException, ExecutorException;

  /** A simple default implementation that reads metadata via http requests. */
  public static class Default implements MetadataReader {
    public Default() {
      this(new NetHttpTransport().createRequestFactory());
    }

    public Default(HttpRequestFactory requestFactory) {
      this.requestFactory = checkNotNull(requestFactory);
    }

    private final HttpRequestFactory requestFactory;

    /** {@inheritDoc} */
    @Override
    public String readMetadata(String metadataPath) throws IOException, ExecutorException {
      HttpRequest request =
          requestFactory.buildGetRequest(new GenericUrl(METADATA_SERVER + metadataPath));

      // GCE v1 requires requests to the metadata service to specify
      // this header in order to get anything back.
      request.getHeaders().set("Metadata-Flavor", "Google");

      HttpResponse response;
      try {
        response = request.execute();
      } catch (HttpResponseException e) {
        switch (e.getStatusCode()) {
          case STATUS_CODE_UNAUTHORIZED:
          case STATUS_CODE_FORBIDDEN:
            throw new ForbiddenException(e);
          case STATUS_CODE_NOT_FOUND:
            throw new NotFoundException(e);
          default:
            throw e;
        }
      }

      try (InputStreamReader inChars =
          new InputStreamReader(checkNotNull(response.getContent()), Charsets.UTF_8)) {
        StringWriter output = new StringWriter();
        IOUtils.copy(inChars, output);
        return output.toString();
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasMetadata() {
      try {
        readMetadata("");
        return true;
      } catch (IOException | ExecutorException e) {
        return false;
      }
    }

    /**
     * The address of the GCE Metadata service that provides GCE instances with information about
     * the default service account.
     */
    public static final String METADATA_SERVER = "http://metadata/computeMetadata/v1";
  }
}
