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
package org.osc.core.broker.util.db.upgrade;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.h2.util.StringUtils;
import org.osc.core.broker.model.entities.ReleaseInfo;
import org.osc.core.broker.service.api.DBConnectionManagerApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.common.job.FreqType;
import org.osc.core.common.job.ThresholdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReleaseMgr: manage fresh-install and upgrade processes. We only need to
 * create a single record in ReleaseInfo database table to manage this.
 */
public class ReleaseUpgradeMgr {
    /*
     * TARGET_DB_VERSION will be manually changed to the real target db version to which we will upgrade
     */
    public static final int TARGET_DB_VERSION = DBConnectionManagerApi.TARGET_DB_VERSION;

    private static final String DB_UPGRADE_IN_PROGRESS_MARKER_FILE = "dbUpgradeInProgressMarker";

    private static final Logger log = LoggerFactory.getLogger(ReleaseUpgradeMgr.class);

    public static void initDb(EncryptionApi encrypter, DBConnectionParameters params,
            DBConnectionManager dbMgr) throws Exception {
        if (!isLastUpgradeSucceeded()) {
            // If last upgrade wasn't successful (upgrade marker file is still present), revert back to previous backed up DB.
            // We're doing file reversal before we opening the database, otherwise, file will be opened and copy will not succeed.
            revertToBackupDbFile();
        }

        // if DB password is set to default then replace it with the secure one
        replaceDefaultDBPassword(params, dbMgr);
        ReleaseInfo currentRecord = getCurrentReleaseInfo(dbMgr);

        if (currentRecord == null) { // not found, initial version
            log.info("Fresh-installing security broker database");
            // create db schema first
            Schema.createSchema(dbMgr);
        } else {
            int curDbVer = currentRecord.getDbVersion();
            log.info("Current database schema version: {}", curDbVer);

            if (TARGET_DB_VERSION > curDbVer) {
                if (isLastUpgradeSucceeded()) {
                    backupDbFile();
                }
                createUpgradeMarkerFile();

                log.info("Upgrading security broker database from current version: {} to target version: {}", curDbVer,
                        TARGET_DB_VERSION);

                // call upgrade process logics here. After done with the upgrade,
                // make sure to update the ReleaseInfo record with the TARGET DB VERSION
                // use switch statement without break statement (fall-thru flow) to force incremental upgrade chain.
                try (Connection connection = dbMgr.getSQLConnection();
                     Statement stmt = connection.createStatement()) {
                    connection.setAutoCommit(false);

                    try {
                        performUpdateChain(curDbVer, stmt, encrypter);
                        connection.commit();
                    } catch (Exception ex) {
                        log.error("Error while initializing database.", ex);
                        try {
                            connection.rollback();
                        } catch (Exception he) {
                            log.error("Error rolling back transaction", he);
                        }
                        throw ex;
                    }
                }
            }
        }

        // If we've reached here, it is safe to assume upgrade is successful
        deleteUpgradeMarkerFile();
    }

    private static void replaceDefaultDBPassword(DBConnectionParameters params,
            DBConnectionManager dbMgr) throws Exception {
        if (params.isDefaultPasswordSet()) {
            dbMgr.replaceDefaultDBPassword();
        }
    }

    /**
     * switch (curDbVer) { case 0: //upgrade from version 0 to 1
     * case 1: //upgrade from version 1 to 2 case 2: //upgrade from
     * version 2 to 3 ... case k - 1: //upgrade from version k-1 to
     * k ... case TARGET_DB_VERSION - 1: // upgrade to target db
     * version and update current db version in database with
     * TARGET_DB_VERSION value default: break; //do nothing }
     */
    @SuppressWarnings("fallthrough")
    private static void performUpdateChain(int curDbVer, Statement stmt, EncryptionApi encrypter) throws Exception {
        switch (curDbVer) {
            case 12:
                upgrade12to13(stmt); // v1.00 -> v1.20
            case 13:
                upgrade13to14(stmt);
            case 14:
                upgrade14to15(stmt);
            case 15:
                upgrade15to34(stmt); // v1.20 -> v2.00
            case 34:
                upgrade34to35(stmt);
            case 35:
                upgrade35to36(stmt);
            case 36:
                upgrade36to37(stmt);
            case 37:
                upgrade37to38(stmt);
            case 38:
                upgrade38to39(stmt);
            case 39:
                upgrade39to40(stmt);
            case 40:
                upgrade40to41(stmt);
            case 41:
                upgrade41to42(stmt);
            case 42:
                upgrade42to43(stmt);
            case 43:
                upgrade43to44(stmt);
            case 44:
                upgrade44to45(stmt);
            case 45:
                upgrade45to46(stmt);
            case 46:
                upgrade46to47(stmt);
            case 47:
                upgrade47to48(stmt);
            case 48:
                upgrade48to49(stmt);
            case 49:
                upgrade49to50(stmt);
            case 50:
                upgrade50to51(stmt);
            case 51:
                upgrade51to52(stmt);
            case 52:
                upgrade52to53(stmt);
            case 53:
                upgrade53to54(stmt);
            case 54:
                upgrade54to55(stmt);
            case 55:
                upgrade55to56(stmt);
            case 56:
                upgrade56to57(stmt);
            case 57:
                upgrade57to58(stmt);
            case 58:
                upgrade58to59(stmt);
            case 59:
                upgrade59to60(stmt);
            case 60:
                upgrade60to61(stmt);
            case 61:
                upgrade61to62(stmt);
            case 62:
                upgrade62to63(stmt);
            case 63:
                upgrade63to64(stmt);
            case 64:
                upgrade64to65(stmt); // V2.0 -> V2.5
            case 65:
                upgrade65to66(stmt);
            case 66:
                upgrade66to67(stmt);
            case 67:
                upgrade67to68(stmt);
            case 68:
                upgrade68to69(stmt);
            case 69:
                upgrade69to70(stmt);
            case 70:
                upgrade70to71(stmt);
            case 71:
                upgrade71to72(stmt);
            case 72:
                upgrade72to73(stmt, encrypter);
            case 73:
                upgrade73to74(stmt);
            case 74:
                upgrade74to75(stmt);
            case 75:
                upgrade75to76(stmt);
            case 76:
                upgrade76to77(stmt);
            case 77:
                upgrade77to78(stmt);
            case 78:
                upgrade78to79(stmt);
            case 79:
                upgrade79to80(stmt);
            case 80:
                upgrade80to81(stmt);
            case 81:
                upgrade81to82(stmt);
            case 82:
                upgrade82to83(stmt);
            case 83:
                upgrade83to84(stmt);
            case 84:
                upgrade84to85(stmt);
            case 85:
                upgrade85to86(stmt);
            case 86:
                upgrade86to87(stmt);
            case 87:
                upgrade87to88(stmt);
            case 88:
                upgrade88to89(stmt);
            case 89:
                upgrade89to90(stmt);
            case 90:
            	upgrade90to91(stmt);
            case 91:
            	upgrade91to92(stmt);
            case 92:
            	upgrade92to93(stmt);
            case 93:
            	upgrade93to94(stmt);
            case 94:
            	upgrade94to95(stmt);
            case TARGET_DB_VERSION:
                if (curDbVer < TARGET_DB_VERSION) {
                    execSql(stmt, "UPDATE RELEASE_INFO SET db_version = " + TARGET_DB_VERSION + " WHERE id = 1;");
                }
                break;

            default:
                log.error("Current DB version is unknown !!!");
        }
    }

    private static void upgrade94to95(Statement stmt) throws SQLException {
		execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR drop constraint FK_VC_LAST_JOB;");
		execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR drop constraint FK_VC_LAST_JOB_UNIQUE;");
		execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR drop column if exists last_job_id_fk;");
	}

