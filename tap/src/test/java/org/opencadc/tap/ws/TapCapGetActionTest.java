/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2026.                            (c) 2026.
 *  Government of Canada                 Gouvernement du Canada
 *
 ************************************************************************
 */

package org.opencadc.tap.ws;

import ca.nrc.cadc.reg.Capabilities;
import ca.nrc.cadc.reg.CapabilitiesReader;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.util.PropertiesReader;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TapCapGetActionTest {

    private static final long CONFIGURED_BYTE_LIMIT = 2L * 1024L * 1024L;

    @Before
    public void setAlmaPropertiesDir() throws Exception {
        final var properties = getClass().getClassLoader().getResource("org.opencadc.alma.properties");
        Assert.assertNotNull(properties);
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY,
                new java.io.File(properties.toURI()).getParentFile().getAbsolutePath());
    }

    @Test
    public void applyUploadByteLimitUsesAlmaProperties() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/capabilities-tap-upload-limit.xml")) {
            Assert.assertNotNull(in);
            Capabilities caps = new CapabilitiesReader(false)
                    .read(new InputStreamReader(in, StandardCharsets.UTF_8));
            TapCapGetAction.applyUploadByteLimit(caps);

            final var tap = caps.findCapability(Standards.TAP_10);
            Assert.assertNotNull(tap);
            Element uploadLimit = null;
            for (Element e : extensionMetadata(tap)) {
                if ("uploadLimit".equals(e.getName())) {
                    uploadLimit = e;
                    break;
                }
            }
            Assert.assertNotNull(uploadLimit);
            Assert.assertEquals(Long.toString(CONFIGURED_BYTE_LIMIT),
                    uploadLimit.getChildText("default"));
            Assert.assertEquals(Long.toString(CONFIGURED_BYTE_LIMIT),
                    uploadLimit.getChildText("hard"));
            Assert.assertEquals("byte", uploadLimit.getChild("default").getAttributeValue("unit"));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Element> extensionMetadata(final ca.nrc.cadc.reg.Capability capability)
            throws Exception {
        Method accessor = ca.nrc.cadc.reg.Capability.class.getDeclaredMethod("getExtensionMetadata");
        accessor.setAccessible(true);
        return (List<Element>) accessor.invoke(capability);
    }
}