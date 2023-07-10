/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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

package org.opencadc.datalink;

import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.opencadc.alma.AlmaID;
import org.opencadc.alma.AlmaIDFactory;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.SpectralWindowID;
import org.opencadc.alma.deliverable.RequestHandlerQuery;


/**
 * DataLink query to parse and handle the JSON from ALMA's Data Portal service.
 */
public class DataLinkQuery extends RequestHandlerQuery {
    private static final String UNKNOWN_HIERARCHY_DOCUMENT_STRING =
            "{\"id\":null,\"name\":\"%s\",\"type\":\"ASDM\",\"sizeInBytes\":-1,\"permission\":\"UNKNOWN\","
            + "\"children\":[],\"allMousUids\":[]}";

    private static final Logger LOGGER = Logger.getLogger(DataLinkQuery.class);

    public DataLinkQuery(final AlmaProperties almaProperties) {
        super(almaProperties);
    }

    /**
     * Obtain the HierarchyItem representing the top level downwards from the given UID.
     *
     * @param almaID The UID to start from.
     * @return HierarchyItem for the UID, or a NotFound HierarchyItem.
     */
    public HierarchyItem query(final AlmaID almaID) {
        try {
            final JSONObject document = new JSONObject(new JSONTokener(downwardsJSONStream(almaID)));
            final JSONObject baseDocument;

            if (almaID instanceof SpectralWindowID) {
                baseDocument = document;
            } else {
                baseDocument = getBaseDocument(almaID, document);
            }

            if (baseDocument == null) {
                throw new IOException("No entry found for " + almaID);
            }

            return HierarchyItem.fromJSONObject(baseDocument);
        } catch (IOException ioException) {
            LOGGER.error(String.format("JSON for %s not found or there was an error acquiring it.\n\n%s",
                                       almaID.getID(), ioException));
            return HierarchyItem.fromJSONObject(new JSONObject(String.format(UNKNOWN_HIERARCHY_DOCUMENT_STRING,
                                                                             almaID.getID())));
        } catch (ResourceNotFoundException resourceNotFoundException) {
            LOGGER.fatal("Unable to find Registry lookup.");
            throw new RuntimeException(resourceNotFoundException.getMessage(), resourceNotFoundException);
        }
    }

    /**
     * Obtain the main document for the requested ID.  Walk the JSON document until one of the <code>children</code>'s
     * ID matches the requested ID, then use the children of that entry to create Links.
     *
     * @param almaID   The ALMA ID to check.
     * @param document The Document received from the data source.
     * @return JSONObject, or null if the ID cannot be found.
     */
    private JSONObject getBaseDocument(final AlmaID almaID, final JSONObject document) {
        final JSONArray childrenJSONArray = document.getJSONArray("children");
        if (matchesEntry(almaID, document)) {
            return document;
        } else {
            final int length = childrenJSONArray.length();
            for (int i = 0; i < length; i++) {
                final JSONObject child = childrenJSONArray.getJSONObject(i);
                final JSONObject baseDocument = getBaseDocument(almaID, child);

                if (baseDocument != null) {
                    return baseDocument;
                }
            }

            return null;
        }
    }

    private boolean matchesEntry(final AlmaID almaID, final JSONObject document) {
        final String objectName = document.get("name").toString();
        final String sanitizedID = almaID.sanitize();

        try {
            return AlmaIDFactory.createID(objectName).sanitize().equals(sanitizedID);
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    /**
     * Obtain an InputStream to JSON data representing the hierarchy of elements.
     *
     * @param almaID The UID to query for.
     * @return InputStream to feed to a JSON Object.
     * @throws IOException Any errors are passed back up the stack.
     */
    InputStream downwardsJSONStream(final AlmaID almaID) throws IOException, ResourceNotFoundException {
        final ExpansionURL downwardsQueryURL = new ExpansionURL(almaID, this.almaProperties);

        LOGGER.debug(String.format("Base URL for Request Handler is %s", downwardsQueryURL));
        final HttpGet httpGet = createHttpGet(downwardsQueryURL.toURL());
        httpGet.run();

        final Throwable throwable = httpGet.getThrowable();
        if (throwable != null) {
            throw new IOException(throwable.getMessage(), throwable);
        } else {
            return httpGet.getInputStream();
        }
    }
}
