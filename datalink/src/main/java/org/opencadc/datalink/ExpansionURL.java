/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2023.                            (c) 2023.
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

import ca.nrc.cadc.net.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.AlmaID;
import org.opencadc.alma.ObsUnitSetID;
import org.opencadc.alma.SpectralWindowID;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a URL to get the JSON expansion document of an ID from the RequestHandler.
 */
public class ExpansionURL {
    private static final Logger LOGGER = LogManager.getLogger(ExpansionURL.class);

//    Examples of how this regular expression is used are:
//    hostURL = "https://mysite.com/rh";
//    myOUSID = "uid___X34_C43";
//    mySPWID = "2023.05.66.spw1";
//
//    // https://mysite.com/rh/ous/expand/uid___X34_C43/downwards
//    ousURL = String.format(DOWNWARDS_ENDPOINT_TEMPLATE, hostURL, "ous", myOUSID);
//
//    // https://mysite.com/rh/spw/expand/2023.05.66.spw1/downwards
//    spwURL = String.format(DOWNWARDS_ENDPOINT_TEMPLATE, hostURL, "spw", mySPWID);
    private static final String DOWNWARDS_ENDPOINT_TEMPLATE = "%s/%s/expand/%s/downwards";

    private static final String SPW_ENDPOINT = "spw";
    private static final String OUS_ENDPOINT = "ous";

    private static final Map<Class<? extends AlmaID>, String> ALMA_ID_ENDPOINT_MAPPING = new HashMap<>();

    private final AlmaID almaID;
    private final AlmaProperties almaProperties;

    public ExpansionURL(final AlmaID almaID, final AlmaProperties almaProperties) {
        this.almaID = almaID;
        this.almaProperties = almaProperties;

        ALMA_ID_ENDPOINT_MAPPING.put(SpectralWindowID.class, ExpansionURL.SPW_ENDPOINT);
        ALMA_ID_ENDPOINT_MAPPING.put(ObsUnitSetID.class, ExpansionURL.OUS_ENDPOINT);
    }


    /**
     * Obtain the absolute URL of the expansion location.
     * @return  URL, never null.
     * @throws IOException      If the URL cannot be used, or there is no usable base URL from the properties.
     * @throws ResourceNotFoundException    If no base URL was configured.
     */
    public URL toURL() throws IOException, ResourceNotFoundException {
        final URL baseServiceURL = this.almaProperties.lookupRequestHandlerURL();
        LOGGER.debug(String.format("Using Base Request Handler URL %s", baseServiceURL));

        return new URL(String.format(DOWNWARDS_ENDPOINT_TEMPLATE, baseServiceURL.toExternalForm(),
                                     ExpansionURL.ALMA_ID_ENDPOINT_MAPPING.get(almaID.getClass()),
                                     almaID.getEndpointID()));
    }
}
