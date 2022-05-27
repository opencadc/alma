
/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2019.                            (c) 2019.
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

package org.opencadc.soda;


import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.ParamExtractor;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.log.WebServiceLogInfo;
import ca.nrc.cadc.net.HttpTransfer;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.uws.ErrorSummary;
import ca.nrc.cadc.uws.ErrorType;
import ca.nrc.cadc.uws.ExecutionPhase;
import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.Result;
import ca.nrc.cadc.uws.server.JobNotFoundException;
import ca.nrc.cadc.uws.server.JobPersistenceException;
import ca.nrc.cadc.uws.server.JobRunner;
import ca.nrc.cadc.uws.server.JobUpdater;
import ca.nrc.cadc.uws.util.JobLogInfo;
import org.apache.log4j.Logger;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.logging.web.ByteCountingSyncOutput;
import org.opencadc.soda.server.AlmaStreamingSodaPlugin;
import org.opencadc.soda.server.Cutout;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Job Runner implementation that relies on the StreamingSodaPlugin to write out the cutout rather than issuing a
 * redirect.
 */
public class AlmaSodaJobRunner implements JobRunner {
    private static final Logger LOGGER = Logger.getLogger(AlmaSodaJobRunner.class);
    private static final String CONTENT_DISPOSITION = "content-disposition";

    static final String RESULT_OK = "ok";

    private final SodaParamValidator sodaParamValidator = new SodaParamValidator();

    private final AlmaProperties almaProperties;
    private JobUpdater jobUpdater;
    protected SyncOutput syncOutput;
    protected Job job;
    private WebServiceLogInfo logInfo;


    public AlmaSodaJobRunner(final AlmaProperties sodaProperties) {
        this.almaProperties = sodaProperties;
    }

    public AlmaSodaJobRunner() {
        this(new AlmaProperties());
    }


    @Override
    public void setJobUpdater(final JobUpdater jobUpdater) {
        this.jobUpdater = jobUpdater;
    }

    @Override
    public void setJob(final Job job) {
        this.job = job;
    }

    /**
     * Set the job runner to do synchronous output to the specified output.
     *
     * @param output The SyncOutput instance
     */
    @Override
    public void setSyncOutput(final SyncOutput output) {
        if (output != null) {
            this.syncOutput = new ByteCountingSyncOutput(output);
        }
    }

    /**
     * Execute the job.
     */
    @Override
    public void run() {
        logInfo = new JobLogInfo(job);
        LOGGER.info(logInfo.start());
        long start = System.currentTimeMillis();

        try {
            doit();
        } catch (Exception ex) {
            LOGGER.error("unexpected exception", ex);
        }

        logInfo.setElapsedTime(System.currentTimeMillis() - start);
        LOGGER.info(logInfo.end());
    }

