
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

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.Assert;
import org.opencadc.alma.AlmaUID;
import org.opencadc.alma.deliverable.HierarchyItem;
import org.opencadc.alma.deliverable.RequestHandlerQuery;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.PropertiesReader;

import static org.mockito.Mockito.*;


public class DataLinkIteratorTest {

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
         *
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(DataLink o1, DataLink o2) {
            final int comparison;
            final int compareID = o1.getID().compareTo(o2.getID());
            if (compareID == 0) {
                final boolean containsAllSemantics =
                        o2.getSemantics().containsAll(o1.getSemantics());
                final int compareSemantics = containsAllSemantics ? 0 : -1;

                comparison = compareSemantics == 0 ? compareSemantics : o1.contentType.compareTo(o2.contentType);
            } else {
                comparison = compareID;
            }

            return comparison;
        }
    }

    @Test
    public void runThroughFiltering() throws Throwable {
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");

        final RequestHandlerQuery mockRequestHandlerQuery = mock(RequestHandlerQuery.class);
        final DataLinkURLBuilder dataLinkURLBuilder =
                new DataLinkURLBuilder(new URL("https://myhost.com/mydatalink/sync"),
                                       new URL("https://myhost.com/mysoda/sync"));
        final Iterator<URI> dataSetIDIterator =
                Collections.singletonList(URI.create("uid://A001/X879/X8f1")).iterator();
        final HierarchyItem hierarchy = fromJSONFile(DataLinkIteratorTest.class.getSimpleName() + ".json");

        when(mockRequestHandlerQuery.query(new AlmaUID("uid://A001/X879/X8f1"))).thenReturn(hierarchy);

        final List<DataLink> expectedDataLinks = new ArrayList<>();
        final List<DataLink> resultDataLinks = new ArrayList<>();
        new DataLinkIterator(dataLinkURLBuilder, dataSetIDIterator, mockRequestHandlerQuery)
                .forEachRemaining(resultDataLinks::add);
        final String itemFileNameTemplate = "%s.%d.json";

        int index = 1;
        final HierarchyItem hierarchyItemOne =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemOne, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemOne.getId())),
                                             DataLink.Term.THIS,
                                             Collections.singletonList(DataLink.Term.DATALINK)));
        final HierarchyItem hierarchyItemTwo =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemTwo, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemTwo.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        final HierarchyItem hierarchyItemThree =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemThree, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemThree.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        final HierarchyItem hierarchyItemFour =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemFour, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemFour.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        final HierarchyItem hierarchyItemFive =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemFive, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemFive.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        final HierarchyItem hierarchyItemSix =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemSix, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemSix.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        final HierarchyItem hierarchyItemSeven =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index++));
        expectedDataLinks.add(createDataLink(hierarchyItemSeven, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemSeven.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        final HierarchyItem hierarchyItemEight =
                fromJSONFile(String.format(itemFileNameTemplate, DataLinkIteratorTest.class.getSimpleName(), index));
        expectedDataLinks.add(createDataLink(hierarchyItemEight, "application/x-tar",
                                             new URL(String.format("https://myhost.com/mydatalink/sync?ID=%s",
                                                                   hierarchyItemEight.getId())),
                                             DataLink.Term.PROGENITOR,
                                             Collections.singletonList(DataLink.Term.PKG)));

        // Even footing before comparing each item individually.
        resultDataLinks.sort(new DataLinkComparator());
        expectedDataLinks.sort(new DataLinkComparator());

        Assert.assertEquals("Wrong sizes.", expectedDataLinks.size(), resultDataLinks.size());

        final DataLinkComparator comparator = new DataLinkComparator();
        for (int i = 0; i < resultDataLinks.size(); i++) {
            final DataLink resultDataLink = resultDataLinks.get(i);
            final DataLink expectedDataLink = expectedDataLinks.get(i);

            Assert.assertEquals("DataLinks are not equal.", 0, comparator.compare(resultDataLink, expectedDataLink));
        }
    }

    private HierarchyItem fromJSONFile(final String filename) throws Throwable {
        final File jsonFile = FileUtil.getFileFromResource(filename, DataLinkIteratorTest.class);
        final FileInputStream fileInputStream = new FileInputStream(jsonFile);
        final JSONObject jsonObject = new JSONObject(new JSONTokener(fileInputStream));
        return HierarchyItem.fromJSONObject(jsonObject);
    }

    private DataLink createDataLink(final HierarchyItem hierarchyItem, final String contentType,
                                    final URL accessURL, final DataLink.Term semantic,
                                    final List<DataLink.Term> otherSemantics) {
        final DataLink dataLink = new DataLink(hierarchyItem.getId(), semantic);

        otherSemantics.forEach(dataLink::addSemantics);

        dataLink.accessURL = accessURL;
        dataLink.contentType = contentType;

        return dataLink;
    }
}
