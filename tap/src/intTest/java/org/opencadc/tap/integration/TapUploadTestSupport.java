/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2026.                            (c) 2026.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 ************************************************************************
 */

package org.opencadc.tap.integration;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.RunnableAction;
import ca.nrc.cadc.conformance.uws2.JobResultWrapper;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableInfo;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.security.auth.Subject;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helpers for TAP UPLOAD integration tests (inline, HTTP, HTTPS).
 */
public final class TapUploadTestSupport {

    static final URI TAP_RESOURCE_ID = URI.create("ivo://almascience.org/tap");

    /**
     * Do not allocate inline upload payloads larger than this in integration tests.
     */
    static final long MAX_TESTABLE_HARD_UPLOAD_BYTES = 32L * 1024L * 1024L;

    /**
     * Public VOTable fixture shipped in the WAR for upload-http/upload-https tests (TAP §2.7.6).
     */
    private static final String FIXTURE_PATH = "upload-fixtures/TAPUploadTest-1.xml";

  /**
   * {@code /capability[@standardID="ivo://ivoa.net/std/TAP"]/uploadLimit/hard} with TAPRegExt elements
   * (local-name fallback for namespace-qualified {@code tr:uploadLimit} children).
   */
    private static final String HARD_UPLOAD_LIMIT_XPATH =
            "//*[local-name()='capability' and @standardID='ivo://ivoa.net/std/TAP']"
                    + "/*[local-name()='uploadLimit']/*[local-name()='hard']";

    private static final Logger log = Logger.getLogger(TapUploadTestSupport.class);

    private TapUploadTestSupport() {
    }

