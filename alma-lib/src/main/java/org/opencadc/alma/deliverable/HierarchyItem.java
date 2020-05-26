/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
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

package org.opencadc.alma.deliverable;

import alma.asdm.domain.Deliverable;
import alma.asdm.domain.identifiers.Uid;
import org.json.JSONArray;
import org.json.JSONObject;

import ca.nrc.cadc.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Represents a JSON Document parsed into a model from the RequestHandler hierarchy.
 */
public class HierarchyItem {

    private final String id;
    private final String name;
    private final Deliverable type;
    private final long sizeInBytes;
    private final boolean readable;
    private final HierarchyItem[] childrenArray;
    private final Uid[] mousIDArray;

    /**
     * Create a hierarchy item from a JSON Document.
     *
     * @param document The JSONObject of the query.
     * @return A new JSONHierarcyItem instance.  Never null.
     */
    public static HierarchyItem fromJSONObject(final JSONObject document) {
        final String itemID = document.get("id").toString();
        final JSONArray childrenJSONArray = document.getJSONArray("children");
        final List<HierarchyItem> childrenList = new ArrayList<>(childrenJSONArray.length());

        childrenJSONArray.forEach(child -> {
            final JSONObject jsonObject = (JSONObject) child;
            childrenList.add(HierarchyItem.fromJSONObject(jsonObject));
        });

        final JSONArray mousIDJSONArray = document.getJSONArray("allMousUids");
        final List<Uid> mousIDList = new ArrayList<>(mousIDJSONArray.length());

        mousIDJSONArray.forEach(mousID -> mousIDList.add(new Uid(mousID.toString())));

        return new HierarchyItem((StringUtil.hasText(itemID) && !itemID.equals("null")) ? itemID : null,
                                 document.get("name").toString(),
                                 Deliverable.valueOf(document.get("type").toString()),
                                 document.getLong("sizeInBytes"),
                                 document.get("permission").toString().equals("ALLOWED"),
                                 childrenList.toArray(new HierarchyItem[0]),
                                 mousIDList.toArray(new Uid[0]));
    }

    /**
     * Complete constructor.
     *
     * @param id            The identifier in the form of uid://c0/c1/c2.  Can be null.
     * @param name          This article's name.
     * @param type          This article's type.
     * @param sizeInBytes   The size in bytes.
     * @param readable      Whether this document's file is readable.
     * @param childrenArray The Array of child objects.
     * @param mousIDArray   The Array of MOUS IDs associated.
     */
    public HierarchyItem(String id, String name, Deliverable type, long sizeInBytes, boolean readable,
                         HierarchyItem[] childrenArray, Uid[] mousIDArray) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
        this.readable = readable;
        this.childrenArray = childrenArray;
        this.mousIDArray = mousIDArray;
    }

    public String getId() {
        return id;
    }

    public String getNullSafeId() {
        return StringUtil.hasText(this.id) ? this.id : this.name;
    }

    public String getName() {
        return name;
    }

    public Deliverable getType() {
        return type;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public boolean fileExists() {
        return sizeInBytes > 0L;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean hasChildren() {
        return this.childrenArray.length > 0;
    }

    public HierarchyItem[] getChildrenArray() {
        return childrenArray;
    }

    public Uid[] getMousIDArray() {
        return mousIDArray;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HierarchyItem that = (HierarchyItem) o;
        return sizeInBytes == that.sizeInBytes &&
               readable == that.readable &&
               id.equals(that.id) &&
               name.equals(that.name) &&
               type == that.type &&
               Arrays.equals(childrenArray, that.childrenArray) &&
               Arrays.equals(mousIDArray, that.mousIDArray);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, name, type, sizeInBytes, readable);
        result = 31 * result + Arrays.hashCode(childrenArray);
        result = 31 * result + Arrays.hashCode(mousIDArray);
        return result;
    }
}
