
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

package org.opencadc.datalink;

import alma.asdm.domain.Deliverable;
import alma.asdm.domain.DeliverableInfo;
import ca.nrc.cadc.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;


public class DataLinkURLBuilder {

    private final DataLinkProperties dataLinkProperties;
    private final URL dataLinkServiceEndpoint;
    private final URL cutoutServiceEndpoint;


    public DataLinkURLBuilder(final URL dataLinkServiceEndpoint, final URL cutoutServiceEndpoint) {
        this(new DataLinkProperties(), dataLinkServiceEndpoint, cutoutServiceEndpoint);
    }

    public DataLinkURLBuilder(final DataLinkProperties dataLinkProperties, final URL dataLinkServiceEndpoint,
                              final URL cutoutServiceEndpoint) {
        this.dataLinkProperties = dataLinkProperties;
        this.dataLinkServiceEndpoint = dataLinkServiceEndpoint;
        this.cutoutServiceEndpoint = cutoutServiceEndpoint;
    }

    URL createRecursiveDataLinkURL(final DeliverableInfo deliverableInfo) throws MalformedURLException {
        return createServiceLinkURL(deliverableInfo, dataLinkServiceEndpoint);
    }

    URL createCutoutLinkURL(final DeliverableInfo deliverableInfo) throws MalformedURLException {
        return createServiceLinkURL(deliverableInfo, cutoutServiceEndpoint);
    }

    private URL createServiceLinkURL(final DeliverableInfo deliverableInfo, final URL serviceURLEndpoint)
            throws MalformedURLException {
        final String urlFile = String.format("%s%sID=%s", serviceURLEndpoint.getFile(),
                                             StringUtil.hasText(serviceURLEndpoint.getQuery()) ? "&" : "?",
                                             deliverableInfo.getIdentifier());

        return new URL(serviceURLEndpoint.getProtocol(), serviceURLEndpoint.getHost(),
                       serviceURLEndpoint.getPort(), urlFile);
    }

    URL createDownloadURL(final DeliverableInfo deliverableInfo) throws MalformedURLException {
        final String secureSchemeHost = dataLinkProperties.getFirstPropertyValue("secureSchemeHost");
        final String downloadPath = dataLinkProperties.getFirstPropertyValue("downloadPath");

        final String sanitizedURL = String.join("/", new String[] {
                sanitizePath(secureSchemeHost),
                sanitizePath(downloadPath)
        });

        return new URL(String.join("/", new String[] {
                sanitizePath(new URL(sanitizedURL).toExternalForm()),

                // For ASDMs, the Display Name is the right now to shove out as it's sanitized.
                sanitizePath(deliverableInfo.getType() == Deliverable.ASDM ?
                             deliverableInfo.getDisplayName() : deliverableInfo.getIdentifier())
        }));
    }

    private String sanitizePath(final String pathItem) {
        final String sanitizedPath;
        if (!StringUtil.hasLength(pathItem)) {
            sanitizedPath = "";
        } else {
            final StringBuilder stringBuilder = new StringBuilder(pathItem.trim());

            while (stringBuilder.indexOf("/") == 0) {
                stringBuilder.deleteCharAt(0);
            }

            while (stringBuilder.lastIndexOf("/") == (stringBuilder.length() - 1)) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }

            sanitizedPath = stringBuilder.toString();
        }

        return sanitizedPath;
    }
}
