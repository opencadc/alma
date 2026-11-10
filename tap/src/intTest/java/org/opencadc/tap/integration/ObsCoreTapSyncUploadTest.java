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
 ************************************************************************
 */

package org.opencadc.tap.integration;

import ca.nrc.cadc.conformance.uws2.JobResultWrapper;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableInfo;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.tap.integration.TapSyncUploadTest;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * TAP sync UPLOAD conformance (inline, http, https) per TAP 1.1 §2.7.6 / DALI.
 */
public class ObsCoreTapSyncUploadTest extends TapSyncUploadTest {
    private static final Logger log = Logger.getLogger(ObsCoreTapSyncUploadTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.tap", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.tap.integration", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.conformance.uws2", Level.INFO);
    }

    public ObsCoreTapSyncUploadTest() {
        super(TapUploadTestSupport.TAP_RESOURCE_ID);
        File inlineVotable = FileUtil.getFileFromResource("TAPUploadTest-1.xml", ObsCoreTapSyncUploadTest.class);
        setTestFile(inlineVotable);
        try {
            setTestURL(TapUploadTestSupport.getFixtureUrl());
        } catch (Exception e) {
            throw new IllegalStateException("failed to resolve upload fixture URL", e);
        }
    }

    /**
     * VOTable 1.3 upload with xtype metadata (TAP 1.1 / DALI serialization).
     */
    @Test
    public void testUploadVOTable13() {
        try {
            File votable = FileUtil.getFileFromResource("UploadTest-TAP-1.1.xml", ObsCoreTapSyncUploadTest.class);
            String tableName = "plane_tab";
            Map<String, Object> params = new HashMap<>();
            params.put("upload1", votable);
            params.put("UPLOAD", tableName + ",param:upload1");
            params.put("LANG", "ADQL");
            params.put("QUERY", "SELECT planeID, productID FROM tap_upload." + tableName);

            JobResultWrapper result = createAndExecuteSyncParamJobPOST("testUploadVOTable13", params);

            Assert.assertNull(result.throwable);
            Assert.assertEquals(200, result.responseCode);
            Assert.assertNotNull(result.syncOutput);

            VOTableDocument vot = readVOTable(result.syncOutput);
            Assert.assertEquals("OK", queryStatus(vot));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testUploadJoinWithFilter() {
        try {
            File votable = FileUtil.getFileFromResource("TAPUploadTest-1.xml", ObsCoreTapSyncUploadTest.class);
            String tableName = "collections";
            Map<String, Object> params = new HashMap<>();
            params.put("upload1", votable);
            params.put("UPLOAD", tableName + ",param:upload1");
            params.put("LANG", "ADQL");
            params.put("QUERY",
                    "SELECT collection, observationID FROM tap_upload." + tableName
                            + " WHERE collection = 'BLAST'");

            JobResultWrapper result = createAndExecuteSyncParamJobPOST("testUploadJoinWithFilter", params);

            Assert.assertNull(result.throwable);
            Assert.assertEquals(200, result.responseCode);
            Assert.assertNotNull(result.syncOutput);

            VOTableDocument vot = readVOTable(result.syncOutput);
            Assert.assertEquals("OK", queryStatus(vot));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    private static VOTableDocument readVOTable(byte[] data) throws Exception {
        VOTableReader reader = new VOTableReader();
        return reader.read(new ByteArrayInputStream(data));
    }

    private static String queryStatus(VOTableDocument vot) {
        VOTableResource vr = vot.getResourceByType("results");
        Assert.assertNotNull(vr);
        String status = null;
        for (VOTableInfo info : vr.getInfos()) {
            if ("QUERY_STATUS".equals(info.getName())) {
                status = info.getValue();
            }
        }
        return status;
    }
}