    public static URL getFixtureUrl() throws MalformedURLException {
        final RegistryClient registryClient = new RegistryClient();
        final URL sync = registryClient.getServiceURL(TAP_RESOURCE_ID, Standards.TAP_10);
        String base = sync.toExternalForm();
        if (base.endsWith("/sync")) {
            base = base.substring(0, base.length() - "/sync".length());
        } else if (base.endsWith("/sync/")) {
            base = base.substring(0, base.length() - "/sync/".length());
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return URI.create(base + FIXTURE_PATH).toURL();
    }

    /**
     * Fetches the TAP service capabilities document and returns the advertised hard upload byte limit.
     */
    public static long readHardUploadByteLimit() throws Exception {
        final RegistryClient registryClient = new RegistryClient();
        final URL capUrl = registryClient.getServiceURL(TAP_RESOURCE_ID, Standards.VOSI_CAPABILITIES);
        final byte[] xml = getCapabilitiesXml(capUrl);
        return parseHardUploadByteLimit(xml);
    }

    /**
     * @return {@code true} if the oversized-upload test should be skipped (limit too large to materialize locally).
     */
    public static boolean skipHardUploadByteLimitTest(final Logger testLog) throws Exception {
        final long hardLimit = readHardUploadByteLimit();
        if (hardLimit > MAX_TESTABLE_HARD_UPLOAD_BYTES) {
            testLog.info(String.format(
                    "TAP uploadLimit hard is %d bytes (> %d); oversized-upload integration test skipped",
                    hardLimit, MAX_TESTABLE_HARD_UPLOAD_BYTES));
            return true;
        }
        testLog.info(String.format(
                "TAP uploadLimit hard is %d bytes; running oversized-upload rejection test", hardLimit));
        return false;
    }

    /**
     * Creates a temp VOTable whose file size exceeds the advertised hard {@code uploadLimit}.
     */
    public static File createOversizedUploadVotableForCapabilitiesLimit() throws Exception {
        final long hardLimit = readHardUploadByteLimit();
        return createOversizedUploadVOTable(hardLimit);
    }

    static Map<String, Object> inlineUploadJobParams(final File votable, final String tableName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("upload1", votable);
        params.put("UPLOAD", tableName + ",param:upload1");
        params.put("LANG", "ADQL");
        params.put("QUERY", "SELECT * FROM tap_upload." + tableName);
        return params;
    }

    static File createOversizedUploadVOTable(final long hardLimitBytes) throws IOException {
        final long payloadBytes = hardLimitBytes + 64L * 1024L;
        final File file = File.createTempFile("tap-upload-byte-limit-inttest-", ".xml");
        final String prefix = """
                <?xml version="1.0" encoding="UTF-8"?>
                <VOTABLE xmlns="http://www.ivoa.net/xml/VOTable/v1.2" version="1.2">
                  <RESOURCE type="results">
                    <TABLE>
                      <FIELD name="data" datatype="char" arraysize="*"/>
                      <DATA>
                        <TABLEDATA>
                          <TR><TD>
                """;
        final String suffix = """
                          </TD></TR>
                        </TABLEDATA>
                      </DATA>
                    </TABLE>
                  </RESOURCE>
                </VOTABLE>
                """;
        try (Writer writer = new FileWriter(file)) {
            writer.write(prefix);
            final char[] filler = new char[64 * 1024];
            Arrays.fill(filler, 'X');
            long written = 0;
            while (written < payloadBytes) {
                final int chunk = (int) Math.min(filler.length, payloadBytes - written);
                writer.write(filler, 0, chunk);
                written += chunk;
            }
            writer.write(suffix);
        }
        return file;
    }

    public static void assertUploadRejectedForByteLimit(final JobResultWrapper result, final long hardLimit)
            throws Exception {
        final String expectedFragment = "exceeds maximum of " + hardLimit + " bytes";
        if (result.throwable != null) {
            Assert.assertTrue(result.throwable.getMessage(), result.throwable.getMessage().contains(expectedFragment)
                    || result.throwable.toString().contains(expectedFragment));
            return;
        }
        final String body = result.syncOutput == null
                ? ""
                : new String(result.syncOutput, StandardCharsets.UTF_8);
        if (result.responseCode != 200) {
            Assert.assertTrue("expected byte-limit message in error response: " + body,
                    body.contains(expectedFragment) || body.contains("exceeds maximum"));
            return;
        }
        final VOTableDocument vot = new VOTableReader().read(new ByteArrayInputStream(result.syncOutput));
        final String status = queryStatus(vot);
        Assert.assertEquals("ERROR", status);
        Assert.assertTrue("expected byte-limit message in VOTable", body.contains(expectedFragment)
                || body.contains("exceeds maximum"));
    }

    private static String queryStatus(final VOTableDocument vot) {
        final VOTableResource vr = vot.getResourceByType("results");
        Assert.assertNotNull(vr);
        for (final VOTableInfo info : vr.getInfos()) {
            if ("QUERY_STATUS".equals(info.getName())) {
                return info.getValue();
            }
        }
        return null;
    }

    private static byte[] getCapabilitiesXml(final URL capUrl) throws IOException {
        final ByteArrayOutputStream dest = new ByteArrayOutputStream();
        final HttpGet get = new HttpGet(capUrl, dest);
        get.setFollowRedirects(true);
        final Subject subject = AuthenticationUtil.getAnonSubject();
        Subject.doAs(subject, new RunnableAction(get));
        if (get.getThrowable() != null) {
            throw new IOException("failed to GET capabilities from " + capUrl, get.getThrowable());
        }
        Assert.assertEquals("capabilities GET " + capUrl, 200, get.getResponseCode());
        return dest.toByteArray();
    }

    private static long parseHardUploadByteLimit(final byte[] xml) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setNamespaceAware(true);
        final Document doc = dbf.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));

        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(final String prefix) {
                if ("tr".equals(prefix)) {
                    return "http://www.ivoa.net/xml/TAPRegExt/v1.0";
                }
                if ("vosi".equals(prefix)) {
                    return "http://www.ivoa.net/xml/VOSICapabilities/v1.0";
                }
                return XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(final String namespaceURI) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(final String namespaceURI) {
                return null;
            }
        });

        Node hard = (Node) xpath.evaluate(HARD_UPLOAD_LIMIT_XPATH, doc, XPathConstants.NODE);
        if (hard == null) {
            final String tapCapsXpath =
                    "/vosi:capabilities/vosi:capability[@standardID='ivo://ivoa.net/std/TAP']"
                            + "/tr:uploadLimit/tr:hard";
            hard = (Node) xpath.evaluate(tapCapsXpath, doc, XPathConstants.NODE);
        }
        Assert.assertNotNull("uploadLimit/hard not found in TAP capabilities", hard);
        final String unit = hard.getAttributes() == null || hard.getAttributes().getNamedItem("unit") == null
                ? "byte"
                : hard.getAttributes().getNamedItem("unit").getNodeValue();
        Assert.assertTrue("uploadLimit hard unit must be byte, was: " + unit,
                unit == null || unit.isEmpty() || "byte".equals(unit));
        final String text = hard.getTextContent().trim();
        Assert.assertFalse("uploadLimit/hard is empty", text.isEmpty());
        return Long.parseLong(text);
    }
}