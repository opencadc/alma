
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.alma.AlmaUID;
import org.opencadc.alma.deliverable.HierarchyItem;
import org.opencadc.alma.deliverable.RequestHandlerQuery;

import ca.nrc.cadc.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * Iterator to provide streaming over the DeliverableInfo items that the DataPacker expands.
 */
public class DataLinkIterator implements Iterator<DataLink> {

    private static final Logger LOGGER = LogManager.getLogger(DataLinkIterator.class);

    private static final String DEFAULT_UNKNOWN_CONTENT_TYPE = "application/octet-stream";
    private static final String VOTABLE_CONTENT_TYPE = "application/x-votable+xml;content=datalink";
    private static final String FITS_CONTENT_TYPE = "image/fits";

    private final Queue<DataLink> dataLinkQueue = new LinkedList<>();
    private final DataLinkURLBuilder dataLinkURLBuilder;
    private final Iterator<String> datasetIDIterator;
    private final RequestHandlerQuery requestHandlerQuery;


    DataLinkIterator(final DataLinkURLBuilder dataLinkURLBuilder, final Iterator<String> datasetIDIterator,
                     final RequestHandlerQuery requestHandlerQuery) {
        this.dataLinkURLBuilder = dataLinkURLBuilder;
        this.datasetIDIterator = datasetIDIterator;
        this.requestHandlerQuery = requestHandlerQuery;
    }

