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

package org.opencadc.conesearch;

import ca.nrc.cadc.dali.ParamExtractor;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.net.HttpPost;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.uws.ErrorSummary;
import ca.nrc.cadc.uws.ErrorType;
import ca.nrc.cadc.uws.ExecutionPhase;
import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.Result;
import ca.nrc.cadc.uws.server.JobRunner;
import ca.nrc.cadc.uws.server.JobUpdater;
import ca.nrc.cadc.uws.util.JobLogInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.conesearch.config.ConeSearchConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ConeSearchRunner implements JobRunner {
    private final static Logger LOGGER = LogManager.getLogger(ConeSearchRunner.class);

    private Job job;
    private JobUpdater jobUpdater;
    private SyncOutput syncOutput;
    private JobLogInfo logInfo;


    @Override
    public void setJobUpdater(JobUpdater jobUpdater) {
        this.jobUpdater = jobUpdater;
    }

    @Override
    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public void setSyncOutput(SyncOutput output) {
        this.syncOutput = output;
    }

    @Override
    public void run() {
        LOGGER.debug("RUN ConeSearchRunner: " + job.ownerSubject);

        this.logInfo = new JobLogInfo(job);

        final String startMessage = logInfo.start();
        LOGGER.info(startMessage);

        final long t1 = System.currentTimeMillis();
        doIt();
        logInfo.setElapsedTime(System.currentTimeMillis() - t1);

        LOGGER.info(logInfo.end());
    }

    private void doIt() {
        final ConeSearchConfig coneSearchConfig = new ConeSearchConfig();
        URL url = null;
        try {
            final ExecutionPhase ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.QUEUED, ExecutionPhase.EXECUTING,
                                                          new Date());
            if (!ExecutionPhase.EXECUTING.equals(ep)) {
                final String message = job.getID() + ": QUEUED -> EXECUTING [FAILED] -- DONE";
                logInfo.setSuccess(false);
                logInfo.setMessage(message);
                return;
            }
            LOGGER.debug(job.getID() + ": QUEUED -> EXECUTING [OK]");

            final String[] pathElements = job.getRequestPath().split("/");
            final ParamExtractor parameterExtractor = new ParamExtractor(
                    Arrays.asList(ConeParameterValidator.RA_PARAM, ConeParameterValidator.DEC_PARAM,
                                  ConeParameterValidator.SR_PARAM, ConeParameterValidator.RESPONSEFORMAT,
                                  ConeParameterValidator.VERB_PARAM, ConeParameterValidator.MAXREC));
            final TAPQueryGenerator tapQueryGenerator =
                    new TAPQueryGenerator(pathElements[pathElements.length - 1],
                                          coneSearchConfig.getPositionColumnName(),
                                          coneSearchConfig.getLowVerbositySelectList(),
                                          coneSearchConfig.getMidVerbositySelectList(),
                                          coneSearchConfig.getHighVerbositySelectList(),
                                          parameterExtractor.getParameters(job.getParameterList()));
            final Map<String, Object> parameters = tapQueryGenerator.getParameterMap();

            // the implementation assumes that the /tap/sync service follows the
            // POST-redirect-GET (PrG) pattern; cadcUWS SyncServlet does
            final URL tapSyncURL = new URL(coneSearchConfig.getTapBaseURL().toExternalForm() + "/sync");

            // POST ADQL query to TAP but do not follow redirect to execute it.
            final HttpPost post = new HttpPost(tapSyncURL, parameters, false);
            post.run();

            // Create an ErrorSummary and throw RuntimeException if the POST failed.
            if (post.getThrowable() != null) {
                throw new RuntimeException("sync TAP query (" + tapSyncURL.toExternalForm()
                                           + ") failed because "
                                           + post.getThrowable().getMessage());
            }

            // redirect the caller to the G part of the /tap/sync PrG pattern
            url = post.getRedirectURL();
            LOGGER.debug("redirectURL " + url);
            syncOutput.setCode(303);
            syncOutput.setHeader("Location", url.toExternalForm());

            // Mark the Job as completed adding the URL to the query results.
            final List<Result> results = new ArrayList<>();
            results.add(new Result("result", new URI(url.toExternalForm())));
            jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, ExecutionPhase.COMPLETED, results, new Date());
        } catch (Throwable t) {
            logInfo.setSuccess(false);
            logInfo.setMessage(t.getMessage());
            LOGGER.debug("FAIL", t);

            try {
                final VOTableWriter writer = new VOTableWriter();
                syncOutput.setHeader("Content-Type", VOTableWriter.CONTENT_TYPE);
                // TODO: chose suitable response code here (assume bad input for now)
                syncOutput.setCode(400);
                writer.write(t, syncOutput.getOutputStream());
            } catch (IOException ioe) {
                LOGGER.debug("Error writing error document " + ioe.getMessage());
            }

            final ErrorSummary errorSummary = new ErrorSummary(t.getMessage(), ErrorType.FATAL, url);
            try {
                jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, ExecutionPhase.ERROR,
                                    errorSummary, new Date());
            } catch (Throwable oops) {
                LOGGER.debug("failed to set final error status after " + t, oops);
            }
        }
    }
}
