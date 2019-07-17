
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

package org.opencadc.soda.ws;

import org.apache.log4j.Logger;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.rest.RestAction;
import ca.nrc.cadc.util.Base64;

import java.io.PrintWriter;


public class EchoAction extends RestAction {

    private static final Logger LOGGER = Logger.getLogger(EchoAction.class);

    /**
     * Create inline content handler to process non-form data. Non-form data could
     * be a document or part of a multi-part request). Null return value is allowed
     * if the service never expects non-form data or wants to ignore non-form data.
     *
     * @return configured InlineContentHandler
     */
    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return null;
    }

    /**
     * Implemented by subclass
     * The following exceptions, when thrown by this function, are
     * automatically mapped into HTTP errors by RestAction class:
     * java.lang.IllegalArgumentException : 400
     * java.security.AccessControlException : 403
     * java.security.cert.CertificateException : 403
     * ca.nrc.cadc.net.ResourceNotFoundException : 404
     * ca.nrc.cadc.net.ResourceAlreadyExistsException : 409
     * ca.nrc.cadc.io.ByteLimitExceededException : 413
     * ca.nrc.cadc.net.TransientException : 503
     * java.lang.RuntimeException : 500
     * java.lang.Error : 500
     *
     * @throws Exception for standard application failure
     */
    @Override
    public void doAction() throws Exception {
        Stuff msg = parseStuff(syncInput.getPath());

        syncOutput.setCode(msg.code);
        if (msg.contentType != null) {
            syncOutput.setHeader("Content-Type", msg.contentType);
        }
        if (msg.body != null) {
            PrintWriter pw = new PrintWriter(syncOutput.getOutputStream());
            pw.println(msg.body);
            pw.flush();
            pw.close();
        }
    }

    private class Stuff {

        int code;
        String contentType;
        String body;
    }

    private Stuff parseStuff(String path) {
        Stuff ret = new Stuff();
        try {
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            String msg = Base64.decodeString(path);
            LOGGER.debug("parse msg: " + msg);
            String[] parts = msg.split("[|]");
            for (String s : parts) {
                LOGGER.debug("msg part: " + s);
            }
            if (parts.length > 0) {
                ret.code = Integer.parseInt(parts[0]);
            }
            if (parts.length > 1) {
                ret.contentType = parts[1];
            }
            if (parts.length > 2) {
                ret.body = parts[2];
            }
        } catch (NumberFormatException ex) {
            ret.code = 400;
            ret.contentType = "text/plain";
            ret.body = "BUG: invalid message in URL";
        }
        return ret;
    }
}
