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
package com.google.jenkins.plugins.credentials.oauth;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Key;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.InputStream;

/**
 * For "Robot" service account client secrets a key piece of information is the email address
 * contained within "client_secrets.json", which the existing {@code GoogleClientSecrets} class does
 * not parse. This makeshift partial copy of {@code GoogleClientSecrets} implements *just* the
 * "client_email" parsing.
 *
 * @author Matt Moore
 */
@Deprecated
@SuppressWarnings("deprecation")
public final class LegacyJsonKey extends GenericJson {

  /** Details for web applications. */
  @Key private Details web;

  /** Returns the details for web applications. */
  public Details getWeb() {
    return web;
  }

  public void setWeb(Details web) {
    this.web = web;
  }

  /** Container for our new field, modeled after: {@code GoogleClientSecrets.Details} */
  public static final class Details extends GenericJson {
    /** Client email. */
    @Key("client_email")
    private String clientEmail;

    public void setClientEmail(String clientEmail) {
      this.clientEmail = clientEmail;
    }

    /** Returns the client email. */
    public String getClientEmail() {
      return clientEmail;
    }
  }

  /** Loads the {@code client_secrets.json} file from the given input stream. */
  public static LegacyJsonKey load(JsonFactory jsonFactory, InputStream inputStream)
      throws IOException {
    return jsonFactory.fromInputStream(inputStream, Charsets.UTF_8, LegacyJsonKey.class);
  }
}
