/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2011.                            (c) 2011.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package org.opencadc.datalink;

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.HttpPost;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;


/**
 * @author pdowler
 */
class TestUtil {
    static final String[] EXPECTED_COLUMNS = new String[] {
            "ID",
            "access_url",
            "service_def",
            "error_message",
            "semantics",
            "local_semantics",
            "description",
            "content_type",
            "content_length",
            "content_qualifier",
            "link_auth",
            "link_authorized"
    };

    private static final Logger log = Logger.getLogger(TestUtil.class);

    static final URI DATALINK_SERVICE_ID = System.getenv("DATALINK_SERVICE_ID") == null
                                           ? URI.create("ivo://almascience.org/datalink")
                                           : URI.create(System.getenv("DATALINK_SERVICE_ID"));

    /**
     * Compare the GET and POST FIELD.
     *
     * @param getFields  GET FIELD's.
     * @param postFields POST FIELD's.
     */
    static void compareFields(List<VOTableField> getFields, List<VOTableField> postFields) {
        Assert.assertEquals("GET and POST have different number of FIELD's", getFields.size(), postFields.size());
        Integer[] getIndexes = getFieldIndexes(getFields);
        Integer[] postIndexes = getFieldIndexes(postFields);
        Assert.assertEquals("GET and POST uri FIELD ordering is different", getIndexes[0], postIndexes[0]);
        //Assert.assertEquals("GET and POST productType FIELD ordering is different", getIndexes[1], postIndexes[1]);
        Assert.assertEquals("GET and POST accessURL FIELD ordering is different", getIndexes[2], postIndexes[2]);
    }

    /**
     * Check the List of VOTableField for the expected values, and return an
     * Integer array of the index of the value in the List.
     *
     * @param fields List of VOTableField
     * @return Integer array of indexes to values in the VOTableField.
     */
    static Integer[] getFieldIndexes(List<VOTableField> fields) {
        Assert.assertNotNull("VOTable FIELD: should not be null", fields);
        Assert.assertEquals("GET VOTable FIELD: should have 9", fields.size(), 12);
        Integer[] indexes = new Integer[EXPECTED_COLUMNS.length];
        Arrays.fill(indexes, null);

        final List<String> expectedColumns = Arrays.asList(EXPECTED_COLUMNS);
        int index = 0;
        for (VOTableField field : fields) {
            log.debug(field);
            final int listColumnIndex = expectedColumns.indexOf(field.getName());
            indexes[listColumnIndex] = index++;
        }
        Assert.assertNotNull("ID not found", indexes[0]);
        Assert.assertNotNull("access_url not found", indexes[1]);
        Assert.assertNotNull("service_def not found", indexes[2]);
        Assert.assertNotNull("description not found", indexes[3]);
        Assert.assertNotNull("semantics not found", indexes[4]);
        Assert.assertNotNull("content_type not found", indexes[5]);
        Assert.assertNotNull("content_length not found", indexes[6]);
        Assert.assertNotNull("error_message not found", indexes[7]);
        Assert.assertNotNull("readable not found", indexes[8]);
        return indexes;
    }

    static void checkContent(VOTableTable tab) throws Exception {
        Integer[] indices = TestUtil.getFieldIndexes(tab.getFields());
        log.info(String.format("Checking content against %s", Arrays.toString(indices)));
        final List<String> expectedColumns = Arrays.asList(EXPECTED_COLUMNS);

        int uriCol = indices[expectedColumns.indexOf("ID")];
        int urlCol = indices[expectedColumns.indexOf("access_url")];
        int srvCol = indices[expectedColumns.indexOf("service_def")];
        int semCol = indices[expectedColumns.indexOf("semantics")];
        int errCol = indices[expectedColumns.indexOf("error_message")];

        for (final Iterator<List<Object>> iter = tab.getTableData().iterator(); iter.hasNext(); ) {
            final List<Object> row = iter.next();
            Object uriO = row.get(uriCol);
            Object urlO = row.get(urlCol);
            Object srvO = row.get(srvCol);
            Object errO = row.get(errCol);
            Object semO = row.get(semCol);
            Assert.assertNotNull("ID value", uriO);
            Assert.assertNotNull("semantics value", semO);
            if (urlO != null) {
                Assert.assertNull(srvO);
                Assert.assertNull(errO);
                // TODO: would be nice to check the value
                //Assert.assertEquals(DataLink.Term.THIS.getValue(), semO);
            } else if (srvO != null) {
                Assert.assertNull(urlO);
                Assert.assertNull(errO);
            } else {
                Assert.assertNotNull(errO);
            }

            if (urlO != null) {
                URL url = new URL((String) urlO);
                Assert.assertEquals("proto", "https", url.getProtocol());
            }
        }
    }

