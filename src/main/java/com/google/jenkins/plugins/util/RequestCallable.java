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

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} that only throws {@link IOException} or {@link ExecutorException}.
 *
 * @param <T> the return type for the request.
 */
public abstract class RequestCallable<T> implements Callable<T> {

  /** {@inheritDoc} */
  @Override
  public abstract T call() throws IOException, ExecutorException;

  /** @return whether this request can be retry. */
  public boolean canRetry() {
    return true;
  }

  /** @return a {@link RequestCallable} that executes a request. */
  public static <R> RequestCallable<R> from(final AbstractGoogleJsonClientRequest<R> request) {
    return new RequestCallable<R>() {

      /** {@inheritDoc} */
      @Override
      public R call() throws IOException {
        return request.execute();
      }

      /** {@inheritDoc} */
      @Override
      public boolean canRetry() {
        return (request.getMediaHttpUploader() == null);
      }
    };
  }
}
