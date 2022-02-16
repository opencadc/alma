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

package org.opencadc.alma.data.fits;

import ca.nrc.cadc.log.WebServiceLogInfo;
import ca.nrc.cadc.rest.SyncInput;
import ca.nrc.cadc.rest.SyncOutput;
import nom.tam.fits.Header;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.RandomAccessDataObject;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.alma.logging.LoggingEvent;
import org.opencadc.alma.logging.web.WebServiceMetaData;
import org.opencadc.fits.FitsOperations;
import org.opencadc.soda.SodaParamValidator;

import static org.mockito.Mockito.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FITSActionTest {
    final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    @Test
    public void testVerifyArgumentsMissingFile() throws Exception {
        final FITSAction testSubject = new FITSAction();

        final Map<String, String[]> inputParameters = new HashMap<>();
        inputParameters.put("cutout", new String[]{"[0][80:500]", "[10]"});

        when(mockRequest.getMethod()).thenReturn("GET");
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        }
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://almascience.org/data/fits"));
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(inputParameters.keySet()));

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            when(mockRequest.getParameterValues(entry.getKey())).thenReturn(entry.getValue());
        }

        final SyncInput syncInput = new SyncInput(mockRequest, null);
        syncInput.init();
        testSubject.setSyncInput(syncInput);
        try {
            testSubject.verifyArguments();
            Assert.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            Assert.assertTrue("Wrong message: " + message, message.contains("Usage"));
        }

        verify(mockRequest).getMethod();
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            verify(mockRequest).getHeaderNames();
        }
        verify(mockRequest).getRequestURL();
        verify(mockRequest).getParameterNames();

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            verify(mockRequest).getParameterValues(entry.getKey());
        }
    }

    @Test
    public void testVerifyArgumentsMultipleFiles() throws Exception {
        final FITSAction testSubject = new FITSAction();
        final Map<String, String[]> inputParameters = new HashMap<>();

        inputParameters.put("SUB", new String[]{"[SCI,10]"});
        inputParameters.put("file", new String[]{"/my/file/1", "/my/file/2"});

        when(mockRequest.getMethod()).thenReturn("GET");
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        }
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(inputParameters.keySet()));

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            when(mockRequest.getParameterValues(entry.getKey())).thenReturn(entry.getValue());
        }

        final SyncInput syncInput = new SyncInput(mockRequest, null);
        syncInput.init();
        testSubject.setSyncInput(syncInput);
        try {
            testSubject.verifyArguments();
            Assert.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            Assert.assertEquals("Wrong message: " + message, "Only one file parameter can be provided.", message);
        }

        verify(mockRequest).getMethod();
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            verify(mockRequest).getHeaderNames();
        }
        verify(mockRequest).getParameterNames();

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            verify(mockRequest).getParameterValues(entry.getKey());
        }
    }

    @Test
    public void testVerifyArgumentsComplete() throws Exception {
        final FITSAction testSubject = new FITSAction();
        final Map<String, String[]> inputParameters = new HashMap<>();

        inputParameters.put("SUB", new String[]{"[SCI,10]"});

        // Same file twice should be reduced to the same file.
        inputParameters.put("file", new String[]{"/my/file/1", "/my/file/1"});

        when(mockRequest.getMethod()).thenReturn("GET");
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        }
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(inputParameters.keySet()));

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            when(mockRequest.getParameterValues(entry.getKey())).thenReturn(entry.getValue());
        }

        final SyncInput syncInput = new SyncInput(mockRequest, null);
        syncInput.init();

        testSubject.setSyncInput(syncInput);
        testSubject.verifyArguments();

        verify(mockRequest).getMethod();
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            verify(mockRequest).getHeaderNames();
        }
        verify(mockRequest).getParameterNames();

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            verify(mockRequest).getParameterValues(entry.getKey());
        }
    }

    @Test
    public void testDoActionHeaders() throws Exception {
        final FitsOperations mockFitsOperations = mock(FitsOperations.class);
        final RandomAccessDataObject mockRandomAccessDataObject = mock(RandomAccessDataObject.class);
        final WebServiceMetaData mockWebServiceMetaData = mock(WebServiceMetaData.class);
        final URL metaDataURL = new URL("file:/path/to/file");
        final WebServiceLogInfo mockLogInfo = mock(WebServiceLogInfo.class);

        final List<Header> headers = new ArrayList<>(3);

        headers.add(ImageHDU.manufactureHeader(new ImageData(new int[20][100])));
        headers.add(ImageHDU.manufactureHeader(new ImageData(new int[120][1100])));
        headers.add(ImageHDU.manufactureHeader(new ImageData(new int[220][2100])));

        final FITSAction testSubject = new FITSAction() {
            @Override
            FitsOperations getOperator(RandomAccessDataObject randomAccessDataObject) {
                return mockFitsOperations;
            }

            @Override
            RandomAccessDataObject getRandomAccessDataObject() {
                return mockRandomAccessDataObject;
            }

            @Override
            void sendToLogger(LoggingEvent loggingEvent) {
                // Do nothing.
            }

            @Override
            WebServiceMetaData getWebServiceMetaData() throws IOException {
                return mockWebServiceMetaData;
            }
        };

        testSubject.setLogInfo(mockLogInfo);

        final Map<String, String[]> inputParameters = new HashMap<>();

        inputParameters.put(SodaParamValidator.META, new String[]{Boolean.toString(true)});

        // Same file twice should be reduced to the same file.
        inputParameters.put("file", new String[]{"/archive/hst/hst-mef.fits"});

        when(mockRequest.getMethod()).thenReturn("GET");
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        }

        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://almasite.com/test/app"));
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(inputParameters.keySet()));

        for (final Map.Entry<String, String[]> entry : inputParameters.entrySet()) {
            when(mockRequest.getParameterValues(entry.getKey())).thenReturn(entry.getValue());
        }

        final TestServletOutputStream testServletOutputStream = new TestServletOutputStream();
        when(mockResponse.getOutputStream()).thenReturn(testServletOutputStream);

        when(mockFitsOperations.getHeaders()).thenReturn(headers);

        when(mockWebServiceMetaData.getTitle()).thenReturn("Test Application");
        when(mockWebServiceMetaData.getVersion()).thenReturn("3.4.0");

        final SyncInput syncInput = new SyncInput(mockRequest, null);
        syncInput.init();

        final SyncOutput syncOutput = new SyncOutput(mockResponse);
        testSubject.setSyncInput(syncInput);
        testSubject.setSyncOutput(syncOutput);
        testSubject.doAction();

        verify(mockRequest).getMethod();
        if (Logger.getLogger(SyncInput.class).isDebugEnabled()) {
            verify(mockRequest).getHeaderNames();
        }

        verify(mockRequest).getRequestURL();
        verify(mockRequest).getParameterNames();
        verify(mockResponse).getOutputStream();

        // Nothing actually counted.
        verify(mockLogInfo).setBytes(0L);

        verify(mockWebServiceMetaData).getTitle();
        verify(mockWebServiceMetaData).getVersion();
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
