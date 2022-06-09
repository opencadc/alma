
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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import ca.nrc.cadc.reg.Standards;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.AlmaUID;

import ca.nrc.cadc.util.StringUtil;


/**
 * Iterator to provide streaming over the DeliverableInfo items that the DataPacker expands.
 */
public class DataLinkIterator implements Iterator<DataLink> {

    private static final Logger LOGGER = LogManager.getLogger(DataLinkIterator.class);

    private static final String DEFAULT_UNKNOWN_CONTENT_TYPE = "application/octet-stream";
    private static final String FITS_CONTENT_TYPE = "image/fits";

    private final Queue<DataLink> dataLinkQueue = new LinkedList<>();
    private final DataLinkURLBuilder dataLinkURLBuilder;
    private final Iterator<String> datasetIDIterator;
    private final DataLinkQuery dataLinkQuery;
    private final AlmaProperties almaProperties;


    DataLinkIterator(final DataLinkURLBuilder dataLinkURLBuilder, final Iterator<String> datasetIDIterator,
                     final DataLinkQuery dataLinkQuery, final AlmaProperties almaProperties) {
        this.dataLinkURLBuilder = dataLinkURLBuilder;
        this.datasetIDIterator = datasetIDIterator;
        this.dataLinkQuery = dataLinkQuery;
        this.almaProperties = almaProperties;
    }

