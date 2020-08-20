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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.alma.AlmaUID;

import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.PropertiesReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static org.mockito.Mockito.*;


public class RequestHandlerQueryTest {

    @Test
    public void createURL() throws Exception {
        final URI resourceURI = URI.create("ivo://alma.org/rh");
        final HttpGet mockHttpGet = mock(HttpGet.class);
        final RegistryClient mockRegistryClient = mock(RegistryClient.class);
        final RequestHandlerQuery testSubject = new RequestHandlerQuery(resourceURI) {
            @Override
            HttpGet createHttpGet(URL requestHandlerEndpointURL) {
                return mockHttpGet;
            }

            @Override
            RegistryClient createRegistryClient() {
                return mockRegistryClient;
            }
        };

        when(mockRegistryClient.getAccessURL(resourceURI)).thenReturn(
                new URL("https://alma.org/requestHandler/tree"));
        final URL endpointURL = testSubject.lookupBaseServiceURL(new AlmaUID("uid://C0/C1/C313"));
        Assert.assertEquals("Wrong endpoint",
                            new URL("https://alma.org/requestHandler/tree/ous/expand/uid___C0_C1_C313/downwards"),
                            endpointURL);
    }

    @Test
    public void query() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");
        final URI resourceURI = URI.create("ivo://alma.org/rh");
        final File testFile = FileUtil.getFileFromResource(RequestHandlerQueryTest.class.getSimpleName() + ".json",
                                                           RequestHandlerQueryTest.class);
        final JSONObject testDocument = new JSONObject(new JSONTokener(new FileReader(testFile)));
        final RequestHandlerQuery testSubject = new RequestHandlerQuery(resourceURI) {
            /**
             * Obtain an InputStream to JSON data representing the hierarchy of elements.
             *
             * @param almaUID The UID to query for.
             * @return InputStream to feed to a JSON Object.
             *
             * @throws IOException Any errors are passed back up the stack.
             */
            @Override
            InputStream jsonStream(AlmaUID almaUID) throws IOException, ResourceNotFoundException {
                return new FileInputStream(testFile);
            }
        };
        final AlmaUID testAlmaUIDOne = new AlmaUID("uid://A001/X133d/X1eef");

        final HierarchyItem resultHierarchyItem = testSubject.query(testAlmaUIDOne);

        Assert.assertEquals("Wrong document.", HierarchyItem.fromJSONObject(testAlmaUIDOne, testDocument),
                            resultHierarchyItem);

        final AlmaUID testAlmaUIDTwo = new AlmaUID("2018.1.00526.S_uid___A001_X133d_X1eef_001_of_001.tar");
        final HierarchyItem resultSubHierarchyItem = testSubject.query(testAlmaUIDTwo);
        HierarchyItem expectedSubHierarchyItem = null;

        for (final Object o : testDocument.getJSONArray("children")) {
            final JSONObject jsonObject = (JSONObject) o;
            if (testAlmaUIDTwo.getSanitisedUid().equals(jsonObject.get("name").toString())) {
                expectedSubHierarchyItem = HierarchyItem.fromJSONObject(testAlmaUIDTwo, jsonObject);
            }
        }

        Assert.assertEquals("Wrong subdocument", expectedSubHierarchyItem, resultSubHierarchyItem);
    }
}
