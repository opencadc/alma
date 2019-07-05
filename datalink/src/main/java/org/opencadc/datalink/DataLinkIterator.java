
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

import ca.nrc.cadc.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Iterator to provide streaming over the DeliverableInfo items that the DataPacker expands.
 */
public class DataLinkIterator implements Iterator<DataLink> {

    private static final String DEFAULT_UNKNOWN_CONTENT_TYPE = "application/octet-stream";

    private final Queue<DeliverableInfo> currentDeliverableInfoStack = new LinkedList<>();
    private final DataLinkURLBuilder dataLinkURLBuilder;
    private final Iterator<String> datasetIDIterator;
    private final DataPacker dataPacker;
    private final String requestID;


    DataLinkIterator(final DataLinkURLBuilder dataLinkURLBuilder, final Iterator<String> datasetIDIterator,
                     final DataPacker dataPacker, final String requestID) {
        this.dataLinkURLBuilder = dataLinkURLBuilder;
        this.datasetIDIterator = datasetIDIterator;
        this.dataPacker = dataPacker;
        this.requestID = requestID;
    }

    @Override
    public boolean hasNext() {
        if (currentDeliverableInfoStack.isEmpty()) {
            if (datasetIDIterator.hasNext()) {
                final DeliverableInfo currentTopLevelDeliverableInfo =
                        dataPacker.expand(new Uid(datasetIDIterator.next()), false);
                visitSubDeliverables(currentTopLevelDeliverableInfo);
                return !currentDeliverableInfoStack.isEmpty();
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean isMOUS(final DeliverableInfo deliverableInfo) {
        return deliverableInfo.getType() == Deliverable.MOUS;
    }

    private void visitSubDeliverables(final DeliverableInfo deliverableInfo) {
        if (deliverableInfo != null) {
            if (!isMOUS(deliverableInfo)) {
                for (final DeliverableInfo nextDeliverableInfo : deliverableInfo.getSubDeliverables()) {
                    visitSubDeliverables(nextDeliverableInfo);
                }
            } else {
                // MOUS sub-deliverables should be TAR Files.  Add them, and their children (leaf files).
                for (final DeliverableInfo mousChildFile : deliverableInfo.getSubDeliverables()) {
                    if (mousChildFile != null) {
                        currentDeliverableInfoStack.add(mousChildFile);
                        final Stream<DeliverableInfo> filterStream =
                                mousChildFile.getSubDeliverables().stream().filter(Objects::nonNull);
                        currentDeliverableInfoStack.addAll(filterStream.collect(Collectors.toList()));
                    }
                }
            }
        }
    }

    /**
     * This next method will simply take from the current stack.
     *
     * @return DataLink     Next DataLink created from the next DeliverableInfo in the Queue.
     */
    @Override
    public DataLink next() {
        return createDataLink(Objects.requireNonNull(currentDeliverableInfoStack.poll()));
    }

    private DataLink createDataLink(final DeliverableInfo deliverableInfo) {
        final DataLink dataLink = new DataLink(deliverableInfo.getIdentifier(),
                                               determineTerm(deliverableInfo.getType()));
        try {
            dataLink.accessURL = new URL(dataLinkURLBuilder.createDownloadURL(deliverableInfo, requestID));
        } catch (MalformedURLException e) {
            // If it's invalid, then just don't set it.
        }

        dataLink.contentLength = determineSizeInBytes(deliverableInfo);
        dataLink.contentType = determineContentType(deliverableInfo);

        // TODO: How to determine this?
        dataLink.readable = true;

        return dataLink;
    }

    private Long determineSizeInBytes(final DeliverableInfo deliverableInfo) {
        final long configuredSizeInBytes = deliverableInfo.getSizeInBytes();
        return configuredSizeInBytes >= 0 ? configuredSizeInBytes : null;
    }

    private String determineContentType(final DeliverableInfo deliverableInfo) {
        final String displayFileName = deliverableInfo.getDisplayName();
        final String contentType;

        if (StringUtil.hasText(displayFileName)) {
            final String guessedContentType = URLConnection.guessContentTypeFromName(deliverableInfo.getDisplayName());
            contentType = StringUtil.hasText(guessedContentType) ? guessedContentType : DEFAULT_UNKNOWN_CONTENT_TYPE;
        } else {
            contentType = DEFAULT_UNKNOWN_CONTENT_TYPE;
        }

        return contentType;
    }

    private DataLink.Term determineTerm(final Deliverable deliverableType) {
        final DataLink.Term dataLinkTerm;
        if (deliverableType.isTarfile()) {
            dataLinkTerm = DataLink.Term.PKG;
        } else if (deliverableType.isAuxiliary()) {
            dataLinkTerm = DataLink.Term.AUXILIARY;
        } else {
            dataLinkTerm = DataLink.Term.THIS;
        }

        return dataLinkTerm;
    }
}
