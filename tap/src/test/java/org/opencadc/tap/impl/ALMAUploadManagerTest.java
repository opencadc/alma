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

package org.opencadc.tap.impl;

import ca.nrc.cadc.io.ByteLimitExceededException;
import ca.nrc.cadc.uws.Parameter;
import ca.nrc.cadc.util.PropertiesReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opencadc.alma.AlmaProperties;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ALMAUploadManagerTest {

    private static final long CONFIGURED_BYTE_LIMIT = 2L * 1024L * 1024L;

    @Before
    public void setAlmaPropertiesDir() throws Exception {
        final URL properties = getClass().getClassLoader().getResource("org.opencadc.alma.properties");
        Assert.assertNotNull("test org.opencadc.alma.properties not on classpath", properties);
        final File configDir = new File(properties.toURI()).getParentFile();
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, configDir.getAbsolutePath());
        Assert.assertEquals(CONFIGURED_BYTE_LIMIT,
                new AlmaProperties().getTAPUploadByteLimit(ALMAUploadManager.DEFAULT_UPLOAD_BYTE_LIMIT));
    }

    @Test
    public void testUploadRejectsVOTableExceedingConfiguredByteLimit() throws Exception {
        final ALMAUploadManager uploadManager = new ALMAUploadManager();
        uploadManager.setDataSource(Mockito.mock(DataSource.class));

        final File uploadFile = createOversizedVOTable(CONFIGURED_BYTE_LIMIT + 64 * 1024L);
        try {
            final String location = uploadFile.toURI().toString();
            final List<Parameter> paramList =
                    List.of(new Parameter("UPLOAD", "oversized," + location));

            try {
                uploadManager.upload(paramList, "testjobid");
                Assert.fail("expected byte-limit rejection");
            } catch (IllegalArgumentException ex) {
                Assert.assertEquals(
                        "Size of upload file exceeds maximum of " + CONFIGURED_BYTE_LIMIT + " bytes.",
                        ex.getMessage());
                Assert.assertNotNull(ex.getCause());
                Assert.assertTrue(ex.getCause() instanceof ByteLimitExceededException);
                Assert.assertEquals(CONFIGURED_BYTE_LIMIT,
                        ((ByteLimitExceededException) ex.getCause()).getLimit(), 0L);
            }
        } finally {
            if (!uploadFile.delete()) {
                uploadFile.deleteOnExit();
            }
        }
    }

    private static File createOversizedVOTable(final long payloadBytes) throws IOException {
        final File file = File.createTempFile("alma-upload-byte-limit-", ".xml");
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
}