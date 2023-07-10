
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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import ca.nrc.cadc.net.ResourceNotFoundException;
import org.opencadc.alma.AlmaIDFactory;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.AlmaID;



/**
 * Iterator to provide streaming over the DeliverableInfo items that the DataPacker expands.
 */
public class DataLinkIterator implements Iterator<DataLink> {
    private final Queue<DataLink> dataLinkQueue = new LinkedList<>();
    private final Iterator<String> datasetIDIterator;
    private final AlmaProperties almaProperties;
    private final DataLinkURLBuilder dataLinkURLBuilder;


    DataLinkIterator(final Iterator<String> datasetIDIterator, final AlmaProperties almaProperties)
            throws IOException, ResourceNotFoundException {
        this.datasetIDIterator = datasetIDIterator;
        this.almaProperties = almaProperties;
        this.dataLinkURLBuilder = new DataLinkURLBuilder(this.almaProperties);
    }

    @Override
    public boolean hasNext() {
        if (dataLinkQueue.isEmpty()) {
            final DataLinkQuery dataLinkQuery = createQuery();
            if (datasetIDIterator.hasNext()) {
                final AlmaID nextAlmaID = toAlmaID(datasetIDIterator.next());
                final HierarchyItem hierarchyItem = dataLinkQuery.query(nextAlmaID);
                final HierarchyVisitor hierarchyVisitor =
                        new HierarchyVisitor(nextAlmaID, hierarchyItem, this.almaProperties, this.dataLinkURLBuilder);

                if (hierarchyItem.hasChildren()) {
                    hierarchyVisitor.visitChildren(this.dataLinkQueue);
                } else {
                    dataLinkQueue.add(hierarchyVisitor.createNotFoundDataLink());
                }

                return !dataLinkQueue.isEmpty();
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Tests can override this.
     * @return  DataLinkQuery instance.
     */
    DataLinkQuery createQuery() {
        return new DataLinkQuery(this.almaProperties);
    }

    /**
     * Tests can override this.
     * @param value     Value to create an AlmaID from.
     * @return  AlmaID instance.
     */
    AlmaID toAlmaID(final String value) {
        return AlmaIDFactory.createID(value);
    }

    /**
     * This next method will simply take from the current stack.
     *
     * @return DataLink     Next DataLink created from the next DeliverableInfo in the Queue.
     */
    @Override
    public DataLink next() {
        return dataLinkQueue.poll();
    }
}
