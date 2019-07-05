
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
import alma.asdm.domain.identifiers.AsdmUid;
import alma.asdm.domain.identifiers.Uid;
import alma.asdm.service.DataPacker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;


public class DataLinkIterator implements Iterator<DataLink> {

    static final String DEFAULT_CONTENT_TYPE = "application/x-tar";
    static final String README_CONTENT_TYPE = "text/plain";

    private final Queue<DeliverableInfo> currentDeliverableInfoStack = new LinkedList<>();
    private final DataLinkURLBuilder dataLinkURLBuilder;
    private final Iterator<String> datasetIDIterator;
    private final DataPacker dataPacker;
    private final String requestID;


    public DataLinkIterator(final DataLinkURLBuilder dataLinkURLBuilder, final Iterator<String> datasetIDIterator,
                            final DataPacker dataPacker, final String requestID) {
        this.dataLinkURLBuilder = dataLinkURLBuilder;
        this.datasetIDIterator = datasetIDIterator;
        this.dataPacker = dataPacker;
        this.requestID = requestID;
    }

    @Override
    public boolean hasNext() {
        return datasetIDIterator.hasNext() || !currentDeliverableInfoStack.isEmpty();
    }

    private boolean isMOUS(final DeliverableInfo deliverableInfo) {
        return deliverableInfo.getType() == Deliverable.MOUS;
    }

    private void visitSubDeliverables(final DeliverableInfo deliverableInfo) {
        if (!isMOUS(deliverableInfo)) {
            for (final DeliverableInfo nextDeliverableInfo : deliverableInfo.getSubDeliverables()) {
                visitSubDeliverables(nextDeliverableInfo);
            }
        } else {
            currentDeliverableInfoStack.addAll(deliverableInfo.getSubDeliverables());
        }
    }

    @Override
    public DataLink next() {
        final DataLink nextDataLink;
        if (currentDeliverableInfoStack.isEmpty()) {
            final String nextDatasetID = datasetIDIterator.next();
            final DeliverableInfo nextDeliverableInfo = dataPacker.expand(new Uid(nextDatasetID), false);

            visitSubDeliverables(nextDeliverableInfo);
            nextDataLink = this.next();
        } else {
            final DeliverableInfo deliverableInfo = currentDeliverableInfoStack.poll();
            final DataLink.Term term = deliverableInfo.getType().isAuxiliary()
                    ? DataLink.Term.AUXILIARY : DataLink.Term.THIS;
            nextDataLink = new DataLink(deliverableInfo.getIdentifier(), term);
            try {
                nextDataLink.accessURL = new URL(dataLinkURLBuilder.createDownloadURL(deliverableInfo, requestID));
            } catch (MalformedURLException e) {
                // If it's invalid, then just don't set it.
            }

            nextDataLink.contentLength = deliverableInfo.getSizeInBytes();
            nextDataLink.contentType = deliverableInfo.getType() == Deliverable.PIPELINE_AUXILIARY_README ?
                    README_CONTENT_TYPE : DEFAULT_CONTENT_TYPE;

            // TODO: How to determine this?
            nextDataLink.readable = true;
        }

        return nextDataLink;
    }
}
