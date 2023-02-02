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

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.Assert;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.AlmaUID;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.PropertiesReader;

import static org.mockito.Mockito.*;


public class DataLinkIteratorTest {

    public DataLinkIteratorTest() {
        Log4jInit.setLevel("org.opencadc.datalink", Level.DEBUG);
    }

    static class DataLinkComparator implements Comparator<DataLink> {

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         * The implementor must ensure that {@code sgn(compare(x, y)) ==
         * -sgn(compare(y, x))} for all {@code x} and {@code y}.  (This
         * implies that {@code compare(x, y)} must throw an exception if and only
         * if {@code compare(y, x)} throws an exception.)<p>
         * The implementor must also ensure that the relation is transitive:
         * {@code ((compare(x, y)>0) && (compare(y, z)>0))} implies
         * {@code compare(x, z)>0}.<p>
         * Finally, the implementor must ensure that {@code compare(x, y)==0}
         * implies that {@code sgn(compare(x, z))==sgn(compare(y, z))} for all
         * {@code z}.<p>
         * It is generally the case, but <i>not</i> strictly required that
         * {@code (compare(x, y)==0) == (x.equals(y))}.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."<p>
         * In the foregoing description, the notation
         * {@code sgn(}<i>expression</i>{@code )} designates the mathematical
         * <i>signum</i> function, which is defined to return one of {@code -1},
         * {@code 0}, or {@code 1} according to whether the value of
         * <i>expression</i> is negative, zero, or positive, respectively.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * first argument is less than, equal to, or greater than the
         * second.
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(DataLink o1, DataLink o2) {
            int comparison;
            final int compareID = o1.getID().compareTo(o2.getID());
            if (compareID == 0) {
                final boolean semanticsMatch = o2.getSemantics().equals(o1.getSemantics());
                comparison = semanticsMatch ? 0 : -1;
                final boolean accessURLMatches = Objects.equals(o1.accessURL, o2.accessURL);
                final int accessURLComparison = accessURLMatches ? 0 :
                                                Objects.compare(o1.accessURL, o2.accessURL,
                                                                (o11, o21) -> {
                                                                    if (o11 == o21) {
                                                                        return 0;
                                                                    } else if (o11 == null) {
                                                                        return -1;
                                                                    } else if (o21 == null) {
                                                                        return 1;
                                                                    } else {
                                                                        return o11.toExternalForm().compareTo(
                                                                                o21.toExternalForm());
                                                                    }
                                                                });
                if (comparison == 0) {
                    comparison = accessURLComparison;
                }

                final boolean contentTypeMatches = Objects.equals(o1.contentType, o2.contentType);
                final int contentTypeComparison = contentTypeMatches ? 0 :
                                                  Objects.compare(o1.contentType, o2.contentType,
                                                                  (o11, o21) -> {
                                                                      if (o11 == null && o21 == null) {
                                                                          return 0;
                                                                      } else if (o11 == null) {
                                                                          return -1;
                                                                      } else if (o21 == null) {
                                                                          return 1;
                                                                      } else {
                                                                          return o11.compareTo(o21);
                                                                      }
                                                                  });

                if (comparison == 0) {
                    comparison = contentTypeComparison;
                }
            } else {
                comparison = compareID;
            }

            return comparison;
        }
    }

    @Test
    public void runThroughFiltering() throws Throwable {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");
        final AlmaProperties mockAlmaProperties = mock(AlmaProperties.class);
        final DataLinkQuery mockDataLinkQuery = mock(DataLinkQuery.class);
        final Iterator<String> dataSetIDIterator = Collections.singletonList("uid://A001/X879/X8f1").iterator();
        final AlmaUID uid = new AlmaUID("uid://A001/X879/X8f1");
        final URL datalinkURL = new URL("https://alma.com/datalink");
        final URL sodaURL = new URL("https://alma.com/soda");
        final URL dataPortalURL = new URL("https://alma.com/dataportal");
        final URI sodaURI = URI.create("ivo://alma.com/soda");

        final HierarchyItem hierarchy = fromJSONFile(uid, DataLinkIteratorTest.class.getSimpleName() + ".json");
        when(mockDataLinkQuery.query(uid)).thenReturn(hierarchy);
        when(mockAlmaProperties.lookupDataLinkServiceURL()).thenReturn(datalinkURL);
        when(mockAlmaProperties.lookupSodaServiceURL()).thenReturn(sodaURL);
        when(mockAlmaProperties.lookupDataPortalURL()).thenReturn(dataPortalURL);
        when(mockAlmaProperties.getSodaServiceURI()).thenReturn(sodaURI);

        final DataLinkURLBuilder dataLinkURLBuilder = new DataLinkURLBuilder(mockAlmaProperties);
        final List<DataLink> expectedDataLinks = new ArrayList<>();
        final List<DataLink> resultDataLinks = new ArrayList<>();
        new DataLinkIterator(dataLinkURLBuilder, dataSetIDIterator, mockDataLinkQuery, mockAlmaProperties)
                .forEachRemaining(resultDataLinks::add);
        final String itemFileNameTemplate = "%s.%d.json";

        int index = 1;
        final HierarchyItem hierarchyItemOne =
                fromJSONFile(uid, String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(),
                                                index++));
        expectedDataLinks.add(createDataLink(hierarchyItemOne, "text/plain",
                                             new URL(String.format("%s/%s", dataPortalURL.toExternalForm(),
                                                                   hierarchyItemOne.getName())),
                                             DataLink.Term.DOCUMENTATION));

