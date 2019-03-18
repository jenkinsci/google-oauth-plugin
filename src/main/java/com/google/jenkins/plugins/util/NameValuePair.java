/*
 * Copyright 2013-2019 Google LLC
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A name-value pair helper class.
 *
 * @param <N> The type for the name
 * @param <V> The type for the value
 */
public class NameValuePair<N, V> {
  /**
   * Construct a pair from the given name and value.
   */
  public NameValuePair(N name, V value) {
    this.name = checkNotNull(name);
    this.value = checkNotNull(value);
  }

  /**
   * Fetches the name
   */
  public N getName() {
    return this.name;
  }

  /**
   * Fetches the value
   */
  public V getValue() {
    return this.value;
  }

  private final N name;
  private final V value;
}
