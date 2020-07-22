
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


import ca.nrc.cadc.util.PropertiesReader;

import org.junit.Test;
import org.junit.Assert;
import org.opencadc.alma.deliverable.HierarchyItem;

import java.net.URL;

import static org.mockito.Mockito.*;


public class DataLinkURLBuilderTest {

    @Test
    public void createDownloadURL() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);
        final DataLinkURLBuilder testSubject = new DataLinkURLBuilder(null, null);

        when(mockHierarchyItem.getNullSafeId(true)).thenReturn("uid___C71_C72_C73.tmp");
        when(mockHierarchyItem.getType()).thenReturn(HierarchyItem.Type.MOUS);

        final String downloadURL = testSubject.createDownloadURL(mockHierarchyItem).toExternalForm();

        Assert.assertEquals("Wrong URL.",
                            "https://myhost.com/mydownloads/uid___C71_C72_C73.tmp",
                            downloadURL);

        verify(mockHierarchyItem, times(1)).getNullSafeId(true);
        verify(mockHierarchyItem, times(1)).getType();
    }

    @Test
    public void createRecursiveDataLinkURL() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final URL serviceEndpoint = new URL("https://myhost.com/datalink/endpoint");
        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);
        final DataLinkURLBuilder testSubject = new DataLinkURLBuilder(serviceEndpoint, null);

        when(mockHierarchyItem.getNullSafeId(true)).thenReturn("2016.1.00161.S_uid___C81_C82_C83_auxiliary.tar");

        final String recursiveDataLinkURL =
                testSubject.createRecursiveDataLinkURL(mockHierarchyItem).toExternalForm();

        Assert.assertEquals("Wrong URL.",
                            "https://myhost.com/datalink/endpoint?ID=2016.1.00161.S_uid___C81_C82_C83_auxiliary.tar",
                            recursiveDataLinkURL);

        verify(mockHierarchyItem, times(1)).getNullSafeId(true);
    }

    @Test
    public void createCutoutLinkURL() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final URL serviceEndpoint = new URL("https://myhost.com/soda/endpoint");
        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);
        final DataLinkURLBuilder testSubject = new DataLinkURLBuilder(null, serviceEndpoint);

        when(mockHierarchyItem.getNullSafeId(true)).thenReturn("2016.1.00161.S_uid___C81_C82_C83_sci.fits");

        final String recursiveDataLinkURL = testSubject.createCutoutLinkURL(mockHierarchyItem).toExternalForm();

        Assert.assertEquals("Wrong URL.",
                            "https://myhost.com/soda/endpoint?ID=2016.1.00161.S_uid___C81_C82_C83_sci.fits",
                            recursiveDataLinkURL);

        verify(mockHierarchyItem, times(1)).getNullSafeId(true);
    }

    @Test
    public void createRecursiveDataLinkURLWithQuery() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final URL serviceEndpoint = new URL("https://myhost.com/datalink/endpoint?recurse=true");
        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);
        final DataLinkURLBuilder testSubject = new DataLinkURLBuilder(serviceEndpoint, null);

        when(mockHierarchyItem.getNullSafeId(true)).thenReturn("2016.1.00161.S_uid___C81_C82_C83_auxiliary.tar");

        final String recursiveDataLinkURL =
                testSubject.createRecursiveDataLinkURL(mockHierarchyItem).toExternalForm();

        Assert.assertEquals("Wrong URL.",
                            "https://myhost.com/datalink/endpoint?recurse=true&ID=2016.1.00161" +
                            ".S_uid___C81_C82_C83_auxiliary.tar",
                            recursiveDataLinkURL);

        verify(mockHierarchyItem, times(1)).getNullSafeId(true);
    }
}
