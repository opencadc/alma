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

package org.opencadc.alma.deliverable;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.opencadc.alma.AlmaUID;

import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.reg.client.RegistryClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;


public class RequestHandlerQuery {

    private static final Logger LOGGER = Logger.getLogger(RequestHandlerQuery.class);
    private static final String ALT_REGISTRY_LOOKUP = "https://www.almascience.org/reg/applications";
    private static final String UNKNOWN_HIERARCHY_DOCUMENT_STRING =
            "{\"id\":null,\"name\":\"%s\",\"type\":\"ASDM\",\"sizeInBytes\":-1,\"permission\":\"UNKNOWN\","
            + "\"children\":[],\"allMousUids\":[]}";
    private final URI requestHandlerResourceID;

    public RequestHandlerQuery(final URI requestHandlerResourceID) {
        this.requestHandlerResourceID = requestHandlerResourceID;
    }

    /**
     * Obtain the HierarchyItem representing the top level downwards from the given UID.
     * @param almaUID   The UID to start from.
     * @return  HierarchyItem for the UID, or a NotFound HierarchyItem.
     */
    public HierarchyItem query(final AlmaUID almaUID) {
        try {
            final JSONObject document = new JSONObject(new JSONTokener(jsonStream(almaUID)));
            return HierarchyItem.fromJSONObject(almaUID, document);
        } catch (IOException ioException) {
            LOGGER.error(String.format("JSON for %s not found or there was an error acquiring it.\n\n%s",
                                       almaUID.getUID(), ioException));
            return HierarchyItem.fromJSONObject(almaUID, 
                                                new JSONObject(String.format(UNKNOWN_HIERARCHY_DOCUMENT_STRING,
                                                               almaUID.getUID())));
        } catch (ResourceNotFoundException resourceNotFoundException) {
            LOGGER.fatal("Unable to find Registry lookup.");
            throw new RuntimeException(resourceNotFoundException.getMessage(), resourceNotFoundException);
        }
    }

    /**
     * Obtain an InputStream to JSON data representing the hierarchy of elements.
     *
     * @param almaUID The UID to query for.
     * @return InputStream to feed to a JSON Object.
     *
     * @throws IOException Any errors are passed back up the stack.
     */
    InputStream jsonStream(final AlmaUID almaUID) throws IOException, ResourceNotFoundException {
        final URL requestHandlerURL = lookupBaseServiceURL(almaUID);
        LOGGER.debug(String.format("Base URL for Request Handler is %s", requestHandlerURL));
        final HttpGet httpGet = createHttpGet(requestHandlerURL);
        httpGet.run();

        final Throwable throwable = httpGet.getThrowable();
        if (throwable != null) {
            throw new IOException(throwable.getMessage(), throwable);
        } else {
            return httpGet.getInputStream();
        }
    }

    URL lookupBaseServiceURL(final AlmaUID almaUID) throws IOException, ResourceNotFoundException {
        final RegistryClient registryClient = createRegistryClient();
        final URL baseAccessURL = registryClient.getAccessURL(requestHandlerResourceID);
        LOGGER.debug(String.format("Using Request Handler URL %s", baseAccessURL));
        return new URL(String.format("%s/ous/expand/%s/downwards", baseAccessURL.toExternalForm(),
                                     almaUID.getSanitisedUid()));
    }

    HttpGet createHttpGet(final URL requestHandlerEndpointURL) {
        return new HttpGet(requestHandlerEndpointURL, true);
    }

    RegistryClient createRegistryClient() throws MalformedURLException {
        return new RegistryClient(new URL(ALT_REGISTRY_LOOKUP));
    }
}