    @Override
    public boolean hasNext() {
        if (dataLinkQueue.isEmpty()) {
            if (datasetIDIterator.hasNext()) {
                final AlmaUID currentUID = new AlmaUID(datasetIDIterator.next());
                final HierarchyItem hierarchyItem = requestHandlerQuery.query(currentUID);

                if (hierarchyItem.hasChildren()) {
                    visitSubDeliverables(hierarchyItem);
                } else {
                    dataLinkQueue.add(createNotFoundDataLink(hierarchyItem));
                }

                return !dataLinkQueue.isEmpty();
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private void visitSubDeliverables(final HierarchyItem hierarchyItem) {
        Arrays.stream(hierarchyItem.getChildrenArray()).forEach(h -> {
            final HierarchyItem.Type type = h.getType();
            if (type.isLeaf() || type.isAuxiliary() || type.isOus() || type.isTarfile()) {
                dataLinkQueue.addAll(createDataLinks(h));
            }
        });
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
     * @param hierarchyItem The HierarchyItem to create from.
     * @return Collection of DataLink entries.  Never null.
     */
    List<DataLink> createDataLinks(final HierarchyItem hierarchyItem) {
        final List<DataLink> dataLinkCollection = new ArrayList<>();
        final HierarchyItem.Type deliverableType = hierarchyItem.getType();

        dataLinkCollection.add(createDataLink(hierarchyItem));

        switch (deliverableType) {
            case PIPELINE_AUXILIARY_TARFILE:
                dataLinkCollection.add(createRecursiveDataLink(hierarchyItem, DataLink.Term.AUXILIARY));
                break;
            case PIPELINE_PRODUCT_TARFILE:
                dataLinkCollection.add(createRecursiveDataLink(hierarchyItem, DataLink.Term.THIS));
                break;
            case PIPELINE_PRODUCT:
                if (getIdentifier(hierarchyItem).endsWith(".fits")) {
                    dataLinkCollection.add(createCutoutDataLink(hierarchyItem));
                } else {
                    LOGGER.debug(String.format("No cutout available for %s.", hierarchyItem.getNullSafeId()));
                }
                break;
            default:
                LOGGER.debug(String.format("Nothing to add for %s.", hierarchyItem.getNullSafeId()));
                break;
        }

        return dataLinkCollection;
    }

    private DataLink createNotFoundDataLink(final HierarchyItem hierarchyItem) {
        final DataLink errorDataLink = new DataLink(hierarchyItem.getNullSafeId(), DataLink.Term.ERROR);
        errorDataLink.errorMessage = String.format("NotFoundFault: %s", hierarchyItem.getNullSafeId());

        return errorDataLink;
    }

    private DataLink createDataLink(final HierarchyItem hierarchyItem) {
        final List<DataLink.Term> dataLinkTerms = determineTerms(hierarchyItem);
        final DataLink.Term primarySemantic = dataLinkTerms.get(0);

        // Already got it.
        dataLinkTerms.remove(0);
        final DataLink dataLink;

        if (primarySemantic == DataLink.Term.DATALINK) {
            dataLink = createRecursiveDataLink(hierarchyItem, null);
        } else {
            //
            // TODO: Is it safe to assume that if the size is less than zero that the file doesn't exist?  This
            // TODO: makes that assumption.  If a UID that does not exist is passed in, the DataPacker will still
            // TODO: return a result, so we'll hide it here if the size is less than zero bytes.
            //
            // jenkinsd 2019.07.11
            //
            if (!hierarchyItem.fileExists()) {
                dataLink = createNotFoundDataLink(hierarchyItem);
            } else {
                dataLink = new DataLink(hierarchyItem.getNullSafeId(), primarySemantic);
                try {
                    dataLink.accessURL = dataLinkURLBuilder.createDownloadURL(hierarchyItem);
                } catch (MalformedURLException e) {
                    LOGGER.warn("Access URL creation failed.", e);
                    dataLink.errorMessage = String.format("Unable to create access URL for %s.",
                                                          hierarchyItem.getNullSafeId());
                }

                dataLink.contentLength = determineSizeInBytes(hierarchyItem);
                dataLink.contentType = determineContentType(hierarchyItem);
                dataLink.readable = hierarchyItem.isReadable();

                dataLinkTerms.forEach(dataLink::addSemantics);
            }
        }

        return dataLink;
    }

    private DataLink createRecursiveDataLink(final HierarchyItem hierarchyItem, final DataLink.Term dataLinkTerm) {
        final DataLink dataLink = new DataLink(hierarchyItem.getNullSafeId(), DataLink.Term.DATALINK);

        if (dataLinkTerm != null) {
            dataLink.addSemantics(dataLinkTerm);
        }

        try {
            dataLink.accessURL = createRecursiveURL(hierarchyItem);
        } catch (MalformedURLException e) {
            LOGGER.warn("Recursive URL creation failed.", e);
            dataLink.errorMessage = String.format("Unable to create recursive URL for %s.",
                                                  hierarchyItem.getNullSafeId());
        }

        dataLink.contentType = VOTABLE_CONTENT_TYPE;

        return dataLink;
    }

    private DataLink createCutoutDataLink(final HierarchyItem hierarchyItem) {
        final DataLink dataLink = new DataLink(hierarchyItem.getNullSafeId(), DataLink.Term.CUTOUT);

        try {
            dataLink.accessURL = createCutoutURL(hierarchyItem);
        } catch (MalformedURLException e) {
            LOGGER.warn("Cutout URL creation failed.", e);
            dataLink.errorMessage = String.format("Unable to create Cutout URL for %s.",
                                                  hierarchyItem.getNullSafeId());
        }

        dataLink.contentType = FITS_CONTENT_TYPE;

        return dataLink;
    }

    private URL createRecursiveURL(final HierarchyItem hierarchyItem) throws MalformedURLException {
        return dataLinkURLBuilder.createRecursiveDataLinkURL(hierarchyItem);
    }

    private URL createCutoutURL(final HierarchyItem hierarchyItem) throws MalformedURLException {
        return dataLinkURLBuilder.createCutoutLinkURL(hierarchyItem);
    }

    private Long determineSizeInBytes(final HierarchyItem hierarchyItem) {
        final long configuredSizeInBytes = hierarchyItem.getSizeInBytes();
        return configuredSizeInBytes >= 0 ? configuredSizeInBytes : null;
    }

    private String determineContentType(final HierarchyItem hierarchyItem) {
        final String fileCheckInput = getIdentifier(hierarchyItem);
        final String contentType;

        if (StringUtil.hasText(fileCheckInput)) {
            final String guessedContentType = URLConnection.guessContentTypeFromName(fileCheckInput);
            contentType = StringUtil.hasText(guessedContentType) ? guessedContentType : DEFAULT_UNKNOWN_CONTENT_TYPE;
        } else {
            contentType = DEFAULT_UNKNOWN_CONTENT_TYPE;
        }

        return contentType;
    }

    private String getIdentifier(final HierarchyItem hierarchyItem) {
        return hierarchyItem == null ? null : (hierarchyItem.getType() == HierarchyItem.Type.ASDM
                                               ? hierarchyItem.getName()
                                               : hierarchyItem.getNullSafeId());
    }

    private List<DataLink.Term> determineTerms(final HierarchyItem hierarchyItem) {
        final List<DataLink.Term> dataLinkTermCollection = new ArrayList<>();
        final HierarchyItem.Type deliverableType = hierarchyItem.getType();

        if (isPackageFile(hierarchyItem)) {
            dataLinkTermCollection.add(DataLink.Term.PKG);
        }

        if (deliverableType.isAuxiliary()) {
            dataLinkTermCollection.add(DataLink.Term.AUXILIARY);
        }

        if (deliverableType == HierarchyItem.Type.PIPELINE_AUXILIARY_CALIBRATION) {
            dataLinkTermCollection.add(DataLink.Term.CALIBRATION);
        } else if (deliverableType == HierarchyItem.Type.ASDM) {
            dataLinkTermCollection.add(DataLink.Term.PROGENITOR);
        } else if (deliverableType.isOus()) {
            dataLinkTermCollection.add(DataLink.Term.DATALINK);
        } else if (!deliverableType.isAuxiliary()) {
            dataLinkTermCollection.add(DataLink.Term.THIS);
        }

        return dataLinkTermCollection;
    }

    private boolean isPackageFile(final HierarchyItem hierarchyItem) {
        final String identifier = getIdentifier(hierarchyItem);
        return hierarchyItem.getType().isTarfile()
               || (StringUtil.hasLength(identifier) && identifier.trim().endsWith(".tar"));
    }
}
