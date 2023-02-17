
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

package org.opencadc.soda.server;

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.rest.SyncOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.opencadc.alma.AlmaID;
import org.opencadc.soda.SodaQuery;


public class AlmaStreamingSodaPlugin implements StreamingSodaPlugin, SodaPlugin {
    private static final Logger LOGGER = Logger.getLogger(AlmaStreamingSodaPlugin.class);
    private final SodaQuery sodaQuery;

    public AlmaStreamingSodaPlugin(final SodaQuery sodaQuery) {
        this.sodaQuery = sodaQuery;
    }


    /**
     * Perform cutout operation and write output.
     *
     * @param uri         target resource (file)
     * @param cutout      parsed cutout request
     * @param extraParams additional params not already handled
     * @param out         streaming output destination
     * @throws IOException failure to read or write data
     */
    @Override
    public void write(URI uri, Cutout cutout, Map<String, List<String>> extraParams, SyncOutput out)
            throws IOException {
        final URL cutoutURL = toURL(1, uri, cutout, extraParams);
        LOGGER.info(String.format("Trying to cutout from %s", cutoutURL));
        final HttpGet httpGet = createDownloader(cutoutURL, out.getOutputStream());
        httpGet.run();

        final Throwable getError = httpGet.getThrowable();

        if (getError != null) {
            if (getError instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) getError;
            } else if (getError instanceof ResourceNotFoundException) {
                throw new IllegalArgumentException(getError.getMessage(), getError);
            } else if (getError instanceof RuntimeException) {
                throw (RuntimeException) getError;
            }
        }
    }

    /**
     * Convert a cutout request to a specific data (file) to a URL for the result.
     * The URL could be to an on-the-fly cutout backend (for SODA-sync) or the plugin
     * method could retrieve data, perform the cutout operation, store the result
     * in temporary storage, and return a URL to the result (SODA-async).
     *
     * @param serialNum   number that increments for each call to the plugin within a single request
     * @param uri         the ID value that identifies the data (file)
     * @param cutouts     holder for all cutout requests
     * @param extraParams custom parameters and values (may be empty)
     * @return a URL to the result of the operation
     * @throws IOException failure to read or write data
     */
    @Override
    public URL toURL(int serialNum, URI uri, Cutout cutouts, Map<String, List<String>> extraParams) throws IOException {
        final AlmaUID almaUID = new AlmaUID(uri.toString());
        try {
            return sodaQuery.toCutoutURL(almaUID, cutouts);
        } catch (ResourceNotFoundException resourceNotFoundException) {
            throw new IOException(resourceNotFoundException.getMessage(), resourceNotFoundException);
        }
    }

    HttpGet createDownloader(final URL url, final OutputStream outputStream) {
        return new HttpGet(url, outputStream);
    }
}
