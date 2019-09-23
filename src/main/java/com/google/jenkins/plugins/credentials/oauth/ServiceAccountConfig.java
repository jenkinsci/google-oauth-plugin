/*
 * Copyright 2014 Google LLC
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

import com.cloudbees.plugins.credentials.SecretBytes;
import com.google.common.base.Strings;
import hudson.model.Describable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;

/**
 * general abstraction for providing google service account authentication mechanism. subclasses
 * need to provide an accountId and a private key to use for authenticating a service account
 */
public abstract class ServiceAccountConfig
    implements Describable<ServiceAccountConfig>, Serializable {
  private static final Logger LOGGER = Logger.getLogger(ServiceAccountConfig.class.getName());
  private static final long serialVersionUID = 6355493019938144806L;

  public abstract String getAccountId();

  public abstract PrivateKey getPrivateKey();

  @Deprecated // Used only for compatibility purposes.
  @CheckForNull
  protected SecretBytes getSecretBytesFromFile(@CheckForNull String filePath) {
    Jenkins.get().checkPermission(Jenkins.RUN_SCRIPTS);

    if (Strings.isNullOrEmpty(filePath)) {
      LOGGER.log(Level.SEVERE, "Provided file path is null or empty.");
      return null;
    }

    try {
      return SecretBytes.fromBytes(FileUtils.readFileToByteArray(new File(filePath)));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, String.format("Failed to read previous key from %s", filePath), e);
      return null;
    }
  }

  /** abstract descriptor for service account authentication */
  public abstract static class Descriptor extends hudson.model.Descriptor<ServiceAccountConfig> {}
}
