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

import java.io.Serializable;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

/**
 * An abstraction interface for instantiating the dependencies of
 * the {@code GoogleRobotCredentials}.
 */
public class GoogleRobotCredentialsModule implements Serializable {
  /**
   * The HttpTransport to use for credential related requests.
   */
  public HttpTransport getHttpTransport() {
    return new NetHttpTransport();
  }

  /**
   * The HttpTransport to use for credential related requests.
   */
  public JsonFactory getJsonFactory() {
    return new JacksonFactory();
  }

  /**
   * For {@link Serializable}
   */
  private static final long serialVersionUID = 1L;
}