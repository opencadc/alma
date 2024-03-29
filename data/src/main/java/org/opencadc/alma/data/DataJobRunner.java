/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.io.ByteCountOutputStream;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.Parameter;
import ca.nrc.cadc.uws.ParameterUtil;
import ca.nrc.cadc.uws.server.JobRunner;
import ca.nrc.cadc.uws.server.JobUpdater;
import nom.tam.util.RandomAccessFileIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.alma.logging.web.ByteCountingSyncOutput;
import org.opencadc.fits.FitsOperations;
import org.opencadc.fits.NoOverlapException;
import org.opencadc.fits.RandomAccessStorageObject;
import org.opencadc.soda.SodaParamValidator;
import org.opencadc.soda.server.Cutout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataJobRunner implements JobRunner {
    private static final Logger LOGGER = LogManager.getLogger(DataJobRunner.class);
    private static final SodaParamValidator SODA_PARAM_VALIDATOR = new SodaParamValidator();

    protected SyncOutput syncOutput;
    protected Job job;

    @Override
    public void setJobUpdater(JobUpdater jobUpdater) {
        // Not supported
    }

    @Override
    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public void setSyncOutput(SyncOutput output) {
        if (output != null) {
            this.syncOutput = new ByteCountingSyncOutput(output);
        }
    }

    @Override
    public void run() {
        try {
            verifyArguments();

            // The caller will close this stream.
            final ByteCountOutputStream byteCountOutputStream = new ByteCountOutputStream(syncOutput.getOutputStream());
            write(byteCountOutputStream);
            byteCountOutputStream.flush();
        } catch (NoOverlapException | ResourceNotFoundException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        } catch (IOException e) {
            // error on client write
            String msg = "write output error";
            LOGGER.debug(msg, e);
            if (e.getMessage() != null) {
                msg += ": " + e.getMessage();
            }
            throw new IllegalArgumentException(msg, e);
        }
    }

    void throwUsageError() {
        final String requestURI = this.job.getRequestPath();
        throw new IllegalArgumentException(
                String.format("""

                                      Usage:\s
                                      %s?file=[ABSOLUTE_FILE_PATH]
                                      OR for a sub-region:
                                      %s?file=[ABSOLUTE_FILE_PATH]&[SODA_PARAM]=[CUTOUT_SPEC]
                                      OR for headers only:
                                      %s?file=[ABSOLUTE_FILE_PATH]&headers=true

                                      See https://www.ivoa.net/documents/SODA/20170517/REC-SODA-1.0.html#tth_sEc3.3 for SODA_PARAM values.""",
                              requestURI,
                              requestURI,
                              requestURI));
    }

    final void verifyArguments() {
        final Set<String> requestedFilePaths =
                new HashSet<>(ParameterUtil.findParameterValues("file", job.getParameterList()));
        final boolean hasFile = !requestedFilePaths.isEmpty();

        if (!hasFile) {
            throwUsageError();
        } else if (requestedFilePaths.size() > 1) {
            throw new IllegalArgumentException("Only one file parameter can be provided.");
        }
    }

    protected File getFile() {
        final String requestedFilePath = ParameterUtil.findParameterValue("file", job.getParameterList());
        LOGGER.debug("Searching for file " + requestedFilePath);

        return new File(requestedFilePath);
    }

    void write(final ByteCountOutputStream byteCountOutputStream) throws ResourceNotFoundException, IOException,
                                                                         NoOverlapException {
        final SodaCutout sodaCutout = new SodaCutout();
        final RandomAccessFileIO randomAccessFileIO = getRandomAccessDataObject();

        if (sodaCutout.hasNoOperations()) {
            LOGGER.debug("FitsOperations.empty: START");
            // If nothing is provided, then simply write the entire file out.
            final int bufferSize = 64 * 1024; // 64KB buffer has proven a good performance size.
            final byte[] buffer = new byte[bufferSize];
            int byteCount;
            while ((byteCount = randomAccessFileIO.read(buffer)) >= 0) {
                byteCountOutputStream.write(buffer, 0, byteCount);
            }
            LOGGER.debug("FitsOperations.empty: OK");
        } else {
            final List<String> conflicts = sodaCutout.getConflicts();
            if (!conflicts.isEmpty()) {
                throw new IllegalArgumentException("Conflicting SODA parameters found: " + conflicts);
            }

            final FitsOperations fitsOperations = getOperator(randomAccessFileIO);

            if (sodaCutout.hasSUB() || sodaCutout.hasWCS()) {
                final Cutout cutout = new Cutout();

                if (sodaCutout.hasSUB()) {
                    LOGGER.debug("SUB supplied");
                    cutout.pixelCutouts = SODA_PARAM_VALIDATOR.validateSUB(
                            Collections.singletonMap(SodaParamValidator.SUB, sodaCutout.requestedSubs));
                } else if (sodaCutout.hasWCS()) {
                    LOGGER.debug("WCS supplied.");

                    if (sodaCutout.hasCIRCLE()) {
                        LOGGER.debug("CIRCLE supplied.");
                        final List<Circle> validCircles = SODA_PARAM_VALIDATOR.validateCircle(
                                Collections.singletonMap(SodaParamValidator.CIRCLE, sodaCutout.requestedCircles));

                        cutout.pos = assertSingleWCS(SodaParamValidator.CIRCLE, validCircles);
                    }

                    if (sodaCutout.hasPOLYGON()) {
                        LOGGER.debug("POLYGON supplied.");
                        final List<Polygon> validPolygons = SODA_PARAM_VALIDATOR.validatePolygon(
                                Collections.singletonMap(SodaParamValidator.POLYGON, sodaCutout.requestedPolygons));

                        cutout.pos = assertSingleWCS(SodaParamValidator.POLYGON, validPolygons);
                    }

                    if (sodaCutout.hasPOS()) {
                        LOGGER.debug("POS supplied.");
                        final List<Shape> validShapes = SODA_PARAM_VALIDATOR.validatePOS(
                                Collections.singletonMap(SodaParamValidator.POS, sodaCutout.requestedPOSs));

                        cutout.pos = assertSingleWCS(SodaParamValidator.POS, validShapes);
                    }

                    if (sodaCutout.hasBAND()) {
                        LOGGER.debug("BAND supplied.");
                        final List<Interval> validBandIntervals = SODA_PARAM_VALIDATOR.validateBAND(
                                Collections.singletonMap(SodaParamValidator.BAND, sodaCutout.requestedBands));

                        cutout.band = assertSingleWCS(SodaParamValidator.BAND, validBandIntervals);
                    }

                    if (sodaCutout.hasTIME()) {
                        LOGGER.debug("TIME supplied.");
                        final List<Interval> validTimeIntervals = SODA_PARAM_VALIDATOR.validateTIME(
                                Collections.singletonMap(SodaParamValidator.TIME, sodaCutout.requestedTimes));

                        cutout.time = assertSingleWCS(SodaParamValidator.TIME, validTimeIntervals);
                    }

                    if (sodaCutout.hasPOL()) {
                        LOGGER.debug("POL supplied.");
                        cutout.pol = SODA_PARAM_VALIDATOR.validatePOL(
                                Collections.singletonMap(SodaParamValidator.POL, sodaCutout.requestedPOLs));

                        if (cutout.pol.size() != sodaCutout.requestedPOLs.size()) {
                            LOGGER.debug("Accepted " + cutout.pol + " valid POL states but "
                                         + sodaCutout.requestedPOLs + " was requested.");
                        }
                    }
                }

                fitsOperations.cutoutToStream(cutout, byteCountOutputStream);
            } else if (sodaCutout.isMETA()) {
                LOGGER.debug("META supplied");
                fitsOperations.headersToStream(byteCountOutputStream);
            } else {
                throw new IllegalArgumentException("BUG: unhandled SODA parameters");
            }
        }
    }

    private <T> T assertSingleWCS(final String key, final List<T> wcsValues) {
        if (wcsValues.isEmpty()) {
            LOGGER.debug("No valid " + key + "s found.");
            return null;
        } else if (wcsValues.size() > 1) {
            throw new IllegalArgumentException("More than one " + key + " provided.");
        } else {
            return wcsValues.get(0);
        }
    }

    /**
     * Allow tests to override.
     *
     * @return FitsOperations instance.  Never null.
     */
    FitsOperations getOperator(final RandomAccessFileIO randomAccessDataObject) {
        return new FitsOperations(randomAccessDataObject);
    }

    RandomAccessFileIO getRandomAccessDataObject() throws ResourceNotFoundException {
        try {
            return new RandomAccessStorageObject(getFile(), "r");
        } catch (FileNotFoundException fileNotFoundException) {
            throw new ResourceNotFoundException(fileNotFoundException.getMessage());
        }
    }

    /**
     * Simple encompassing class to handle cutout checks.
     */
    private final class SodaCutout {
        final List<String> requestedSubs;
        final List<String> requestedCircles;
        final List<String> requestedPolygons;
        final List<String> requestedPOSs;
        final List<String> requestedBands;
        final List<String> requestedTimes;
        final List<String> requestedPOLs;
        final boolean requestedMeta;

        public SodaCutout() {
            final List<Parameter> parameters = job.getParameterList();
            this.requestedSubs = ParameterUtil.findParameterValues(SodaParamValidator.SUB, parameters);
            this.requestedCircles = ParameterUtil.findParameterValues(SodaParamValidator.CIRCLE, parameters);
            this.requestedPolygons = ParameterUtil.findParameterValues(SodaParamValidator.POLYGON, parameters);
            this.requestedPOSs = ParameterUtil.findParameterValues(SodaParamValidator.POS, parameters);
            this.requestedBands = ParameterUtil.findParameterValues(SodaParamValidator.BAND, parameters);
            this.requestedTimes = ParameterUtil.findParameterValues(SodaParamValidator.TIME, parameters);
            this.requestedPOLs = ParameterUtil.findParameterValues(SodaParamValidator.POL, parameters);
            this.requestedMeta = "true".equals(ParameterUtil.findParameterValue(SodaParamValidator.META, parameters));
        }

        boolean hasSUB() {
            return requestedSubs != null && !requestedSubs.isEmpty();
        }

        public boolean hasWCS() {
            return hasCIRCLE() || hasPOLYGON() || hasPOS() || hasBAND() || hasTIME() || hasPOL();
        }

        public boolean hasPOL() {
            return this.requestedPOLs != null && !this.requestedPOLs.isEmpty();
        }

        public boolean hasPOS() {
            return this.requestedPOSs != null && !this.requestedPOSs.isEmpty();
        }

        public boolean hasCIRCLE() {
            return this.requestedCircles != null && !this.requestedCircles.isEmpty();
        }

        public boolean hasPOLYGON() {
            return this.requestedPolygons != null && !this.requestedPolygons.isEmpty();
        }

        public boolean hasBAND() {
            return this.requestedBands != null && !this.requestedBands.isEmpty();
        }

        public boolean hasTIME() {
            return this.requestedTimes != null && !this.requestedTimes.isEmpty();
        }

        boolean isMETA() {
            return requestedMeta;
        }

        boolean hasNoOperations() {
            return !hasSUB() && !isMETA() && !hasWCS();
        }

        /**
         * Obtain a list of conflicting parameters, if any.  Conflicting parameters include combining the META parameter
         * with any cutout, as well as combining the SUB parameter with any WCS (CIRCLE, POLYGON, etc.) cutout.
         *
         * @return  List of SODA Parameter names, or empty List.  Never null.
         */
        List<String> getConflicts() {
            final List<String> conflicts = new ArrayList<>();
            if (isMETA() && hasSUB()) {
                conflicts.add(SodaParamValidator.META);
                conflicts.add(SodaParamValidator.SUB);
            }

            final List<String> shapeConflicts = new ArrayList<>();
            if (hasCIRCLE()) {
                shapeConflicts.add(SodaParamValidator.CIRCLE);
            }

            if (hasPOLYGON()) {
                shapeConflicts.add(SodaParamValidator.POLYGON);
            }

            if (hasPOS()) {
                shapeConflicts.add(SodaParamValidator.POS);
            }

            // Only one spatial axis cutout is permitted.
            if (shapeConflicts.size() > 1) {
                conflicts.addAll(shapeConflicts);
            }

            return conflicts;
        }
    }
}
