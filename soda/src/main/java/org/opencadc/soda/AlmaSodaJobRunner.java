
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

package org.opencadc.soda;


import alma.asdm.action.asdm.AsdmWalker;
import alma.asdm.dao.BulkStoreDao;
import alma.asdm.dao.BulkStoreDaoImpl;
import alma.asdm.dao.MetaDataDao;
import alma.asdm.dao.MetaDataDaoImpl;
import alma.asdm.dao.ProductMetaDataDao;
import alma.asdm.dao.ProductMetaDataDaoImpl;
import alma.asdm.service.AsdmService;
import alma.asdm.service.AssetManagerService;
import alma.asdm.service.AssetManagerServiceImpl;
import alma.asdm.service.DataPacker;
import alma.asdm.service.DataPackerImpl;
import alma.asdm.service.OusService;
import alma.asdm.service.ProductService;
import alma.asdm.service.RetrieveService;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.alma.deliverable.DeliverableInfoWalker;
import org.opencadc.soda.server.AbstractSodaJobRunner;
import org.opencadc.soda.server.AlmaStreamingSodaPlugin;
import org.opencadc.soda.server.SodaPlugin;
import org.opencadc.soda.server.SodaURLBuilder;
import ca.nrc.cadc.db.DBUtil;

import javax.naming.NamingException;
import javax.sql.DataSource;


public class AlmaSodaJobRunner extends AbstractSodaJobRunner {

    private static final String NGAS_HOSTS_PROPERTY_NAME = "ngasHosts";
    private static final String NGAS_TIMEOUT_SECONDS_PROPERTY_NAME = "ngasTimeoutSeconds";
    private static final String ALMA_DB_JNDI_NAME_KEY = "almaSodaJDBCName";
    private static final String DEFAULT_ALMA_DB_JNDI_NAME = "jdbc/soda";

    private final AlmaProperties almaProperties;


    public AlmaSodaJobRunner(AlmaProperties sodaProperties) {
        this.almaProperties = sodaProperties;
    }

    public AlmaSodaJobRunner() {
        this(new AlmaProperties());
    }

    @Override
    public SodaPlugin getSodaPlugin() {
        return new AlmaStreamingSodaPlugin(createDataPacker(), createDeliverableInfoWalker(),
                                           createDeliverableURLBuilder());
    }

    DeliverableInfoWalker createDeliverableInfoWalker() {
        return new DeliverableInfoWalker();
    }

    SodaURLBuilder createDeliverableURLBuilder() {
        return new SodaURLBuilder(almaProperties);
    }

    DataPacker createDataPacker() {
        try {
            final DataSource dataSource = DBUtil.findJNDIDataSource(
                    almaProperties.getFirstPropertyValue(ALMA_DB_JNDI_NAME_KEY, DEFAULT_ALMA_DB_JNDI_NAME));
            final MetaDataDao metaDataDao = new MetaDataDaoImpl(dataSource);
            final BulkStoreDao bulkStoreDao =
                    new BulkStoreDaoImpl(dataSource, almaProperties.getFirstPropertyValue(NGAS_HOSTS_PROPERTY_NAME),
                                         Integer.parseInt(almaProperties
                                                                  .getFirstPropertyValue(
                                                                          NGAS_TIMEOUT_SECONDS_PROPERTY_NAME,
                                                                          "0")));
            final ProductMetaDataDao productMetaDataDao = new ProductMetaDataDaoImpl(dataSource);
            final AsdmWalker asdmWalker = new AsdmWalker(metaDataDao, bulkStoreDao, 1L);
            final ProductService productService = new ProductService(productMetaDataDao, bulkStoreDao);
            final AsdmService asdmService = new AsdmService(asdmWalker, metaDataDao, bulkStoreDao);
            final OusService ousService = new OusService(asdmService, productService, metaDataDao);
            final RetrieveService retrieveService = new RetrieveService(asdmService, productService);
            final AssetManagerService assetManagerService = new AssetManagerServiceImpl(asdmService, productService,
                                                                                        metaDataDao, retrieveService,
                                                                                        ousService,
                                                                                        1);
            return new DataPackerImpl(asdmService, ousService, null, productService, metaDataDao, assetManagerService);
        } catch (NamingException ne) {
            // This means the DataSource was not configured.
            throw new RuntimeException("Unable to proceed when no DataSource is configured.", ne);
        }
    }
}
