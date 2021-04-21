
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

package org.opencadc.soda.server;

import org.opencadc.alma.AlmaProperties;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.DoubleInterval;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;

import java.net.URL;

import org.junit.Test;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.opencadc.alma.deliverable.HierarchyItem;

import static org.mockito.Mockito.*;


public class SodaURLBuilderTest {

    public SodaURLBuilderTest() {
        Configurator.setLevel("org.opencadc.soda", Level.DEBUG);
    }

    @Test
    public void createCircleCutoutURL() throws Exception {
        final AlmaProperties mockAlmaProperties = mock(AlmaProperties.class);
        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);
        final Circle circle = new Circle(new Point(12.0D, 56.0D), 0.6D);
        final Cutout shapeCutout = new Cutout();
        shapeCutout.pos = circle;

        final SodaURLBuilder testSubject = new SodaURLBuilder(mockAlmaProperties);

        when(mockHierarchyItem.getNullSafeId(true)).thenReturn("1977.11.25_uid___C1_C2_C3.fits");

        when(mockAlmaProperties.getFirstPropertyValue("secureSchemeHost", null)).thenReturn("https://almaservices.com");
        when(mockAlmaProperties.getFirstPropertyValue("downloadPath", null)).thenReturn("/sodacutout/download");

        final URL expectedURL = new URL(
                "https://almaservices.com/sodacutout/download/1977.11.25_uid___C1_C2_C3.fits?CIRCLE=12.0+56.0+0.6");
        final URL resultURL = testSubject.createCutoutURL(mockHierarchyItem, shapeCutout);
        Assert.assertEquals("Wrong cutout URL.", expectedURL, resultURL);

        verify(mockAlmaProperties, times(1)).getFirstPropertyValue("secureSchemeHost", null);
        verify(mockAlmaProperties, times(1)).getFirstPropertyValue("downloadPath", null);
        verify(mockHierarchyItem, times(1)).getNullSafeId(true);
    }

    @Test
    public void createPolygonCutoutURL() throws Exception {
        final AlmaProperties mockAlmaProperties = mock(AlmaProperties.class);
        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);

        final Polygon polygon = new Polygon();
        polygon.getVertices().add(new Point(12.4D, 56.7D));
        polygon.getVertices().add(new Point(5.6D, 44.5D));
        polygon.getVertices().add(new Point(18.3D, 33.5D));

        final Cutout shapeCutout = new Cutout();
        shapeCutout.pos = polygon;
        final SodaURLBuilder testSubject = new SodaURLBuilder(mockAlmaProperties);

        when(mockHierarchyItem.getType()).thenReturn(HierarchyItem.Type.MOUS);
        when(mockHierarchyItem.getNullSafeId(true)).thenReturn("1977.11.25_uid___C1_C2_C3.fits");

        when(mockAlmaProperties.getFirstPropertyValue("secureSchemeHost", null)).thenReturn("https://almaservices.com");
        when(mockAlmaProperties.getFirstPropertyValue("downloadPath", null)).thenReturn("/sodacutout/download");

        final URL expectedURL = new URL(
                "https://almaservices.com/sodacutout/download/1977.11.25_uid___C1_C2_C3.fits?POLYGON=12.4+56.7+5" +
                ".6+44.5+18.3+33.5");
        final URL resultURL = testSubject.createCutoutURL(mockHierarchyItem, shapeCutout);
        Assert.assertEquals("Wrong cutout URL.", expectedURL, resultURL);

        verify(mockAlmaProperties, times(1)).getFirstPropertyValue("secureSchemeHost", null);
        verify(mockAlmaProperties, times(1)).getFirstPropertyValue("downloadPath", null);
        verify(mockHierarchyItem, times(1)).getNullSafeId(true);
        verify(mockHierarchyItem, times(1)).getType();
    }

    @Test
    public void createBandCutoutURL() throws Exception {
        final AlmaProperties mockAlmaProperties = mock(AlmaProperties.class);
        final HierarchyItem mockHierarchyItem = mock(HierarchyItem.class);
        final DoubleInterval bandInterval = new DoubleInterval(9.8D, 76.4);
        final Cutout bandCutout = new Cutout();
        bandCutout.band = bandInterval;

        final SodaURLBuilder testSubject = new SodaURLBuilder(mockAlmaProperties);

        when(mockHierarchyItem.getName()).thenReturn("1977.11.25_uid___C1_C2_C3.fits");
        when(mockHierarchyItem.getType()).thenReturn(HierarchyItem.Type.ASDM);

        when(mockAlmaProperties.getFirstPropertyValue("secureSchemeHost", null)).thenReturn("https://almaservices.com");
        when(mockAlmaProperties.getFirstPropertyValue("downloadPath", null)).thenReturn("/sodacutout/download");

        final URL expectedURL = new URL(
                "https://almaservices.com/sodacutout/download/1977.11.25_uid___C1_C2_C3.fits?BAND=9.8+76.4");
        final URL resultURL = testSubject.createCutoutURL(mockHierarchyItem, bandCutout);
        Assert.assertEquals("Wrong cutout URL.", expectedURL, resultURL);

        verify(mockAlmaProperties, times(1)).getFirstPropertyValue("secureSchemeHost", null);
        verify(mockAlmaProperties, times(1)).getFirstPropertyValue("downloadPath", null);
        verify(mockHierarchyItem, times(1)).getName();
        verify(mockHierarchyItem, times(1)).getType();
    }
}
