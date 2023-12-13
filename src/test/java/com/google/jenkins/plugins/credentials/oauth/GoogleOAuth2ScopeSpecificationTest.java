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

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.cloudbees.plugins.credentials.domains.DomainSpecification.Result;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.MockitoAnnotations;

/** Tests for {@link GoogleOAuth2ScopeSpecification}. */
public class GoogleOAuth2ScopeSpecificationTest {
    // Allow for testing using JUnit4, instead of JUnit3.
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @WithoutJenkins
    public void testBasics() throws Exception {
        GoogleOAuth2ScopeSpecification spec = new GoogleOAuth2ScopeSpecification(GOOD_SCOPES);

        assertThat(spec.getSpecifiedScopes(), hasItems(GOOD_SCOPE1, GOOD_SCOPE2));
    }

    @Test
    public void testUnknownRequirement() throws Exception {
        GoogleOAuth2ScopeSpecification spec = new GoogleOAuth2ScopeSpecification(GOOD_SCOPES);

        OAuth2ScopeRequirement requirement = new OAuth2ScopeRequirement() {
            @Override
            public Collection<String> getScopes() {
                return GOOD_SCOPES;
            }
        };

        // Verify that even with the right scopes the type kind excludes
        // the specification from matching this requirement
        assertEquals(Result.UNKNOWN, spec.test(requirement));
    }

    @Test
    public void testKnownRequirements() throws Exception {
        GoogleOAuth2ScopeSpecification spec = new GoogleOAuth2ScopeSpecification(GOOD_SCOPES);

        GoogleOAuth2ScopeRequirement goodReq = new GoogleOAuth2ScopeRequirement() {
            @Override
            public Collection<String> getScopes() {
                return GOOD_SCOPES;
            }
        };
        GoogleOAuth2ScopeRequirement badReq = new GoogleOAuth2ScopeRequirement() {
            @Override
            public Collection<String> getScopes() {
                return BAD_SCOPES;
            }
        };

        // Verify that with the right type of requirement that
        // good scopes match POSITIVEly and bad scopes match NEGATIVEly
        assertEquals(Result.POSITIVE, spec.test(goodReq));
        assertEquals(Result.NEGATIVE, spec.test(badReq));
    }

    private static String GOOD_SCOPE1 = "foo";
    private static String GOOD_SCOPE2 = "baz";
    private static String BAD_SCOPE = "bar";
    private static Collection<String> GOOD_SCOPES = ImmutableList.of(GOOD_SCOPE1, GOOD_SCOPE2);
    private static Collection<String> BAD_SCOPES = ImmutableList.of(GOOD_SCOPE1, BAD_SCOPE);
}
