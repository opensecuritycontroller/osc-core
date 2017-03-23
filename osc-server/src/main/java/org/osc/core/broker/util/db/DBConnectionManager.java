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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * This component provides a single OSGi service which offers access to
 * ready-configured database resources. Database access must not be set
 * up manually by other components.
 */
@Component(service=DBConnectionManager.class, configurationPid="org.osc.core.broker.util.db",
configurationPolicy=ConfigurationPolicy.REQUIRE)
public class DBConnectionManager {

    private static final Logger log = Logger.getLogger(DBConnectionManager.class);

    @Reference
    DBConnectionParameters connectionParams;

    @Reference(target="(osgi.unit.name=osc-server)")
    EntityManagerFactoryBuilder emfBuilder;

    @Reference(target="(osgi.jdbc.driver.class=org.h2.Driver)")
    DataSourceFactory dsf;

    private volatile EntityManagerFactory emf;

    private volatile DataSource ds;

    private Map<String, Object> properties;

    @Activate
    void start(Map<String, Object> properties) throws Exception {

        this.properties = properties;
        Properties jdbcProps = new Properties();
        jdbcProps.setProperty(DataSourceFactory.JDBC_URL, this.connectionParams.getConnectionURL());
        jdbcProps.setProperty(DataSourceFactory.JDBC_USER, this.connectionParams.getLogin());
        jdbcProps.setProperty(DataSourceFactory.JDBC_PASSWORD, this.connectionParams.getPassword());

        this.ds = this.dsf.createDataSource(jdbcProps);


        Map<String, Object> jpaProps = new HashMap<>(properties);

        jpaProps.put("javax.persistence.dataSource", this.ds);

        this.emf = this.emfBuilder.createEntityManagerFactory(jpaProps);

    }

    @Deactivate
    void shutdown() {
        // Close caches and connection pools
        try {
            this.emf.close();
        } catch (PersistenceException e) {
            log.error("Error during shutdown of EMF.", e);
            // Ignore errors
        }

        try (Connection conn = this.ds.getConnection()) {
            conn.prepareCall("SHUTDOWN COMPACT").executeUpdate();
        } catch (SQLException e) {
            log.error("Error during shutdown of DB.", e);
            // Ignore errors
        }
    }

    public EntityManagerFactory getEmf() {
        return this.emf;
    }

    public Connection getSQLConnection() throws SQLException {
        return this.ds.getConnection();
    }

    public  void replaceDefaultDBPassword() throws Exception {
        try (Connection connection = getSQLConnection();
                PreparedStatement changePasswordStatement = connection.prepareStatement("ALTER USER " + this.connectionParams.getLogin().toUpperCase() + " SET PASSWORD ?")) {

            connection.setAutoCommit(false);

            String newPassword = RandomStringUtils.randomAscii(32);

            try {
                // change password in DB
                changePasswordStatement.setString(1, newPassword);
                changePasswordStatement.execute();

                // put password in keystore
                this.connectionParams.updatePassword(newPassword);

                connection.commit();
            } catch (Exception ex) {
                log.error("Error while changing DB password.", ex);

                try {
                    connection.rollback();
                } catch (Exception he) {
                    log.error("Error rolling back transaction", he);
                }

                throw ex;
            }
            EntityManagerFactory oldEmf = this.emf;
            // reinitialize session factory
            start(this.properties);

            try {
                oldEmf.close();
            } catch (PersistenceException pe) {
                // Ignore this
            }
        }
    }
}