	private static void upgrade93to94(Statement stmt) throws SQLException {
        execSql(stmt, "create table DISTRIBUTED_APPLIANCE_INSTANCE_POD_PORT (" +
                    "dai_fk bigint not null, " +
                    "pod_port_fk bigint not null," +
                    "primary key (dai_fk, pod_port_fk));");

        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE_POD_PORT " +
                    "add constraint FK_DAI_PODP_DAI " +
                    "foreign key (dai_fk) " +
                    "references DISTRIBUTED_APPLIANCE_INSTANCE;");

        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE_POD_PORT " +
                "add constraint FK_DAI_PODP_PODP " +
                "foreign key (pod_port_fk) " +
                "references POD_PORT;");
    }

    private static void upgrade92to93(Statement stmt) throws SQLException {
    	execSql(stmt,
                "alter table SECURITY_GROUP add column sfc_fk bigint;");

    	execSql(stmt,
					"alter table SECURITY_GROUP " +
					"add constraint FK_SG_SFC " +
					"foreign key (sfc_fk) " +
					"references SERVICE_FUNCTION_CHAIN;");
    }

    private static void upgrade91to92(Statement stmt) throws SQLException {
    	execSql(stmt,
                "alter table SERVICE_FUNCTION_CHAIN_VIRTUAL_SYSTEM add column vs_order bigint not null;");

    	execSql(stmt,
                "alter table SERVICE_FUNCTION_CHAIN add column vc_fk bigint;");

    	execSql(stmt,
    			"alter table SERVICE_FUNCTION_CHAIN " +
    			"add constraint FK_SFC_VC " +
    			"foreign key (vc_fk) " +
    			"references VIRTUALIZATION_CONNECTOR;");
    }

	private static void upgrade90to91(Statement stmt) throws SQLException {

		execSql(stmt, "alter table SECURITY_GROUP_INTERFACE drop constraint UK_SGI_VS_TAG;");

		execSql(stmt, "alter table SECURITY_GROUP_INTERFACE drop constraint FK_SGI_POLICY;");

		execSql(stmt, "alter table SECURITY_GROUP_INTERFACE add column mgr_security_group_id varchar(255);");

		execSql(stmt, "update SECURITY_GROUP_INTERFACE AS sgi SET sgi.mgr_security_group_id = "
				+ "(select sg.mgr_id from SECURITY_GROUP sg where sg.id = sgi.security_group_fk);");

		execSql(stmt, "alter table SECURITY_GROUP drop column if exists mgr_id;");

		execSql(stmt, "create table SECURITY_GROUP_INTERFACE_POLICY (sgi_fk bigint not null, "
				+ "policy_fk bigint not null, primary key (sgi_fk, policy_fk));");

		execSql(stmt, "alter table SECURITY_GROUP_INTERFACE_POLICY add constraint FK_SGI_POLICY_SGI "
				+ "foreign key (sgi_fk) references SECURITY_GROUP_INTERFACE;");

		execSql(stmt, "alter table SECURITY_GROUP_INTERFACE_POLICY add constraint FK_SGI_POLICY_POLICY "
				+ "foreign key (policy_fk) references POLICY;");

		execSql(stmt, "INSERT INTO SECURITY_GROUP_INTERFACE_POLICY "
				+ "(sgi_fk, policy_fk) SELECT ID, policy_fk FROM SECURITY_GROUP_INTERFACE where policy_fk is not null;");

		execSql(stmt, "alter table SECURITY_GROUP_INTERFACE drop column if exists policy_fk;");
	}

    private static void upgrade89to90(Statement stmt) throws SQLException {
        execSql(stmt, "alter table VM_PORT add column inspection_hook_id varchar(255) " +
                "after mac_address;");
    }

    private static void upgrade88to89(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN IF EXISTS external_id;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE alter column os_server_id RENAME TO " + "external_id;");
    }

    private static void upgrade87to88(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DEPLOYMENT_SPEC add column port_group_id varchar(255) " +
                      "after inspection_network_id;");
    }

    private static void upgrade86to87(Statement stmt) throws SQLException {
        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION add column image_pull_secret_name varchar(255);");
        execSql(stmt, "alter table DEPLOYMENT_SPEC add column namespace varchar(255);");
        execSql(stmt, "alter table DEPLOYMENT_SPEC add column external_id varchar(255);");

        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column external_id varchar(255);");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column inspection_element_id varchar(255);");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column inspection_element_parent_id varchar(255);");

        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column region varchar(255) NULL;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column project_name varchar(255) NULL;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column project_id varchar(255) NULL;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column management_network_name varchar(255) NULL;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column management_network_id varchar(255) NULL;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column inspection_network_name varchar(255) NULL;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "alter column inspection_network_id varchar(255) NULL;");
    }

    private static void upgrade85to86(Statement stmt) throws SQLException {
        execSql(stmt, "alter table LABEL add column name varchar(255) not null;");
    }

    private static void upgrade83to84(Statement stmt) throws SQLException {
        execSql(stmt, "CREATE TABLE SERVICE_FUNCTION_CHAIN (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "name varchar(255) not null," +
                "primary key (id));");

        execSql(stmt, "CREATE TABLE SERVICE_FUNCTION_CHAIN_VIRTUAL_SYSTEM (" +
                        "sfc_fk bigint not null, " +
                        "virtual_system_fk bigint not null," +
                        "primary key (sfc_fk, virtual_system_fk));");
        execSql(stmt,
                "alter table SERVICE_FUNCTION_CHAIN_VIRTUAL_SYSTEM " +
                "add constraint FK_SFC_VS_SFC " +
                "foreign key (sfc_fk) " +
                "references SERVICE_FUNCTION_CHAIN;");
        execSql(stmt,
                "alter table SERVICE_FUNCTION_CHAIN_VIRTUAL_SYSTEM " +
                "add constraint FK_SFC_VS_VS " +
                "foreign key (virtual_system_fk) " +
                "references VIRTUAL_SYSTEM;");
        execSql(stmt,
                "alter table SERVICE_FUNCTION_CHAIN " +
                "add constraint UK_SFC_NAME unique (name);");
    }

    private static void upgrade84to85(Statement stmt) throws SQLException {
        execSql(stmt, "create table POD (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "external_id varchar(255) not null," +
                "node varchar(255) not null," +
                "name varchar(255) not null," +
                "namespace varchar(255) not null," +
                "primary key (id));");
        execSql(stmt, "create table POD_PORT (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "external_id varchar(255) not null," +
                "mac_address varchar(255) not null," +
                "pod_fk bigint," +
                "parent_id varchar(255)," +
                "primary key (id)" +
                ");");
        execSql(stmt, "create table LABEL (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "value varchar(255) not null," +
                "primary key (id)" +
                ");");
        execSql(stmt, "create table POD_PORT_IP_ADDRESS ("
                    + "pod_port_fk bigint not null, ip_address varchar(255));");
        execSql(stmt, "create table POD_LABEL (" +
                 "pod_fk bigint not null, " +
                 "label_fk bigint not null," +
                 "primary key (pod_fk, label_fk) );");
        execSql(stmt, "alter table POD_PORT_IP_ADDRESS add constraint " +
                 "FK_POD_PORT_IP_ADDRESS foreign key (pod_port_fk) references POD_PORT;");
        execSql(stmt, "alter table POD " +
                "add constraint UK_POD_EXT_ID unique (" +
                "external_id);");
        execSql(stmt,
                "alter table SECURITY_GROUP_MEMBER add column label_fk bigint;");
        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add constraint FK_SGM_LABEL " +
                "foreign key (label_fk) " +
                "references LABEL;");
    }

	private static void upgrade82to83(Statement stmt) throws SQLException {
        execSql(stmt,
                "alter table SECURITY_GROUP_INTERFACE add column security_group_fk bigint;");
        execSql(stmt,
                "update SECURITY_GROUP_INTERFACE AS sgi SET sgi.security_group_fk = "
                + "(select gi.security_group_fk from GROUP_INTERFACE gi where gi.security_group_interface_fk = sgi.id);");
        execSql(stmt, "drop table GROUP_INTERFACE;");
        execSql(stmt,
                "alter table SECURITY_GROUP_INTERFACE add constraint FK_SECURITY_GROUP foreign key "
                + "(security_group_fk) references SECURITY_GROUP;");
    }

    private static void upgrade81to82(Statement stmt) throws SQLException {
        // DS references
        execSql(stmt, "alter table DEPLOYMENT_SPEC drop constraint UK_VS_TENANT_REGION;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC alter column tenant_name RENAME TO " + "project_name;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC alter column tenant_id RENAME TO " + "project_id;");
        execSql(stmt,
                "alter table DEPLOYMENT_SPEC add constraint UK_VS_PROJECT_REGION unique (vs_fk, project_id, region);");
        // SG references
        execSql(stmt, "alter table SECURITY_GROUP drop constraint UK_NAME_TENANT;");

        execSql(stmt, "alter table SECURITY_GROUP alter column tenant_name RENAME TO " + "project_name;");
        execSql(stmt, "alter table SECURITY_GROUP alter column tenant_id RENAME TO " + "project_id;");

        execSql(stmt, "alter table SECURITY_GROUP add constraint UK_NAME_PROJECT unique (name, project_id);");

        // VC References
        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR alter column admin_tenant_name RENAME TO "
                + "admin_project_name;");
    }

    private static void upgrade80to81(Statement stmt) throws SQLException {
        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR add column admin_domain_id varchar(255);");
        // For any existing openstack installations which are currently V2, default domain needs to be used to continue operations
        execSql(stmt, "update VIRTUALIZATION_CONNECTOR SET admin_domain_id = 'default' WHERE virtualization_type = 'OPENSTACK'");
    }

    private static void upgrade79to80(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN IF EXISTS nsx_agent_id;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN IF EXISTS nsx_host_id;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN IF EXISTS nsx_host_name;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN IF EXISTS nsx_host_vsm_uuid;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN IF EXISTS nsx_vm_id;");
        execSql(stmt, "alter table SECURITY_GROUP DROP COLUMN IF EXISTS nsx_agent_id;");
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE DROP COLUMN IF EXISTS nsx_vsm_uuid;");
        execSql(stmt, "alter table VIRTUAL_SYSTEM DROP COLUMN IF EXISTS nsx_service_id;");
        execSql(stmt, "alter table VIRTUAL_SYSTEM DROP COLUMN IF EXISTS nsx_service_instance_id;");
        execSql(stmt, "alter table VIRTUAL_SYSTEM DROP COLUMN IF EXISTS nsx_service_manager_id;");
        execSql(stmt, "alter table VIRTUAL_SYSTEM DROP COLUMN IF EXISTS nsx_vsm_uuid;");
        execSql(stmt, "drop table IF EXISTS VIRTUAL_SYSTEM_POLICY;");
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE drop column if exists virtual_system_policy_fk;");
        execSql(stmt, "drop table IF EXISTS VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID;");
        execSql(stmt, "alter table VIRTUAL_SYSTEM drop column if exists nsx_deployment_spec_id;");
        execSql(stmt, "drop table IF EXISTS VIRTUAL_SYSTEM_MGR_FILE;");
        execSql(stmt, "DELETE FROM USER u WHERE role='SYSTEM_NSX';");
    }

    private static void upgrade78to79(Statement stmt) throws SQLException {
        execSql(stmt, "DROP table VIRTUAL_SYSTEM_MGR_FILE;");
    }

    private static void upgrade77to78(Statement stmt) throws SQLException {
        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR ADD COLUMN last_job_id_fk bigint;");
        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR " +
                "add constraint FK_VC_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id) ON DELETE SET NULL;");
        execSql(stmt,  "alter table VIRTUALIZATION_CONNECTOR " +
                "add constraint FK_VC_LAST_JOB_UNIQUE unique (last_job_id_fk);");

    }

    private static void upgrade76to77(Statement stmt) throws SQLException {
         execSql(stmt, "alter table SECURITY_GROUP_INTERFACE ADD COLUMN network_elem_id varchar(255);");
     }

    private static void upgrade75to76(Statement stmt) throws SQLException {
        execSql(stmt, "DELETE FROM USER u WHERE role='SYSTEM_AGENT';");
    }

    private static void upgrade74to75(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN password;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN agent_version_str;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN agent_version_major;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN agent_version_minor;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN agent_type;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE DROP COLUMN is_policy_map_out_of_sync;");
    }

    private static void upgrade73to74(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE ADD COLUMN mgmt_os_port_id varchar(255);");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE ADD COLUMN mgmt_mac_address varchar(255);");
    }

    /**
     * 3DES encrypted passwords -> AES-CTR encrypted passwords
     */
    @SuppressWarnings("deprecation")
    private static void upgrade72to73(Statement stmt, EncryptionApi encrypter) throws SQLException, EncryptionException {
        updatePasswordScheme(stmt, "user", "password", encrypter);
        updatePasswordScheme(stmt, "appliance_manager_connector", "password", encrypter);
        updatePasswordScheme(stmt, "virtualization_connector", "controller_password", encrypter);
        updatePasswordScheme(stmt, "virtualization_connector", "provider_password", encrypter);
        updatePasswordScheme(stmt, "distributed_appliance", "mgr_secret_key", encrypter);
        updatePasswordScheme(stmt, "distributed_appliance_instance", "password", encrypter);

        String sqlQuery = "SELECT vc_fk, value FROM virtualization_connector_provider_attr WHERE key = 'rabbitMQPassword';";

        Map<Integer, String> attrs = new HashMap<>();

        try (ResultSet result = stmt.executeQuery(sqlQuery)) {
            while (result.next()) {
                String value = result.getString("value");
                try {
                    value = encrypter.decryptDES(value);
                } catch (EncryptionException e) {
                    log.warn("Password is not encrypted with DES", e);
                }
                attrs.put(result.getInt("vc_fk"), encrypter.encryptAESCTR(value));
            }
        }

        try (PreparedStatement preparedStatementUpdate = stmt.getConnection().prepareStatement("UPDATE virtualization_connector_provider_attr SET value = ? WHERE vc_fk = ? AND key = 'rabbitMQPassword'")) {
            for (Map.Entry<Integer, String> entry : attrs.entrySet()) {
                preparedStatementUpdate.setString(1, entry.getValue());
                preparedStatementUpdate.setInt(2, entry.getKey());
                preparedStatementUpdate.executeUpdate();
            }
        }
    }

    /**
     * Added SSL aliases support
     */
    private static void upgrade71to72(Statement stmt) throws SQLException {
        execSql(stmt, "CREATE TABLE SSL_CERTIFICATE_ATTR(" +
                "ID BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL," +
                "CREATED_BY VARCHAR(255)," +
                "CREATED_TIMESTAMP TIMESTAMP," +
                "DELETED_BY VARCHAR(255)," +
                "DELETED_TIMESTAMP TIMESTAMP," +
                "MARKED_FOR_DELETION BOOLEAN," +
                "UPDATED_BY VARCHAR(255)," +
                "UPDATED_TIMESTAMP TIMESTAMP," +
                "VERSION BIGINT," +
                "SSL_ALIAS VARCHAR(255) NOT NULL," +
                "SSL_SHA1 VARCHAR(255) NOT NULL);");
        execSql(stmt, "CREATE TABLE SSL_CERTIFICATE_ATTR_VIRTUALIZATION_CONNECTOR(" +
                "VIRTUALIZATION_CONNECTOR_ID BIGINT NOT NULL," +
                "SSL_CERTIFICATE_ATTR_ID BIGINT NOT NULL," +
                "CONSTRAINT VC_ID_FK FOREIGN KEY (VIRTUALIZATION_CONNECTOR_ID) REFERENCES VIRTUALIZATION_CONNECTOR (ID) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT SSL_CERT_ATTR_VC_FK FOREIGN KEY (SSL_CERTIFICATE_ATTR_ID) REFERENCES SSL_CERTIFICATE_ATTR (ID) ON UPDATE CASCADE ON DELETE CASCADE);");
        execSql(stmt, "CREATE TABLE SSL_CERTIFICATE_ATTR_APPL_MAN_CONNECTOR(" +
                "APPLIANCE_MANAGER_CONNECTOR_ID BIGINT NOT NULL," +
                "SSL_CERTIFICATE_ATTR_ID BIGINT NOT NULL," +
                "CONSTRAINT AMC_ID_FK FOREIGN KEY (APPLIANCE_MANAGER_CONNECTOR_ID) REFERENCES APPLIANCE_MANAGER_CONNECTOR (ID) ON UPDATE CASCADE ON DELETE CASCADE," +
                "CONSTRAINT SSL_CERT_ATTR_AMC_FK FOREIGN KEY (SSL_CERTIFICATE_ATTR_ID) REFERENCES SSL_CERTIFICATE_ATTR (ID) ON UPDATE CASCADE ON DELETE CASCADE);");
    }

    private static void upgrade70to71(Statement stmt) throws SQLException {
        execSql(stmt, "alter table VM_PORT"
                + " ADD COLUMN parent_id varchar(255);");
    }

    private static void upgrade69to70(Statement stmt) throws SQLException {
        execSql(stmt, "alter table SECURITY_GROUP"
                + " ADD COLUMN network_elem_id varchar(255);");
    }

    /**
     * We added upgrade68to69 to support different upgrade scenario's for both 2.5 and Trunk.
     * Merging 2.5 to trunk had database version mismatch.
     */
    private static void upgrade68to69(Statement stmt) throws SQLException {
        upgrade64to65(stmt);
    }

    private static void upgrade67to68(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE ADD COLUMN agent_type varchar not null default 'AGENT'");
    }

    /**
     * We added upgrade66to67 to support different upgrade scenario's.
     * Merging 2.5 to trunk had database version mismatch.
     */
    private static void upgrade66to67(Statement stmt) throws SQLException {
        if (!tableExists(stmt.getConnection(), "OS_SECURITY_GROUP_REFERENCE")) {
            upgrade63to64(stmt);
        }
    }

    private static void upgrade65to66(Statement stmt) throws SQLException {
        if (!existsDepolymentSpecs(stmt) || areAllDeploymentSpecsDynamic(stmt) || areAllDeploymentSpecsStatic(stmt)){
            execSql(stmt, "alter table DEPLOYMENT_SPEC drop constraint FK_DEPLOYMENT_SPEC_VS;");
            execSql(stmt, "alter table DEPLOYMENT_SPEC drop constraint UK_VS_TENANT_REGION_DYNAMIC;");
            execSql(stmt, "drop table DEPLOYMENT_SPEC_SECURITY_GROUP_INTERFACE;");
            execSql(stmt, "alter table DEPLOYMENT_SPEC drop column dynamic;");
            execSql(stmt, "alter table DEPLOYMENT_SPEC add constraint UK_VS_TENANT_REGION unique (vs_fk, tenant_id, region);");
            execSql(stmt, "alter table DEPLOYMENT_SPEC add constraint FK_DEPLOYMENT_SPEC_VS foreign key (vs_fk) references VIRTUAL_SYSTEM(id);");

        } else {
            execSql(stmt, "alter table DEPLOYMENT_SPEC drop constraint FK_DEPLOYMENT_SPEC_VS;");
            execSql(stmt, "alter table HOST drop constraint UK_HOST_ID;");
            execSql(stmt, "alter table DEPLOYMENT_SPEC drop constraint UK_VS_TENANT_REGION_DYNAMIC;");

            deleteDynamicHost(stmt);
            //change FK from dynamic to static DS for a dual of D/S in a given R/T/VS
            updateDeploymentSpecFK(stmt, "HOST", "ds_host_fk");
            updateDeploymentSpecFK(stmt, "DISTRIBUTED_APPLIANCE_INSTANCE", "deployment_spec_fk");
            execSql(stmt, "drop table DEPLOYMENT_SPEC_SECURITY_GROUP_INTERFACE;");
            deleteDynamicDeploymentSpecs(stmt);
            execSql(stmt, "alter table DEPLOYMENT_SPEC drop column dynamic;");
            execSql(stmt, "alter table HOST add constraint UK_HOST_ID unique (" +
                    "ds_host_fk, openstack_id);");
            execSql(stmt, "alter table DEPLOYMENT_SPEC add constraint UK_VS_TENANT_REGION unique (vs_fk, tenant_id, region);");
            execSql(stmt, "alter table DEPLOYMENT_SPEC add constraint FK_DEPLOYMENT_SPEC_VS foreign key (vs_fk) references VIRTUAL_SYSTEM(id);");
        }

        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE "
                + "alter column tag varchar(255) NULL;");
        execSql(stmt, "alter table  VIRTUAL_SYSTEM "
                + "alter column domain_fk bigint NULL;");

      //support for two interface
        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION ADD COLUMN additional_nic_for_inspection bit not null default 0;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE alter column inspection_os_port_id RENAME TO "
                + "inspection_os_ingress_port_id;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE alter column inspection_mac_address RENAME TO "
                + "inspection_ingress_mac_address;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE ADD COLUMN inspection_os_egress_port_id varchar(255);");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE ADD COLUMN inspection_egress_mac_address varchar(255);");
        execSql(stmt, "update DISTRIBUTED_APPLIANCE_INSTANCE "
                + "set inspection_os_egress_port_id = inspection_os_ingress_port_id;");
        execSql(stmt, "update DISTRIBUTED_APPLIANCE_INSTANCE "
                + "set inspection_egress_mac_address = inspection_ingress_mac_address;");
    }

    private static void upgrade64to65(Statement stmt) throws SQLException {
        execSql(stmt, "alter table DEPLOYMENT_SPEC ADD COLUMN IF NOT EXISTS os_sg_reference_fk bigint;");

        execSql(stmt, "alter table DEPLOYMENT_SPEC "
                + "add constraint IF NOT EXISTS FK_DS_OS_SG_REFERENCE "
                + "foreign key (os_sg_reference_fk) "
                + "references OS_SECURITY_GROUP_REFERENCE;");

        execSql(stmt, "alter table OS_SECURITY_GROUP_REFERENCE drop column if exists vs_fk;");

        execSql(stmt, "alter table OS_SECURITY_GROUP_REFERENCE drop column if exists region;");
    }

    private static void upgrade63to64(Statement stmt) throws SQLException {
        // @formatter:off
      execSql(stmt, "create table OS_SECURITY_GROUP_REFERENCE (" +
              "id bigint generated by default as identity," +
              "created_by varchar(255)," +
              "created_timestamp timestamp," +
              "deleted_by varchar(255)," +
              "deleted_timestamp timestamp," +
              "marked_for_deletion boolean," +
              "updated_by varchar(255)," +
              "updated_timestamp timestamp," +
              "version bigint," +
              "region varchar(255) not null," +
              "sg_ref_id varchar(255) not null," +
              "sg_ref_name varchar(255) not null," +
              "vs_fk bigint not null," +
              "primary key (id)" +
              ");"
      );

      execSql(stmt, "alter table OS_SECURITY_GROUP_REFERENCE "
             + "add constraint UK_OSSG_OSID unique (sg_ref_id);");

      execSql(stmt, "alter table OS_SECURITY_GROUP_REFERENCE "
              + "add constraint FK_VS_OS_SG_REFERENCE "
              + "foreign key (vs_fk) "
              + "references VIRTUAL_SYSTEM;");

        // @formatter:on
    }

    private static void upgrade62to63(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "create table VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID ("
                + "virtual_system_fk bigint not null,"
                + "host_version varchar(255) not null,"
                + "nsx_deployment_spec_id varchar(255)"
                + ");");

        execSql(stmt, "alter table  VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID "
                + "ADD primary key(virtual_system_fk, host_version);");

        execSql(stmt, "alter table VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID add constraint " +
                "FK_VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID foreign key (virtual_system_fk) " +
                "references VIRTUAL_SYSTEM;");

        execSql(stmt, "INSERT INTO VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID (virtual_system_fk, "
                +"nsx_deployment_spec_id, host_version) SELECT ID, nsx_deployment_spec_id, 'VMWARE_V5_5' " +
                "FROM VIRTUAL_SYSTEM  where nsx_deployment_spec_id is not null;");

        execSql(stmt, "alter table VIRTUAL_SYSTEM drop column nsx_deployment_spec_id ;");

        // @formatter:on
    }

    private static void upgrade61to62(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "create table APPLIANCE_SOFTWARE_VERSION_IMAGE_PROPERTIES ("
                + "asv_fk bigint not null, key varchar(255), value varchar(255));");

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION_IMAGE_PROPERTIES add constraint "
                + "FK_ASV_IMAGE_PROPERTY foreign key (asv_fk) references APPLIANCE_SOFTWARE_VERSION;");

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION_IMAGE_PROPERTIES add constraint "
                + "UK_ASV_IMAGE_PROPERTY_KEY unique (asv_fk, key);");

        execSql(stmt, "create table APPLIANCE_SOFTWARE_VERSION_CONFIG_PROPERTIES ("
                + "asv_fk bigint not null, key varchar(255), value varchar(255));");

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION_CONFIG_PROPERTIES add constraint "
                + "FK_ASV_CONFIG_PROPERTY foreign key (asv_fk) references APPLIANCE_SOFTWARE_VERSION;");

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION_CONFIG_PROPERTIES add constraint "
                + "UK_ASV_CONFIG_PROPERTY_KEY unique (asv_fk, key);");

        // @formatter:on
    }

    private static void upgrade60to61(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "drop table TALKBACK_INFO;");

       // @formatter:on
    }

    private static void upgrade59to60(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER ADD COLUMN subnet_fk bigint;");

        execSql(stmt, "alter table VM_PORT ADD COLUMN subnet_fk bigint;");

        execSql(stmt,"create table OS_SUBNET (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "region varchar(255) not null," +
                "subnet_id varchar(255) not null," +
                "name varchar(255) not null," +
                "network_id varchar(255)not null," +
                "protect_external boolean default false,"+
                "primary key (id)" +
                ");");

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add constraint FK_SGM_SUBNET " +
                "foreign key (subnet_fk) " +
                "references OS_SUBNET;");

        execSql(stmt,  "alter table VM_PORT " +
                "add constraint FK_VMP_SUBNET " +
                "foreign key (subnet_fk) " +
                "references OS_SUBNET;");

        execSql(stmt,  "alter table OS_SUBNET " +
                "add constraint UK_SUBNET_ID " +
                "unique (subnet_id);");

        // @formatter:on
    }

    private static void upgrade58to59(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "update SECURITY_GROUP_INTERFACE set failure_policy_type='NA' where failure_policy_type is null");
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE alter column failure_policy_type varchar(255) not null");

        // @formatter:on
    }

    private static void upgrade57to58(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table ALERT alter column object_id bigint null");
        execSql(stmt, "alter table ALERT alter column object_type varchar(255) null");
        execSql(stmt, "alter table ALERT alter column object_name varchar(255) null");

        // @formatter:on
    }

    private static void upgrade56to57(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "update VIRTUALIZATION_CONNECTOR "
                + "set CONTROLLER_TYPE = 'NONE' where VIRTUALIZATION_TYPE = 'VMWARE'");

        execSql(stmt, "create table TASK_CHILD (" +
            "task_id bigint not null," +
            "child_id bigint not null," +
            "primary key (task_id, child_id));");

        execSql(stmt, "alter table TASK_CHILD " +
            "add constraint FK_TASK_CHILD_ID " +
            "foreign key (child_id) " +
            "references TASK;");

        execSql(stmt, "alter table TASK_CHILD " +
            "add constraint FK_TASK_CHILD_TASK_ID " +
            "foreign key (task_id) " +
            "references TASK;");

        // @formatter:on
    }

    private static void upgrade55to56(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table OS_IMAGE_REFERENCE "
                + "add column asv_fk bigint;");

        execSql(stmt, "alter table OS_IMAGE_REFERENCE add constraint "
                + "FK_ASV_OS_IMAGE_REFERENCE foreign key (asv_fk) references APPLIANCE_SOFTWARE_VERSION;");

        // @formatter:on
    }

    private static void upgrade54to55(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table SECURITY_GROUP " +
                "add constraint UK_NAME_TENANT unique (" +
                "name, tenant_id);");

        // @formatter:on
    }

    private static void upgrade53to54(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table DEPLOYMENT_SPEC " +
                "add column last_job_id_fk bigint;");

        execSql(stmt, "alter table DEPLOYMENT_SPEC " +
                "add constraint FK_DS_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id) ON DELETE SET NULL;");

        // @formatter:on
    }

    private static void upgrade52to53(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table SECURITY_GROUP " +
                "add column last_job_id_fk bigint;");

        execSql(stmt, "alter table SECURITY_GROUP " +
                "add constraint FK_SG_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id) ON DELETE SET NULL;");

        // @formatter:on
    }

    private static void upgrade51to52(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "update VIRTUALIZATION_CONNECTOR "
                + "set CONTROLLER_TYPE = 'NONE' where VIRTUALIZATION_TYPE = 'OPENSTACK' and CONTROLLER_TYPE IS NULL");

        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR alter column "
                + "CONTROLLER_TYPE varchar(255) not null");

        // @formatter:on
    }

    private static void upgrade50to51(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "create table VM_PORT_IP_ADDRESS ("
                + "vm_port_fk bigint not null, ip_address varchar(255));");

        execSql(stmt, "alter table VM_PORT_IP_ADDRESS add constraint " +
                "FK_VM_PORT_IP_ADDRESS foreign key (vm_port_fk) references VM_PORT;");

        // @formatter:on
    }

    private static void upgrade49to50(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "create table APPLIANCE_SOFTWARE_VERSION_ENCAPSULATION_TYPE_ATTR ("
                + "appliance_software_version_fk bigint not null, encapsulation_type varchar(255));");

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION_ENCAPSULATION_TYPE_ATTR add constraint "
                + "FK_ASV_ASV_ENCAPSULATION foreign key (appliance_software_version_fk) references APPLIANCE_SOFTWARE_VERSION;");

        // @formatter:on
    }

    private static void upgrade48to49(Statement stmt) throws SQLException {
        // @formatter:off
        // NO-OP. This step needed to be temporarily added. We cant remove this method as some of the DB's are already
        // at this version
        // execSql(stmt, "update HOST set openstack_id = name");
        // @formatter:on
    }

    private static void upgrade47to48(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table DEPLOYMENT_SPEC alter column floating_pool_name varchar(255) null;");

        // @formatter:on
    }

    private static void upgrade46to47(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR "
                + "add column admin_tenant_name varchar(255);");

        execSql(stmt, "update VIRTUALIZATION_CONNECTOR "
                + "set ADMIN_TENANT_NAME = PROVIDER_USERNAME where VIRTUALIZATION_TYPE = 'OPENSTACK'");

        // @formatter:on
    }

    private static void upgrade45to46(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "create table VIRTUALIZATION_CONNECTOR_PROVIDER_ATTR ("
                + "vc_fk bigint not null, key varchar(255), value varchar(255));");

        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR_PROVIDER_ATTR add constraint "
                + "FK_PROVIDER_ATTR_VC foreign key (vc_fk) references VIRTUALIZATION_CONNECTOR;");

        execSql(stmt, "alter table VIRTUALIZATION_CONNECTOR_PROVIDER_ATTR add constraint "
                + "UK_PROVIDER_ATTR_KEY unique (vc_fk, key);");

        // @formatter:on
    }

    private static void upgrade44to45(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table HOST add constraint UK_HOST_ID unique (ds_host_fk, openstack_id);");

        // @formatter:on
    }

    private static void upgrade43to44(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table DEPLOYMENT_SPEC drop column all_hosts_in_region;");

        // @formatter:on
    }

    private static void upgrade42to43(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "ALTER TABLE DISTRIBUTED_APPLIANCE_INSTANCE ALTER COLUMN NSX_PREFIX_LENGTH RENAME TO "
                + "mgmt_subnet_prefix_length;");
        execSql(stmt, "ALTER TABLE DISTRIBUTED_APPLIANCE_INSTANCE ALTER COLUMN NSX_GATEWAY RENAME TO "
                + "mgmt_gateway_address;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column mgmt_ip_address varchar(255);");

        // @formatter:on
    }

    private static void upgrade41to42(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table DEPLOYMENT_SPEC add column all_hosts_in_region bit not null default 0;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC drop constraint UK_VS_TENANT;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC alter column dynamic bit not null default 0;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC add constraint UK_VS_TENANT_REGION_DYNAMIC unique (vs_fk, tenant_id, region, dynamic);");
        execSql(stmt, "alter table DEPLOYMENT_SPEC drop column security_group_interface_fk;");

        execSql(stmt, "create table DEPLOYMENT_SPEC_SECURITY_GROUP_INTERFACE (" +
                "deployment_spec_fk bigint not null, " +
                "security_group_interface_fk bigint not null," +
                "primary key (deployment_spec_fk, security_group_interface_fk) );");

        stmt.execute(
                "alter table DEPLOYMENT_SPEC_SECURITY_GROUP_INTERFACE " +
                "add constraint FK_DS_SGI_DS " +
                "foreign key (deployment_spec_fk) " +
                "references DEPLOYMENT_SPEC;"
                );

       stmt.execute(
                "alter table DEPLOYMENT_SPEC_SECURITY_GROUP_INTERFACE " +
                "add constraint FK_DS_SGI_SGI " +
                "foreign key (security_group_interface_fk) " +
                "references SECURITY_GROUP_INTERFACE;"
                );

     // @formatter:on

    }

    private static void upgrade40to41(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table ALERT add column message varchar(255);");
        // @formatter:on
    }

    private static void upgrade39to40(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table VIRTUAL_SYSTEM add column name varchar(255);");
        execSql(stmt, "update VIRTUAL_SYSTEM as vs set name = " +
                "( select CONCAT(name, '_' , vs.id) from DISTRIBUTED_APPLIANCE where id = vs.distributed_appliance_fk );");
        execSql(stmt, "alter table VIRTUAL_SYSTEM alter column name varchar(255) not null;");
        // @formatter:on
    }

    private static void upgrade38to39(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table VM drop column dai_fk;");
        execSql(stmt, "create table DISTRIBUTED_APPLIANCE_INSTANCE_VM_PORT (" +
                "dai_fk bigint not null, " +
                "vm_port_fk bigint not null," +
                "primary key (dai_fk, vm_port_fk));");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE_VM_PORT " +
                "add constraint FK_DAI_VMP_DAI " +
                "foreign key (dai_fk) " +
                "references DISTRIBUTED_APPLIANCE_INSTANCE;");
        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE_VM_PORT " +
                "add constraint FK_DAI_VMP_VMP " +
                "foreign key (vm_port_fk) " +
                "references VM_PORT;");
     // @formatter:on

    }

    private static void upgrade37to38(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE add column chain_order bigint not null default 0;");
        // @formatter:on
    }

    private static void upgrade36to37(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table DEPLOYMENT_SPEC add column dynamic boolean default false;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC add column security_group_interface_fk  bigint;");
        execSql(stmt, "alter table DEPLOYMENT_SPEC " +
                "add constraint FK_SGI_DS " +
                "foreign key (security_group_interface_fk) " +
                "references SECURITY_GROUP_INTERFACE(id) ON DELETE SET NULL;"
        );

        // @formatter:on
    }

    private static void upgrade35to36(Statement stmt) throws SQLException {
        // @formatter:off
        execSql(stmt, "alter table VM_PORT " +
                "add constraint UK_VMP_ID " +
                "unique (os_port_id);"
        );
        execSql(stmt, "alter table OS_NETWORK " +
                "drop constraint UK_OSNETWORK_REGION_OSID;"
        );

        execSql(stmt, "alter table OS_NETWORK " +
                "add constraint UK_OSNETWORK_OSID " +
                "unique (openstack_id);"
        );
        execSql(stmt, "alter table OS_IMAGE_REFERENCE " +
                "drop constraint UK_REGION_IMAGE_ID;"
        );
        execSql(stmt, "alter table OS_IMAGE_REFERENCE " +
                "add constraint UK_IMAGE_OSID " +
                "unique (image_ref_id);"
        );
        execSql(stmt, "alter table OS_FLAVOR_REFERENCE " +
                "drop constraint UK_REGION_FLAVOR_ID;"
        );
        execSql(stmt, "alter table OS_FLAVOR_REFERENCE " +
                "add constraint UK_FLAVOR_OSID unique (flavor_ref_id);"
        );

     // @formatter:on

    }

    private static void upgrade34to35(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table ALARM add column enable_alarm bit default 0;");
        execSql(stmt, "update ALARM set enable_alarm=0;");
        execSql(stmt, "alter table ALARM alter column enable_alarm bit not null;");
        execSql(stmt, "alter table ALARM alter column recipient_email varchar(255) null;");

        // @formatter:on
    }

    private static void upgrade15to34(Statement stmt) throws SQLException {
        // @formatter:off

        stmt.execute(
                "create table HOST (" +
                        "id bigint generated by default as identity," +
                        "created_by varchar(255)," +
                        "created_timestamp timestamp," +
                        "deleted_by varchar(255)," +
                        "deleted_timestamp timestamp," +
                        "marked_for_deletion boolean," +
                        "updated_by varchar(255)," +
                        "updated_timestamp timestamp," +
                        "version bigint," +
                        "host varchar(255) not null," +
                        "ds_host_fk bigint not null," +
                        "primary key (id)" +
                   ");"
        );
        stmt.execute(
                "create table AVAILABILITY_ZONE (" +
                        "id bigint generated by default as identity," +
                        "created_by varchar(255)," +
                        "created_timestamp timestamp," +
                        "deleted_by varchar(255)," +
                        "deleted_timestamp timestamp," +
                        "marked_for_deletion boolean," +
                        "updated_by varchar(255)," +
                        "updated_timestamp timestamp," +
                        "version bigint," +
                        "region varchar(255) not null," +
                        "zone varchar(255) not null," +
                        "ds_fk bigint not null," +
                        "primary key (id)" +
                    ");"
        );
        stmt.execute(
                "create table DEPLOYMENT_SPEC (" +
                        "id bigint generated by default as identity," +
                        "created_by varchar(255)," +
                        "created_timestamp timestamp," +
                        "deleted_by varchar(255)," +
                        "deleted_timestamp timestamp," +
                        "marked_for_deletion boolean," +
                        "updated_by varchar(255)," +
                        "updated_timestamp timestamp," +
                        "version bigint," +
                        "name varchar(255) not null," +
                        "region varchar(255) not null," +
                        "tenant_name varchar(255) not null," +
                        "tenant_id varchar(255) not null," +
                        "management_network_name varchar(255) not null," +
                        "management_network_id varchar(255) not null," +
                        "instance_count bigint not null default 1," +
                        "shared bit not null default 0," +
                        "inspection_network_id varchar(255) not null," +
                        "inspection_network_name varchar(255) not null," +
                        "floating_pool_name varchar(255) not null," +
                        "vs_fk bigint not null," +
                        "primary key (id)" +
                    ");"
        );

        stmt.execute(
                "create table OS_IMAGE_REFERENCE (" +
                        "id bigint generated by default as identity," +
                        "created_by varchar(255)," +
                        "created_timestamp timestamp," +
                        "deleted_by varchar(255)," +
                        "deleted_timestamp timestamp," +
                        "marked_for_deletion boolean," +
                        "updated_by varchar(255)," +
                        "updated_timestamp timestamp," +
                        "version bigint," +
                        "region varchar(255) not null," +
                        "image_ref_id varchar(255)," +
                        "vs_fk bigint not null," +
                        "primary key (id)" +
                    ");"
        );

        stmt.execute(
                "alter table OS_IMAGE_REFERENCE " +
                        "add constraint UK_REGION_IMAGE_ID unique (region, image_ref_id);"
        );

        stmt.execute(
                "alter table OS_IMAGE_REFERENCE " +
                        "add constraint FK_VS_OS_IMAGE_REFERENCE foreign key (vs_fk) " +
                        "references VIRTUAL_SYSTEM;"
        );

        stmt.execute(
                "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                        "ADD COLUMN os_host_name VARCHAR(255);"
                );
        stmt.execute(
                "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                        "ADD COLUMN os_availability_zone_name VARCHAR(255);"
                );
        stmt.execute(
                "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                        "ADD COLUMN os_server_id VARCHAR(255);"
                );

        stmt.execute(
                "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                        "ADD COLUMN deployment_spec_fk BIGINT;"
                );
        stmt.execute(
                "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                        "add constraint FK_DAI_DEPLOYMENT_SPEC " +
                        "foreign key (deployment_spec_fk) " +
                        "references DEPLOYMENT_SPEC;"
                );

        stmt.execute(
                "alter table DEPLOYMENT_SPEC add constraint UK_VS_TENANT unique (" +
                            "vs_fk, tenant_id, region);");

        stmt.execute(
                "alter table DEPLOYMENT_SPEC add constraint FK_DEPLOYMENT_SPEC_VS " +
                        "foreign key (vs_fk) " +
                        "references VIRTUAL_SYSTEM;");

        stmt.execute(
                "alter table AVAILABILITY_ZONE add constraint FK_DS " +
                        "foreign key (ds_fk) " +
                        "references DEPLOYMENT_SPEC(id);");

        stmt.execute(
                "alter table HOST add constraint FK_HOST_DS " +
                        "foreign key (ds_host_fk) " +
                        "references DEPLOYMENT_SPEC(id);");

        stmt.execute(
                "alter table VIRTUALIZATION_CONNECTOR alter column controller_ip_address varchar(255) null;");
        stmt.execute(
                "alter table VIRTUALIZATION_CONNECTOR alter column controller_username varchar(255) null;");
        stmt.execute(
                "alter table VIRTUALIZATION_CONNECTOR alter column controller_password varchar(255) null;");

        execSql(stmt, "create table HOST_AGGREGATE (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "openstack_id varchar(255) not null," +
                "ds_fk bigint not null," +
                "primary key (id)" +
                ");"
        );

        stmt.execute(
                "ALTER TABLE HOST_AGGREGATE " +
                        "add constraint FK_HOST_AGGREGATE_DS " +
                        "foreign key (ds_fk) " +
                        "references DEPLOYMENT_SPEC(id);"
        );

        execSql(stmt, "create table OS_FLAVOR_REFERENCE (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "region varchar(255) not null," +
                "flavor_ref_id varchar(255)," +
                "vs_fk bigint not null," +
                "primary key (id)" +
                ");"
        );

        execSql(stmt, "alter table OS_FLAVOR_REFERENCE " +
                "add constraint UK_REGION_FLAVOR_ID unique (region, flavor_ref_id);"
        );
        execSql(stmt, "alter table OS_FLAVOR_REFERENCE " +
                "add constraint FK_OS_FLAVOR_REFERENCE_VS " +
                "foreign key (vs_fk) " +
                "references VIRTUAL_SYSTEM;"
        );

        execSql(stmt, "alter table SECURITY_GROUP add(" +
                "tenant_id varchar(255) null," +
                "tenant_name varchar(255) null);"
            );

        execSql(stmt, "create table VM (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "region varchar(255) not null," +
                "vm_id varchar(255) not null," +
                "name varchar(255) not null," +
                "host varchar(255) null," +
                "dai_fk bigint," +
                "primary key (id)" +
                ");"
            );

        execSql(stmt, "create table VM_PORT (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "os_network_id varchar(255) not null," +
                "os_port_id varchar(255) not null," +
                "mac_address varchar(255) not null," +
                "vm_fk bigint not null," +
                "primary key (id)" +
                ");"
            );

        stmt.execute(
            "alter table VM " +
                "add constraint FK_VM_DAI " +
                "foreign key (dai_fk) " +
                "references DISTRIBUTED_APPLIANCE_INSTANCE ON DELETE SET NULL;"
            );

        execSql(stmt, "alter table VM " +
            "add constraint UK_VM_ID unique (" +
            "vm_id);"
            );

        execSql(stmt, "alter table VM_PORT " +
            "add constraint FK_VMP_VM " +
            "foreign key (vm_fk) " +
            "references VM;"
            );

        execSql(stmt, "alter table VM_PORT " +
            "add constraint UK_VM_PORT_MAC unique (" +
            "mac_address);"
            );


        stmt.execute(
            "alter table HOST " +
                "ADD COLUMN openstack_id varchar(255) not null;"
            );
        stmt.execute(
            "alter table HOST " +
                "ALTER COLUMN host RENAME TO name;"
            );
        stmt.execute(
            "alter table HOST_AGGREGATE " +
                "ADD COLUMN name varchar(255) not null;"
            );
        stmt.execute(
            "alter table VIRTUALIZATION_CONNECTOR " +
                "ADD COLUMN controller_type varchar(255);"
            );

        stmt.execute(
                "UPDATE VIRTUALIZATION_CONNECTOR SET controller_type='NSX';"
                );

        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                "ADD COLUMN inspection_os_port_id varchar(255);"
            );
        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE_INSTANCE " +
                "ADD COLUMN inspection_mac_address varchar(255);"
            );

        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "add (user_configurable bit not null default 0, policy_fk bigint);"
        );
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "alter column nsx_vsm_uuid varchar(255) null;"
        );
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "add constraint FK_SGI_POLICY " +
                "foreign key (policy_fk) " +
                "references POLICY;"
        );

        stmt.execute(
            "alter table APPLIANCE_MANAGER_CONNECTOR drop constraint FK_MC_LAST_JOB;"
        );
        stmt.execute(
            "alter table APPLIANCE_MANAGER_CONNECTOR " +
                "add constraint FK_MC_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id) ON DELETE SET NULL;"
        );

        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE drop constraint FK_DA_LAST_JOB;"
        );
        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE " +
                "add constraint FK_DA_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id);"
                );

        execSql(stmt, "create table JOBS_ARCHIVE (" +
               "id bigint generated by default as identity," +
               "created_by varchar(255)," +
               "created_timestamp timestamp," +
               "deleted_by varchar(255)," +
               "deleted_timestamp timestamp," +
               "marked_for_deletion boolean," +
               "updated_by varchar(255)," +
               "updated_timestamp timestamp," +
               "version bigint," +
               "auto_schedule boolean not null," +
               "frequency varchar(255) not null," +
               "threshold_unit varchar(255) not null," +
               "threshold_value int not null," +
               "last_trigger_timestamp timestamp," +
               "primary key (id)" +
               ");"
        );

        // Add job archiving default settings - auto scheduled disabled, run weekly, purge jobs older then 1 year
        execSql(stmt, "INSERT INTO JOBS_ARCHIVE (id, version, auto_schedule, frequency, threshold_value, threshold_unit) VALUES "
                + "(1, 0, 0, '" + FreqType.WEEKLY + "', 1, '" + ThresholdType.YEARS + "');");

        execSql(stmt, "alter table VIRTUAL_SYSTEM add (encapsulation_type varchar(255));");

        execSql(stmt, "update VIRTUAL_SYSTEM as vs set ENCAPSULATION_TYPE='MPLS' " +
                "where vs.VIRTUALIZATION_CONNECTOR_FK in " +
                "(select id from VIRTUALIZATION_CONNECTOR where VIRTUALIZATION_TYPE ='OPENSTACK');"
        );

        execSql(stmt, "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column floating_ip_id varchar(255);");

        execSql(stmt, "create table EMAIL_SETTINGS (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "mail_server varchar(255) not null," +
                "port int not null," +
                "email_id varchar(255) not null," +
                "password varchar(255)," +
                "primary key (id)" +
                ");"
        );

        execSql(stmt, "create table ALARM (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "name varchar(255) not null," +
                "event_type varchar(255) not null," +
                "regex_match varchar(255)," +
                "severity varchar(255) not null," +
                "action varchar(255) not null," +
                "recipient_email varchar(255) not null," +
                "primary key (id)" +
                ");"
        );

        execSql(stmt, "create table ALERT (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "name varchar(255) not null," +
                "object_name varchar(255) not null," +
                "event_type varchar(255) not null," +
                "object_id bigint not null," +
                "object_type varchar(255) not null," +
                "severity varchar(255) not null," +
                "acknowledgement_status varchar(255) not null," +
                "time_acknowledged_timestamp timestamp," +
                "acknowledged_by varchar(255)," +
                "primary key (id)" +
                ");"
        );

        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "add column failure_policy_type varchar(255);"
        );

        execSql(stmt, "create table OS_NETWORK (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "region varchar(255) not null," +
                "name varchar(255) not null," +
                "openstack_id varchar(255) not null," +
                "primary key (id)" +
                ");"
        );

        execSql(stmt, "alter table OS_NETWORK " +
                "add constraint UK_OSNETWORK_REGION_OSID " +
                "unique (region, openstack_id);"
        );

        execSql(stmt, "alter table SECURITY_GROUP " +
                "add column protect_all bit not null default 1;"
        );

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "ADD COLUMN vm_fk bigint null;"
            );

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add column network_fk bigint;"
        );

        execSql(stmt, "alter table VM_PORT " +
                "alter column vm_fk bigint null;"
        );
        execSql(stmt, "alter table VM_PORT " +
                "add column network_fk bigint;"
        );

        execSql(stmt, "alter table VM_PORT " +
                "add constraint FK_VMP_NETWORK " +
                "foreign key (network_fk) " +
                "references OS_NETWORK;"
        );

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add constraint FK_SGM_NETWORK " +
                "foreign key (network_fk) " +
                "references OS_NETWORK;"
        );

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add constraint FK_SGM_VM " +
                "foreign key (vm_fk) " +
                "references VM;"
                );

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER drop constraint UK_SGM_SG_ADDRESS;");

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add constraint UK_SGM_SG_VM_NETWORK_ADDRESS " +
                "unique (security_group_fk, vm_fk, network_fk, address);"
        );

        // @formatter:on
    }

    private static void upgrade14to15(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table JOB add column FAILURE_REASON varchar(255);");

        // @formatter:on
    }

    private static void upgrade13to14(Statement stmt) throws SQLException {
        // @formatter:off

        execSql(stmt, "alter table SECURITY_GROUP add column MGR_ID varchar(255);");

        // @formatter:on
    }

    private static void upgrade12to13(Statement stmt) throws SQLException {
        // @formatter:off
        stmt.execute(
                "ALTER TABLE VIRTUALIZATION_CONNECTOR ALTER COLUMN NSX_IP_ADDRESS RENAME TO \"CONTROLLER_IP_ADDRESS\";");
        stmt.execute(
                "ALTER TABLE VIRTUALIZATION_CONNECTOR ALTER COLUMN NSX_USERNAME RENAME TO \"CONTROLLER_USERNAME\";");
        stmt.execute(
                "ALTER TABLE VIRTUALIZATION_CONNECTOR ALTER COLUMN NSX_PASSWORD RENAME TO \"CONTROLLER_PASSWORD\";");
        stmt.execute(
                "ALTER TABLE VIRTUALIZATION_CONNECTOR ALTER COLUMN VCENTER_IP_ADDRESS RENAME TO \"PROVIDER_IP_ADDRESS\";");
        stmt.execute(
                "ALTER TABLE VIRTUALIZATION_CONNECTOR ALTER COLUMN VCENTER_USERNAME RENAME TO \"PROVIDER_USERNAME\";");
        stmt.execute(
                "ALTER TABLE VIRTUALIZATION_CONNECTOR ALTER COLUMN VCENTER_PASSWORD RENAME TO \"PROVIDER_PASSWORD\";");

        execSql(stmt, "create table SECURITY_GROUP (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "name varchar(255) not null," +
                "nsx_id varchar(255)," +
                "vc_fk bigint not null," +
                "primary key (id)" +
                ");"
            );

        execSql(stmt, "alter table SECURITY_GROUP " +
                "add constraint FK_SG_VC " +
                "foreign key (vc_fk) " +
                "references VIRTUALIZATION_CONNECTOR;"
            );

        execSql(stmt, "create table SECURITY_GROUP_MEMBER (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "security_group_fk bigint not null," +
                "member_type varchar(255) not null," +
                "address varchar(255)," +
                "primary key (id)" +
                ");"
            );

        execSql(stmt, "alter table SECURITY_GROUP_MEMBER " +
                "add constraint UK_SGM_SG_ADDRESS " +
                "unique (security_group_fk, address);"
        );

        stmt.execute(
            "alter table SECURITY_GROUP_INTERFACE " +
                "ALTER COLUMN mgr_policy_id RENAME TO mgr_interface_id;"
            );
        stmt.execute(
            "alter table SECURITY_GROUP_INTERFACE " +
                "ADD COLUMN security_group_fk bigint;"
            );
        stmt.execute(
            "alter table SECURITY_GROUP_INTERFACE " +
                "add constraint FK_SGI_SG " +
                "foreign key (security_group_fk) " +
                "references SECURITY_GROUP;"
            );

        execSql(stmt, "create table GROUP_INTERFACE (" +
                "security_group_fk bigint not null, " +
                "security_group_interface_fk bigint not null," +
                "primary key (security_group_fk, security_group_interface_fk) );"
        );

        execSql(stmt, "alter table GROUP_INTERFACE " +
                "add constraint FK_GI_SG " +
                "foreign key (security_group_fk) " +
                "references SECURITY_GROUP;");

        execSql(stmt, "alter table GROUP_INTERFACE " +
                "add constraint FK_GI_SGI " +
                "foreign key (security_group_interface_fk) " +
                "references SECURITY_GROUP_INTERFACE;");

        stmt.execute(
            "create table TASK_OBJECT (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "task_fk bigint not null," +
                "name varchar(255) not null," +
                "object_type varchar(255) not null," +
                "object_id bigint not null," +
                "primary key (id)" +
            ");"
        );

        stmt.execute(
            "alter table TASK_OBJECT " +
                "add constraint FK_TASK_OBJECT_TASK " +
                "foreign key (task_fk) " +
                "references TASK;"
            );

        stmt.execute(
            "alter table TASK_OBJECT " +
                "add constraint UK_TASK_OBJ_TYPE_OBJ_ID " +
                "unique (task_fk, object_type, object_id);"
            );

        stmt.execute(
            "create table JOB_OBJECT (" +
                "id bigint generated by default as identity," +
                "created_by varchar(255)," +
                "created_timestamp timestamp," +
                "deleted_by varchar(255)," +
                "deleted_timestamp timestamp," +
                "marked_for_deletion boolean," +
                "updated_by varchar(255)," +
                "updated_timestamp timestamp," +
                "version bigint," +
                "job_fk bigint not null," +
                "name varchar(255) not null," +
                "object_type varchar(255) not null," +
                "object_id bigint not null," +
                "primary key (id)" +
            ");"
        );

        stmt.execute(
            "alter table JOB_OBJECT " +
                "add constraint FK_JOB_OBJECT_JOB " +
                "foreign key (job_fk) " +
                "references JOB;"
            );

        stmt.execute(
            "alter table JOB_OBJECT " +
                "add constraint UK_JOB_OBJ_TYPE_OBJ_ID " +
                "unique (job_fk, object_type, object_id);"
            );

        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column nsx_gateway varchar(255);"
        );
        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE_INSTANCE add column nsx_prefix_length varchar(255);"
        );

        stmt.execute(
            "alter table APPLIANCE_MANAGER_CONNECTOR drop constraint FK_MC_LAST_JOB;"
        );
        stmt.execute(
            "alter table APPLIANCE_MANAGER_CONNECTOR " +
                "add constraint FK_MC_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id) ON DELETE SET NULL;"
        );

        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE drop constraint FK_DA_LAST_JOB;"
        );
        stmt.execute(
            "alter table DISTRIBUTED_APPLIANCE " +
                "add constraint FK_DA_LAST_JOB " +
                "foreign key (last_job_id_fk) " +
                "references JOB(id);"
         );

        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "alter column nsx_vsm_uuid varchar(255) null;"
        );
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "alter column nsx_service_profile_id rename to \"TAG\";"
        );
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "alter column virtual_system_policy_fk bigint null;"
        );
        execSql(stmt, "alter table SECURITY_GROUP_INTERFACE " +
                "add constraint UK_SGI_VS_TAG unique (virtual_system_fk, tag);"
        );

        stmt.execute(
                "ALTER TABLE POLICY ADD COLUMN domain_fk BIGINT");
        stmt.execute(
                "ALTER TABLE POLICY " +
                        "add constraint FK_PO_DOMAIN " +
                        "foreign key (domain_fk) " +
                        "references DOMAIN;"
                );

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION " +
                "add (minimum_cpus int,memory_in_mb int,disk_in_gb int);"
        );

        execSql(stmt, "alter table APPLIANCE_SOFTWARE_VERSION " +
                "add constraint UK_ASV_IMAGE_URL unique (image_url);"
        );

        stmt.execute(
            "ALTER TABLE DISTRIBUTED_APPLIANCE_INSTANCE ADD COLUMN appliance_config BLOB;"
            );

        // SMC related DB changes
        stmt.execute(
                "alter table APPLIANCE_MANAGER_CONNECTOR alter column password varchar(255) null;");
        stmt.execute(
                "alter table APPLIANCE_MANAGER_CONNECTOR alter column username varchar(255) null;");

        // @formatter:on
    }

  //A given V/T/R combination can have max 1 dynamic and max 1 static DS
    //A host has 1 Deployment spec
    //A given pair of DS (dynamic/static) belonging to VTR will be referred by a pair of hosts
    // HOST=> ID  OpenStack_id    DS_HOST_FK || Depl Spec => ID  Dynamic  R/T/V
    //        1   jstack-compute  100        ||              100 True     R1/T1/V1
    //        2   jstack-compute  200        ||              200 False    R1/T1/V1
    // delete the ID 1 host which is having a dynamic Deployment spec, and then later delete the DDS 100
    private static void deleteDynamicHost(Statement stmt) throws SQLException {
        String sql = "delete from HOST hs  where exists (  "  +
             "           SELECT ID, openstack_id from (                        "  +
             "                SELECT a.ID, a.dynamic, b.region, b.tenant_id, b.vs_fk, b.openstack_id, b.hostHas_DS_DDS_by_RTV from DEPLOYMENT_SPEC a "  +
             "               JOIN "  +
             "               ( "  +
             "               select region, tenant_id, vs_fk, openstack_id, count(*) as hostHas_DS_DDS_by_RTV from ( "  +
             "                   select m.ID as host_id, m.OPENSTACK_ID, n.DSID, n.region, n.tenant_id, n.vs_fk, n.dynamic from HOST m "  +
             "                   JOIN "  +
             "                   ( "  +
             "                       select x.id as DSID, x.region, x.tenant_id, x.vs_fk, x.DYNAMIC, y.countByVSRegTenant from DEPLOYMENT_SPEC AS x "  +
             "                       JOIN "  +
             "                       ( "  +
             "                           select region, tenant_id, vs_fk, count(*) as countByVSRegTenant from DEPLOYMENT_SPEC  group by region, tenant_id, vs_fk "  +
             "                       ) AS y "  +
             "                       ON x.region = y.region and x.tenant_id = y.tenant_id and x.vs_fk = y.vs_fk AND y.countByVSRegTenant  = 2  "  +
             "                   ) AS n "  +
             "                   ON m.DS_HOST_FK = n.DSid "  +
             "               ) as p "  +
             "               group by region, tenant_id, vs_fk, openstack_id "  +
             "               ) b "  +
             "               ON a.region = b.region and a.tenant_id=b.tenant_id and a.vs_fk=b.vs_fk "  +
             "           ) as sel_id where dynamic=true and hostHas_DS_DDS_by_RTV=2 "  +
             "           and hs.ds_host_fk = sel_id.id and hs.openstack_id = sel_id.openstack_id "  +
             "  ) ; ";
        execSql(stmt, sql);
    }

    private static void updateDeploymentSpecFK(Statement stmt, String table, String foreignKey) throws SQLException {
        String sql =String.format("update %s a SET a.%s = " +
                "(" +
                "select sel.id  from DEPLOYMENT_SPEC ds LEFT  JOIN (" +
                "select d_s.*, jn.countByVSRegTenant  from DEPLOYMENT_SPEC AS d_s " +
                "JOIN (" +
                "select region, tenant_id, vs_fk, count(*) as countByVSRegTenant from DEPLOYMENT_SPEC  group by region, tenant_id, vs_fk) AS jn " +
                "ON d_s.region = jn.region and d_s.tenant_id = jn.tenant_id and d_s.vs_fk = jn.vs_fk ) As sel " +
                "ON ds.region = sel.region and ds.tenant_id = sel.tenant_id and ds.vs_fk = sel.vs_fk " +
                "AND ( " +
                "(sel.countByVSRegTenant = 1 ) OR " +
                "(sel.countByVSRegTenant = 2 AND ds.DYNAMIC = TRUE AND ds.DYNAMIC <> sel.DYNAMIC OR ds.DYNAMIC = FALSE AND ds.DYNAMIC = sel.DYNAMIC AND ds.ID = sel.ID ) " +
                ") " +
                "WHERE a.%s = ds.id " +
                ");", table, foreignKey, foreignKey);
        execSql(stmt, sql);
    }

    private static void deleteDynamicDeploymentSpecs(Statement stmt) throws SQLException{
        // for any given VS/Region/Tenant, having both static and dynamic deployment specs,
        // delete the dynamic deployment spec
        String sql = "   DELETE FROM DEPLOYMENT_SPEC ds WHERE EXISTS   "  +
                     "   (  "  +
                     "       select * from (  "  +
                     "         select ds1.ID, ds1.region, ds1.tenant_id, ds1.vs_fk, ds1.DYNAMIC, sel_cnt.countByVSRegTenant  from DEPLOYMENT_SPEC AS ds1   "  +
                     "         JOIN   "  +
                     "         (  "  +
                     "             select region, tenant_id, vs_fk, count(*) as countByVSRegTenant from DEPLOYMENT_SPEC  group by region, tenant_id, vs_fk  "  +
                     "         ) AS sel_cnt   "  +
                     "         ON ds1.region = sel_cnt.region and ds1.tenant_id = sel_cnt.tenant_id and ds1.vs_fk = sel_cnt.vs_fk  "  +
                     "       )  AS sel  "  +
                     "       WHERE ds.region = sel.region AND ds.tenant_id = sel.tenant_id AND ds.vs_fk = sel.vs_fk AND sel.countByVSRegTenant =2 and ds.DYNAMIC=TRUE"  +
                     "  ) ; ";
        execSql(stmt, sql);
    }

    private static boolean existsDepolymentSpecs(Statement stmt) throws SQLException {
        String sql = "select count(*) as count from DEPLOYMENT_SPEC;";
        String existsDS = null;
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                existsDS = rs.getString(1);
            }
        }

        return !StringUtils.isNullOrEmpty(existsDS) && Integer.parseInt(existsDS) > 0;
    }

    private static boolean areAllDeploymentSpecsDynamic(Statement stmt) throws SQLException {
        return checkAllStaticOrDynamic(stmt, Boolean.TRUE);
    }

    private static boolean areAllDeploymentSpecsStatic(Statement stmt) throws SQLException {
        return checkAllStaticOrDynamic(stmt, Boolean.FALSE);
    }

    private static boolean checkAllStaticOrDynamic(Statement stmt, boolean flag) throws SQLException {
        String sql = String.format("SELECT (SELECT COUNT(*) FROM DEPLOYMENT_SPEC ) - (SELECT COUNT(*) FROM DEPLOYMENT_SPEC WHERE dynamic=%s) AS Difference;", flag);
        String val = null;
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                val = rs.getString(1);
            }
        }
        return !StringUtils.isNullOrEmpty(val) && Integer.parseInt(val) == 0;
    }

    @SuppressWarnings("deprecation")
    private static void updatePasswordScheme(Statement statement, String tableName, String columnName, EncryptionApi encrypter) throws SQLException, EncryptionException {
        String sqlQuery = "SELECT id, " + columnName + " FROM " + tableName + ";";
        Map<Integer, String> idsAndPasswords = new HashMap<>();

        try (ResultSet result = statement.executeQuery(sqlQuery)) {
            while (result.next()) {
                idsAndPasswords.put(result.getInt("id"),
                        encrypter.encryptAESCTR(encrypter.decryptDES(result.getString(columnName))));
            }
        }

        try (PreparedStatement preparedStatementUpdate = statement.getConnection().prepareStatement("UPDATE " + tableName + " SET " + columnName + " = ? WHERE id = ?")) {
            for (Map.Entry<Integer, String> entry : idsAndPasswords.entrySet()) {
                preparedStatementUpdate.setString(1, entry.getValue());
                preparedStatementUpdate.setInt(2, entry.getKey());
                preparedStatementUpdate.executeUpdate();
            }
        }
    }

    private static void backupDbFile() throws IOException {
        FileUtils.copyFile(new File("vmiDCDB.h2.db"), new File("vmiDCDB.h2.db.bak"));
    }

    private static void revertToBackupDbFile() throws IOException {
        FileUtils.copyFile(new File("vmiDCDB.h2.db.bak"), new File("vmiDCDB.h2.db"));
    }

    private static ReleaseInfo getCurrentReleaseInfo(DBConnectionManager dbMgr) throws Exception {
        ReleaseInfo releaseInfo = null;

        try (Connection connection = dbMgr.getSQLConnection()) {
            if (tableExists(connection, "RELEASE_INFO")) {
                releaseInfo = getDbVersion(connection);
            }
        }
        return releaseInfo;
    }

    private static boolean tableExists(Connection connection, final String tableName) throws SQLException {
        boolean tableExists;
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(null, null, tableName, null)) {
            tableExists = resultSet.next();
        }
        return tableExists;
    }

    private static ReleaseInfo getDbVersion(Connection connection) throws SQLException {
        String sql = "SELECT * FROM release_info;";
        ReleaseInfo releaseInfo = null;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery(sql)) {
                if (result.next()) {
                    int dbVersion = result.getInt("db_version");
                    log.info("DB version: {}", dbVersion);
                    releaseInfo = new ReleaseInfo();
                    releaseInfo.setId(1L);
                    releaseInfo.setDbVersion(dbVersion);
                }
            }
        }
        return releaseInfo;
    }

    private static boolean isLastUpgradeSucceeded() {
        return !new File(DB_UPGRADE_IN_PROGRESS_MARKER_FILE).exists();
    }

    private static void createUpgradeMarkerFile() {
        try {
            FileUtils.touch(new File(DB_UPGRADE_IN_PROGRESS_MARKER_FILE));
        } catch (IOException e) {
            log.error("Fail to create upgrade in progress marker file");
        }
    }

    private static void deleteUpgradeMarkerFile() {
        try {
            FileUtils.forceDelete(new File(DB_UPGRADE_IN_PROGRESS_MARKER_FILE));
        } catch (IOException e) {
            log.error("Fail to delete upgrade in progress marker file");
        }
    }

    private static void execSql(Statement stmt, String sql) throws SQLException {
        log.info("Execute sql: {}", sql);
        stmt.execute(sql);
    }
}
