/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
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

package org.opencadc.conesearch;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class to produce an ADQL query from a set of Parameters
 */
public class TAPQueryGenerator {
    private static final int DEF_MAXREC = 1000;
    private static final int MAX_MAXREC = Integer.MAX_VALUE;
    private static final Logger LOGGER = LogManager.getLogger(TAPQueryGenerator.class);

    protected final String tableName;
    protected final String positionColumnName;
    protected final String lowVerbositySelectList;
    protected final String midVerbositySelectList;
    protected final String highVerbositySelectList;
    protected final Map<String, List<String>> parameters;


    /**
     *
     * @param tableName          The name of the Catalog Table to query.
     * @param positionColumnName The name of the positional column to compare against the cone's position,
     *                           preferably spatially indexed.@param positionColumnName
     * @param lowVerbositySelectList    The query select list for VERB=1
     * @param midVerbositySelectList    The query select list for VERB=2
     * @param highVerbositySelectList    The query select list for VERB=3
     */
    public TAPQueryGenerator(final String tableName, final String positionColumnName,
                             final String lowVerbositySelectList, final String midVerbositySelectList,
                             final String highVerbositySelectList, final Map<String, List<String>> parameters) {
        if (!StringUtil.hasText(tableName) || !StringUtil.hasText(positionColumnName)
            || !StringUtil.hasText(lowVerbositySelectList) || !StringUtil.hasText(midVerbositySelectList)
            || !StringUtil.hasText(highVerbositySelectList) || parameters == null || parameters.isEmpty()) {
            throw new IllegalArgumentException("tableName, positionColumnName, lowVerbositySelectList, "
                                               + "midVerbositySelectList, highVerbositySelectList, and parameters "
                                               + "are all required.\n"
                                               + "(" + tableName + ", " + positionColumnName + ", "
                                               + lowVerbositySelectList + ", " + midVerbositySelectList + ", "
                                               + highVerbositySelectList + ", " + parameters + ")");
        }

        this.tableName = tableName;
        this.positionColumnName = positionColumnName;
        this.lowVerbositySelectList = lowVerbositySelectList;
        this.midVerbositySelectList = midVerbositySelectList;
        this.highVerbositySelectList = highVerbositySelectList;
        this.parameters = parameters;
    }

    /**
     * Map with supported Simple Cone Search 1.1 parameters.
     * <a href="https://www.ivoa.net/documents/ConeSearch/20200828/WD-ConeSearch-1.1-20200828.html#tth_sEc2.1">...</a>
     *
     * @return map of parameter names and values
     */
    public Map<String, Object> getParameterMap() {
        final Map<String, Object> queryParameterMap = new HashMap<>();
        final ConeParameterValidator coneParameterValidator = new ConeParameterValidator();

        // Obtain and, if necessary, provide a default RESPONSEFORMAT.
        queryParameterMap.put(ConeParameterValidator.RESPONSEFORMAT,
                              coneParameterValidator.getResponseFormat(parameters, VOTableWriter.CONTENT_TYPE));

        // Obtain and validate the VERB (verbosity) output.
        final int outputVerbosity = coneParameterValidator.validateVERB(parameters);

        // Obtain and validate the MAXREC value with a default if necessary.
        final int maxRecordCount = coneParameterValidator.getMaxRec(parameters, TAPQueryGenerator.DEF_MAXREC,
                                                                    TAPQueryGenerator.MAX_MAXREC);
        queryParameterMap.put(ConeParameterValidator.MAXREC, maxRecordCount);

        queryParameterMap.put("LANG", "ADQL");
        final String query = getQuery(outputVerbosity, coneParameterValidator.validateCone(parameters));
        LOGGER.debug("Cone Search TAP query:\n" + query);
        queryParameterMap.put("QUERY", query);
        return queryParameterMap;
    }

    private String getQuery(final int outputVerbosity, final Circle circle) {
        return "SELECT "
               + getSelectList(outputVerbosity)
               + " FROM "
               + this.tableName
               + " WHERE 1 = CONTAINS("
               + this.positionColumnName
               + ", "
               + "CIRCLE('ICRS', "
               + circle.getCenter().getLongitude()
               + ", "
               + circle.getCenter().getLatitude()
               + ", "
               + circle.getRadius()
               + "))";
    }


    private String getSelectList(final int outputVerbosity) {
        switch (outputVerbosity) {
            case ConeParameterValidator.MIN_VERB_VALUE: {
                return getLowVerbositySelectList();
            }

            case ConeParameterValidator.MAX_VERB_VALUE: {
                return getHighVerbositySelectList();
            }

            // Default output is MID (2)
            default: {
                return getMidVerbositySelectList();
            }
        }
    }

    /**
     * Obtain a select list for the Low Verbosity (1).
     * @return  String select columns, or "*", never null.
     *          Example: "ra, dec, footprint"
     */
    public String getLowVerbositySelectList() {
        return this.lowVerbositySelectList;
    }

    /**
     * Obtain a select list for the Medium Verbosity (2), which is the default.
     * @return  String select columns, or "*", never null.
     *          Example: "obs_id, release_date, ra, dec, footprint"
     */
    public String getMidVerbositySelectList() {
        return this.midVerbositySelectList;
    }

    /**
     * Obtain a select list for the High Verbosity (3).
     * @return  String select columns, or "*", never null.
     *          Example: "*"
     */
    public String getHighVerbositySelectList() {
        return this.highVerbositySelectList;
    }
}
