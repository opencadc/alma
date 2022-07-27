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

package org.opencadc.conesearch.config;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.MultiValuedProperties;
import ca.nrc.cadc.util.PropertiesReader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Configuration of this web application.
 */
public class ConeSearchConfig {
    private static final String CONFIG_FILE_NAME = "cadc-conesearch.properties";
    private static final Logger LOGGER = LogManager.getLogger(ConeSearchConfig.class);

    private final MultiValuedProperties properties;


    public ConeSearchConfig() {
        this(new PropertiesReader(ConeSearchConfig.CONFIG_FILE_NAME));
    }

    /**
     * Complete constructor.
     * @param propertiesReader      A reader on the configuration file.
     *
     * @throws IllegalArgumentException     If the configuration file cannot be read.
     */
    ConeSearchConfig(final PropertiesReader propertiesReader) {
        if (propertiesReader.canRead()) {
            this.properties = propertiesReader.getAllProperties();

            if (this.properties == null) {
                throw new IllegalArgumentException("No configuration in " + ConeSearchConfig.CONFIG_FILE_NAME);
            } else {
                LOGGER.debug("Configuration load: OK");
                this.properties.keySet().forEach(k -> LOGGER.debug(k + " -> " + this.properties.getProperty(k)));
            }
        } else {
            throw new IllegalArgumentException("Unable to read " + ConeSearchConfig.CONFIG_FILE_NAME);
        }
    }

    /**
     * Obtain the base TAP URL to use. This method will read in an optionally configured 'tapURI' property and if it
     * appears to be a URL, then assume an unregistered TAP service was configured and treat it as the base URL,
     * otherwise attempt to use it as a URI and look the URL up in the Registry.
     *
     * @return URL Base URL of the TAP service to use.
     *
     * @throws MalformedURLException If a URL cannot be created from the specified string.
     */
    public URL getTapBaseURL() throws MalformedURLException {
        final URI configuredTapURI = URI.create(getTapURI());

        final AuthMethod am = AuthenticationUtil.getAuthMethod(AuthenticationUtil.getCurrentSubject());
        if (configuredTapURI.getScheme().equals("ivo")) {
            final RegistryClient regClient = new RegistryClient();
            // Attempt to load the URI as a resource URI from the Registry.
            return regClient.getServiceURL(configuredTapURI, Standards.TAP_10, (am == null) ? AuthMethod.ANON : am);
        } else {
            // Fallback and assume the URI is an absolute URL.
            return configuredTapURI.toURL();
        }
    }

    /**
     * Obtain the (ideally) spatially indexed column name to compare the center of the cone's position to.
     * @return  String column name.
     */
    public String getPositionColumnName() {
        return properties.getFirstPropertyValue(ConfigurationParameterNames.POSITION_COLUMN_NAME.getPropertyKey());
    }

    /**
     * Obtain the configured select list for VERB=1.
     * @return  String comma-delimited column names (e.g. col_a, col_b, col_c).
     */
    public String getLowVerbositySelectList() {
        return properties.getFirstPropertyValue(ConfigurationParameterNames.LOW_VERBOSITY_SELECT_LIST.getPropertyKey());
    }

    /**
     * Obtain the configured select list for VERB=2.
     * @return  String comma-delimited column names (e.g. col_a, col_b, col_c).
     */
    public String getMidVerbositySelectList() {
        return properties.getFirstPropertyValue(ConfigurationParameterNames.MID_VERBOSITY_SELECT_LIST.getPropertyKey());
    }

    /**
     * Obtain the configured select list for VERB=3.
     * @return  String comma-delimited column names (e.g. *).
     */
    public String getHighVerbositySelectList() {
        return properties.getFirstPropertyValue(ConfigurationParameterNames.HIGH_VERBOSITY_SELECT_LIST.getPropertyKey());
    }

    /**
     * Obtain the configured TAP URI.
     * @return      String URI value.  Never null.
     */
    String getTapURI() {
        return properties.getFirstPropertyValue(ConfigurationParameterNames.TAP_URI.getPropertyKey());
    }
}
