/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2011.                            (c) 2011.
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
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package org.opencadc.tap.impl;

import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import ca.nrc.cadc.tap.AdqlQuery;
import ca.nrc.cadc.tap.expression.OracleExpressionDeParser;
import ca.nrc.cadc.tap.parser.OracleQuerySelectDeParser;
import ca.nrc.cadc.tap.parser.QuerySelectDeParser;
import ca.nrc.cadc.tap.parser.converter.OracleCeilingConverter;
import ca.nrc.cadc.tap.parser.converter.OracleRegionConverter;
import ca.nrc.cadc.tap.parser.converter.OracleSubstringConverter;
import ca.nrc.cadc.tap.parser.converter.TableNameConverter;
import ca.nrc.cadc.tap.parser.converter.TableNameReferenceConverter;
import ca.nrc.cadc.tap.parser.navigator.ExpressionNavigator;
import ca.nrc.cadc.tap.parser.navigator.SelectNavigator;


/**
 * TAP service implementors must implement this class and add customisations of the
 * navigatorList as shown below. Custom query visitors can be used to validate or modify
 * the query; the base class runs all the visitors in the navigatorList once before
 * converting the result into SQL for execution.
 *
 * @author pdowler
 */
public class AdqlQueryImpl extends AdqlQuery {

    public AdqlQueryImpl() {
        super();
    }

    @Override
    protected void init() {
        super.init();

        // TAP-1.1 tap_schema version is encoded in table names
        final TableNameConverter tnc = new TableNameConverter(true);
        tnc.put("ivoa.obscore", "alma.obscore");
        tnc.put("tap_schema.schemas", "tap_schema.schemas11");
        tnc.put("tap_schema.tables", "tap_schema.tables11");
        tnc.put("tap_schema.columns", "tap_schema.columns11");
        tnc.put("tap_schema.keys", "tap_schema.keys11");
        tnc.put("tap_schema.key_columns", "tap_schema.key_columns11");

        final TableNameReferenceConverter tnrc = new TableNameReferenceConverter(tnc.map);

        // For Oracle, the CEILING function is actually CEIL.
        navigatorList.add(new OracleCeilingConverter(new ExpressionNavigator(), tnrc, tnc));
        navigatorList.add(new OracleSubstringConverter(new ExpressionNavigator(), tnrc, tnc));
        navigatorList.add(new OracleRegionConverter(new ExpressionNavigator(), tnrc, tnc));
        navigatorList.add(new SelectNavigator(new ExpressionNavigator(), tnrc, tnc));

        // TODO: add more custom query visitors here
    }

    /**
     * Provide implementation of expression deparser if the default (BaseExpressionDeParser)
     * is not sufficient. For example, postgresql+pg_sphere requires the PgsphereDeParser to
     * support spoint and spoly. the default is to return a new BaseExpressionDeParser.
     *
     * @param dep The SelectDeParser used.
     * @param sb  StringBuffer to write to.
     * @return ExpressionDeParser implementation.  Never null.
     */
    @Override
    protected ExpressionDeParser getExpressionDeparser(SelectDeParser dep, StringBuffer sb) {
        return new OracleExpressionDeParser(dep, sb);
    }

    /**
     * Provide implementation of select deparser if the default (SelectDeParser) is not sufficient.
     *
     * @return  QuerySelectDeParser
     */
    @Override
    protected QuerySelectDeParser getSelectDeParser() {
        return new OracleQuerySelectDeParser();
    }
}
