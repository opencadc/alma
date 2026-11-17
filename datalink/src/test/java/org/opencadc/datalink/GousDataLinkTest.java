/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2026.                            (c) 2026.
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
 *  the GNU Affero General Public        la "GNU Affero General Public
 *  License as published by the          License" telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l'espoir qu'il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d'ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n'est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package org.opencadc.datalink;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.PropertiesReader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.alma.AlmaIDFactory;
import org.opencadc.alma.AlmaProperties;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GousDataLinkTest {

    @Test
    public void gousQueryExposesRecursiveDataLinkForMemberMous() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final AlmaProperties mockAlmaProperties = mock(AlmaProperties.class);
        when(mockAlmaProperties.lookupDataLinkServiceURL()).thenReturn(URI.create("https://alma.com/datalink").toURL());
        when(mockAlmaProperties.lookupSodaServiceURL()).thenReturn(URI.create("https://alma.com/soda").toURL());
        when(mockAlmaProperties.lookupDataPortalURL()).thenReturn(URI.create("https://alma.com/dataportal").toURL());

        final File jsonFile = FileUtil.getFileFromResource(GousDataLinkTest.class.getSimpleName() + ".json",
                                                           GousDataLinkTest.class);
        final HierarchyItem gousItem =
                HierarchyItem.fromJSONObject(new JSONObject(new JSONTokener(new FileInputStream(jsonFile))));

        final List<DataLink> dataLinks = new ArrayList<>();
        new HierarchyVisitor(AlmaIDFactory.createID("uid://A001/X1590/X30a9"), gousItem, mockAlmaProperties,
                             new DataLinkURLBuilder(mockAlmaProperties)).visitChildren(dataLinks);

        final long recursiveMousLinks = dataLinks.stream()
                                                 .filter(link -> link.serviceDef != null
                                                                 && link.serviceDef.equals(
                                                                         "DataLink.uid___A001_X1590_X30aa"))
                                                 .count();
        final long notFoundLinks = dataLinks.stream()
                                            .filter(link -> DataLink.Term.ERROR.equals(link.getSemantics()))
                                            .count();

        Assert.assertEquals("Expected one recursive DataLink for the member MOUS.", 1L, recursiveMousLinks);
        Assert.assertEquals("OUS containers must not be reported as missing files.", 0L, notFoundLinks);
    }

    @Test
    public void obsUnitSetQueryFindsNestedMousUnderGous() throws Exception {
        final File jsonFile = FileUtil.getFileFromResource(GousDataLinkTest.class.getSimpleName() + ".json",
                                                           GousDataLinkTest.class);
        final ObsUnitSetQuery query = new ObsUnitSetQuery(null) {
            @Override
            java.io.InputStream downwardsJSONStream(org.opencadc.alma.AlmaID almaID) {
                try {
                    return new FileInputStream(jsonFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        final HierarchyItem mousItem = query.query(AlmaIDFactory.createID("uid://A001/X1590/X30aa"));

        Assert.assertEquals("uid://A001/X1590/X30aa", mousItem.getName());
        Assert.assertEquals(HierarchyItem.Type.MOUS, mousItem.getType());
    }
}