    /**
     * Compare that two TableData objects contain the same results.
     */
    static void compareTableData(TableData getTableData, TableData postTableData, int urlCol, int sdfCol)
            throws Exception {
        Iterator<List<Object>> getIterator = getTableData.iterator();
        Iterator<List<Object>> postIterator = postTableData.iterator();
        Assert.assertNotNull("Iterator to GET TableData should not be null", getIterator);
        Assert.assertNotNull("Iterator to POST TableData should not be null", postIterator);
        while (getIterator.hasNext()) {
            Assert.assertTrue("Expected POST TABLEDATA row, but found none", postIterator.hasNext());
            List<Object> getRow = getIterator.next();
            List<Object> postRow = postIterator.next();
            Assert.assertNotNull("GET TABLEDATA row is null", getRow);
            Assert.assertNotNull("POST TABLEDATA row is null", postRow);
            Assert.assertEquals("GET and POST row column count is different", getRow.size(), postRow.size());
            for (int i = 0; i < getRow.size(); i++) {
                Object getObject = getRow.get(i);
                Object postObject = postRow.get(i);
                log.debug("column[" + i + "] GET=" + getObject + ", POST=" + postObject);
                if (getObject == null) {
                    Assert.assertNull("POST TABLEDATA row value is null", postObject);
                } else {
                    Assert.assertNotNull("POST TABLEDATA row value is null", postObject);
                    if (i == urlCol) {
                        String gs = (String) getObject;
                        URL gurl = new URL(gs);
                        String ps = (String) postObject;
                        URL purl = new URL(ps);
                        Assert.assertEquals(gurl.getProtocol(), purl.getProtocol());
                        Assert.assertEquals(gurl.getHost(), purl.getHost());
                        Assert.assertEquals(gurl.getPath(), purl.getPath());
                        if (gurl.getQuery() == null) {
                            Assert.assertNull(purl.getQuery());
                        } else {
                            String[] gp = gurl.getQuery().split("&");
                            String[] pp = purl.getQuery().split("&");
                            Assert.assertEquals(gp.length, pp.length);
                            for (int ii = 0; ii < gp.length; ii++) {
                                String s1 = gp[ii].toLowerCase();
                                String s2 = pp[ii].toLowerCase();
                                if (s1.startsWith("runid")) {
                                    Assert.assertTrue(s2.startsWith("runid"));
                                } else {
                                    Assert.assertEquals(s1, s2);
                                }
                            }
                        }
                    } else if (i != sdfCol) {
                        Assert.assertEquals("GET and POST row values are different", getObject, postObject);
                    } // End if - otherwise it's dynamic and cannot compare
                }
            }
        }
        Assert.assertFalse("POST TABLEDATA has more rows than the GET TABLEDATA", postIterator.hasNext());
    }

    /**
     * Get the given query.
     *
     * @param endpoint   URL endpoint.
     * @param parameters Parameters.
     * @return VOtable
     */
    static VOTableDocument get(URL endpoint, String[] parameters) throws IOException {
        return get(endpoint, parameters, 200);
    }

    static VOTableDocument get(URL endpoint, String[] parameters, int expectedCode) throws IOException {
        URL url = getQueryURL(endpoint, parameters);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        log.debug("GET " + url);
        final HttpGet get = new HttpGet(url, out);
        get.setFollowRedirects(false);
        get.run();

        final String str;

        if (get.getRedirectURL() != null) {
            str = TestUtil.followGetRedirect(get.getRedirectURL());
        } else {
            log.debug("throwable", get.getThrowable());
            Assert.assertEquals("HTTP status code from " + endpoint + " (" + Arrays.toString(parameters) + ")",
                                expectedCode, get.getResponseCode());
            if (expectedCode < 400 && get.getThrowable() != null) {
                Assert.fail("GET of " + url + " failed because " + get.getThrowable().getMessage());
            }
            str = out.toString();
        }

        return TestUtil.readVOTable(str);
    }

    static VOTableDocument readVOTable(final String input) throws IOException {
        log.debug("GET response: \n" + input);
        VOTableReader reader = new VOTableReader();
        VOTableDocument votable = reader.read(input);
        Assert.assertNotNull("VOTable should not be null", votable);
        return votable;
    }

    static String followGetRedirect(final URL endpoint) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        log.debug("GET " + endpoint);
        final HttpGet get = new HttpGet(endpoint, out);
        get.setFollowRedirects(false);
        get.run();

        if (get.getRedirectURL() != null) {
            return TestUtil.followGetRedirect(get.getRedirectURL());
        } else {
            return out.toString();
        }
    }


    /**
     * POST the given parameters and return the defaulty format: VOTable.
     */
    static VOTableDocument post(URL endpoint, Map<String, Object> parameters) throws IOException {
        final String response = post(endpoint, parameters, null);
        return TestUtil.readVOTable(response);
        //VOTableReader reader = new VOTableReader();
        //VOTableDocument votable = reader.read(response);
        //Assert.assertNotNull("VOTable should not be null", votable);
        //return votable;
    }

    /**
     * POST the given parameters plus an optional  RESPONSEFORMAT=responseFormat and return
     * the raw response in the specified format.
     */
    static String post(URL endpoint, Map<String, Object> parameters, String responseFormat) throws IOException {
        // POST parameters.
        URL url = getQueryURL(endpoint, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // POST the query.
        log.debug("POST " + url);
        if (responseFormat != null) {
            parameters.put("RESPONSEFORMAT", responseFormat);
        }
        HttpPost post = new HttpPost(url, parameters, out);
        post.setFollowRedirects(false);
        post.run();

        if (post.getThrowable() != null) {
            Assert.fail("POST of " + url.toString() + " failed because " + post.getThrowable().getMessage());
        }

        final URL redirectURL = post.getRedirectURL();
        final String output;
        if (redirectURL != null) {
            output = TestUtil.followGetRedirect(redirectURL);
        } else {
            output = out.toString();
            log.debug("POST response: \n" + output);
        }

        return output;
    }

    /**
     * Using the resourceURL, build a query URL.
     *
     * @param parameters query parameters.
     * @return query URL
     */
    static URL getQueryURL(URL baseUrl, String[] parameters) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        if (parameters != null) {
            sb.append("?");
            for (String s : parameters) {
                sb.append(s);
                sb.append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPath() + sb);
    }
}
