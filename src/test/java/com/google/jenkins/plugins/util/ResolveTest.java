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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/** Tests for {@link Resolve}'s static methods. */
public class ResolveTest {

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testBasicResolve() {
    String basicInput = "la dee da $BUILD_NUMBER";

    assertThat(
        Resolve.resolveBuiltin(basicInput), Matchers.not(Matchers.containsString("BUILD_NUMBER")));
  }

  @Test
  public void testUserOverride() {
    String basicInput = "$BUILD_NUMBER";

    assertEquals(
        OVERRIDE,
        Resolve.resolveBuiltinWithCustom(
            basicInput, Collections.singletonMap("BUILD_NUMBER", OVERRIDE)));
  }

  @Test
  public void testJustUserOverrides() {
    String basicInput = "$bar";

    assertEquals(
        OVERRIDE, Resolve.resolveCustom(basicInput, Collections.singletonMap("bar", OVERRIDE)));
  }

  @Test
  public void testNoVariable() {
    assertEquals(UNKNOWN_VAR, Resolve.resolveBuiltin(UNKNOWN_VAR));
  }

  private static final String OVERRIDE = "my variable override";
  private static final String UNKNOWN_VAR = "$foo";
}
