
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
import alma.asdm.domain.identifiers.Uid;
import alma.asdm.service.DataPacker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.Assert;

import ca.nrc.cadc.util.PropertiesReader;

import static org.mockito.Mockito.*;


public class DataLinkIteratorTest {

    @Test
    public void runThrough() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final String deliverableInfoMOUS1ID = "uid://C1/C2/C3";
        final String deliverableInfoMOUS2ID = "uid://C4/C5/C6";

        final DeliverableInfo deliverableInfoProject1 = new DeliverableInfo("2016.1.00057.S",
                                                                            Deliverable.PROJECT);
        final DeliverableInfo deliverableInfoMOUS = new DeliverableInfo(deliverableInfoMOUS1ID, Deliverable.MOUS);
        final DeliverableInfo deliverableInfoMOUSTAR = new DeliverableInfo("uid___C7_C8_C9.tar",
                                                                           Deliverable.PIPELINE_PRODUCT_TARFILE);
        deliverableInfoMOUSTAR.setDisplayName("uid___C7_C8_C9.tar");
        deliverableInfoMOUSTAR.setOwner(deliverableInfoMOUS);

        deliverableInfoMOUS.setOwner(deliverableInfoProject1);

        deliverableInfoProject1.setSubDeliverables(Collections.singletonList(deliverableInfoMOUS));
        deliverableInfoMOUS.setSubDeliverables(Collections.singletonList(deliverableInfoMOUSTAR));

        final DeliverableInfo deliverableInfoProject2 = new DeliverableInfo("2016.0.00055.S",
                                                                            Deliverable.PROJECT);
        final DeliverableInfo deliverableInfoSubMOUS = new DeliverableInfo(deliverableInfoMOUS2ID,
                                                                           Deliverable.MOUS);
        final DeliverableInfo deliverableInfoSubMOUSAux = new DeliverableInfo("uid___C10_C11_C12.tar",
                                                                              Deliverable.PIPELINE_AUXILIARY_TARFILE);
        deliverableInfoSubMOUSAux.setDisplayName("uid___C10_C11_C12.tar");
        deliverableInfoSubMOUSAux.setOwner(deliverableInfoSubMOUS);

        deliverableInfoSubMOUS.setOwner(deliverableInfoProject2);

        deliverableInfoProject2.setSubDeliverables(Collections.singletonList(deliverableInfoSubMOUS));
        deliverableInfoSubMOUS.setSubDeliverables(Collections.singletonList(deliverableInfoSubMOUSAux));

        final List<String> uidList = Arrays.asList(deliverableInfoMOUS1ID, deliverableInfoMOUS2ID);
        final Iterator<String> uidIterator = uidList.iterator();

        final DataPacker mockDataPacker = mock(DataPacker.class);

        final DataLinkURLBuilder dataLinkURLBuilder = new DataLinkURLBuilder();

        when(mockDataPacker.expand(new Uid(deliverableInfoMOUS1ID), false)).thenReturn(deliverableInfoMOUS);
        when(mockDataPacker.expand(new Uid(deliverableInfoMOUS2ID), false)).thenReturn(deliverableInfoSubMOUS);

        final String[] expectedAccessURLs = new String[] {
                "https://myhost.com/mydownloads/uid___C7_C8_C9.tar",
                "https://myhost.com/mydatalink/recurse/uid___C7_C8_C9.tar",
                "https://myhost.com/mydownloads/uid___C10_C11_C12.tar",
                "https://myhost.com/mydatalink/recurse/uid___C10_C11_C12.tar",
                };

        final String[] resultAccessURLs = new String[expectedAccessURLs.length];

        final DataLinkIterator testSubject = new DataLinkIterator(dataLinkURLBuilder, uidIterator, mockDataPacker) {
            @Override
            URL lookupRecursiveURL(DeliverableInfo deliverableInfo) throws MalformedURLException {
                return new URL("https://myhost.com/mydatalink/recurse/" + deliverableInfo.getIdentifier());
            }
        };

        int index = 0;

        while (testSubject.hasNext()) {
            final DataLink nextDataLink = testSubject.next();
            resultAccessURLs[index++] = nextDataLink.accessURL.toExternalForm();
        }

        Assert.assertArrayEquals("Wrong access URLs.", expectedAccessURLs, resultAccessURLs);

        verify(mockDataPacker, times(1)).expand(new Uid(deliverableInfoMOUS1ID), false);
        verify(mockDataPacker, times(1)).expand(new Uid(deliverableInfoMOUS2ID), false);
    }

    @Test
    public void createDataLinks() throws Exception {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final DataLinkIterator testSubject = new DataLinkIterator(new DataLinkURLBuilder(), null, null);

        final DeliverableInfo deliverableInfoOne = new DeliverableInfo("uid___C7_C8_C9.tar",
                                                                       Deliverable.PIPELINE_PRODUCT);
        final List<DataLink> dataLinksOne = testSubject.createDataLinks(deliverableInfoOne);
        Assert.assertEquals("Should have one element.", 1, dataLinksOne.size());
        final DataLink datalinkOne = dataLinksOne.get(0);

        Assert.assertArrayEquals("Wrong semantics.", new DataLink.Term[] {DataLink.Term.PKG, DataLink.Term.THIS},
                                 datalinkOne.getSemantics().toArray());


        final DeliverableInfo deliverableInfoTwo = new DeliverableInfo("uid__C8_C9_C100.aux.tar",
                                                                       Deliverable.PIPELINE_AUXILIARY_TARFILE);
        final List<DataLink> dataLinksTwo = testSubject.createDataLinks(deliverableInfoTwo);
        Assert.assertEquals("Should contain two elements.", 2, dataLinksTwo.size());
        final DataLink datalinkTwo = dataLinksTwo.get(0);

        Assert.assertArrayEquals("Wrong semantics.", new DataLink.Term[] {DataLink.Term.PKG, DataLink.Term.AUXILIARY},
                                 datalinkTwo.getSemantics().toArray());

        final DataLink datalinkTwoPointOne = testSubject.createDataLinks(deliverableInfoTwo).get(1);

        Assert.assertArrayEquals("Wrong semantics.",
                                 new DataLink.Term[] {DataLink.Term.DATALINK, DataLink.Term.AUXILIARY},
                                 datalinkTwoPointOne.getSemantics().toArray());


        final DeliverableInfo deliverableInfoThree = new DeliverableInfo("README.aux.txt",
                                                                         Deliverable.PIPELINE_AUXILIARY_README);
        final List<DataLink> dataLinksThree = testSubject.createDataLinks(deliverableInfoThree);
        Assert.assertEquals("Should have one element.", 1, dataLinksThree.size());
        final DataLink datalinkThree = dataLinksThree.get(0);

        Assert.assertArrayEquals("Wrong semantics.", new DataLink.Term[] {DataLink.Term.AUXILIARY},
                                 datalinkThree.getSemantics().toArray());


        final DeliverableInfo deliverableInfoFour = new DeliverableInfo("uid__C5_C6.science.fits",
                                                                        Deliverable.PIPELINE_PRODUCT);
        final List<DataLink> dataLinksFour = testSubject.createDataLinks(deliverableInfoFour);
        Assert.assertEquals("Should have one element.", 1, dataLinksFour.size());
        final DataLink datalinkFour = dataLinksFour.get(0);

        Assert.assertArrayEquals("Wrong semantics.", new DataLink.Term[] {DataLink.Term.THIS},
                                 datalinkFour.getSemantics().toArray());
    }
}
