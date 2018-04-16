/*
 * Copyright 2018 CloudBees, Inc.
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
package com.google.jenkins.plugins.credentials.oauth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import hudson.XmlFile;
import jenkins.model.Jenkins;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests which involve real Jenkins instance.
 * @author Oleg Nenashev
 * @see RemotableGoogleCredentialsTest
 */
@For(RemotableGoogleCredentials.class)
public class RemotableGoogleCredentialsIntegrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    @Issue("JENKINS-50216")
    public void checkSerializationRoundtrip() throws Exception {
        File file = new File(tmp.getRoot(), "remotableGoogleCredentials.xml");
        XmlFile xml = new XmlFile(Jenkins.XSTREAM2, file);

        GoogleCredential googleCredential = new GoogleCredential();
        googleCredential.setExpiresInSeconds(120L);
        GoogleRobotCredentials creds = new GoogleRobotCredentialsTest.FakeGoogleCredentials(
                "myproject", googleCredential);

        RemotableGoogleCredentials credentials = new RemotableGoogleCredentials(creds, new Requirement(), new GoogleRobotCredentialsModule(), true);
        xml.write(credentials);

        // now Reload it
        RemotableGoogleCredentials read = (RemotableGoogleCredentials) xml.read();
        assertFalse("Deserialized token should not be considered as expired", read.isTokenExpired());
    }

    @Test
    @Issue("JENKINS-50216")
    public void shouldDeserializeOldFormat() throws Exception {
        File file = new File(tmp.getRoot(), "remotableGoogleCredentials.xml");
        try (InputStream istream = RemotableGoogleCredentialsTest.class.getResourceAsStream("jodaDateTimeXML.xml");
            OutputStream ostream = new FileOutputStream(file)) {
            org.apache.commons.io.IOUtils.copy(istream, ostream);
        }

        XmlFile xml = new XmlFile(Jenkins.XSTREAM2, file);
        RemotableGoogleCredentials read = (RemotableGoogleCredentials) xml.read();
        DateTime expiration = read.getTokenExpirationTime();
        assertNotNull("Expiration token should be read from the Old XML format", expiration);
        assertEquals(1523889095013L, expiration.getMillis());
        assertEquals("Europe/Zurich", expiration.getZone().getID());
    }

    @Test
    @Issue("JENKINS-50216")
    public void shouldDeserializeBrokenXMLAsNull() throws Exception {
        File file = new File(tmp.getRoot(), "remotableGoogleCredentials.xml");
        try (InputStream istream = RemotableGoogleCredentialsTest.class.getResourceAsStream("jodaDateTimeBroken.xml");
            OutputStream ostream = new FileOutputStream(file)) {
            org.apache.commons.io.IOUtils.copy(istream, ostream);
        }

        XmlFile xml = new XmlFile(Jenkins.XSTREAM2, file);
        RemotableGoogleCredentials read = (RemotableGoogleCredentials) xml.read();
        DateTime expiration = read.getTokenExpirationTime();
        assertNull("Expiration token should be null", expiration);
        assertTrue("Deserialized token should be considered as expired", read.isTokenExpired());
    }

    private static class Requirement extends GoogleOAuth2ScopeRequirement {

        @Override
        public Collection<String> getScopes() {
            return Arrays.asList("foo", "bar");
        }
    }
}
