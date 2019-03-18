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
package com.google.jenkins.plugins.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

import hudson.Util;

/**
 * Container class for static methods that resolve form entries with
 * typical form data, for use in Jenkins {@code doCheckFoo} methods.
 *
 * @author Matt Moore
 */
public final class Resolve {
  /**
   * Replaces Jenkins build variables (e.g. {@code $BUILD_NUMBER})
   * with sample values, so the result can be form-validated.
   *
   * @param input The unresolved form input string
   * @return the string with substitutions made for built-in variables.
   */
  public static String resolveBuiltin(String input) {
    return resolveBuiltinWithCustom(
        checkNotNull(input), Collections.<String, String>emptyMap());
  }

  /**
   * Replaces Jenkins build variables (e.g. {@code $BUILD_NUMBER})
   * and custom user variables (e.g. {@code $foo}) with sample values,
   * so the result can be form-validated.
   *
   * @param input The unresolved form input string
   * @param customEnvironment The sample variable values to resolve
   * @return the string with substitutions made for built-in variables.
   */
  public static String resolveBuiltinWithCustom(
      String input, Map<String, String> customEnvironment) {
    checkNotNull(input);
    checkNotNull(customEnvironment);

    // Combine customEnvironment and sampleEnvironment into a new map.
    Map<String, String> combinedEnvironment = new HashMap<String, String>();
    combinedEnvironment.putAll(defaultValues);
    combinedEnvironment.putAll(customEnvironment);  // allow overriding defaults

    return resolveCustom(input, combinedEnvironment);
  }

  /**
   * Replaces a user's custom Jenkins variables (e.g. {@code $foo})
   * with provided sample values, so the result can be form-validated.
   *
   * @param input The unresolved form input string
   * @param customEnvironment The sample variable values to resolve
   * @return the string with substitutions made for variables.
   */
  public static String resolveCustom(
      String input, Map<String, String> customEnvironment) {
    checkNotNull(input);
    checkNotNull(customEnvironment);

    return Util.replaceMacro(input, customEnvironment);
  }

  private static Map<String, String> defaultValues =
      new HashMap<String, String>();

  // See: http://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project
  static {
    defaultValues.put("BUILD_NUMBER", "42");
    defaultValues.put("BUILD_ID", "2005-08-22_23-59-59");
    defaultValues.put("BUILD_URL",
        "http://buildserver/jenkins/job/MyJobName/666/");
    defaultValues.put("NODE_NAME", "master");
    defaultValues.put("JOB_NAME", "hello world");
    defaultValues.put("BUILD_TAG", "jenkins-job name-42");
    defaultValues.put("JENKINS_URL", "https://build.mydomain.org/");
    defaultValues.put("EXECUTOR_NUMBER", "3");
    defaultValues.put("JAVA_HOME", "/usr/bin/");
    defaultValues.put("WORKSPACE", "/tmp/jenkins/");
    defaultValues.put("SVN_REVISION", "r1234");
    defaultValues.put("CVS_BRANCH", "TODO");
    defaultValues.put("GIT_COMMIT", "a48b5b3273a1");
    defaultValues.put("GIT_BRANCH", "origin/master");

    defaultValues = Collections.unmodifiableMap(defaultValues);
  }

  /**
   * Not intended for instantiation.
   */
  private Resolve() {
    throw new UnsupportedOperationException("Not intended for instantiation");
  }
}