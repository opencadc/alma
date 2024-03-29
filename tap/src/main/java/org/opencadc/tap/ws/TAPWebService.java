/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2018.                            (c) 2018.
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
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package org.opencadc.tap.ws;

import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vosi.Availability;
import ca.nrc.cadc.vosi.AvailabilityPlugin;
import ca.nrc.cadc.vosi.avail.CheckDataSource;
import ca.nrc.cadc.vosi.avail.CheckException;
import org.apache.log4j.Logger;


/**
 * VOSI Plugin interface for the AvailabilityServlet.
 *
 * @author jenkinsd
 */
public class TAPWebService implements AvailabilityPlugin {
    private static final Logger log = Logger.getLogger(TAPWebService.class);

    private final static String OMIT_ALMA_TEST_PROPERTY = "omit-alma-test";
    private final static String TAPDS_NAME = "jdbc/tapuser";
    // note tap_schema table names
    private final static String TAPDS_TEST =
            "select SCHEMA_NAME from TAP_SCHEMA.SCHEMAS11 where SCHEMA_NAME='TAP_SCHEMA'";
    private final static String ALMA_TEST = "select DATASET_ID from ALMA.ASA_SCIENCE where ROWNUM = 1";

    private String applicationName;


    public TAPWebService() {

    }

    /**
     * Set application name. The appName is a string unique to this application.
     *
     * @param appName unique application name
     */
    @Override
    public void setAppName(String appName) {
        this.applicationName = appName;
    }

    @Override
    public Availability getStatus() {
        boolean isGood = true;
        String note = String.format("The%s service is accepting queries",
                                    StringUtil.hasText(applicationName) ? " " + applicationName : "");
        try {
            // Test query using standard TAP data source
            check(TAPDS_TEST);

            // Won't run in Travis.
            if (!omitALMATest()) {
                check(ALMA_TEST);
            }
        } catch (CheckException ce) {
            // tests determined that the resource is not working
            isGood = false;
            note = ce.getMessage();
        } catch (Throwable t) {
            // the test itself failed
            log.error("web service status test failed", t);
            isGood = false;
            note = "test failed, reason: " + t;
        }
        return new Availability(isGood, note);
    }

    /**
     * A very lightweight method that can be called every few seconds to test if a service is (probably) working.
     * This method is to be implemented by all services.
     *
     * @return true if successful, false otherwise
     */
    @Override
    public boolean heartbeat() {
        return true;
    }

    private void check(final String query) throws CheckException {
        new CheckDataSource(TAPDS_NAME, query).check();
    }

    /**
     * Omit this test in Travis since the ALMA database is unreachable.
     *
     * @return True if the omit-alma-test system property is set.
     */
    private boolean omitALMATest() {
        return Boolean.parseBoolean(System.getProperty(OMIT_ALMA_TEST_PROPERTY));
    }

    @Override
    public void setState(String string) {
        throw new UnsupportedOperationException();
    }
}
