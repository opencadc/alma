
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class AlmaIDTest {

    @Before
    public void setup() {
        Configurator.setLevel(AlmaID.class.getCanonicalName(), Level.DEBUG);
    }

    @Test
    public void constructFromDeSanitizedMOUSID() {
        final AlmaID testSubject = AlmaIDFactory.createID("uid://C0/C1/C2");
        assertTrue("Wrong type.", testSubject instanceof ObsUnitSetID);
        assertEquals("Wrong ID", "uid://C0/C1/C2", testSubject.getID());
        assertEquals("Wrong sanitized ID", "uid___C0_C1_C2", testSubject.sanitize());
        assertEquals("Wrong desanitized ID", "uid://C0/C1/C2", testSubject.desanitize());
    }

    @Test
    public void constructFromSanitizedMOUSID() {
        final AlmaID testSubject = AlmaIDFactory.createID("uid___C0_C1_C2");
        assertTrue("Wrong type.", testSubject instanceof ObsUnitSetID);
        assertEquals("Wrong ID", "uid___C0_C1_C2", testSubject.getID());
        assertEquals("Wrong sanitized ID", "uid___C0_C1_C2", testSubject.sanitize());
        assertEquals("Wrong desanitized ID", "uid://C0/C1/C2", testSubject.desanitize());
        assertEquals("Wrong endpoint", "uid___C0_C1_C2", testSubject.getEndpointID());
    }

    @Test
    public void constructFromBadInput() {
        try {
            AlmaIDFactory.createID("");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // Good.
        }

        try {
            AlmaIDFactory.createID(null);
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    @Test
    public void constructFromTARFileID() {
        final AlmaID testSubject = AlmaIDFactory.createID("2016.1.00161.S_uid___A002_Xc4f3ae_X537a.asdm.sdm.tar");
        assertTrue("Wrong type.", testSubject instanceof ObsUnitSetID);
        assertEquals("Wrong endpoint ID.", "uid___A002_Xc4f3ae_X537a", testSubject.getEndpointID());
        assertEquals("Wrong sanitized ID.", "uid___A002_Xc4f3ae_X537a",
                     testSubject.getEndpointID());
    }

    @Test
    public void constructFromEnergyID() {
        final AlmaID testSubject = AlmaIDFactory.createID("uid://A001/X1465/X162.source.Serp_02.spw.17");
        assertTrue("Wrong type.", testSubject instanceof SpectralWindowID);
        assertEquals("Wrong MOUS ID.", "uid___A001_X1465_X162.source.Serp_02.spw.17",
                     testSubject.getEndpointID());
    }
}
