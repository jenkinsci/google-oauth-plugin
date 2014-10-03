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

import java.io.Serializable;
import java.security.PrivateKey;

import hudson.model.Describable;

/**
 * general abstraction for providing google service account authentication
 * mechanism. subclasses need to provide an accountId and a private key to use
 * for authenticating a service account
 */
public abstract class ServiceAccountConfig
        implements Describable<ServiceAccountConfig>, Serializable {
  private static final long serialVersionUID = 6355493019938144806L;

  public abstract String getAccountId();

  public abstract PrivateKey getPrivateKey();

  /**
   * abstract descriptor for service account authentication
   */
  public abstract static class Descriptor
          extends hudson.model.Descriptor<ServiceAccountConfig> {
  }
}
