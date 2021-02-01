/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2021.                            (c) 2021.
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
*
************************************************************************
*/

package org.opencadc.data.integration;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.Log4jInit;
import nom.tam.fits.Fits;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Integration test to pull existing test FITS files from CADC VOSpace (Vault) into a local directory, then PUT them
 * into the local test ALMA Data and run cutouts from the service.  This test depends on:
 * <ul>
 *   <li>A local ALMA Data</li>
 *   <li>A local inventory Postgres database for ALMA Data</li>
 *   <li>A Registry to point to this local ALMA Data that is accessible from this test</li>
 *   <li>A proxy for this ALMA Data</li>
 * </ul>
 */
public class FitsOperationsTest extends DataIntTest {
    private final static Logger LOGGER = Logger.getLogger(FitsOperationsTest.class);
    private final static URI VOSPACE_URI = URI.create("ivo://cadc.nrc.ca/vault");
    private final static Path LOCAL_DATA_PATH = new File(System.getProperty("user.home")
                                                         + "/.config/test-data").toPath();
    private final static String SERVICE_DATA_FILE_DIR = "/data";

    protected URL filesVaultURL;

    static {
        Log4jInit.setLevel("org.opencadc.data", Level.DEBUG);
        Log4jInit.setLevel("ca.nrc.cadc.net", Level.INFO);
    }

    public FitsOperationsTest() throws Exception {
        super();

        final RegistryClient regClient = new RegistryClient();
        filesVaultURL = new URL(regClient.getServiceURL(VOSPACE_URI, Standards.VOSPACE_FILES_20, AuthMethod.ANON)
                                + "/ALMA/test-data/cutouts");
        LOCAL_DATA_PATH.toFile().mkdirs();
    }

    @Test
    public void test4DCube() throws Exception {
        final String testFilePrefix = "test-4d-cube";
        final String testFileExtension = "fits";
        final URI fileURI = URI.create(testFilePrefix + "." + testFileExtension);
        final String[] cutoutSpecs = new String[] {
                "[30:45,50:75,10:105,*]"
        };

        downloadAndCompare(fileURI, cutoutSpecs, testFilePrefix, testFileExtension);
    }

    private void downloadAndCompare(final URI artifactURI, final String[] cutoutSpecs, final String testFilePrefix,
                                    final String testFileExtension) throws Exception {
        ensureFile(artifactURI);
        final StringBuilder queryStringBuilder = new StringBuilder("?");
        Arrays.stream(cutoutSpecs).
                forEach(cut -> queryStringBuilder.append("SUB=").append(NetUtil.encode(cut)).append("&"));

        // Ensure there is an ampersand ready for the file parameter
        queryStringBuilder.deleteCharAt(queryStringBuilder.lastIndexOf("&")).append("&");
        queryStringBuilder.append("file=").append(SERVICE_DATA_FILE_DIR).append("/").append(testFilePrefix).
                append(".").append(testFileExtension);

        final URL fileSUBURL = new URL(filesURL + queryStringBuilder.toString());
        final File outputFile = Files.createTempFile(testFilePrefix + "-", "." + testFileExtension).toFile();
        LOGGER.debug("Writing cutout to " + outputFile);

        // Perform the cutout.
        LOGGER.debug("Testing cutout with " + fileSUBURL);
        try (final FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            final HttpGet cutoutClient = new HttpGet(fileSUBURL, fileOutputStream);
            cutoutClient.setFollowRedirects(true);
            cutoutClient.run();
            fileOutputStream.flush();
        }
        LOGGER.debug("Cutout complete -> " + artifactURI);

        // setup
        final File expectedFile = new File(LOCAL_DATA_PATH.toFile(), testFilePrefix + "-cutout.fits");
        ensureLocalFile(expectedFile);

        final RandomAccessDataObject expectedCutout = new RandomAccessFileExt(expectedFile, "r");
        final Fits expectedFits = new Fits(expectedCutout);

        final RandomAccessDataObject resultCutout = new RandomAccessFileExt(outputFile, "r");
        final Fits resultFits = new Fits(resultCutout);

        FitsTest.assertFitsEqual(expectedFits, resultFits);
    }

    /**
     * Perform the cutout against a known file in the local service.
     * @param fileURI   The URI to ensure.
     * @throws Exception    Any errors.
     */
    private void ensureFile(final URI fileURI) throws Exception {
        LOGGER.info("ensureLocalFile(" + fileURI + ")");
        final String schemePath = fileURI.getSchemeSpecificPart();
        final String fileName = schemePath.substring(schemePath.lastIndexOf("/") + 1);
        final File localFile = new File(LOCAL_DATA_PATH.toFile(), fileName);

        ensureLocalFile(localFile);

        LOGGER.info("cleanFilePut(" + fileURI + "): OK");
    }

    private void ensureLocalFile(final File localFile) throws Exception {
        if (!localFile.exists() || (localFile.length() == 0)) {
            localFile.delete();
            final URL downloadURL = new URL(filesVaultURL + "/" + localFile.getName());
            LOGGER.info("File " + localFile + " does not exist.  Downloading from " + downloadURL);
            try (final FileOutputStream fileOutputStream = new FileOutputStream(localFile)) {
                final HttpGet download = new HttpGet(downloadURL, fileOutputStream);
                download.setFollowRedirects(true);
                download.run();

                fileOutputStream.flush();
                Assert.assertNull("Should be no error.", download.getThrowable());
            }
        }
    }
}
