package com.google.jenkins.plugins;

import com.google.jenkins.plugins.util.ExecutorTest;
import com.google.jenkins.plugins.util.MetadataReaderTest;
import com.google.jenkins.plugins.util.MockExecutorTest;
import com.google.jenkins.plugins.util.NameValuePairTest;
import com.google.jenkins.plugins.util.ResolveTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(
    value = {
      ExecutorTest.class,
      MetadataReaderTest.class,
      MockExecutorTest.class,
      NameValuePairTest.class,
      ResolveTest.class
    })
public class UtilTestSuite {}
