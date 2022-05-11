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

package org.opencadc.alma.logging;


import org.opencadc.alma.logging.formatting.JSONFormatter;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents a logging entry for a request.
 */
public class LoggingEvent {
    private static final String PROGRAM = "VO";
    private static final String ARCHIVE = "ALMA";
    private static final String MEDIUM = "HTTP";

    public static final String REMOTE_LOGGER_NAME = "alma.remote.logger";

    private long startTimeMilliseconds;
    private long endTimeMilliseconds;

    private final Map<LoggingEventKey, Object> loggingAttributes = new HashMap<>();
    private final JSONFormatter formatter = new JSONFormatter();
    private final boolean strict;


    /**
     * Abstract constructor.
     *
     * @param ipAddress     The IP of the caller.
     * @param version       This application's version.
     * @param name          This resource's (service's) name.
     * @param userAgent     The user agent submitting the request.
     * @param strict        Whether to ensure that all required keys are present.
     */
    public LoggingEvent(final String ipAddress, final String version, final String name, final String userAgent,
                        final boolean strict) {
        this.strict = strict;

        set(LoggingEventKey.IP_ADDRESS, ipAddress);
        set(LoggingEventKey.VERSION, version);
        set(LoggingEventKey.RESOURCE_NAME, name);
        set(LoggingEventKey.USER_AGENT, userAgent);
        set(LoggingEventKey.PROGRAM, LoggingEvent.PROGRAM);
        set(LoggingEventKey.ARCHIVE, LoggingEvent.ARCHIVE);
        set(LoggingEventKey.MEDIUM, LoggingEvent.MEDIUM);
    }

    /**
     * Set the start time for this logger.
     */
    public void startTimer() {
        if (isStarted()) {
            throw new IllegalStateException("Already being timed.");
        } else {
            this.startTimeMilliseconds = currentTimeMillis();
            set(LoggingEventKey.START_DATE, new Date(startTimeMilliseconds));
        }
    }

    /**
     * Override for testing.
     * @return  long milliseconds.
     */
    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    boolean isStarted() {
        return startTimeMilliseconds > 0L && endTimeMilliseconds == 0L;
    }

    boolean isComplete() {
        return !isStarted() && endTimeMilliseconds > 0L;
    }

    /**
     * Set the end time and set the duration.
     * @return The message.
     */
    public String stopTimer() {
        if (isStarted()) {
            endTimeMilliseconds = currentTimeMillis();
            set(LoggingEventKey.DURATION, calculateDuration());

            if (strict) {
                final String[] missingRequiredKeys = Arrays.stream(LoggingEventKey.requiredValues())
                                                           .filter(key -> !this.loggingAttributes.containsKey(key))
                                                           .map(LoggingEventKey::getKeyLabel)
                                                           .toArray(String[]::new);
                if (missingRequiredKeys.length > 0) {
                    throw new IllegalStateException("Missing required logging keys: "
                                                    + Arrays.toString(missingRequiredKeys));
                }
            }

            return formatter.asString(this.loggingAttributes);
        } else {
            throw new IllegalStateException("Logging event is not started.  Use startTimer().");
        }
    }

    /**
     * Override for testing.
     * @return  The duration of execution.
     */
    long calculateDuration() {
        if (isComplete()) {
            return endTimeMilliseconds - startTimeMilliseconds;
        } else {
            throw new IllegalStateException("Not completed yet.  Use stopTimer().");
        }
    }

    /**
     * Set a logging parameter to be written out.
     *
     * @param key   The key to set.
     * @param value The value.  Never null.
     */
    public void set(LoggingEventKey key, Object value) {
        if (value != null) {
            loggingAttributes.putIfAbsent(key, value);
        }
    }
}
