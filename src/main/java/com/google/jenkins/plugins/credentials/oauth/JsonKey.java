/*
 * Copyright 2014-2019 Google LLC
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Key;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import hudson.util.Secret;

/**
 * The
 * <a href="https://console.developers.google.com">Google Developer Console</a>
 * provides private keys for service accounts in two different ways. one of
 * them is a .json file that can be downloaded from the
 * <a href="https://console.developers.google.com">Google Developer Console</a>.
 * <p>
 * The structure of this json file is:
 * <code>
 *     {
 *       "private_key":"-----BEGIN PRIVATE KEY-----\n
 *                      ...
 *                      \n-----END PRIVATE KEY-----\n",
 *       "client_email":"...@developer.gserviceaccount.com",
 *       ...
 *     }
 * </code>
 */
public final class JsonKey extends GenericJson {
  @Key("client_email")
  private String clientEmail;
  @Key("private_key")
  private String privateKey;

  public static JsonKey load(JsonFactory jsonFactory, InputStream inputStream)
      throws IOException {
    InputStreamReader reader = new InputStreamReader(inputStream,
        Charsets.UTF_8);
    try {
      Secret decoded = Secret.fromString(CharStreams.toString(reader));
      return jsonFactory.fromString(decoded.getPlainText(), JsonKey.class);
    } finally {
      inputStream.close();
    }
  }

  public String getClientEmail() {
    return clientEmail;
  }

  public void setClientEmail(String clientEmail) {
    this.clientEmail = clientEmail;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }
}
