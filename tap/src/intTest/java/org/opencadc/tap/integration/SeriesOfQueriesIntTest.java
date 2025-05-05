package org.opencadc.tap.integration;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.net.HttpPost;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.vosi.avail.CheckWebService;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SeriesOfQueriesIntTest {
    private static final Logger LOGGER = Logger.getLogger(SeriesOfQueriesIntTest.class);
    private static final URI SERVICE_URI = URI.create(System.getProperty("SERVICE_ID", "ivo://almascience.org/tap"));
    private static final URI SCHEMA = URI.create(System.getProperty("SCHEMA", "ivoa"));
    private static final Map<String, String> QUERY_PARAMETERS = new HashMap<>();
    private static final String[] QUERIES = new String[] {
            "SELECT TOP 1 s_region, target_name, obs_id FROM %s.obscore where obs_id='uid://A001/X145/X199' and target_name='ESO005-G004'",
            "SELECT TOP 1000 * FROM %s.obscore WHERE 1=CONTAINS(POINT('ICRS',s_ra,s_dec), CIRCLE('ICRS',204.269845,-29.862594,0.1))",
            "SELECT TOP 100 * FROM %s.obscore WHERE 1=CONTAINS(POINT('ICRS',s_ra,s_dec), POLYGON('ICRS',204.0,-29.7,204.2,-29.7,204.2,-30,204.0,-30))" ,
            "select distinct proposal_id from %s.obscore WHERE (INTERSECTS(CIRCLE('ICRS',356.6203971,12.7926505,0.005), s_region) = 1) AND calib_level>1 AND data_rights='Public'",
            "SELECT column_name,datatype FROM TAP_SCHEMA.COLUMNS WHERE COLUMN_NAME='access_url'",
            "SELECT count(1) FROM %s.obscore",
            "SELECT TOP 100 * FROM %s.obscore"
    };

    static {
        SeriesOfQueriesIntTest.QUERY_PARAMETERS.put("LANG", "ADQL");
        SeriesOfQueriesIntTest.QUERY_PARAMETERS.put("FORMAT", "votable");

        Log4jInit.setLevel(SeriesOfQueriesIntTest.class.getPackageName(), Level.DEBUG);
    }


    @Test
    public void run() throws Exception {
        checkAvailability();

        final Date date = new Date();
        final String prefix = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
        final Path outputDirectory = Files.createTempDirectory(prefix);

        for (int i = 0; i < SeriesOfQueriesIntTest.QUERIES.length; i++) {
            checkQuery(i + 1, SeriesOfQueriesIntTest.QUERIES[i], outputDirectory);
        }
    }

    private void checkAvailability() throws Exception {
        final CheckWebService checkWebService = new CheckWebService(this.lookupServiceURL(Standards.VOSI_AVAILABILITY),
                                                                    true);
        checkWebService.check();
    }

    private void checkQuery(final int index, final String query, final Path outputDirectory) throws Exception {
        LOGGER.info("Checking query " + query);
        final Writer writer = new StringWriter();
        final OutputStream writerOutputStream = new WriterOutputStream(writer, StandardCharsets.UTF_8);
        final URL serviceURL = lookupServiceURL(Standards.TAP_10);
        final URL queryURL = new URL(serviceURL.toExternalForm() + "/sync");
        LOGGER.info("Checking against " + queryURL.toExternalForm());

        final HttpPost httpPost = new HttpPost(queryURL, encodeQueryParameters(query), writerOutputStream);
        httpPost.setFollowRedirects(true);
        httpPost.prepare();

        final Throwable error = httpPost.getThrowable();
        if (error != null) {
            throw new IllegalStateException(error.getMessage(), error);
        } else {
            final VOTableReader voTableReader = new VOTableReader();
            final VOTableDocument document = voTableReader.read(httpPost.getInputStream());
            final VOTableWriter voTableWriter = new VOTableWriter();
            final Path outputFilePath = Path.of(outputDirectory.toString(), getClass().getName() + "--" + index + ".xml");
            try (final FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath.toFile())) {
                voTableWriter.write(document, fileOutputStream);
                fileOutputStream.flush();
                LOGGER.info("Finished writing to " + outputFilePath);
            }
        }
    }

    private Map<String, Object> encodeQueryParameters(final String query) {
        final String encodedQuery = String.format(query, SeriesOfQueriesIntTest.SCHEMA);
        final Map<String, Object> queryParameters = new HashMap<>(SeriesOfQueriesIntTest.QUERY_PARAMETERS);
        queryParameters.put("QUERY", encodedQuery);

        return queryParameters;
    }

    URL lookupServiceURL(final URI standard) {
        final RegistryClient registryClient = new RegistryClient();
        return registryClient.getServiceURL(SeriesOfQueriesIntTest.SERVICE_URI, standard, AuthMethod.ANON);
    }
}
