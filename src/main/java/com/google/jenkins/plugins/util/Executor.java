/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.util;

import static java.util.logging.Level.SEVERE;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_FORBIDDEN;
import static com.google.api.client.http.HttpStatusCodes.STATUS_CODE_NOT_FOUND;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.api.client.http.HttpResponseException;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * Interface for a class that executes requests on behalf of a Json API client.
 *
 * NOTE: This can be used to intercept or mock all "execute" requests.
 */
public abstract class Executor {
  private static final Logger logger =
      Logger.getLogger(Executor.class.getName());

  /**
   * Executes the request, returning a response of the appropriate type.
   *
   * @param <T> The type of the expected response
   * @param request The request we are issuing
   * @return a Json object of the given type
   * @throws IOException if anything goes wrong
   */
  public <T> T execute(final AbstractGoogleJsonClientRequest<T> request)
      throws IOException, ExecutorException {
    return execute(RequestCallable.from(request));
  }

  /**
   * Executes the request, returning a response of the appropriate type.
   *
   * @param <T> The type of the expected response
   * @param request The request we are issuing
   * @return a Json object of the given type
   * @throws IOException if anything goes wrong
   */
  public abstract <T> T execute(RequestCallable<T> block)
      throws IOException, ExecutorException;

  /**
   * Surface this as a canonical means by which to sleep, so that clients can
   * layer their own retry logic on top of the executor using the same sleep
   * facility;
   */
  public void sleep() {
    Uninterruptibles.sleepUninterruptibly(
        SLEEP_DURATION_SECONDS, TimeUnit.SECONDS);
  }

  /**
   * Surface this as a canonical means by which to sleep, so that clients can
   * layer their own retry logic on top of the executor using the same sleep
   * facility;
   * @param retryAttempt indicates how many times we had retried, to allow for
   *                     increasing back-off time.
   */
  public void sleep(int retryAttempt) {
    sleep();
  }

  /**
   * Seconds to sleep between API request retry attempts
   */
  private static final long SLEEP_DURATION_SECONDS = 15;

  /**
   * A default, failure-tolerant implementation of the {@link Executor} class.
   */
  public static class Default extends Executor {
    public Default() {
      this(RETRY_COUNT, true /* compose retry */);
    }

    /**
     * @param maxRetry the maximum number of retries to attempt in
     *                 {@link #execute(RequestCallable)}.
     * @param composeRetry whether nested retries block cause retries to compose
     *                     or not. If set to false, we will wrap the exception
     *                     of the last retry step in an instance of
     *                     {@link MaxRetryExceededException}, which prevents
     *                     any further retries.
     */
    public Default(int maxRetry, boolean composeRetry) {
      this.maxRetry = maxRetry;
      this.composeRetry = composeRetry;
    }

    private boolean composeRetry;

    private int getMaxRetry() {
      return maxRetry;
    }
    private final int maxRetry;

    private IOException propagateRetry(IOException lastException)
        throws IOException, ExecutorException {
      if (composeRetry) {
        throw lastException;
      } else {
        throw new MaxRetryExceededException(lastException);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(RequestCallable<T> block)
        throws IOException, ExecutorException {
      IOException lastException = null;
      for (int i = 0; i < getMaxRetry(); ++i) {
        try {
          return checkNotNull(block).call();
        } catch (HttpResponseException e) {
          lastException = e;
          // Wrap a set of exception conditions, which when returned from
          // Google APIs are indicative of a state that is unlikely to
          // change.
          if (e.getStatusCode() == STATUS_CODE_NOT_FOUND) {
            throw new NotFoundException(e);
          }
          if (e.getStatusCode() == STATUS_CODE_FORBIDDEN) {
            throw new ForbiddenException(e);
          }
          if (e.getStatusCode() == 409 /* STATUS_CODE_CONFLICT */) {
            throw new ConflictException(e);
          }
          // Many other status codes may simply relate to ephemeral
          // service availability hiccups, that could simply go away
          // on retry.
          logger.log(SEVERE, Messages.Executor_HttpError(), e);
        } catch (SocketTimeoutException e) {
          logger.log(SEVERE, Messages.Executor_TimeoutError(), e);
          lastException = e;
        }

        if (!block.canRetry()) {
          // If this request contained a media upload, then it cannot simply
          // be retried.
          break;
        }
        // Pause before we retry
        sleep(i);
      }
      throw propagateRetry(checkNotNull(lastException));
    }

    private static final int RETRY_COUNT = 5;
  }
}
