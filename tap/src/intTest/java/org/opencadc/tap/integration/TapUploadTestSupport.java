/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2026.                            (c) 2026.
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
 ************************************************************************
 */

package org.opencadc.tap.integration;

import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Helpers for TAP UPLOAD integration tests (inline, HTTP, HTTPS).
 */
public final class TapUploadTestSupport {

    static final URI TAP_RESOURCE_ID = URI.create("ivo://almascience.org/tap");

  /**
   * Public VOTable fixture shipped in the WAR for upload-http/upload-https tests (TAP §2.7.6).
   */
  private static final String FIXTURE_PATH = "upload-fixtures/TAPUploadTest-1.xml";

    private TapUploadTestSupport() {
    }

    public static URL getFixtureUrl() throws MalformedURLException {
        final RegistryClient registryClient = new RegistryClient();
        final URL sync = registryClient.getServiceURL(TAP_RESOURCE_ID, Standards.TAP_10);
        String base = sync.toExternalForm();
        if (base.endsWith("/sync")) {
            base = base.substring(0, base.length() - "/sync".length());
        } else if (base.endsWith("/sync/")) {
            base = base.substring(0, base.length() - "/sync/".length());
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return URI.create(base + FIXTURE_PATH).toURL();
    }
}
