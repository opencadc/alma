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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.io.ByteCountOutputStream;
import ca.nrc.cadc.io.WriteException;
import ca.nrc.cadc.net.HttpTransfer;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.util.StringUtil;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.opencadc.alma.data.BaseAction;
import org.opencadc.alma.data.CutoutFileNameFormat;
import org.opencadc.fits.FitsOperations;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.SodaParamValidator;
import org.opencadc.soda.server.Cutout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FITSAction extends BaseAction {
    private final Logger LOGGER = LogManager.getLogger(FITSAction.class);
    private static final SodaParamValidator SODA_PARAM_VALIDATOR = new SodaParamValidator();
    private static final String CUTOUT_PARAMETER_KEY = SodaParamValidator.SUB;
    private static final String CONTENT_DISPOSITION = "Content-Disposition";


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

        try {
            // The caller will close this stream.
            final ByteCountOutputStream byteCountOutputStream = new ByteCountOutputStream(syncOutput.getOutputStream());
            write(byteCountOutputStream);
            super.logInfo.setBytes(byteCountOutputStream.getByteCount());

            byteCountOutputStream.flush();
        } catch (WriteException e) {
            // error on client write
            String msg = "write output error";
            LOGGER.debug(msg, e);
            if (e.getMessage() != null) {
                msg += ": " + e.getMessage();
            }
            throw new IllegalArgumentException(msg, e);
        }
    }

    void write(final ByteCountOutputStream byteCountOutputStream) throws ResourceNotFoundException, IOException,
                                                                         FitsException {
        final String headerRequest = syncInput.getParameter("headers");
        final Map<String, List<String>> sodaParameterMap = getSodaParameters();

        if (StringUtil.hasText(headerRequest) && Boolean.parseBoolean(headerRequest)) {
            LOGGER.debug("FitsOperations.headers: START");
            try (final RandomAccessDataObject randomAccessDataObject = getRandomAccessDataObject()) {
                final ArrayDataOutput output = new BufferedDataOutputStream(byteCountOutputStream);
                final FitsOperations fitsOperations = getOperator(randomAccessDataObject);
                for (final Header header : fitsOperations.getHeaders()) {
                    header.write(output);
                }

                output.flush();
                LOGGER.debug("FitsOperations.headers: OK");
            }
        } else if (sodaParameterMap.isEmpty()) {
            LOGGER.debug("FitsOperations.empty: START");
            // If nothing is provided, then simply write the entire file out.
            try (final RandomAccessDataObject randomAccessDataObject = getRandomAccessDataObject()) {
                final int bufferSize = 64 * 1024; // 64KB buffer has proven a good performance size.
                final byte[] buffer = new byte[bufferSize];
                int byteCount;
                while ((byteCount = randomAccessDataObject.read(buffer)) >= 0) {
                    byteCountOutputStream.write(buffer, 0, byteCount);
                }

                byteCountOutputStream.flush();
            }
            LOGGER.debug("FitsOperations.empty: OK");
        } else {
            final CutoutFileNameFormat cutoutFileNameFormat = new CutoutFileNameFormat(getFile().getName());
            final Cutout cutout = new Cutout();

            final List<ExtensionSlice> slices = SODA_PARAM_VALIDATOR.validateSUB(sodaParameterMap);
            if (!slices.isEmpty()) {
                cutout.pixelCutouts = slices;
            }

            final List<Circle> circles = SODA_PARAM_VALIDATOR.validateCircle(sodaParameterMap);
            if (!circles.isEmpty()) {
                cutout.pos = circles.get(0);
            }

            final List<Polygon> polygons = SODA_PARAM_VALIDATOR.validatePolygon(sodaParameterMap);
            if (!polygons.isEmpty()) {
                cutout.pos = polygons.get(0);
            }

            final List<Interval> bands = SODA_PARAM_VALIDATOR.validateBAND(sodaParameterMap);
            if (!bands.isEmpty()) {
                cutout.band = bands.get(0);
            }

            final List<Interval> times = SODA_PARAM_VALIDATOR.validateTIME(sodaParameterMap);
            if (!times.isEmpty()) {
                cutout.time = times.get(0);
            }

            final List<String> polarizations = SODA_PARAM_VALIDATOR.validatePOL(sodaParameterMap);
            if (!polarizations.isEmpty()) {
                cutout.pol = polarizations;
            }

            if (!isEmpty(cutout)) {
                syncOutput.setHeader(CONTENT_DISPOSITION,
                                     "inline; filename=\"" + cutoutFileNameFormat.format(cutout) + "\"");
                syncOutput.setHeader(HttpTransfer.CONTENT_TYPE, "application/fits");

                try (final RandomAccessDataObject randomAccessDataObject = getRandomAccessDataObject()) {
                    final FitsOperations fitsOperations = getOperator(randomAccessDataObject);
                    fitsOperations.cutoutToStream(cutout, byteCountOutputStream);
                }
            } else {
                throw new IllegalArgumentException("No usabel cutouts supplied.");
            }
        }
    }

    private boolean isEmpty(final Cutout cutout) {
        return cutout.pos == null && cutout.band == null && cutout.time == null
               && (cutout.pol == null || cutout.pol.isEmpty()) && cutout.custom == null
               && cutout.customAxis == null && (cutout.pixelCutouts == null || cutout.pixelCutouts.isEmpty());
    }

    private Map<String, List<String>> getSodaParameters() {
        final Set<String> parameterNames = syncInput.getParameterNames();
        final Map<String, List<String>> paramMap = new HashMap<>();

        if (parameterNames.contains(SodaParamValidator.SUB)) {
            paramMap.put(SodaParamValidator.SUB, syncInput.getParameters(SodaParamValidator.SUB));
        } else if (parameterNames.contains(SodaParamValidator.CIRCLE)) {
            paramMap.put(SodaParamValidator.CIRCLE, syncInput.getParameters(SodaParamValidator.CIRCLE));
        } else if (parameterNames.contains(SodaParamValidator.POLYGON)) {
            paramMap.put(SodaParamValidator.POLYGON, syncInput.getParameters(SodaParamValidator.POLYGON));
        } else if (parameterNames.contains(SodaParamValidator.POS)) {
            final List<String> posParamValues = syncInput.getParameters(SodaParamValidator.POS);
            posParamValues.forEach(s -> {
                if (s.startsWith(SodaParamValidator.CIRCLE)) {
                    paramMap.put(SodaParamValidator.CIRCLE,
                                 Collections.singletonList(s.substring(SodaParamValidator.CIRCLE.length()).trim()));
                } else if (s.startsWith(SodaParamValidator.POLYGON)) {
                    paramMap.put(SodaParamValidator.POLYGON,
                                 Collections.singletonList(s.substring(SodaParamValidator.POLYGON.length()).trim()));
                } else if (s.startsWith("RANGE")) {
                    paramMap.put("RANGE", Collections.singletonList(s.substring("RANGE".length()).trim()));
                } else {
                    LOGGER.error("Unsupported POS: " + s);
                }
            });
        } else if (parameterNames.contains(SodaParamValidator.BAND)) {
            paramMap.put(SodaParamValidator.BAND, syncInput.getParameters(SodaParamValidator.BAND));
        } else if (parameterNames.contains(SodaParamValidator.TIME)) {
            paramMap.put(SodaParamValidator.TIME, syncInput.getParameters(SodaParamValidator.TIME));
        } else if (parameterNames.contains(SodaParamValidator.POL)) {
            paramMap.put(SodaParamValidator.POL, syncInput.getParameters(SodaParamValidator.POL));
        }

        return paramMap;
    }

    /**
     * Allow tests to override.
     *
     * @return FitsOperations instance.  Never null.
     */
    FitsOperations getOperator(final RandomAccessDataObject randomAccessDataObject) {
        return new FitsOperations(randomAccessDataObject);
    }

    RandomAccessDataObject getRandomAccessDataObject() throws ResourceNotFoundException {
        try {
            return new RandomAccessFileExt(getFile(), "r");
        } catch (FileNotFoundException fileNotFoundException) {
            throw new ResourceNotFoundException(fileNotFoundException.getMessage());
        }
    }
}
