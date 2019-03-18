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

import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Tests for {@link NameValuePair}
 */
public class NameValuePairTest {
  @Test
  public void testBasicString() {
    final String first = "a";
    final String second = "b";
    NameValuePair<String, String> pair =
        new NameValuePair<String, String>(first, second);

    assertSame(first, pair.getName());
    assertSame(second, pair.getValue());
  }

  @Test
  public void testBasicWithObject() {
    final String first = "a";
    final Object second = new Object();
    NameValuePair<String, Object> pair =
        new NameValuePair<String, Object>(first, second);

    assertSame(first, pair.getName());
    assertSame(second, pair.getValue());
  }
}