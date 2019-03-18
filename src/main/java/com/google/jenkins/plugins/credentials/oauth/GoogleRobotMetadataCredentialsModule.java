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
package com.google.jenkins.plugins.credentials.oauth;

import com.google.jenkins.plugins.util.MetadataReader;

/**
 * An abstraction interface for instantiating the dependencies of
 * the {@code GoogleRobotMetadataCredentials}.
 */
public class GoogleRobotMetadataCredentialsModule
    extends GoogleRobotCredentialsModule {
  /**
   * Retrieve a MetadataReader for accessing stuff encoded in the
   * instance metadata.
   */
  public MetadataReader getMetadataReader() {
    return new MetadataReader.Default();
  }

  /**
   * For {@link java.io.Serializable}
   */
  private static final long serialVersionUID = 1L;
}