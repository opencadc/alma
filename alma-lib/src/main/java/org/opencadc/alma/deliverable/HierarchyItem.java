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

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencadc.alma.AlmaUID;

import ca.nrc.cadc.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Represents a JSON Document parsed into a model from the RequestHandler hierarchy.
 */
public class HierarchyItem {

    private final AlmaUID uid;
    private final String id;
    private final String name;
    private final Type type;
    private final long sizeInBytes;
    private final boolean readable;
    private final HierarchyItem[] childrenArray;
    private final AlmaUID[] mousIDArray;

    /**
     * Create a hierarchy item from a JSON Document.
     *
     * @param document The JSONObject of the query.
     * @return A new HierarchyItem instance.  Never null.
     */
    public static HierarchyItem fromJSONObject(final AlmaUID almaUID, final JSONObject document) {
        final String itemID = document.get("id").toString();
        final JSONArray childrenJSONArray = document.getJSONArray("children");
        final List<HierarchyItem> childrenList = new ArrayList<>(childrenJSONArray.length());

        childrenJSONArray.forEach(child -> {
            final JSONObject jsonObject = (JSONObject) child;
            childrenList.add(HierarchyItem.fromJSONObject(almaUID, jsonObject));
        });

        final JSONArray mousIDJSONArray = document.getJSONArray("allMousUids");
        final List<AlmaUID> mousIDList = new ArrayList<>(mousIDJSONArray.length());

        mousIDJSONArray.forEach(mousID -> mousIDList.add(new AlmaUID(mousID.toString())));

        return new HierarchyItem(almaUID, 
                                 (StringUtil.hasText(itemID) && !itemID.trim().equalsIgnoreCase("null")) 
                                    ? itemID : null,
                                 document.get("name").toString(),
                                 Type.valueOf(document.get("type").toString()),
                                 document.getLong("sizeInBytes"),
                                 document.get("permission").toString().equals("ALLOWED"),
                                 childrenList.toArray(new HierarchyItem[0]),
                                 mousIDList.toArray(new AlmaUID[0]));
    }

    /**
     * Complete constructor.
     *
     * @param uid           The identifier in the form of uid://c0/c1/c2.  Can NOT be null.
     * @param id            This article's ID parameter.
     * @param name          This article's name.
     * @param type          This article's type.
     * @param sizeInBytes   The size in bytes.
     * @param readable      Whether this document's file is readable.
     * @param childrenArray The Array of child objects.
     * @param mousIDArray   The Array of MOUS IDs associated.
     */
    public HierarchyItem(AlmaUID uid, String id, String name, Type type, long sizeInBytes, boolean readable,
                         HierarchyItem[] childrenArray, AlmaUID[] mousIDArray) {
        this.uid = uid;
        this.id = id;
        this.name = name;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
        this.readable = readable;
        this.childrenArray = childrenArray;
        this.mousIDArray = mousIDArray;
    }

    public String getUidString() {
        return this.getUid().toString();
    }

    public AlmaUID getUid() {
        return uid;
    }

    public String getId() {
        return id;
    }

    public String getNullSafeId() {
        return StringUtil.hasText(this.id) ? this.id : this.name;
    }

    public String getNullSafeId(final boolean sanitize) {
        return sanitize ? this.getNullSafeId().replace(':', '_').replaceAll("/", "_") : this.getNullSafeId();
    }

    public String getName() {
        return name;
    }

