package com.google.jenkins;

import com.google.jenkins.plugins.CredentialsOAuthTestSuite;
import com.google.jenkins.plugins.UtilTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = {CredentialsOAuthTestSuite.class, UtilTestSuite.class})
public class GoogleOAuthPluginTestSuite {}
