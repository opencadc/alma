/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
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

package org.opencadc.alma.data;

import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.Parameter;
import nom.tam.fits.Header;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.RandomAccessDataObject;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.alma.logging.web.WebServiceMetaData;
import org.opencadc.fits.FitsOperations;
import org.opencadc.soda.SodaParamValidator;

import static org.mockito.Mockito.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataJobRunnerTest {

    @Test
    public void testVerifyArgumentsMissingFile() {
        final DataJobRunner testSubject = new DataJobRunner();

        final Map<String, String[]> inputParameters = new HashMap<>();
        inputParameters.put("cutout", new String[]{"[0][80:500]", "[10]"});

        final Job testJob = new Job();
        final List<Parameter> parameterList = new ArrayList<>();
        inputParameters.forEach((k, v) -> Arrays.stream(v).forEach(paramValue -> parameterList.add(
                new Parameter(k, paramValue))));
        testJob.setParameterList(parameterList);
        testSubject.setJob(testJob);
        try {
            testSubject.verifyArguments();
            Assert.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            Assert.assertTrue("Wrong message: " + message, message.contains("Usage"));
        }
    }

    @Test
    public void testVerifyArgumentsMultipleFiles() {
        final DataJobRunner testSubject = new DataJobRunner();
        final Map<String, String[]> inputParameters = new HashMap<>();

        inputParameters.put("SUB", new String[]{"[SCI,10]"});
        inputParameters.put("file", new String[]{"/my/file/1", "/my/file/2"});

        final Job testJob = new Job();
        final List<Parameter> parameterList = new ArrayList<>();
        inputParameters.forEach((k, v) -> Arrays.stream(v).forEach(paramValue -> parameterList.add(
                new Parameter(k, paramValue))));
        testJob.setParameterList(parameterList);
        testSubject.setJob(testJob);
        try {
            testSubject.verifyArguments();
            Assert.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            Assert.assertEquals("Wrong message: " + message, "Only one file parameter can be provided.", message);
        }
    }

    @Test
    public void testVerifyArgumentsComplete() {
        final DataJobRunner testSubject = new DataJobRunner();
        final Map<String, String[]> inputParameters = new HashMap<>();

        inputParameters.put("SUB", new String[]{"[SCI,10]"});

        // Same file twice should be reduced to the same file.
        inputParameters.put("file", new String[]{"/my/file/1", "/my/file/1"});

        final Job testJob = new Job();
        final List<Parameter> parameterList = new ArrayList<>();
        inputParameters.forEach((k, v) -> Arrays.stream(v).forEach(paramValue -> parameterList.add(
                new Parameter(k, paramValue))));
        testJob.setParameterList(parameterList);
        testSubject.setJob(testJob);
        testSubject.verifyArguments();
    }

    @Test
    public void testDoActionHeaders() throws Exception {
        final FitsOperations mockFitsOperations = mock(FitsOperations.class);
        final RandomAccessDataObject mockRandomAccessDataObject = mock(RandomAccessDataObject.class);
        final WebServiceMetaData mockWebServiceMetaData = mock(WebServiceMetaData.class);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final List<Header> headers = new ArrayList<>(3);

        headers.add(ImageHDU.manufactureHeader(new ImageData(new int[20][100])));
        headers.add(ImageHDU.manufactureHeader(new ImageData(new int[120][1100])));
        headers.add(ImageHDU.manufactureHeader(new ImageData(new int[220][2100])));

        final DataJobRunner testSubject = new DataJobRunner() {
            @Override
            FitsOperations getOperator(RandomAccessDataObject randomAccessDataObject) {
                return mockFitsOperations;
            }

            @Override
            RandomAccessDataObject getRandomAccessDataObject() {
                return mockRandomAccessDataObject;
            }
        };

        final Map<String, String[]> inputParameters = new HashMap<>();

        inputParameters.put(SodaParamValidator.META, new String[]{Boolean.toString(true)});

        // Same file twice should be reduced to the same file.
        inputParameters.put("file", new String[]{"/archive/hst/hst-mef.fits"});

        final TestServletOutputStream testServletOutputStream = new TestServletOutputStream();
        when(mockResponse.getOutputStream()).thenReturn(testServletOutputStream);

        when(mockFitsOperations.getHeaders()).thenReturn(headers);

        when(mockWebServiceMetaData.getTitle()).thenReturn("Test Application");
        when(mockWebServiceMetaData.getVersion()).thenReturn("3.4.0");

        final Job testJob = new Job();
        final List<Parameter> parameterList = new ArrayList<>();
        inputParameters.forEach((k, v) -> Arrays.stream(v).forEach(paramValue -> parameterList.add(
                new Parameter(k, paramValue))));
        testJob.setParameterList(parameterList);
        testSubject.setJob(testJob);

        final SyncOutput syncOutput = new SyncOutput(mockResponse);
        testSubject.setSyncOutput(syncOutput);
        testSubject.run();

        verify(mockResponse).getOutputStream();
    }

    static final class TestServletOutputStream extends ServletOutputStream {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        @Override
        public void write(int b) {
            byteArrayOutputStream.write(b);
        }
    }
}
