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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Util class for {@link P12ServiceAccountConfigTest}. */
public class P12ServiceAccountConfigTestUtil {
  private static final String DEFAULT_P12_SECRET = "notasecret";
  private static final String DEFAULT_P12_ALIAS = "privatekey";
  private static File tempFolder;

  public static KeyPair generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
    keyPairGenerator.initialize(1024);
    return keyPairGenerator.generateKeyPair();
  }

  public static String createTempP12KeyFile(KeyPair keyPair)
      throws IOException, OperatorCreationException, CertificateException, NoSuchAlgorithmException,
          KeyStoreException, NoSuchProviderException {
    File tempP12Key = File.createTempFile("temp-key", ".p12", getTempFolder());
    writeKeyToFile(keyPair, tempP12Key);
    return tempP12Key.getAbsolutePath();
  }

  private static File getTempFolder() throws IOException {
    if (tempFolder == null) {
      tempFolder = Files.createTempDirectory("temp" + Long.toString(System.nanoTime())).toFile();
      tempFolder.deleteOnExit();
    }
    return tempFolder;
  }

  private static void writeKeyToFile(KeyPair keyPair, File tempP12Key)
      throws IOException, OperatorCreationException, CertificateException, NoSuchAlgorithmException,
          KeyStoreException, NoSuchProviderException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(tempP12Key);
      KeyStore keyStore = createKeyStore(keyPair);
      keyStore.store(out, DEFAULT_P12_SECRET.toCharArray());
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  private static KeyStore createKeyStore(KeyPair keyPair)
      throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
          OperatorCreationException, NoSuchProviderException {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    keyStore.setKeyEntry(
        DEFAULT_P12_ALIAS,
        keyPair.getPrivate(),
        DEFAULT_P12_SECRET.toCharArray(),
        new Certificate[] {generateCertificate(keyPair)});
    return keyStore;
  }

  private static X509Certificate generateCertificate(KeyPair keyPair)
      throws OperatorCreationException, CertificateException {
    Calendar endCalendar = Calendar.getInstance();
    endCalendar.add(Calendar.YEAR, 10);
    X509v3CertificateBuilder x509v3CertificateBuilder =
        new X509v3CertificateBuilder(
            new X500Name("CN=localhost"),
            BigInteger.valueOf(1),
            Calendar.getInstance().getTime(),
            endCalendar.getTime(),
            new X500Name("CN=localhost"),
            SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
    ContentSigner contentSigner =
        new JcaContentSignerBuilder("SHA1withRSA").build(keyPair.getPrivate());
    X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);
    return new JcaX509CertificateConverter()
        .setProvider("BC")
        .getCertificate(x509CertificateHolder);
  }
}
