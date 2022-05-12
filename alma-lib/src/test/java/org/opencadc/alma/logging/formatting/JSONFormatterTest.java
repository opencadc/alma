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

package org.opencadc.alma.logging.formatting;

import ca.nrc.cadc.date.DateUtil;
import org.junit.Test;
import org.opencadc.alma.logging.LoggingEventKey;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JSONFormatterTest {
    @Test
    public void asStringEmpty() {
        final Map<LoggingEventKey, Object> loggingEventKeyObjectMap = new HashMap<>();
        final JSONFormatter testSubject = new JSONFormatter();

        JSONAssert.assertEquals("Wrong empty JSON.", "{}",
                                testSubject.asString(loggingEventKeyObjectMap), true);
    }

    @Test
    public void asStringRequired() {
        final Map<LoggingEventKey, Object> loggingEventKeyObjectMap = new HashMap<>();
        final JSONFormatter testSubject = new JSONFormatter();

        Arrays.stream(LoggingEventKey.values())
              .filter(LoggingEventKey::isRequired)
              .forEach(key -> loggingEventKeyObjectMap.put(key, key.toString().toLowerCase(Locale.ROOT)));

        JSONAssert.assertEquals("Wrong required JSON.",
                                "{\"duration\": \"duration\", \"ip\": \"ip_address\", "
                                + "\"program\": \"program\", \"query\": \"query\", \"userAgent\": \"user_agent\", "
                                + "\"version\": \"version\"}",
                                testSubject.asString(loggingEventKeyObjectMap), true);
    }

    @Test
    public void asString() {
        final Map<LoggingEventKey, Object> loggingEventKeyObjectMap = new HashMap<>();
        final JSONFormatter testSubject = new JSONFormatter();
        final Calendar calendar = Calendar.getInstance(DateUtil.UTC);

        calendar.set(1977, Calendar.NOVEMBER, 25, 1, 13, 5);
        calendar.set(Calendar.MILLISECOND, 0);

        loggingEventKeyObjectMap.put(LoggingEventKey.DURATION, 88L);
        loggingEventKeyObjectMap.put(LoggingEventKey.IP_ADDRESS, "123.456.789.1");
        loggingEventKeyObjectMap.put(LoggingEventKey.VERSION, "4.4");
        loggingEventKeyObjectMap.put(LoggingEventKey.PROGRAM, "CADC");
        loggingEventKeyObjectMap.put(LoggingEventKey.QUERY, "https://site.com/ivoa/soda?CIRCLE=12.3+33.3+0.5");
        loggingEventKeyObjectMap.put(LoggingEventKey.USER_AGENT, "Mozilla");
        loggingEventKeyObjectMap.put(LoggingEventKey.START_DATE, calendar.getTime());

        JSONAssert.assertEquals("Wrong required JSON.",
                                "{\"duration\": 88, \"ip\": \"123.456.789.1\", \"program\": \"CADC\", "
                                + "\"query\": \"https://site.com/ivoa/soda?CIRCLE=12.3+33.3+0.5\", "
                                + "\"userAgent\": \"Mozilla\", \"version\": \"4.4\", \"startDate\": "
                                + "\"1977-11-25T01:13:05.000Z\"}",
                                testSubject.asString(loggingEventKeyObjectMap), true);
    }
}
