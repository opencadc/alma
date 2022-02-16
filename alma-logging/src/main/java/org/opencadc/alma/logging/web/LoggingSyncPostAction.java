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

package org.opencadc.alma.logging.web;

import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.io.ByteCountOutputStream;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.uws.web.SyncPostAction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.alma.logging.LoggingClient;
import org.opencadc.alma.logging.LoggingEvent;
import org.opencadc.alma.logging.LoggingEventKey;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Properties;


/**
 * POST request action that will remotely log the request if this POST request executes the Job.
 */
public class LoggingSyncPostAction extends SyncPostAction {
    private static final Logger LOGGER = LogManager.getLogger(LoggingSyncGetAction.class);
    private static final Logger REMOTE_LOGGER = LogManager.getLogger(LoggingClient.class.getName());

    @Override
    public void doAction() throws Exception {
        if (isLoggable()) {
            final WebServiceMetaData webServiceMetaData = new WebServiceMetaData(getResource("/META-INF/MANIFEST.MF"));
            final SyncLoggerWrapper loggerWrapper = new SyncLoggerWrapper(syncInput, webServiceMetaData.getVersion(),
                                                                          webServiceMetaData.getTitle());
            final LoggingEvent loggingEvent = loggerWrapper.start();
            loggingEvent.set(LoggingEventKey.USERNAME, logInfo.user);

            try {
                super.doAction();

                loggingEvent.set(LoggingEventKey.SIZE_BYTES_WIRE,
                                 ((ByteCountOutputStream) syncOutput.getOutputStream()).getByteCount());

                // Add others if necessary.
                addLoggingEntries(loggingEvent);
            } catch (Exception exception) {
                loggingEvent.set(LoggingEventKey.ERROR_STRING, exception.getMessage());

                final int code;
                if (exception instanceof IllegalAccessException) {
                    code = HttpServletResponse.SC_BAD_REQUEST;
                } else if (exception instanceof NotAuthenticatedException) {
                    code = HttpServletResponse.SC_UNAUTHORIZED;
                } else if (exception instanceof AccessControlException) {
                    code = HttpServletResponse.SC_FORBIDDEN;
                } else {
                    code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }

                loggingEvent.set(LoggingEventKey.ERROR_CODE, code);
            }

            try {
                REMOTE_LOGGER.log(Level.ALL, loggingEvent.stopTimer());
            } catch (IllegalStateException illegalStateException) {
                LOGGER.error(illegalStateException.getMessage(), illegalStateException);
                throw illegalStateException;
            }
        } else {
            super.doAction();
        }
    }

    boolean isLoggable() {
        final String sval = initParams.get(SyncPostAction.class.getName() + ".execOnPOST");
        return "true".equals(sval);
    }

    /**
     * Classes that extend this will likely want to add further keys that do not apply to all services.  Override this
     * method to do that.
     * @param ignore  The LoggingEvent to set entries on.  Not used by default.
     */
    public void addLoggingEntries(final LoggingEvent ignore) {

    }

    /**
     * Intercept here to add a byte counting output stream.
     * @param syncOutput    The SyncOutput being set by the parent.
     */
    @Override
    public void setSyncOutput(SyncOutput syncOutput) {
        super.setSyncOutput(new ByteCountingSyncOutput(syncOutput));
    }
}
