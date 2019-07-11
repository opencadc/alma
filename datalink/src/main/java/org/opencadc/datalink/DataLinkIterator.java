
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.nrc.cadc.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;


/**
 * Iterator to provide streaming over the DeliverableInfo items that the DataPacker expands.
 */
public class DataLinkIterator implements Iterator<DataLink> {

    private static final Logger LOGGER = LogManager.getLogger(DataLinkIterator.class);

    private static final String DEFAULT_UNKNOWN_CONTENT_TYPE = "application/octet-stream";
    private static final String VOTABLE_CONTENT_TYPE = "application/x-votable+xml;content=datalink";

    private final Queue<DataLink> dataLinkQueue = new LinkedList<>();
    private final DataLinkURLBuilder dataLinkURLBuilder;
    private final Iterator<String> datasetIDIterator;
    private final DataPacker dataPacker;

    private DataLinkUID currentDataLinkUID;


    DataLinkIterator(final DataLinkURLBuilder dataLinkURLBuilder, final Iterator<String> datasetIDIterator,
                     final DataPacker dataPacker) {
        this.dataLinkURLBuilder = dataLinkURLBuilder;
        this.datasetIDIterator = datasetIDIterator;
        this.dataPacker = dataPacker;
    }

    @Override
    public boolean hasNext() {
        if (dataLinkQueue.isEmpty()) {
            if (datasetIDIterator.hasNext()) {
                this.currentDataLinkUID = new DataLinkUID(datasetIDIterator.next());
                final DeliverableInfo currentTopLevelDeliverableInfo = dataPacker.expand(
                        this.currentDataLinkUID.getArchiveUID(), false);

                final DeliverableInfo requestedDeliverableInfo = navigateToRequestedID(currentTopLevelDeliverableInfo);

                if (isDeliverableInfoNotFound(requestedDeliverableInfo)) {
                    dataLinkQueue.add(createNotFoundDataLink(requestedDeliverableInfo));
                } else {
                    visitSubDeliverables(requestedDeliverableInfo);
                }

                return !dataLinkQueue.isEmpty();
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean deliverableInfoMatches(final DeliverableInfo deliverableInfo) {
        final boolean matchesFlag;

        if (deliverableInfo.getType() == Deliverable.ASDM) {
            matchesFlag = currentDataLinkUID.getArchiveUID().equals(new Uid(deliverableInfo.getIdentifier()));
        } else {
            matchesFlag = currentDataLinkUID.getOriginalID().equals(getIdentifier(deliverableInfo));
        }

        return matchesFlag;
    }

    private DeliverableInfo navigateToRequestedID(final DeliverableInfo deliverableInfo) {
        DeliverableInfo di = deliverableInfo;
        final Set<DeliverableInfo> subDeliverables = di.getSubDeliverables();
        for (final Iterator<DeliverableInfo> subDeliverableIterator = subDeliverables.iterator();
             !deliverableInfoMatches(di) && subDeliverableIterator.hasNext(); ) {
            di = navigateToRequestedID(subDeliverableIterator.next());
        }

        return di;
    }

    private void visitSubDeliverables(final DeliverableInfo deliverableInfo) {
        if (deliverableInfo != null) {
            final Set<DeliverableInfo> subDeliverables = deliverableInfo.getSubDeliverables();
            for (final DeliverableInfo nextDeliverableInfo : subDeliverables) {
                dataLinkQueue.addAll(createDataLinks(nextDeliverableInfo));
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
        return dataLinkQueue.poll();
    }

    /**
     * Create all of the necessary DataLink entries for the given source DeliverableInfo.  Some files will produce
     * multiple DataLink entries.
     *
     * @param deliverableInfo The DeliverableInfo to create from.
     * @return Collection of DataLink entries.  Never null.
     */
    List<DataLink> createDataLinks(final DeliverableInfo deliverableInfo) {
        final List<DataLink> dataLinkCollection = new ArrayList<>();
        final Deliverable deliverableType = deliverableInfo.getType();

        dataLinkCollection.add(createDataLink(deliverableInfo));

        switch (deliverableType) {
            case PIPELINE_AUXILIARY_TARFILE:
                dataLinkCollection.add(createRecursiveDataLink(deliverableInfo, DataLink.Term.AUXILIARY));
                break;
            case PIPELINE_PRODUCT_TARFILE:
                dataLinkCollection.add(createRecursiveDataLink(deliverableInfo, DataLink.Term.THIS));
                break;
            case PIPELINE_PRODUCT:
                //if (deliverableInfo.getIdentifier().endsWith(".fits")) {
                //    dataLinkCollection.add(createCutoutDataLink(deliverableInfo));
                //} else {
                //    LOGGER.debug(String.format("No cutout available for %s.", deliverableInfo.getIdentifier()))
                //}
                break;
            default:
                LOGGER.debug(String.format("Nothing to add for %s.", deliverableInfo.getIdentifier()));
                break;
        }

        return dataLinkCollection;
    }

    private DataLink createNotFoundDataLink(final DeliverableInfo deliverableInfo) {
        final DataLink errorDataLink = new DataLink(deliverableInfo.getIdentifier(), DataLink.Term.ERROR);
        errorDataLink.errorMessage = String.format("NotFoundFault: %s", deliverableInfo.getIdentifier());

        return errorDataLink;
    }

    private boolean isDeliverableInfoNotFound(final DeliverableInfo deliverableInfo) {
        return deliverableInfo.getSizeInBytes() < 0;
    }

    private DataLink createDataLink(final DeliverableInfo deliverableInfo) {
        final List<DataLink.Term> dataLinkTerms = determineTerms(deliverableInfo);
        final DataLink.Term primarySemantic = dataLinkTerms.get(0);

        // Already got it.
        dataLinkTerms.remove(0);
        final DataLink dataLink;

        if (primarySemantic == DataLink.Term.DATALINK) {
            dataLink = createRecursiveDataLink(deliverableInfo, null);
        } else {
            //
            // TODO: Is it safe to assume that if the size is less than zero that the file doesn't exist?  This
            // TODO: makes that assumption.  If a UID that does not exist is passed in, the DataPacker will still
            // TODO: return a result, so we'll hide it here if the size is less than zero bytes.
            //
            // jenkinsd 2019.07.11
            //
            if (isDeliverableInfoNotFound(deliverableInfo)) {
                dataLink = createNotFoundDataLink(deliverableInfo);
            } else {
                dataLink = new DataLink(deliverableInfo.getIdentifier(), primarySemantic);
                try {
                    dataLink.accessURL = dataLinkURLBuilder.createDownloadURL(deliverableInfo);
                } catch (MalformedURLException e) {
                    LOGGER.warn("Access URL creation failed.", e);
                    dataLink.errorMessage = String.format("Unable to create access URL for %s.",
                                                          deliverableInfo.getIdentifier());
                }

                dataLink.contentLength = determineSizeInBytes(deliverableInfo);
                dataLink.contentType = determineContentType(deliverableInfo);

                // TODO: How to determine this?
                dataLink.readable = true;

                dataLinkTerms.forEach(dataLink::addSemantics);
            }
        }

        return dataLink;
    }

    private DataLink createRecursiveDataLink(final DeliverableInfo deliverableInfo, final DataLink.Term dataLinkTerm) {
        final DataLink dataLink = new DataLink(deliverableInfo.getIdentifier(), DataLink.Term.DATALINK);

        if (dataLinkTerm != null) {
            dataLink.addSemantics(dataLinkTerm);
        }

        try {
            dataLink.accessURL = createRecursiveURL(deliverableInfo);
        } catch (MalformedURLException e) {
            LOGGER.warn("Recursive URL creation failed.", e);
            dataLink.errorMessage = String.format("Unable to create recursive URL for %s.",
                                                  deliverableInfo.getIdentifier());
        }

        dataLink.contentType = VOTABLE_CONTENT_TYPE;

        return dataLink;
    }

    private URL createRecursiveURL(final DeliverableInfo deliverableInfo) throws MalformedURLException {
        return dataLinkURLBuilder.createRecursiveDataLinkURL(deliverableInfo);
    }

    private Long determineSizeInBytes(final DeliverableInfo deliverableInfo) {
        final long configuredSizeInBytes = deliverableInfo.getSizeInBytes();
        return configuredSizeInBytes >= 0 ? configuredSizeInBytes : null;
    }

    private String determineContentType(final DeliverableInfo deliverableInfo) {
        final String fileCheckInput = getIdentifier(deliverableInfo);
        final String contentType;

        if (StringUtil.hasText(fileCheckInput)) {
            final String guessedContentType = URLConnection.guessContentTypeFromName(fileCheckInput);
            contentType = StringUtil.hasText(guessedContentType) ? guessedContentType : DEFAULT_UNKNOWN_CONTENT_TYPE;
        } else {
            contentType = DEFAULT_UNKNOWN_CONTENT_TYPE;
        }

        return contentType;
    }

    private String getIdentifier(final DeliverableInfo deliverableInfo) {
        return deliverableInfo == null ? null : (deliverableInfo.getType() == Deliverable.ASDM ?
                                                 deliverableInfo.getDisplayName() : deliverableInfo.getIdentifier());
    }

    private List<DataLink.Term> determineTerms(final DeliverableInfo deliverableInfo) {
        final List<DataLink.Term> dataLinkTermCollection = new ArrayList<>();
        final Deliverable deliverableType = deliverableInfo.getType();

        if (isPackageFile(deliverableInfo)) {
            dataLinkTermCollection.add(DataLink.Term.PKG);
        }

        if (deliverableType.isAuxiliary()) {
            dataLinkTermCollection.add(DataLink.Term.AUXILIARY);
        }

        if (deliverableType == Deliverable.PIPELINE_AUXILIARY_CALIBRATION) {
            dataLinkTermCollection.add(DataLink.Term.CALIBRATION);
        } else if (deliverableType == Deliverable.ASDM) {
            dataLinkTermCollection.add(DataLink.Term.PROGENITOR);
        } else if (!deliverableType.isAuxiliary()) {
            dataLinkTermCollection.add(DataLink.Term.THIS);
        } else if (deliverableType.isOus()) {
            dataLinkTermCollection.add(DataLink.Term.DATALINK);
        }

        return dataLinkTermCollection;
    }

    private boolean isPackageFile(final DeliverableInfo deliverableInfo) {
        final String identifier = getIdentifier(deliverableInfo);
        return deliverableInfo.getType().isTarfile()
               || (StringUtil.hasLength(identifier) && identifier.trim().endsWith(".tar"));
    }
}
