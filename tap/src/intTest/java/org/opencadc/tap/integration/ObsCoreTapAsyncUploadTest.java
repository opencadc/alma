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

import ca.nrc.cadc.tap.integration.TapAsyncUploadTest;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import ca.nrc.cadc.uws.ExecutionPhase;
import ca.nrc.cadc.uws.Job;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * TAP async UPLOAD conformance (inline param rewrite and HTTP URL) per TAP §2.7.6.
 */
public class ObsCoreTapAsyncUploadTest extends TapAsyncUploadTest {
    private static final Logger log = Logger.getLogger(ObsCoreTapAsyncUploadTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.tap", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.tap.integration", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.conformance.uws2", Level.INFO);
    }

    public ObsCoreTapAsyncUploadTest() {
        super(TapUploadTestSupport.TAP_RESOURCE_ID);
        File inlineVotable = FileUtil.getFileFromResource("TAPUploadTest-1.xml", ObsCoreTapAsyncUploadTest.class);
        setTestFile(inlineVotable);
        try {
            setTestURL(TapUploadTestSupport.getFixtureUrl());
        } catch (Exception e) {
            throw new IllegalStateException("failed to resolve upload fixture URL", e);
        }
    }

    @Test
    public void testUploadExceedsHardByteLimit() throws Exception {
        if (TapUploadTestSupport.skipHardUploadByteLimitTest(log)) {
            return;
        }
        final long hardLimit = TapUploadTestSupport.readHardUploadByteLimit();
        final File votable = TapUploadTestSupport.createOversizedUploadVotableForCapabilitiesLimit();
        try {
            final Map<String, Object> params =
                    TapUploadTestSupport.inlineUploadJobParams(votable, "oversized");
            final URL jobUrl = createAsyncParamJob("testUploadExceedsHardByteLimit", params);
            Assert.assertNotNull(jobUrl);
            final Job job = executeAsyncJob("testUploadExceedsHardByteLimit", jobUrl, 120L);
            Assert.assertEquals(ExecutionPhase.ERROR, job.getExecutionPhase());
            final String errorSummary = String.valueOf(job.getErrorSummary());
            Assert.assertTrue("expected byte-limit rejection in async job: " + errorSummary,
                    errorSummary.contains("exceeds maximum of " + hardLimit + " bytes")
                            || errorSummary.contains("exceeds maximum"));
        } finally {
            if (!votable.delete()) {
                votable.deleteOnExit();
            }
        }
    }
}
