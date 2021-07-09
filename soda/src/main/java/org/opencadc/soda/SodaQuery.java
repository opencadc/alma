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

package org.opencadc.soda;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Range;
import ca.nrc.cadc.dali.util.CircleFormat;
import ca.nrc.cadc.dali.util.IntervalFormat;
import ca.nrc.cadc.dali.util.PolarizationStateFormat;
import ca.nrc.cadc.dali.util.PolygonFormat;
import ca.nrc.cadc.dali.util.RangeFormat;
import ca.nrc.cadc.dali.util.ShapeFormat;

import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.net.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.AlmaUID;
import org.opencadc.alma.deliverable.RequestHandlerQuery;
import org.opencadc.soda.server.Cutout;


public class SodaQuery extends RequestHandlerQuery {
    private static final Logger LOGGER = LogManager.getLogger(SodaQuery.class);

    private static final String JSON_ERROR_MESSAGE_KEY = "error";
    private static final String JSON_ERROR_STATUS_KEY = "status";
    private static final String JSON_PATH_KEY = "path";
    private static final String JSON_SERVER_NAME_KEY = "serverName";


    public SodaQuery(final AlmaProperties almaProperties) {
        super(almaProperties);
    }

    /**
     * Request the absolute path on disk of the given ALMA UID.  This will make a request to the Request Handler
     * service's location endpoint.
     *
     * @param almaUID       The UID to look up.
     * @return              URL to the SODA endpoint of the file.
     * @throws IOException  For any service URL lookup errors.
     * @throws ResourceNotFoundException    If no service could be located.
     */
    public URL locateFile(final AlmaUID almaUID) throws IOException, ResourceNotFoundException {
        final URL baseServiceURL = this.almaProperties.lookupRequestHandlerURL();
        LOGGER.debug(String.format("Using Base Request Handler URL %s", baseServiceURL));
        final URL downwardsQueryURL = new URL(String.format("%s/data/%s/location",
                                                            baseServiceURL.toExternalForm(),
                                                            almaUID.getSanitisedUid()));

        try (final InputStream jsonStream = jsonStream(downwardsQueryURL)) {
            final JSONObject jsonObject = new JSONObject(new JSONTokener(jsonStream));
            if (jsonObject.keySet().contains(JSON_ERROR_MESSAGE_KEY)) {
                throw new IllegalArgumentException(String.format("Error trying to resolve %s to an absolute path:"
                                                                 + "\nStatus: %d\nMessage: %s",
                                                                 almaUID, jsonObject.getInt(JSON_ERROR_STATUS_KEY),
                                                                 jsonObject.getString(JSON_ERROR_MESSAGE_KEY)));
            } else {
                final String serverName = jsonObject.getString(JSON_SERVER_NAME_KEY);
                final String path = jsonObject.getString(JSON_PATH_KEY);

                return new URL(String.format("http://%s:%s/data/files?file=%s", serverName,
                                             this.almaProperties.getFileSodaServicePort(), NetUtil.encode(path)));
            }
        }
    }

    InputStream jsonStream(final URL url) throws IOException, ResourceNotFoundException {
        final HttpGet httpGet = createHttpGet(url);
        httpGet.run();

        final Throwable throwable = httpGet.getThrowable();
        if (throwable != null) {
            if (throwable instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) throwable;
            } else if (throwable instanceof AccessControlException) {
                throw (AccessControlException) throwable;
            } else {
                throw new IOException("Unable to locate file.\n" + throwable.getMessage(), throwable);
            }
        } else {
            return httpGet.getInputStream();
        }
    }

    public URL toCutoutURL(final AlmaUID almaUID, final Cutout cutout) throws IOException, ResourceNotFoundException {
        return toCutoutURL(locateFile(almaUID), cutout);
    }

    private URL toCutoutURL(final URL downloadURL, final Cutout cutout) throws MalformedURLException {
        if ((cutout == null) || isEmpty(cutout)) {
            return downloadURL;
        } else {
            final StringBuilder cutoutURLString = new StringBuilder(downloadURL.toExternalForm());

            if (cutout.pos != null) {
                if (cutout.pos instanceof Circle) {
                    final CircleFormat f = new CircleFormat();
                    appendQuery(cutoutURLString, SodaParameter.CIRCLE, f.format((Circle) cutout.pos));
                } else if (cutout.pos instanceof Polygon) {
                    final PolygonFormat f = new PolygonFormat();
                    appendQuery(cutoutURLString, SodaParameter.POLYGON, f.format((Polygon) cutout.pos));
                } else if (cutout.pos instanceof Range) {
                    final RangeFormat f = new RangeFormat(true);
                    appendQuery(cutoutURLString, SodaParameter.POS, "RANGE " + f.format((Range) cutout.pos));
                } else {
                    final ShapeFormat f = new ShapeFormat();
                    appendQuery(cutoutURLString, SodaParameter.POS, f.format(cutout.pos));
                }
            }

            if (cutout.band != null) {
                final IntervalFormat f = new IntervalFormat();
                appendQuery(cutoutURLString, SodaParameter.BAND, f.format(cutout.band));
            }

            if (cutout.time != null) {
                IntervalFormat f = new IntervalFormat();
                appendQuery(cutoutURLString, SodaParameter.TIME, f.format(cutout.time));
            }

            if (cutout.pol != null && !cutout.pol.isEmpty()) {
                final PolarizationStateFormat f = new PolarizationStateFormat();
                cutout.pol.forEach(polarizationState -> appendQuery(cutoutURLString, SodaParameter.POL,
                                                                    f.format(polarizationState)));
            }

            if (cutout.pixelCutouts != null && !cutout.pixelCutouts.isEmpty()) {
                final ExtensionSliceFormat f = new ExtensionSliceFormat();
                cutout.pixelCutouts.forEach(pixelCutout -> appendQuery(cutoutURLString, SodaParameter.SUB,
                                                                       f.format(pixelCutout)));
            }

            LOGGER.debug(String.format("CutoutURL from %s is %s.", cutout, cutoutURLString));

            return new URL(cutoutURLString.toString());
        }
    }

    private boolean isEmpty(final Cutout cutout) {
        return cutout.pos == null && cutout.band == null && cutout.time == null
               && (cutout.pol == null || cutout.pol.isEmpty())
               && cutout.custom == null && cutout.customAxis == null
               && (cutout.pixelCutouts == null || cutout.pixelCutouts.isEmpty());
    }

    private void appendQuery(final StringBuilder cutoutURLStringBuilder, final SodaParameter key, final String value) {
        if (cutoutURLStringBuilder.indexOf("?") > 0) {
            cutoutURLStringBuilder.append("&");
        } else {
            cutoutURLStringBuilder.append("?");
        }

        cutoutURLStringBuilder.append(key.name()).append("=").append(NetUtil.encode(value));
    }
}
