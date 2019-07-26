
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

package org.opencadc.alma;

import alma.asdm.domain.identifiers.Uid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ca.nrc.cadc.util.StringUtil;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class that can handle a UID in the form of archiveUID://C0/C1/C2 (or uid___C0_C1_C2), or a Project Tarfile ID that is
 * in the form of 2016.1.00161.S_uid___A002_Xc4f3ae_X537a.asdm.sdm.tar.
 */
public class AlmaUID {

    private static final Logger LOGGER = LogManager.getLogger(AlmaUID.class);
    private static final Pattern UID_PATTERN =
            Pattern.compile("uid[_:]+[_/]+[_/]+\\w[0-9a-fA-F]+[_/]+\\w[0-9a-fA-F]+[_/]+\\w[0-9a-fA-F]+");


    private final String originalID;

    private Uid archiveUID;

    // Is a Filter ID
    private boolean isFilterIDFlag;


    public AlmaUID(final String originalID) {
        if (!StringUtil.hasText(originalID)) {
            throw new IllegalArgumentException("Passed ID cannot be null or empty.");
        }

        this.originalID = originalID;
        parseID();
    }

    private void parseID() {
        final Matcher matcher = UID_PATTERN.matcher(this.originalID);

        if (matcher.find()) {
            final String uidMatch = matcher.group();
            LOGGER.debug(String.format("Matched %s from %s", uidMatch, this.originalID));
            this.isFilterIDFlag = !uidMatch.equals(this.originalID);
            this.archiveUID = new Uid(uidMatch);
        } else {
            throw new IllegalArgumentException(String.format("No UID found in %s", this.originalID));
        }
    }

    public String getOriginalID() {
        return originalID;
    }

    public boolean isFiltering() {
        return isFilterIDFlag;
    }

    public Uid getArchiveUID() {
        return archiveUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AlmaUID almaUID = (AlmaUID) o;
        return isFilterIDFlag == almaUID.isFilterIDFlag &&
               originalID.equals(almaUID.originalID) &&
               archiveUID.equals(almaUID.archiveUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalID, archiveUID, isFilterIDFlag);
    }
}
