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

package org.opencadc.datalink;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencadc.alma.AlmaID;

import org.opencadc.alma.AlmaIDFactory;

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
    private final Type type;
    private final long sizeInBytes;
    private final boolean permissionAllowed;
    private final String fileClass;
    private final String subDirectory;
    private final HierarchyItem[] childrenArray;
    private final AlmaID[] idArray;

    /**
     * Create a hierarchy item from a JSON Document.
     *
     * @param document The JSONObject of the query.
     * @return A new HierarchyItem instance.  Never null.
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
        final List<AlmaID> mousIDList = new ArrayList<>(mousIDJSONArray.length());

        mousIDJSONArray.forEach(mousID -> mousIDList.add(AlmaIDFactory.createID(mousID.toString())));

        return new HierarchyItem(itemID,
                                 document.get("name").toString(),
                                 Type.valueOf(document.get("type").toString()),
                                 document.getLong("sizeInBytes"),
                                 document.get("permission").toString().equals("ALLOWED"),

                                 // fileClass and subDirectory are recent additions and may not yet be supported.
                                 document.has("fileClass") ? document.get("fileClass").toString() : "",
                                 document.has("subDirectory") ? document.get("subDirectory").toString() : "",

                                 childrenList.toArray(new HierarchyItem[0]),
                                 mousIDList.toArray(new AlmaID[0]));
    }

    /**
     * Complete constructor.
     *
     * @param id            This article's ID parameter.  If this is null or "null", then it will be treated as null.
     * @param name          This article's name.
     * @param type          This article's type.
     * @param sizeInBytes   The size in bytes.
     * @param permissionAllowed      Whether this document's file can be accessed with current credentials
     *                            (if any required).
     * @param childrenArray The Array of child objects.
     * @param idArray   The Array of MOUS IDs associated.
     */
    HierarchyItem(String id, String name, Type type, long sizeInBytes, boolean permissionAllowed,
                  String fileClass, String subDirectory, HierarchyItem[] childrenArray, AlmaID[] idArray) {

        if (id == null || id.equals("null")) {
            this.id = null;
        } else {
            this.id = id;
        }

        this.name = name;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
        this.permissionAllowed = permissionAllowed;
        this.fileClass = fileClass;
        this.subDirectory = subDirectory;
        this.childrenArray = childrenArray;
        this.idArray = idArray;
    }

    public String getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return type;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    /**
     * Entries whose content length is zero or less are considered absent.
     * @return  True if there is no size, False otherwise
     */
    public boolean fileMissing() {
        return sizeInBytes <= 0L;
    }

    public boolean isPermissionAllowed() {
        return permissionAllowed;
    }

    public String getFileClass() {
        return fileClass;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public boolean hasChildren() {
        return this.childrenArray.length > 0;
    }

    public HierarchyItem[] getChildrenArray() {
        return childrenArray;
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
               permissionAllowed == that.permissionAllowed &&
               Objects.equals(this.id, that.id) &&
               Objects.equals(this.name, that.name) &&
               type == that.type &&
               Arrays.equals(childrenArray, that.childrenArray) &&
               Arrays.equals(idArray, that.idArray);
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "{"
               + "\"CLASS\": \"" + getClass().getSimpleName() + "\","
               + "\"id\": \"" + this.id + "\","
               + "\"name\": \"" + this.name + "\","
               + "\"type\": \"" + this.type.name() + "\","
               + "\"sizeInBytes\": \"" + this.sizeInBytes + "\","
               + "\"linkAuthorized\": " + this.permissionAllowed + "\","
               + "\"fileClass\": " + this.fileClass + "\","
               + "\"subDirectory\": " + this.subDirectory + "\","
               + "\"children\": " + Arrays.toString(this.childrenArray)
               + "}";
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, name, type, sizeInBytes, permissionAllowed);
        result = 31 * result + Arrays.hashCode(childrenArray);
        result = 31 * result + Arrays.hashCode(idArray);
        return result;
    }

    public enum Type {
        // NB that the order in which these enums are listed is used in DeliverableInfo to determine the display order.
        PROJECT("Project"),
        SGOUS("Science Goal OUS"),
        GOUS("Group OUS"),
        MOUS("Member OUS"),
        // following two are not _actually_ things which we deliver, and hence not a deliverable in that sense. They
        // are used for displaying information
        // in the RequestHandler regarding the SB and SOURCE names for each MOUS.
        SCHEDBLOCK("SchedBlock"),
        SOURCE("Source"),
        // the order of the enums here determines the order of children in the DeliverableInfo class. README should
        // come before PIPELINE files
        PIPELINE_AUXILIARY_README("Auxiliary/Readme"),
        // An on-the-fly tar file which is created from actual, individual files contained in NGAS. This entity
        // doesn't physically
        // exist anywhere, rather it consists of lots of (real, actually existing)....
        PIPELINE_PRODUCT_TARFILE("Product"),
        // ... physical file which exists in NGAS
        PIPELINE_PRODUCT("Product"),
        PIPELINE_AUXILIARY_TARFILE("Auxiliary"),
        PIPELINE_AUXILIARY_CYCLE1TO4_TARFILE("Auxiliary"),
        // TODO remove this after 2018DEC - it is only needed with legacy data during the deployment of 2018DEC.
        //  After the RH schema is
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
        // can be either an individual product file or an individual auxiliary file. The individual file is stored in
        // NGAS. But the
        // individual file is tarred up in the project structure before being downloaded
        PIPELINE_TARRED_INDIVIDUAL_FILE("TarredIndividualFile"),
        // when expanded and delivered via the request handler. The ASDM was Pass status.
        ASDM("Asdm"),
        // when the ASDM is Qa0 semipass
        ASDM_SEMIPASS("SemipassAsdm"),
        // an ASDM when we want direct access to an ASDM just by supplying the UID to the download manager. Normally
        // just used by developers.
        // ASDM will be an ASDM which has been used by the pipeline, and we already have the size cached. A Direct
        // ASDM may not yet have had
        // the size cached
        DIRECT_ACCESS_ASDM("DirectAccessAsdm"),
        // a file which is stored in NGAS and we're not sure about the type. In fact, we don't care. We just want to
        // stream it straight out
        // of NGAS to the user.
        GENERIC_NGAS_FILE("NgasFile"),
        // An on-the-fly tarfile which is created from actual, individual files contained in NGAS. This entity
        // doesn't physically
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
                   // we keep this so that we can work with the data which was expanded with the previous version and
                   // has been cached in RH_FILES. The type will still be PIPELINE_AUXILIARY
                   || this == PIPELINE_AUXILIARY
                   || this == PIPELINE_AUXILIARY_SCRIPT
                   || this == PIPELINE_AUXILIARY_QA
                   || this == PIPELINE_AUXILIARY_CALIBRATION;
        }

        /**
         * Used by the RequestHandler for expanding an OUS.
         *
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

        public boolean isDocumentation() {
            return this == PIPELINE_AUXILIARY_LOG || this == PIPELINE_AUXILIARY_README;
        }
    }
}
