
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

        final String requestID = "88";
        final String deliverableInfoMOUS1ID = "uid://C1/C2/C3";
        final String deliverableInfoMOUS2ID = "uid://C4/C5/C6";

        final DeliverableInfo deliverableInfoProject1 = new DeliverableInfo("2016.1.00057.S",
                                                                            Deliverable.PROJECT);
        final DeliverableInfo deliverableInfoMOUS = new DeliverableInfo(deliverableInfoMOUS1ID,
                                                                        Deliverable.MOUS);
        final DeliverableInfo deliverableInfoMOUSTAR = new DeliverableInfo("uid___C7_C8_C9",
                                                                           Deliverable.PIPELINE_PRODUCT);
        deliverableInfoMOUSTAR.setDisplayName("mous.tar");
        deliverableInfoMOUSTAR.setOwner(deliverableInfoMOUS);

        deliverableInfoMOUS.setOwner(deliverableInfoProject1);

        deliverableInfoProject1.setSubDeliverables(Collections.singletonList(deliverableInfoMOUS));
        deliverableInfoMOUS.setSubDeliverables(Collections.singletonList(deliverableInfoMOUSTAR));

        final DeliverableInfo deliverableInfoProject2 = new DeliverableInfo("2016.0.00055.S",
                                                                            Deliverable.PROJECT);
        final DeliverableInfo deliverableInfoSubMOUS = new DeliverableInfo(deliverableInfoMOUS2ID,
                                                                           Deliverable.MOUS);
        final DeliverableInfo deliverableInfoSubMOUSAux = new DeliverableInfo("uid___C10_C11_C12",
                                                                              Deliverable.PIPELINE_AUXILIARY);
        deliverableInfoSubMOUSAux.setDisplayName("mous-aux.tar");
        deliverableInfoSubMOUSAux.setOwner(deliverableInfoSubMOUS);

        deliverableInfoSubMOUS.setOwner(deliverableInfoProject2);

        deliverableInfoProject2.setSubDeliverables(Collections.singletonList(deliverableInfoSubMOUS));
        deliverableInfoSubMOUS.setSubDeliverables(Collections.singletonList(deliverableInfoSubMOUSAux));

        final List<String> uidList = Arrays.asList(deliverableInfoMOUS1ID, deliverableInfoMOUS2ID);
        final Iterator<String> uidIterator = uidList.iterator();

        final DataPacker mockDataPacker = mock(DataPacker.class);

        final DataLinkURLBuilder dataLinkURLBuilder = new DataLinkURLBuilder("ANON");

        when(mockDataPacker.expand(new Uid(deliverableInfoMOUS1ID), false)).thenReturn(deliverableInfoMOUS);
        when(mockDataPacker.expand(new Uid(deliverableInfoMOUS2ID), false)).thenReturn(deliverableInfoSubMOUS);

        final String[] expectedAccessURLs = new String[] {
                "https://myhost.com/mydataportal/requests/ANON/88/ALMA/uid___C7_C8_C9/mous.tar",
                "https://myhost.com/mydataportal/requests/ANON/88/ALMA/uid___C10_C11_C12/mous-aux.tar"
        };

        final String[] resultAccessURLs = new String[expectedAccessURLs.length];

        int index = 0;
        for (final DataLinkIterator testSubject = new DataLinkIterator(dataLinkURLBuilder, uidIterator,
                                                                       mockDataPacker, requestID);
             testSubject.hasNext(); ) {
            final DataLink nextDataLink = testSubject.next();
            resultAccessURLs[index++] = nextDataLink.accessURL.toExternalForm();
            Assert.assertEquals("Wrong content type.", "application/x-tar", nextDataLink.contentType);
        }

        Assert.assertArrayEquals("Wrong access URLs.", expectedAccessURLs, resultAccessURLs);

        verify(mockDataPacker, times(1)).expand(new Uid(deliverableInfoMOUS1ID), false);
        verify(mockDataPacker, times(1)).expand(new Uid(deliverableInfoMOUS2ID), false);
    }
}
