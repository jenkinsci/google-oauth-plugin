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
package com.google.jenkins.plugins;

import com.google.jenkins.plugins.credentials.oauth.ConfigurationAsCodeTest;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeSpecificationTest;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentialsTest;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotMetadataCredentialsTest;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentialsTest;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfigTest;
import com.google.jenkins.plugins.credentials.oauth.P12ServiceAccountConfigTest;
import com.google.jenkins.plugins.credentials.oauth.RemotableGoogleCredentialsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/** Defines the full test suite involving OAuth credentials. */
@RunWith(Suite.class)
@Suite.SuiteClasses(
    value = {
      ConfigurationAsCodeTest.class,
      GoogleOAuth2ScopeSpecificationTest.class,
      GoogleRobotCredentialsTest.class,
      GoogleRobotMetadataCredentialsTest.class,
      GoogleRobotPrivateKeyCredentialsTest.class,
      JsonServiceAccountConfigTest.class,
      P12ServiceAccountConfigTest.class,
      RemotableGoogleCredentialsTest.class
    })
public class CredentialsOAuthTestSuite {}
