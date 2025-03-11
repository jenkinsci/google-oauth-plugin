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

/** Helper class for Serialization */
class SerializationUtil {
    public static void serialize(Object object, OutputStream out) throws IOException {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
            objectOut.writeObject(object);
        }
    }

    public static <T> T deserialize(Class<T> clazz, InputStream in)
            throws IOException, ClassNotFoundException, ClassCastException {
        try (ObjectInputStream objectIn = new ObjectInputStream(in)) {
            return clazz.cast(objectIn.readObject());
        }
    }
}
