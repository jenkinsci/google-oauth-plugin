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
