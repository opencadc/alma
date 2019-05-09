/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
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
************************************************************************
*/

package org.opencadc.reg.server;


import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.reg.oai.OAIReader;

/**
 *
 * @author pdowler
 */
public class OAIValidityTest {
    private static final Logger log = Logger.getLogger(OAIValidityTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.reg.server", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.reg", Level.INFO);
    }
    
    URL oaiEndpoint;
    
    public OAIValidityTest() { 
        try {
            RegistryClient rc = new RegistryClient();
            this.oaiEndpoint = rc.getServiceURL(VosiCapabilitiesTest.RESOURCE_ID, Standards.REGISTRY_10, AuthMethod.ANON, Standards.INTERFACE_REG_OAI);
            
        } catch (Exception ex) {
            throw new RuntimeException("CONFIG: failed to find OAI endpoint", ex);
        }
    }
    
    @Test
    public void testIdentify() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=Identify");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testListMetadataFormats() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListMetadataFormats");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testListSets() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListSets");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testListIdentifiers() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListIdentifiers&metadataPrefix=ivo_vor");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testListIdentifiersEmpty() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListIdentifiers&metadataPrefix=ivo_vor&until=1999-01-01");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNotNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testListIdentifiersFromUntil() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListIdentifiers&metadataPrefix=ivo_vor&from=2010-01-01T00:00:00Z&until=2018-12-31T11:59:59Z");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testGetRecord() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=GetRecord&metadataPrefix=ivo_vor&identifier=ivo://cadc.nrc.ca/tap");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testGetRecordsFrom() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListRecords&metadataPrefix=ivo_vor&from=2018-01-01T00:00:00Z");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testGetRecordsEmpty() {
        try {
            URL u = new URL(oaiEndpoint.toExternalForm() + "?verb=ListRecords&metadataPrefix=ivo_vor&until=1999-01-01");
            log.info(u.toExternalForm());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(u, bos);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            Assert.assertEquals("text/xml", get.getContentType());
            
            String xml = bos.toString();
            log.debug("xml:\n" + xml);
            
            StringReader sr = new StringReader(xml);
            OAIReader r = new OAIReader();
            Document doc = r.read(sr);
            Element root = doc.getRootElement();
            Element error = root.getChild("error", root.getNamespace());
            Assert.assertNotNull("OAI error", error);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    // useful to debug schema issues since we rely on cadc-registry to provide all the IVOA xsd files
    //@Test
    public void testSchemaMap() {
        for (Map.Entry<String,String> me : ca.nrc.cadc.reg.XMLConstants.SCHEMA_MAP.entrySet()) {
            log.info("schema map: " + me.getKey() + " -> " + me.getValue());
        }
    }
}
