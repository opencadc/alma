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
import ca.nrc.cadc.dali.CommonParamValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validator for Cone Search parameters.
 */
public class ConeParameterValidator extends CommonParamValidator {
    public static final String VERB_PARAM = "VERB";
    public static final String RA_PARAM = "RA";
    public static final String DEC_PARAM = "DEC";
    public static final String SR_PARAM = "SR";

    public static final String MAXREC = "MAXREC";

    static final int MIN_VERB_VALUE = 1;
    static final int MID_VERB_VALUE = 2;
    static final int MAX_VERB_VALUE = 3;

    public Circle validateCone(final Map<String, List<String>> parameters) {
        final String raValue = getFirstParameter(ConeParameterValidator.RA_PARAM, parameters);
        final String decValue = getFirstParameter(ConeParameterValidator.DEC_PARAM, parameters);
        final String searchRadiusValue = getFirstParameter(ConeParameterValidator.SR_PARAM, parameters);
        final Map<String, List<String>> circleValidateParams = new HashMap<>();
        circleValidateParams.put(CommonParamValidator.CIRCLE, Collections.singletonList(
                String.format("%s %s %s", raValue, decValue, searchRadiusValue)));

        try {
            final List<Circle> validCircles = validateCircle(circleValidateParams);

            if (validCircles.isEmpty()) {
                throw new IllegalArgumentException("No valid input cone position center provided.");
            } else {
                return validCircles.get(0);
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Cannot create cone position center from non-numeric input.");
        }
    }

    public int validateVERB(final Map<String, List<String>> parameters) {
        // If not VERB provided, default to 2.
        if (parameters.get(VERB_PARAM) != null && !parameters.get(VERB_PARAM).isEmpty()) {
            final List<Integer> validIntegers = validateInteger(VERB_PARAM, parameters, Arrays.asList(MIN_VERB_VALUE,
                                                                                                      MID_VERB_VALUE,
                                                                                                      MAX_VERB_VALUE));
            if (validIntegers.isEmpty()) {
                throw new IllegalArgumentException("VERB must be 1, 2, or 3.");
            } else {
                return validIntegers.get(0);
            }
        } else {
            return MID_VERB_VALUE;
        }
    }

    int getMaxRec(final Map<String, List<String>> parameters, final int defaultValue, final int maxValue) {
        return validateInteger(ConeParameterValidator.MAXREC, parameters)
                .stream()
                .filter(i -> i <= maxValue)
                .filter(i -> i > 0)
                .findFirst()
                .orElse(defaultValue);
    }

    private List<Integer> validateInteger(final String verbParam, final Map<String, List<String>> parameters,
                                          final List<Integer> validValues) {
        return super.validateInteger(verbParam, parameters)
                    .stream()
                    .filter(validValues::contains)
                    .collect(Collectors.toList());
    }

    private String getFirstParameter(final String key, final Map<String, List<String>> requestParameters) {
        final List<String> values = requestParameters.get(key);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public String getResponseFormat(final Map<String, List<String>> parameters, final String contentType) {
        return validateString(ConeParameterValidator.RESPONSEFORMAT, parameters, null)
                .stream()
                .findFirst()
                .orElse(contentType);
    }
}
