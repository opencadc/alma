
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

package org.opencadc.alma;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.MultiValuedProperties;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.StringUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;


/**
 * Properties configuration for ALMA applications.
 */
public class AlmaProperties extends PropertiesReader {
    private static final Logger LOGGER = Logger.getLogger(AlmaProperties.class);
    private static final String DEFAULT_PROPERTIES_FILE_NAME = "org.opencadc.alma.properties";

    static final String ALMA_REQUEST_HANDLER_SERVICE_URI = "almaRequestHandlerServiceURI";
    static final String ALMA_FILE_SODA_SERVICE_PORT = "almaFileSodaServicePort";
    static final String ALMA_DATALINK_SERVICE_URI = "almaDataLinkServiceURI";
    static final String ALMA_SODA_SERVICE_URI = "almaSODAServiceURI";
    static final String ALMA_DATAPROTAL_SERVICE_URI = "almaDataPortalServiceURI";


    public AlmaProperties() {
        this(DEFAULT_PROPERTIES_FILE_NAME);
    }

    public AlmaProperties(final String filename) {
        super(filename);
    }


    public String getFirstPropertyValue(final String key, final String defaultValue) {
        final MultiValuedProperties allProperties = super.getAllProperties();
        final String s = allProperties.getFirstPropertyValue(key);
        return StringUtil.hasText(s) ? s : defaultValue;
    }

    public URI getRequestHandlerServiceURI() {
        return ensureRequiredURI(ALMA_REQUEST_HANDLER_SERVICE_URI);
    }

    public String getFileSodaServicePort() {
        return ensureRequiredString(ALMA_FILE_SODA_SERVICE_PORT);
    }

    public URI getDataLinkServiceURI() {
        return ensureRequiredURI(ALMA_DATALINK_SERVICE_URI);
    }

    public URI getSodaServiceURI() {
        return ensureRequiredURI(ALMA_SODA_SERVICE_URI);
    }

    public URI getDataPortalServiceURI() {
        return ensureRequiredURI(ALMA_DATAPROTAL_SERVICE_URI);
    }

    URI ensureRequiredURI(final String key) {
        return URI.create(ensureRequiredString(key));
    }

    String ensureRequiredString(final String key) {
        final String configuredValue = this.getFirstPropertyValue(key, null);

        if (!StringUtil.hasText(configuredValue)) {
            throw new IllegalStateException(
                    String.format("\nRequested property value was not found.  Ensure the %s is set in the "
                                  + "%s file.\n", key, DEFAULT_PROPERTIES_FILE_NAME));
        } else {
            return configuredValue;
        }
    }

    public URL lookupDataLinkServiceURL() {
        return lookupServiceURL(getDataLinkServiceURI());
    }

    public URL lookupSodaServiceURL() {
        return lookupServiceURL(getSodaServiceURI());
    }

    public URL lookupDataPortalURL() throws IOException, ResourceNotFoundException {
        return lookupApplicationURL(getDataPortalServiceURI());
    }

    public URL lookupRequestHandlerURL() throws IOException, ResourceNotFoundException {
        final URI requestHandlerServiceURI = getRequestHandlerServiceURI();
        LOGGER.debug(String.format("Looking up Request Handler URL from configured URI %s.", requestHandlerServiceURI));
        return lookupApplicationURL(requestHandlerServiceURI);
    }

    URL lookupServiceURL(final URI serviceURI) {
        return createRegistryClient().getServiceURL(serviceURI, Standards.INTERFACE_PARAM_HTTP, AuthMethod.ANON);
    }

    URL lookupApplicationURL(final URI serviceURI) throws IOException, ResourceNotFoundException {
        return createApplicationsRegistryClient().getAccessURL(serviceURI);
    }

    RegistryClient createApplicationsRegistryClient() throws MalformedURLException {
        // The hostname here doesn't matter as it gets mangled post construction.
        return new RegistryClient(new URL("https://www.almascience.org/reg/applications"));
    }

    RegistryClient createRegistryClient() {
        return new RegistryClient();
    }
}