    @Override
    public boolean hasNext() {
        if (dataLinkQueue.isEmpty()) {
            if (datasetIDIterator.hasNext()) {
                final AlmaUID currentUID = new AlmaUID(datasetIDIterator.next());
                final HierarchyItem hierarchyItem = dataLinkQuery.query(currentUID);

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
        Arrays.stream(hierarchyItem.getChildrenArray()).filter(h -> {
            final HierarchyItem.Type type = h.getType();
            final boolean isAccepted = type.isLeaf() || type.isOus() || type.isTarfile() || type.isAuxiliary()
                                       || type.isDocumentation();
            if (LOGGER.isDebugEnabled() && !isAccepted) {
                LOGGER.debug(String.format("Rejecting %s.", h.getUidString()));
            }
            return isAccepted;
        }).forEach(h -> dataLinkQueue.addAll(createDataLinks(h)));
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

        if (!hierarchyItem.fileExists()) {
            dataLinkCollection.add(createNotFoundDataLink(hierarchyItem));
        } else {
            final DataLink.Term dataLinkTerm = determineSemantic(hierarchyItem);
            final DataLink dataLink = createDataLink(hierarchyItem, dataLinkTerm);
            dataLinkCollection.add(dataLink);

            try {
                if (hierarchyItem.hasChildren()) {
                    final DataLink recursiveDataLink = new DataLink(hierarchyItem.getUidString(), dataLinkTerm);
                    final ServiceDescriptor serviceDescriptor =
                            new ServiceDescriptor(createRecursiveURL(hierarchyItem));
                    final String[] pathSegments = dataLink.accessURL.getPath().split("/");
                    // Use just the filename.
                    serviceDescriptor.id = String.format("DataLink.%s", pathSegments[pathSegments.length - 1]);
                    serviceDescriptor.standardID = Standards.DATALINK_LINKS_10;

                    recursiveDataLink.descriptor = serviceDescriptor;
                    recursiveDataLink.serviceDef = serviceDescriptor.id;
                    recursiveDataLink.contentType = "application/x-votable+xml;content=datalink";

                    setDescription(dataLink, null, null);

                    dataLinkCollection.add(recursiveDataLink);
                }
            } catch (MalformedURLException e) {
                LOGGER.error("Access URL creation failed.", e);
                dataLink.errorMessage = String.format("Unable to create Service Descriptor URL for %s/%s|%s.",
                                                      hierarchyItem.getSubDirectory(), hierarchyItem.getNullSafeId(),
                                                      hierarchyItem.getFileClass());
            }
        }

        if (deliverableType == HierarchyItem.Type.PIPELINE_PRODUCT) {
            if (getIdentifier(hierarchyItem).endsWith(".fits")) {
                dataLinkCollection.add(createCutoutDataLink(hierarchyItem));
            } else {
                LOGGER.debug(String.format("No cutout available for %s.", hierarchyItem.getNullSafeId()));
            }
        } else {
            LOGGER.debug(String.format("Nothing to add for %s.", hierarchyItem.getNullSafeId()));
        }

        return dataLinkCollection;
    }

    private DataLink createNotFoundDataLink(final HierarchyItem hierarchyItem) {
        final DataLink errorDataLink = new DataLink(hierarchyItem.getUidString(), DataLink.Term.ERROR);
        errorDataLink.errorMessage = String.format("NotFoundFault: %s", hierarchyItem.getNullSafeId());
        errorDataLink.contentType = "text/plain";
        errorDataLink.contentLength = (long) errorDataLink.errorMessage.getBytes(StandardCharsets.UTF_8).length;
        errorDataLink.description = "Indicates an error was found while generating this document.";

        return errorDataLink;
    }

    private DataLink createDataLink(final HierarchyItem hierarchyItem, final DataLink.Term semantic) {
        final DataLink dataLink;

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
            dataLink = new DataLink(hierarchyItem.getUidString(), semantic);
            try {
                dataLink.accessURL = dataLinkURLBuilder.createDownloadURL(hierarchyItem);
                dataLink.contentLength = determineSizeInBytes(hierarchyItem);
                dataLink.contentType = determineContentType(hierarchyItem);
                dataLink.readable = hierarchyItem.isReadable();
                dataLink.description = getDescription(dataLink, hierarchyItem.getSubDirectory(),
                                                      hierarchyItem.getFileClass());

                final String subDirectory = hierarchyItem.getSubDirectory();
                final String fileClass = hierarchyItem.getFileClass();

                setDescription(dataLink, subDirectory, fileClass);
            } catch (MalformedURLException e) {
                LOGGER.error("Access URL creation failed.", e);
                dataLink.errorMessage = String.format("Unable to create access URL for %s/%s|%s.",
                                                      hierarchyItem.getSubDirectory(), hierarchyItem.getNullSafeId(),
                                                      hierarchyItem.getFileClass());
            }
        }

        return dataLink;
    }

    private void setDescription(final DataLink dataLink, final String subDirectory, final String fileClass) {
        final StringBuilder descriptionID = new StringBuilder();

        if (StringUtil.hasText(subDirectory)) {
            descriptionID.append(subDirectory).append("/");
        }

        descriptionID.append(dataLink.getID());

        if (StringUtil.hasText(fileClass)) {
            descriptionID.append("|").append(fileClass);
        }

        if (dataLink.getSemantics().contains(DataLink.Term.PACKAGE)) {
            final String appendage;
            if (dataLink.descriptor == null) {
                appendage = "";
            } else {
                appendage = ", or follow the service_def field value to access files directly";
            }

            dataLink.description = String.format("Download all data associated with %s%s.", descriptionID,
                                                 appendage);
        } else if (dataLink.getSemantics().contains(DataLink.Term.DOCUMENTATION)) {
            dataLink.description = String.format("Download documentation for %s.", descriptionID);
        } else {
            // Assumes #this
            dataLink.description = String.format("Download dataset of type: %s, and class: %s.",
                                                 subDirectory,
                                                 (StringUtil.hasText(fileClass) ? fileClass : "N/A"));
        }
    }

    private DataLink createCutoutDataLink(final HierarchyItem hierarchyItem) {
        final DataLink dataLink = new DataLink(hierarchyItem.getUidString(), DataLink.Term.CUTOUT);

        try {
            final URL accessURL = createCutoutURL(hierarchyItem);
            final String[] pathSegments = accessURL.getPath().split("/");
            final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(accessURL);
            serviceDescriptor.id = String.format("SODA.%s", pathSegments[pathSegments.length - 1]);
            serviceDescriptor.standardID = Standards.SODA_SYNC_10;
            serviceDescriptor.resourceIdentifier = almaProperties.getSodaServiceURI();

            dataLink.contentType = FITS_CONTENT_TYPE;
            dataLink.descriptor = serviceDescriptor;
            dataLink.serviceDef = serviceDescriptor.id;
            dataLink.description = String.format("Synchronous SODA sub-image of %s.", hierarchyItem.getUidString());
        } catch (MalformedURLException e) {
            LOGGER.warn("Cutout URL creation failed.", e);
            dataLink.errorMessage = String.format("Unable to create Cutout URL for %s.",
                                                  hierarchyItem.getUidString());
        }

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

    private String getDescription(final DataLink dataLink, final String subDirectory, final String fileClass) {
        final StringBuilder descriptionID = new StringBuilder();

        if (StringUtil.hasText(subDirectory)) {
            descriptionID.append(subDirectory).append("/");
        }

        descriptionID.append(dataLink.getID());

        if (StringUtil.hasText(fileClass)) {
            descriptionID.append("|").append(fileClass);
        }

        final String description;
        if (dataLink.getSemantics().contains(DataLink.Term.PACKAGE)) {
            description = String.format("Download all data associated with %s.", descriptionID);
        } else if (dataLink.getID().toLowerCase(Locale.ROOT).contains("readme")) {
            description = String.format("Download documentation for %s.", descriptionID);
        } else {
            // Assumes #this
            description = String.format("Download the dataset for %s.", descriptionID);
        }

        return description;
    }

    private String getIdentifier(final HierarchyItem hierarchyItem) {
        return hierarchyItem == null ? null : (hierarchyItem.getType() == HierarchyItem.Type.ASDM
                                               ? hierarchyItem.getName()
                                               : hierarchyItem.getNullSafeId());
    }

    private DataLink.Term determineSemantic(final HierarchyItem hierarchyItem) {
        final HierarchyItem.Type deliverableType = hierarchyItem.getType();
        final DataLink.Term semantic;

        if (deliverableType.isAuxiliary()) {
            semantic = DataLink.Term.AUXILIARY;
        } else if (deliverableType == HierarchyItem.Type.PIPELINE_AUXILIARY_CALIBRATION) {
            semantic = DataLink.Term.CALIBRATION;
        } else if (deliverableType == HierarchyItem.Type.ASDM) {
            semantic = DataLink.Term.PROGENITOR;
        } else if (deliverableType.isDocumentation()) {
            semantic = DataLink.Term.DOCUMENTATION;
        } else if (isPackageFile(hierarchyItem)
                   && hierarchyItem.getType() != HierarchyItem.Type.PIPELINE_PRODUCT_TARFILE) {
            // Special case as the PIPELINE_PRODUCT_TARFILE is actually #this.
            semantic = DataLink.Term.PACKAGE;
        } else {
            semantic = DataLink.Term.THIS;
        }

        return semantic;
    }

    private boolean isPackageFile(final HierarchyItem hierarchyItem) {
        final String identifier = getIdentifier(hierarchyItem);
        return hierarchyItem.getType().isTarfile()
               || (StringUtil.hasLength(identifier) && identifier.trim().endsWith(".tar"));
    }
}
