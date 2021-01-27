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

import ca.nrc.cadc.util.StringUtil;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.opencadc.alma.data.BaseAction;
import org.opencadc.fits.FitsOperations;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.SodaParamValidator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FITSAction extends BaseAction {
    private final Logger LOGGER = LogManager.getLogger(FITSAction.class);
    private static final SodaParamValidator SODA_PARAM_VALIDATOR = new SodaParamValidator();
    private static final String CUTOUT_PARAMETER_KEY = SodaParamValidator.SUB;


    void throwUsageError() {
        final String requestURI = syncInput.getRequestURI();
        throw new IllegalArgumentException(String.format("\nUsage: \n%s"
                                                         + "?file=[ABSOLUTE_FILE_PATH]\n"
                                                         + "OR for a sub-region:\n%s"
                                                         + "?file=[ABSOLUTE_FILE_PATH]&%s=[CUTOUT_SPEC]\n"
                                                         + "OR for headers only:\n%s"
                                                         + "?file=[ABSOLUTE_FILE_PATH]&headers=true", requestURI,
                                                         requestURI, SodaParamValidator.SUB, requestURI));
    }

    final void verifyArguments() {
        // Put them into a Set in case the same file was provided more than once, and can be reduced here.
        final Set<String> requestedFilePaths = new HashSet<>(getParametersNullSafe("file"));
        final List<String> cutoutSpec = getParametersNullSafe(CUTOUT_PARAMETER_KEY);

        final boolean hasCutout = !cutoutSpec.isEmpty();
        final boolean hasFile = !requestedFilePaths.isEmpty();
        final String headerRequest = syncInput.getParameter("headers");

        if (!hasFile) {
            throwUsageError();
        } else if (requestedFilePaths.size() > 1) {
            throw new IllegalArgumentException("Only one file parameter can be provided.");
        } else if (hasCutout) {
            LOGGER.debug("Cutting out " + Arrays.toString(cutoutSpec.toArray(new String[0])) + " from "
                         + requestedFilePaths);
        } else if (StringUtil.hasText(headerRequest)) {
            LOGGER.debug("Printing headers from " + requestedFilePaths);
        }
    }

    @Override
    public void doAction() throws Exception {
        verifyArguments();
        final String headerRequest = syncInput.getParameter("headers");
        final List<String> requestedSubs = syncInput.getParameters(CUTOUT_PARAMETER_KEY);

        if (StringUtil.hasText(headerRequest) && Boolean.parseBoolean(headerRequest)) {
            LOGGER.debug("FitsOperations.headers: START");
            try (final RandomAccessDataObject randomAccessDataObject = getRandomAccessDataObject();
                 final ArrayDataOutput output = new BufferedDataOutputStream(syncOutput.getOutputStream())) {
                final FitsOperations fitsOperations = getOperator(randomAccessDataObject);
                for (final Header header : fitsOperations.getHeaders()) {
                    header.write(output);
                }

                output.flush();
                LOGGER.debug("FitsOperations.headers: OK");
            }
        } else if (requestedSubs != null && !requestedSubs.isEmpty()) {
            LOGGER.debug("FitsOperations.cutout: START");

            // If any cutouts were requested
            final Map<String, List<String>> subMap = new HashMap<>();
            subMap.put(CUTOUT_PARAMETER_KEY, requestedSubs);
            final List<ExtensionSlice> slices = SODA_PARAM_VALIDATOR.validateSUB(subMap);

            try (final RandomAccessDataObject randomAccessDataObject = getRandomAccessDataObject()) {
                final FitsOperations fitsOperations = getOperator(randomAccessDataObject);
                fitsOperations.cutoutToStream(slices, syncOutput.getOutputStream());
            }
            LOGGER.debug("FitsOperations.cutout: OK");
        } else {
            // If nothing is provided, then simply write the entire file out.
            try (final InputStream inputStream = new FileInputStream(getFile())) {
                final OutputStream outputStream = syncOutput.getOutputStream();
                final byte[] buffer = new byte[64 * 1024]; // 64KB buffer has proven a good performance size.
                int byteCount;
                while ((byteCount = inputStream.read(buffer)) >= 0) {
                    outputStream.write(buffer, 0, byteCount);
                }

                outputStream.flush();
            }
        }
    }

    /**
     * Allow tests to override.
     *
     * @return FitsOperations instance.  Never null.
     */
    FitsOperations getOperator(final RandomAccessDataObject randomAccessDataObject) {
        return new FitsOperations(randomAccessDataObject);
    }

    RandomAccessDataObject getRandomAccessDataObject() throws FileNotFoundException {
        return new RandomAccessFileExt(getFile(), "r");
    }
}
