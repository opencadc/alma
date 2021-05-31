/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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

package org.opencadc.alma.data;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Range;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.util.StringUtil;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.PixelRange;
import org.opencadc.soda.server.Cutout;

import java.util.List;


/**
 * Calculate an appropriate output filename based on some requested cutout specification.
 */
public class CutoutFileNameFormat {
    private static final String OUTPUT_DELIMITER = "_";
    private final String originalFileName;


    public CutoutFileNameFormat(final String originalFileName) {
        this.originalFileName = originalFileName;
    }

    /**
     * Obtain a new file name based on the provided cutout.
     * @param cutout    The cutout to use in naming.
     * @return      New filename String.  Never null.
     */
    public String format(final Cutout cutout) {
        if (cutout.pixelCutouts != null && !cutout.pixelCutouts.isEmpty()) {
            return format(cutout.pixelCutouts);
        } else if (cutout.pos != null) {
            return format(cutout.pos);
        } else {
            return this.originalFileName;
        }
    }

    /**
     * This is done by replacing values with underscores, and then
     * inserting this underscore value into the file name after the last period.
     * @param slices    The list of extension slice objects.
     * @return      New filename String.  Never null.
     */
    private String format(final List<ExtensionSlice> slices) {
        final StringBuilder appendage = new StringBuilder();

        for (final ExtensionSlice slice : slices) {
            if (slice.extensionIndex != null) {
                appendage.append(slice.extensionIndex);
            } else if (StringUtil.hasLength(slice.extensionName)) {
                appendage.append(slice.extensionName);
                if (slice.extensionVersion != null) {
                    appendage.append(OUTPUT_DELIMITER).append(slice.extensionVersion);
                }
            } else {
                // Assume the default extension, which is zero.
                appendage.append(0);
            }

            // Double underscore to separate extension from pixel ranges.
            appendage.append(OUTPUT_DELIMITER).append(OUTPUT_DELIMITER);

            for (final PixelRange pixelRange : slice.getPixelRanges()) {
                // Indicates ALL (*) value
                if (pixelRange.upperBound == Integer.MAX_VALUE) {
                    appendage.append(OUTPUT_DELIMITER);
                } else {
                    appendage.append(pixelRange.lowerBound).append(OUTPUT_DELIMITER).append(pixelRange.upperBound);
                }

                appendage.append(OUTPUT_DELIMITER);
            }

            appendage.append(OUTPUT_DELIMITER).append(OUTPUT_DELIMITER);
        }

        // Strip off last underscore.
        while (appendage.lastIndexOf(OUTPUT_DELIMITER) == (appendage.length() - 1)) {
            appendage.deleteCharAt(appendage.lastIndexOf(OUTPUT_DELIMITER));
        }

        final StringBuilder fileBuilder = new StringBuilder(this.originalFileName);
        fileBuilder.insert(fileBuilder.lastIndexOf(".") + 1, appendage + ".");

        return fileBuilder.toString();
    }

    private String format(final Shape shape) {
        if (shape instanceof Circle) {
            return format((Circle) shape);
        } else if (shape instanceof Polygon) {
            return format((Polygon) shape);
        } else if (shape instanceof Range) {
            return format((Range) shape);
        } else {
            return this.originalFileName;
        }
    }

    private String format(final Circle circle) {
        final StringBuilder fileBuilder = new StringBuilder(this.originalFileName);
        String appendage = OUTPUT_DELIMITER + OUTPUT_DELIMITER
                           + circle.getCenter().getLongitude() + OUTPUT_DELIMITER
                           + circle.getCenter().getLatitude() + OUTPUT_DELIMITER
                           + circle.getRadius() + OUTPUT_DELIMITER;
        fileBuilder.insert(fileBuilder.lastIndexOf(".") + 1, appendage + ".");

        return fileBuilder.toString();
    }

    private String format(final Polygon polygon) {
        final StringBuilder fileBuilder = new StringBuilder(this.originalFileName);
        final StringBuilder appendage = new StringBuilder();

        polygon.getVertices().forEach(v -> appendage.append(OUTPUT_DELIMITER).append(OUTPUT_DELIMITER)
                                                    .append(v.getLongitude()).append(OUTPUT_DELIMITER)
                                                    .append(v.getLatitude()));

        fileBuilder.insert(fileBuilder.lastIndexOf(".") + 1, appendage + ".");

        return fileBuilder.toString();
    }

    private String format(final Range range) {
        final StringBuilder fileBuilder = new StringBuilder(this.originalFileName);

        fileBuilder.append(OUTPUT_DELIMITER).append(OUTPUT_DELIMITER)
                   .append(range.getLongitude().getLower())
                   .append(OUTPUT_DELIMITER)
                   .append(range.getLongitude().getUpper())
                   .append(OUTPUT_DELIMITER).append(OUTPUT_DELIMITER)
                   .append(range.getLatitude().getLower())
                   .append(OUTPUT_DELIMITER)
                   .append(range.getLatitude().getUpper());

        fileBuilder.insert(fileBuilder.lastIndexOf(".") + 1, ".");

        return fileBuilder.toString();
    }
}
