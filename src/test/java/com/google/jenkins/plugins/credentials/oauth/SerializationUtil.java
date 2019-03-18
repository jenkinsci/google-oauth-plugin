/*
 * Copyright 2014 Google LLC
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
package com.google.jenkins.plugins.credentials.oauth;

import java.io.*;

/**
 * Helper class for Serialization
 */
public class SerializationUtil {
  public static void serialize(Object object, OutputStream out)
          throws IOException {
    ObjectOutputStream objectOut = null;
    try {
      objectOut = new ObjectOutputStream(out);
      objectOut.writeObject(object);
    } finally {
      if (objectOut != null) {
        try {
          objectOut.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  public static <T> T deserialize(Class<T> clazz, InputStream in)
          throws IOException, ClassNotFoundException, ClassCastException {
    ObjectInputStream objectIn = null;
    try {
      objectIn = new ObjectInputStream(in);
      return clazz.cast(objectIn.readObject());
    } finally {
      if (objectIn != null) {
        try {
          objectIn.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
