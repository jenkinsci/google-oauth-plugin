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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.security.GeneralSecurityException;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.MockitoAnnotations;

/** Tests for {@link GoogleRobotCredentials}. */
public class GoogleRobotCredentialsTest {

  // Allow for testing using JUnit4, instead of JUnit3.
  @Rule public JenkinsRule jenkins = new JenkinsRule();

  /** */
  public static class TestRequirement extends TestGoogleOAuth2DomainRequirement {
    public TestRequirement() {
      super(FAKE_SCOPE);
    }
  }

  /** */
  public static class NameProvider extends CredentialsNameProvider<GoogleRobotCredentials> {
    @Override
    public String getName(GoogleRobotCredentials credentials) {
      return NAME;
    }
  }

  /** */
  @NameWith(value = NameProvider.class, priority = 100)
  @RequiresDomain(value = TestRequirement.class)
  public static class FakeGoogleCredentials extends GoogleRobotCredentials {
    public FakeGoogleCredentials(String projectId, GoogleCredential credential) {
      super(CredentialsScope.GLOBAL, "", projectId, new GoogleRobotCredentialsModule());

      this.credential = credential;
    }

    @Override
    public GoogleCredential getGoogleCredential(GoogleOAuth2ScopeRequirement requirement)
        throws GeneralSecurityException {
      if (credential == null) {
        throw new GeneralSecurityException("asdf");
      }
      return credential;
    }

    private GoogleCredential credential;

    @Override
    public String getUsername() {
      return USERNAME;
    }

    /** */
    @Extension
    public static class DescriptorImpl extends AbstractGoogleRobotCredentialsDescriptor {
      public DescriptorImpl() {
        super(FakeGoogleCredentials.class);
      }

      @Override
      public String getDisplayName() {
        return DISPLAY_NAME;
      }
    }
  }

  private GoogleCredential fakeCredential;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    fakeCredential = new GoogleCredential();
  }

  @Test
  @WithoutJenkins
  public void testGettersNullId() throws Exception {
    FakeGoogleCredentials credentials = new FakeGoogleCredentials(PROJECT_ID, fakeCredential);

    assertEquals(PROJECT_ID, credentials.getProjectId());
    assertNotNull(credentials.getId());
    assertSame(fakeCredential, credentials.getGoogleCredential(null));
    assertEquals(Messages.GoogleRobotCredentials_Description(), credentials.getDescription());
  }

  @Test
  public void testGetDescriptor() throws Exception {
    FakeGoogleCredentials credentials = new FakeGoogleCredentials(PROJECT_ID, fakeCredential);

    Descriptor descriptor = credentials.getDescriptor();
    assertThat(descriptor, instanceOf(FakeGoogleCredentials.DescriptorImpl.class));
    assertEquals(DISPLAY_NAME, descriptor.getDisplayName());
  }

  @Test
  @WithoutJenkins
  public void testGetAccessToken() throws Exception {
    FakeGoogleCredentials credentials = new FakeGoogleCredentials(PROJECT_ID, fakeCredential);

    fakeCredential.setAccessToken(ACCESS_TOKEN);
    fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);
    assertEquals(
        ACCESS_TOKEN, Secret.toString(credentials.getAccessToken(null /* scope requirement */)));
  }

  @Test
  @WithoutJenkins
  public void testGetAccessTokenNoCredential() throws Exception {
    FakeGoogleCredentials credentials =
        new FakeGoogleCredentials(PROJECT_ID, null /* credential */);

    assertNull(credentials.getAccessToken(null));
  }

  @Test
  @WithoutJenkins
  public void testForRemote() throws Exception {
    FakeGoogleCredentials credentials = new FakeGoogleCredentials(PROJECT_ID, fakeCredential);

    fakeCredential.setAccessToken(ACCESS_TOKEN);
    fakeCredential.setExpiresInSeconds(EXPIRATION_SECONDS);

    GoogleOAuth2ScopeRequirement requirement =
        new GoogleOAuth2ScopeRequirement() {
          @Override
          public Collection<String> getScopes() {
            return ImmutableList.of();
          }
        };
    GoogleRobotCredentials remotable = credentials.forRemote(requirement);

    assertEquals(USERNAME, remotable.getUsername());
    assertEquals(ACCESS_TOKEN, Secret.toString(remotable.getAccessToken(requirement)));
    assertSame(remotable, remotable.forRemote(requirement));
  }

  @Test
  public void testListBoxEmpty() throws Exception {
    ListBoxModel list = GoogleRobotCredentials.getCredentialsListBox(FakeGoogleCredentials.class);

    assertEquals(0, list.size());

    FakeGoogleCredentials credentials = new FakeGoogleCredentials(PROJECT_ID, fakeCredential);
    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    list = GoogleRobotCredentials.getCredentialsListBox(FakeGoogleCredentials.class);

    assertEquals(1, list.size());
    for (ListBoxModel.Option option : list) {
      assertEquals(NAME, option.name);
      assertEquals(credentials.getId(), option.value);
    }
  }

  @Test
  public void testGetById() throws Exception {
    FakeGoogleCredentials credentials = new FakeGoogleCredentials(PROJECT_ID, fakeCredential);
    assertNull(GoogleRobotCredentials.getById(credentials.getId()));

    SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    assertSame(credentials, GoogleRobotCredentials.getById(credentials.getId()));
    assertNull(GoogleRobotCredentials.getById("not an id"));
  }

  @LocalData
  @Test
  public void testMigration() {
    /* LocalData contains an old credential with no id field and the project id my-google-project.
    On deserialization the id should be filled with the project id. */
    assertNotNull(GoogleRobotCredentials.getById(MIGRATION_PROJECT_ID));
  }

  @Test
  public void testMultipleCredentials() {
    FakeGoogleCredentials credential1 = new FakeGoogleCredentials(PROJECT_ID, null);
    FakeGoogleCredentials credential2 = new FakeGoogleCredentials(PROJECT_ID, null);
    assertNotEquals(credential1, credential2);
  }

  private static final String NAME = "my credential name";
  private static final String FAKE_SCOPE = "my.fake.scope";
  private static final String DISPLAY_NAME = "blah";
  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String MIGRATION_PROJECT_ID = "my-google-project";
  private static final String USERNAME = "mattomata";
  private static final String ACCESS_TOKEN = "ThE.ToKeN";
  private static final long EXPIRATION_SECONDS = 1234;
}
