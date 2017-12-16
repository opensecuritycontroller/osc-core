/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.util.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.crypto.KeyStoreProvider.KeyStoreProviderException;
import org.osc.core.broker.util.db.upgrade.ReleaseUpgradeMgr;
import org.slf4j.LoggerFactory;
import org.osc.core.server.Server;
import org.slf4j.Logger;

public class RestoreUtil {

    private static final Logger log = LoggerFactory.getLogger(RestoreUtil.class);
    private static final String H2_DATABASE_NAME = "vmiDCDB.h2.db";

    public static void restoreDataBase(File backupZipFile, String restoreLocation) {
        try {
            // calling h2 API to restore from zip file
            org.h2.tools.Restore.execute(backupZipFile.getName(), restoreLocation, "vmiDCDB");
        } catch (Exception e) {
            log.error("Restoring DB file " + backupZipFile.getName() + " failed at " + restoreLocation, e);
        }
    }

    public static void validateRestoreBundle(File bkpFile) throws VmidcException {
        if (!H2_DATABASE_NAME.equals(bkpFile.getName())) {
            throw new VmidcException("Restored Database backup does not contain expected files in the uploaded zip file.");
        }
        String connectionUrl = "jdbc:h2:./tmp/vmiDCDB;AUTO_SERVER=TRUE;LOCK_TIMEOUT=10000;MV_STORE=FALSE;";
        log.info("Restoring from database: " + connectionUrl);

        Properties props = new Properties();
		try {
		    DBConnectionParameters connectionParams = new DBConnectionParameters();
		    props.setProperty("user", connectionParams.getLogin());
		    props.setProperty("password", connectionParams.getPassword());
		} catch (IOException ioe) {
			String errorMsg = "Failed to obtain database connection parameters.";
			log.error(errorMsg, ioe);
			throw new VmidcException(errorMsg);
		} catch (KeyStoreProviderException kspe) {
            log.error("Failed to obtain db user password from keystore.", kspe);
            throw new VmidcException("Failed to obtain valid database credentials.");
        }

		Driver driver = new org.h2.Driver();

        try (Connection connection = driver.connect(connectionUrl, props);
             Statement statement = connection.createStatement()){

            String sqlQuery = "SELECT * FROM release_info;";
            try (ResultSet result = statement.executeQuery(sqlQuery)) {
                int dbVersion = -1;
                if (result.next()) {
                    dbVersion = result.getInt("db_version");
                }
                log.info("Backup DB version: " + dbVersion);

                if (dbVersion <= 0) {
                    throw new VmidcException("Fail to retrieve database version.");
                }

                if (dbVersion > ReleaseUpgradeMgr.TARGET_DB_VERSION) {
                    throw new VmidcException("Restored Database version (" + dbVersion
                        + ") was created with a newer server version which cannot restored into this server which can handle target db version "
                        + ReleaseUpgradeMgr.TARGET_DB_VERSION + ".");
                }
            }
        } catch (SQLException e) {
            log.error("Invalid database: " + e.getMessage(), e);
            throw new VmidcException("Failed to read " + Server.SHORT_PRODUCT_NAME + " database version.");
		}
    }

}