    public Type getType() {
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

    public AlmaUID[] getMousIDArray() {
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

    public enum Type {
        // NB that the order in which these enums are listed is used in DeliverableInfo to determine the display order.
        PROJECT("Project"),
        SGOUS("Science Goal OUS"),
        GOUS("Group OUS"),
        MOUS("Member OUS"),
        // following two are not _actually_ things which we deliver, abnd hence not a deliverable in that sense. They are used for displaying information
        // in the RequestHandler regarding the SB and SOURCE names for each MOUS.
        SCHEDBLOCK("SchedBlock"),
        SOURCE("Source"),
        // the order of the enums here determines the order of children in the DeliverableInfo class. README should come before PIIPELINE files
        PIPELINE_AUXILIARY_README("Auxiliary/Readme"),
        // An on-the-fly tarfile which is created from actual, individual files contained in NGAS. This entity doesn't physically
        // exist anywhere, rather it consists of lots of (real, actually existing)....
        PIPELINE_PRODUCT_TARFILE("Product"),
        // ... physical file which exists in NGAS
        PIPELINE_PRODUCT("Product"),
        PIPELINE_AUXILIARY_TARFILE("Auxiliary"),
        PIPELINE_AUXILIARY_CYCLE1TO4_TARFILE("Auxiliary"),
        // TODO remove this after 2018DEC - it is only needed with legacy data during the deployment of 2018DEC. After the RH schema is
        // updated it is replaced by the 5 AUXILIARY enums below.
        PIPELINE_AUXILIARY("Auxiliary"),
        // ICT-13550 the RH should display the subdir of the auxiliary files as well. So I simply expanded the
        // AUXILIARY types. TODO rethink this from end-to-end - I suspect there is a simpler way to do it now.
        PIPELINE_AUXILIARY_CALIBRATION("Auxiliary/Calibration"),
        PIPELINE_AUXILIARY_SCRIPT("Auxiliary/Script"),
        PIPELINE_AUXILIARY_LOG("Auxiliary/Log"),
        PIPELINE_AUXILIARY_QA("Auxiliary/Qa"),
        // there is overlap here with ProductFileClass.CALIBRATION and DataPackerJClient.Entity.PIPELINE_CALIBRATION
        // This does not seem to fit here - there will not be a deliverable to the PI through the RH which is
        // a pipeline calibration node. This is only used so we can specify to the DataPacker that we want to export
        // a calibration tar. We then create a filter to extract some of the product files.
        PIPELINE_CALIBRATION("Calibration"),
        // can be either an individual product file or an individual auxiliary file. The individual file is stored in NGAS. But the
        // individual file is tarred up in the project structure before being downloaded
        PIPELINE_TARRED_INDIVIDUAL_FILE("TarredIndividualFile"),
        // when expanded and delivered via the request handler. The ASDM was Pass status.
        ASDM("Asdm"),
        // when the ASDM is Qa0 semipass
        ASDM_SEMIPASS("SemipassAsdm"),
        // an ASDM when we want direct access to an ASDM just by supplying the UID to the download manager. Normally just used by developers.
        // ASDM will be an ASDM which has been used by the pipeline, and we already have the size cached. A Direct ASDM may not yet have had
        // the size cached
        DIRECT_ACCESS_ASDM("DirectAccessAsdm"),
        // a file which is stored in NGAS and we're not sure about the type. In fact, we don't care. We just want to stream it straight out
        // of NGAS to the user.
        GENERIC_NGAS_FILE("NgasFile"),
        // An on-the-fly tarfile which is created from actual, individual files contained in NGAS. This entity doesn't physically
        // exist anywhere, rather it consists of lots of (real, actually existing)....
        EXTERNAL_TARFILE("External"),
        // ICT-10907, ICT-7441
        // A single tar-file which represents a piece of data (like re-processed data) which has been contributed from
        // somewhere outside of ALMA.
        EXTERNAL("External"),
        // another type of externally produced data
        ADMIT_TARFILE("ADMIT"),
        ADMIT("ADMIT");

        private final String display;

        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        public boolean isOus() {
            return this == SGOUS || this == GOUS || this == MOUS;
        }

        public boolean isTarfile() {
            return this == PIPELINE_AUXILIARY_TARFILE
                   || this == PIPELINE_PRODUCT_TARFILE
                   || this == EXTERNAL_TARFILE;
        }

        public boolean isAuxiliary() {
            return this == PIPELINE_AUXILIARY_TARFILE
                   || this == PIPELINE_AUXILIARY_CYCLE1TO4_TARFILE
                   // we keep this so that we can work with the data which was expanded with the previous version and has been
                   // cached in RH_FILES. The type will still be PIPELINE_AUXILIARY
                   || this == PIPELINE_AUXILIARY
                   || this == PIPELINE_AUXILIARY_SCRIPT
                   || this == PIPELINE_AUXILIARY_QA
                   || this == PIPELINE_AUXILIARY_LOG
                   || this == PIPELINE_AUXILIARY_CALIBRATION
                   || this == PIPELINE_AUXILIARY_README;
        }

        /**
         * Used by the RequestHandler for expanding an OUS.
         * @return true when this is a leaf node in the expanded tree.
         */
        public boolean isLeaf() {
            return this == PIPELINE_PRODUCT
                   || this == ASDM
                   || this == ASDM_SEMIPASS
                   || (this.isAuxiliary() && this != PIPELINE_AUXILIARY_TARFILE)
                   || this == EXTERNAL
                   || this == ADMIT;
        }
    }
}
