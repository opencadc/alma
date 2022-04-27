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

import ca.nrc.cadc.rest.SyncInput;
import ca.nrc.cadc.uws.Job;
import org.opencadc.alma.logging.LoggingEvent;
import org.opencadc.alma.logging.LoggingEventKey;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple POJO to wrap common logic.
 */
public class SyncLoggerWrapper {
    private final SyncInput syncInput;
    private final Job job;
    private final String version;
    private final String title;

    public SyncLoggerWrapper(SyncInput syncInput, Job job, String version, String title) {
        this.syncInput = syncInput;
        this.job = job;
        this.version = version;
        this.title = title;
    }

    public LoggingEvent start() {
        final LoggingEvent loggingEvent = new LoggingEvent(syncInput.getClientIP(), version, title,
                                                           syncInput.getHeader("user-agent"), true);
        loggingEvent.startTimer();
        final List<String> parameters = new ArrayList<>();
        final String requestURI;
        if (this.job == null) {
            syncInput.getParameterNames()
                     .forEach(param -> parameters.add(String.format("%s=%s", param, syncInput.getParameter(param))));
            requestURI = syncInput.getRequestURI();
        } else {
            this.job.getParameterList().forEach(param -> parameters.add(
                    String.format("%s=%s", param.getName(), param.getValue())));
            final URI inputURI = URI.create(syncInput.getRequestURI());
            final int port = inputURI.getPort();
            final String portString = (port == 80 || port == 443 || port < 0) ? "" : Integer.toString(port);
            requestURI = String.format("%s:%s//%s%s", inputURI.getScheme(), portString, inputURI.getHost(),
                                       this.job.getRequestPath());
        }

        final String query = String.join("&", parameters);
        loggingEvent.set(LoggingEventKey.QUERY, String.format("%s%s%s", requestURI,
                                                              parameters.isEmpty() ? "" : "?", query));

        return loggingEvent;
    }
}
