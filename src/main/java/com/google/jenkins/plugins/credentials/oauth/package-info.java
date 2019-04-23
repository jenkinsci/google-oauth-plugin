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

/**
 * This package implements Jenkins plugins providing Google-specific OAuth2 Credentials, Domain
 * Requirements and Specifications.
 *
 * <p>For OAuth2, these are inherently a provider-specific triple because each provider (e.g.
 * Google, Facebook, GitHub) may only provide tokens for their own credentials and scopes. In a
 * nutshell, an OAuth2 access token is like "limited power of attorney". You are giving the bearer
 * of that token permission to interact with the set of limited scopes as the user who provided it.
 *
 * <p>This package provides the following Google-specific triple:
 *
 * <ol>
 *   <li>
 *       <pre>GoogleOAuth2ScopeRequirement
 *  extends OAuth2ScopeRequirement
 *  extends DomainRequirement</pre>
 *   <li>
 *       <pre>GoogleOAuth2ScopeSpecification
 *  extends OAuth2ScopeSpecification&lt;GoogleOAuth2ScopeRequirement&gt;
 *  extends DomainSpecification</pre>
 *   <li>
 *       <pre>GoogleOAuth2Credentials
 *  extends OAuth2Credentials&lt;GoogleOAuth2ScopeRequirement&gt;
 *  extends Credentials</pre>
 * </ol>
 *
 * <p>As the set of scopes determine what you may do with a credential, each plugin asks for an
 * access token by providing a provider-specific {@code OAuth2ScopeRequirement} to {@code
 * OAuth2Credentials.getAccessToken(OAuth2ScopeRequirement)}.
 *
 * <p>When enumerating credentials suitable for use with a given plugin, we only want to show those
 * that allow a suitable set of scopes. This is where {@code OAuth2ScopeRequirement} pairs with
 * {@code OAuth2ScopeSpecification}. An {@code OAuth2ScopeSpecification} is attached to a {@code
 * Domain} and is the superset of scopes to which the contained {@code Credentials} may be applied.
 *
 * <p>However, since entering OAuth2 scopes is unwieldy, we provide the necessary concepts to make
 * it multiple choice. Enter {@code DomainRequirementProvider}, a new {@code ExtensionPoint} that
 * allows {@code OAuth2ScopeSpecification} to automatically discover the set of OAuth2 scopes
 * required by installed plugins.
 *
 * <p>For Example:<br>
 *
 * <pre>
 * {@literal @}RequiredDomain(value = MyGoogleOAuth2Requirement.class)
 * public class Foo extends SomeDescribable
 * </pre>
 *
 * In this example, the {@code DescribableDomainRequirementProvider} would discover that {@code Foo}
 * required the set of scopes specified by {@code MyGoogleOAuth2Requirement}. These would be
 * aggregated with any other required scopes and presented in the UI for any {@code
 * OAuth2ScopeSpecification} whose type parameter is a super-type of {@code
 * MyGoogleOAuth2Requirement}.
 *
 * <p>So for instance if {@code MyGoogleOAuth2Requirement extends} {@code
 * GoogleOAuth2ScopeRequirement} then {@code GoogleOAuth2ScopeSpecification}, which {@code extends}
 * {@code OAuth2ScopeSpecification<GoogleOAuth2ScopeRequirement>}, would have {@code
 * MyGoogleOAuth2Requirement}'s scopes appear in its UI.
 *
 * <p>This package provides two types of {@code GoogleOAuth2Credentials}:
 *
 * <ul>
 *   <li>{@code GoogleRobotMetadataCredentials}: a robot credential that utilizes the Google Compute
 *       Engine "metadata" service attached to a virtual machine for providing access tokens.
 *   <li>{@code GoogleRobotPrivateKeyCredentials}: a robot credential that retrieves access tokens
 *       for a robot account using its {@code client_secrets.json} and private key file.
 * </ul>
 */
package com.google.jenkins.plugins.credentials.oauth;