    void doit() throws IOException {
        try {
            // phase->EXECUTING
            ExecutionPhase ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.QUEUED, ExecutionPhase.EXECUTING,
                                                    new Date());
            if (!ExecutionPhase.EXECUTING.equals(ep)) {
                ep = jobUpdater.getPhase(job.getID());
                LOGGER.debug(job.getID() + ": QUEUED -> EXECUTING [FAILED] -- phase is " + ep);
                logInfo.setSuccess(false);
                logInfo.setMessage("Could not set job phase to EXECUTING.");
                return;
            }
            LOGGER.debug(job.getID() + ": QUEUED -> EXECUTING [OK]");

            // validate params
            final List<String> parameterNames = SodaParamValidator.SODA_PARAMS;
            final ParamExtractor pex = new ParamExtractor(parameterNames);
            final Map<String, List<String>> parameters = pex.getParameters(job.getParameterList());
            LOGGER.debug("soda params: " + SodaParamValidator.SODA_PARAMS.size() + " map params: " + parameters.size());
            final List<String> idList = sodaParamValidator.validateID(parameters);

            final List<Cutout> subCut = wrapSub(sodaParamValidator.validateSUB(parameters));
            final List<Cutout> posCut = wrapPos(sodaParamValidator.validateAllShapes(parameters));
            final List<Cutout> bandCut = wrapBand(sodaParamValidator.validateBAND(parameters));
            final List<Cutout> timeCut = wrapTime(sodaParamValidator.validateTIME(parameters));

            final Cutout polCut = new Cutout();
            polCut.pol = sodaParamValidator.validatePOL(parameters);

            final Map<String, List<String>> extraParams = pex.getExtraParameters(job.getParameterList());

            // check single-valued param limits
            final StringBuilder esb = new StringBuilder();
            if (idList.size() != 1) {
                esb.append("found ").append(idList.size()).append(" ID values, expected 1\n");
            }

            if (syncOutput != null) {
                if (posCut.size() > 1) {
                    esb.append("found ").append(posCut.size()).append(" POS/CIRCLE/POLY values, expected 0-1\n");
                }
                if (bandCut.size() > 1) {
                    esb.append("found ").append(bandCut.size()).append(" BAND values, expected 0-1\n");
                }
                if (timeCut.size() > 1) {
                    esb.append("found ").append(timeCut.size()).append(" TIME values, expected 0-1\n");
                }
            }

            if (esb.length() > 0) {
                throw new IllegalArgumentException(esb.toString());
            }

            final List<URI> ids = new ArrayList<>();
            final StringBuilder invalidIDErrorMessage = new StringBuilder();

            for (final String i : idList) {
                try {
                    ids.add(new URI(i));
                } catch (URISyntaxException ex) {
                    invalidIDErrorMessage.append("invalid URI: ").append(i).append("\n");
                }
            }

            if (invalidIDErrorMessage.length() > 0) {
                throw new IllegalArgumentException("invalid ID(s) found\n" + invalidIDErrorMessage);
            }

            // add single no-op element to make subsequent loops easier
            if (posCut.isEmpty()) {
                posCut.add(new Cutout());
            }

            if (bandCut.isEmpty()) {
                bandCut.add(new Cutout());
            }

            if (timeCut.isEmpty()) {
                timeCut.add(new Cutout());
            }

            if (subCut.isEmpty()) {
                subCut.add(new Cutout());
            }

            final AlmaStreamingSodaPlugin sodaPlugin = getSodaPlugin();
            final List<Result> jobResults = new ArrayList<>();
            int serialNum = 1;
            for (final URI id : ids) {
                final CutoutFileNameFormat cutoutFileNameFormat = new CutoutFileNameFormat(id.toASCIIString());

                // async mode: cartesian product of pos+band+time+custom
                // sync mode: each list has 1 entry (possibly no-op for that axis)
                for (final Cutout pos : posCut) {
                    for (final Cutout band : bandCut) {
                        for (final Cutout time : timeCut) {
                            for (final Cutout sub : subCut) {
                                // collect
                                final Cutout cut = new Cutout();
                                cut.pos = pos.pos;
                                cut.band = band.band;
                                cut.time = time.time;
                                cut.pol = polCut.pol;
                                cut.pixelCutouts = sub.pixelCutouts;
                                if (syncOutput != null) {
                                    syncOutput.setHeader(CONTENT_DISPOSITION, "inline; filename=\""
                                                                              + cutoutFileNameFormat.format(cut)
                                                                              + "\"");
                                    syncOutput.setHeader(HttpTransfer.CONTENT_TYPE.toLowerCase(Locale.ROOT),
                                                         "application/fits");
                                    sodaPlugin.write(id, cut, extraParams, this.syncOutput);
                                } else {
                                    final URL redirectURL = sodaPlugin.toURL(serialNum, id, cut, extraParams);
                                    jobResults.add(new Result(RESULT_OK + "-" + serialNum++,
                                                              redirectURL.toURI()));
                                }
                                LOGGER.debug("wrote cutout URL");
                            }
                        }
                    }
                }
            }

            // phase -> COMPLETED
            final ExecutionPhase fep = ExecutionPhase.COMPLETED;
            LOGGER.debug("setting ExecutionPhase = " + fep + " with results");
            jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, fep, jobResults, new Date());
        } catch (IllegalArgumentException | JobNotFoundException ex) {
            handleError(400, ex.getMessage(), null);
        } catch (IllegalStateException | JobPersistenceException ex) {
            handleError(500, ex.getMessage(), ex);
        } catch (TransientException ex) {
            handleError(503, ex.getMessage(), ex);
        } catch (Throwable unexpected) {
            handleError(500, "unexpected failure: " + unexpected.getMessage(), unexpected);
        }
    }

    static List<Cutout> wrapPos(List<Shape> inner) {
        final List<Cutout> ret = new ArrayList<>(inner.size());
        for (Shape i : inner) {
            Cutout c = new Cutout();
            c.pos = i;
            ret.add(c);
        }
        return ret;
    }

    static List<Cutout> wrapBand(List<Interval> inner) {
        final List<Cutout> ret = new ArrayList<>(inner.size());
        for (Interval<Double> i : inner) {
            final Cutout c = new Cutout();
            c.band = i;

            ret.add(c);
        }
        return ret;
    }

    static List<Cutout> wrapTime(List<Interval> inner) {
        final List<Cutout> ret = new ArrayList<>(inner.size());
        for (Interval<Double> i : inner) {
            final Cutout c = new Cutout();
            c.time = i;

            ret.add(c);
        }
        return ret;
    }

    static List<Cutout> wrapSub(final List<ExtensionSlice> inner) {
        final List<Cutout> returnValue = new ArrayList<>(1);
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = inner;

        returnValue.add(cutout);
        return returnValue;
    }

    private void handleError(int code, String msg, Throwable t) throws IOException {
        logInfo.setMessage(msg);
        if (t != null) {
            LOGGER.error("internal exception", t);
        }

        if (syncOutput != null) {
            syncOutput.setCode(code);
            syncOutput.setHeader(HttpTransfer.CONTENT_TYPE.toLowerCase(Locale.ROOT), "text/plain");
            PrintWriter w = new PrintWriter(syncOutput.getOutputStream());
            w.println(msg);
            w.flush();
        }

        final ExecutionPhase fep = ExecutionPhase.ERROR;
        final ErrorSummary es = new ErrorSummary(msg, ErrorType.FATAL);
        LOGGER.debug("setting ExecutionPhase = " + fep + " with results");
        try {
            jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, fep, es, new Date());
        } catch (JobNotFoundException | JobPersistenceException | TransientException ex) {
            LOGGER.error("oops", ex);
        }
    }

    public AlmaStreamingSodaPlugin getSodaPlugin() {
        return new AlmaStreamingSodaPlugin(createSodaQuery());
    }

    private SodaQuery createSodaQuery() {
        return new SodaQuery(almaProperties);
    }
}