        final HierarchyItem hierarchyItemTwo =
                fromJSONFile(uid, String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(),
                                                index++));
        final DataLink thisLink = createDataLink(hierarchyItemTwo, "application/x-tar",
                                                 new URL(String.format("%s/%s", dataPortalURL.toExternalForm(),
                                                                       hierarchyItemTwo.getName())),
                                                 DataLink.Term.THIS);
        final DataLink recursiveThisLink = createDataLink(hierarchyItemTwo,
                                                          "application/x-votable+xml;content=datalink",
                                                          null, DataLink.Term.THIS);
        final ServiceDescriptor serviceDescriptor =
                new ServiceDescriptor(new URL(datalinkURL.toExternalForm() + "?ID=" + uid));
        serviceDescriptor.standardID = Standards.DATALINK_LINKS_10;

        recursiveThisLink.serviceDef = serviceDescriptor.id;
        recursiveThisLink.contentType = "application/x-votable+xml;content=datalink";
        expectedDataLinks.add(recursiveThisLink);

        thisLink.descriptor = new ServiceDescriptor(new URL(datalinkURL + "?ID=" + hierarchyItemTwo.getName()));
        expectedDataLinks.add(thisLink);

        final HierarchyItem hierarchyItemThree =
                fromJSONFile(uid, String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(),
                                                index++));
        expectedDataLinks.add(createDataLink(hierarchyItemThree, "application/x-tar",
                                             new URL(String.format("%s/%s", dataPortalURL.toExternalForm(),
                                                                   hierarchyItemThree.getName())),
                                             DataLink.Term.AUXILIARY));

        // The rest are progenitors.
        while (index <= 10) {
            final HierarchyItem nextHierarchyItem =
                    fromJSONFile(uid, String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(),
                                                    index++));
            expectedDataLinks.add(createDataLink(nextHierarchyItem, "application/x-tar",
                                                 new URL(String.format("%s/%s", dataPortalURL.toExternalForm(),
                                                                       nextHierarchyItem.getName())),
                                                 DataLink.Term.PROGENITOR));
        }

        // Even footing before comparing each item individually.
        resultDataLinks.sort(new DataLinkComparator());
        expectedDataLinks.sort(new DataLinkComparator());

        Assert.assertEquals("Wrong sizes.", expectedDataLinks.size(), resultDataLinks.size());

        final DataLinkComparator comparator = new DataLinkComparator();
        for (int i = 0; i < resultDataLinks.size(); i++) {
            final DataLink resultDataLink = resultDataLinks.get(i);
            final DataLink expectedDataLink = expectedDataLinks.get(i);

            Assert.assertEquals(String.format("DataLinks at %d are not equal ('%s' : '%s').", i,
                                              resultDataLink, expectedDataLink),
                                0, comparator.compare(resultDataLink, expectedDataLink));
        }
    }

    private HierarchyItem fromJSONFile(final AlmaUID uid, final String filename) throws Throwable {
        final File jsonFile = FileUtil.getFileFromResource(filename, DataLinkIteratorTest.class);
        final FileInputStream fileInputStream = new FileInputStream(jsonFile);
        final JSONObject jsonObject = new JSONObject(new JSONTokener(fileInputStream));
        return HierarchyItem.fromJSONObject(uid, jsonObject);
    }

    private DataLink createDataLink(final HierarchyItem hierarchyItem, final String contentType,
                                    final URL accessURL, final DataLink.Term semantic) {
        final DataLink dataLink = new DataLink(hierarchyItem.getUidString(), semantic);

        dataLink.accessURL = accessURL;
        dataLink.contentType = contentType;

        return dataLink;
    }
}
