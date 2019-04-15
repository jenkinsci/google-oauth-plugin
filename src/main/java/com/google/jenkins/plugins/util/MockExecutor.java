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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.io.IOException;
import java.util.LinkedList;

/**
 * This is an implementation of {@link Executor} that can be injected to inject a set of canned
 * responses to requests including:
 *
 * <ul>
 *   <li>A pre-determined object
 *   <li>Throwing an {@link IOException} or {@link ExecutorException}
 *   <li>Passing through a part of the request as the response
 * </ul>
 */
public class MockExecutor extends Executor {
  public MockExecutor() {
    requestTypes = new LinkedList<Class<?>>();
    responses = new LinkedList<Object>();
    exceptions = new LinkedList<Exception>();
    predicates = new LinkedList<Predicate<?>>();
    sawUnexpected = false;
  }

  /** {@inheritDoc} */
  @Override
  public void sleep() {
    // Never sleep, this is a test library, we want fast tests.
  }

  /** {@inheritDoc} */
  @Override
  public <T> T execute(RequestCallable<T> request) throws IOException, ExecutorException {
    // TODO(nghia): Think about implementing this.
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public <T> T execute(AbstractGoogleJsonClientRequest<T> request)
      throws IOException, ExecutorException {
    Class<?> requestClass = request.getClass();
    if (requestTypes.isEmpty()) {
      sawUnexpected = true;
      throw new IllegalStateException("Unexpected request: " + requestClass);
    }

    // Remove all three states to keep the lists in sync
    Class<?> clazz = requestTypes.removeFirst();
    Object response = responses.removeFirst();
    Exception exception = exceptions.removeFirst();
    Predicate<AbstractGoogleJsonClientRequest<T>> predicate =
        (Predicate<AbstractGoogleJsonClientRequest<T>>) predicates.removeFirst();

    if (requestClass != clazz) {
      sawUnexpected = true;
      throw new IllegalStateException(
          "Unexpected (or out of order) request: " + requestClass + " expected: " + clazz);
    }

    if (!predicate.apply(request)) {
      sawUnexpected = true;
      throw new IllegalStateException(
          "User predicate: " + predicate + " failed for request: " + requestClass);
    }

    if (response == null) {
      if (exception != null) {
        if (exception instanceof IOException) {
          throw (IOException) exception; // throwWhen(IOException)
        } else {
          throw (ExecutorException) exception; // throwWhen(ExecutorException)
        }
      }
      return (T) request.getJsonContent(); // passThruWhen
    }
    return (T) response; // when
  }

  /**
   * When the next request matches the given {@code requestType} and the provided user {@link
   * Predicate} return {@code response} as the response.
   */
  public <T, S extends AbstractGoogleJsonClientRequest<T>, C extends S> void when(
      Class<C> requestType, T response, Predicate<S> predicate) {
    requestTypes.add(checkNotNull(requestType));
    responses.add(response); // must allow null for delete's Void return type
    exceptions.add(null);
    predicates.add(checkNotNull(predicate));
  }

  /**
   * When the next request matches the given {@code requestType} return {@code response} as the
   * response.
   */
  public <T, C extends AbstractGoogleJsonClientRequest<T>> void when(
      Class<C> requestType, T response) {
    when(requestType, response, Predicates.<C>alwaysTrue());
  }

  /**
   * When the next request matches the given {@code requestType} and the provided user {@link
   * Predicate} throw {@code exception} instead of responding.
   */
  public <T, S extends AbstractGoogleJsonClientRequest<T>, C extends S> void throwWhen(
      Class<C> requestType, IOException exception, Predicate<S> predicate) {
    throwWhenInternal(requestType, exception, predicate);
  }

  /**
   * When the next request matches the given {@code requestType} throw {@code exception} instead of
   * responding.
   */
  public <T, C extends AbstractGoogleJsonClientRequest<T>> void throwWhen(
      Class<C> requestType, IOException exception) {
    throwWhen(requestType, exception, Predicates.<C>alwaysTrue());
  }

  /**
   * When the next request matches the given {@code requestType} and the provided user {@link
   * Predicate} throw {@code exception} instead of responding.
   */
  public <T, S extends AbstractGoogleJsonClientRequest<T>, C extends S> void throwWhen(
      Class<C> requestType, ExecutorException exception, Predicate<S> predicate) {
    throwWhenInternal(requestType, exception, predicate);
  }

  /**
   * When the next request matches the given {@code requestType} throw {@code exception} instead of
   * responding.
   */
  public <T, C extends AbstractGoogleJsonClientRequest<T>> void throwWhen(
      Class<C> requestType, ExecutorException exception) {
    throwWhen(requestType, exception, Predicates.<C>alwaysTrue());
  }

  /**
   * When the next request matches the given {@code requestType} and the provided user {@link
   * Predicate} throw {@code exception} instead of responding.
   */
  private <T, S extends AbstractGoogleJsonClientRequest<T>, C extends S> void throwWhenInternal(
      Class<C> requestType, Exception exception, Predicate<S> predicate) {
    requestTypes.add(checkNotNull(requestType));
    responses.add(null);
    exceptions.add(exception);
    predicates.add(checkNotNull(predicate));
  }

  /**
   * When the next request matches the given {@code requestType} and the provided user {@link
   * Predicate} pass through the request's {@code getJsonContent()} cast to the expected response
   * type.
   */
  public <T, S extends AbstractGoogleJsonClientRequest<T>, C extends S> void passThruWhen(
      Class<C> requestType, Predicate<S> predicate) {
    requestTypes.add(checkNotNull(requestType));
    responses.add(null);
    exceptions.add(null);
    predicates.add(checkNotNull(predicate));
  }

  /**
   * When the next request matches the given {@code requestType} pass through the request's {@code
   * getJsonContent()} cast to the expected response type.
   */
  public <T, C extends AbstractGoogleJsonClientRequest<T>> void passThruWhen(Class<C> requestType) {
    passThruWhen(requestType, Predicates.<C>alwaysTrue());
  }

  /** Did we see all of the expected requests? */
  public boolean sawAll() {
    return requestTypes.isEmpty();
  }

  /** Did we see any unexpected requests? */
  public boolean sawUnexpected() {
    return sawUnexpected;
  }

  private final LinkedList<Class<?>> requestTypes;
  private final LinkedList<Object> responses;
  private final LinkedList<Exception> exceptions;
  private final LinkedList<Predicate<?>> predicates;
  private boolean sawUnexpected;
}
