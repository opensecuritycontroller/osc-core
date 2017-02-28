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

import java.io.IOException;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.osc.core.util.FileUtil;
import org.osc.core.util.KeyStoreProvider;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;

import com.mcafee.vmidc.server.Server;

public class DBConnectionParameters {
	private String url;
	private String extraArgs;
	private String login;
	// alias to access db user password in keystore
	private String keystorePasswordAlias;
	// password to access db user password in keystore
	private String keystorePasswordPassword;
	// alias to default db user password in keystore
	private String defaultPasswordAlias;
	// password to default db user password in keystore
	private String defaultPasswordPassword;
	
	// CUSTOM USER SETTINGS
	private String customConnectionURL;
	
	public DBConnectionParameters() throws IOException {
		Properties properties = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
		url = properties.getProperty("db.connection.url", "");
		extraArgs = properties.getProperty("db.connection.url.extraArgs", "");
		login = properties.getProperty("db.connection.login", "");
		keystorePasswordAlias = properties.getProperty("db.connection.password.keystore.alias", "");
		keystorePasswordPassword = properties.getProperty("db.connection.password.keystore.password", "");
		defaultPasswordAlias = properties.getProperty("db.connection.default.password.keystore.alias", "");
		defaultPasswordPassword = properties.getProperty("db.connection.default.password.keystore.password", "");
	}
	
	public String getConnectionURL() {
		if(customConnectionURL != null) {
			return customConnectionURL;
		}
		
		return url + extraArgs;
	}
	
	public void setConnectionURL(String connectionUrl) {
		this.customConnectionURL = connectionUrl;
		
	}
	
	public String getLogin() {
		return login;
	}
	
	/**
	 * Updates the new DB password in keystore
	 * @param newPassword new password
	 * @throws KeyStoreProviderException 
	 */
	public void updatePassword(String newPassword) throws KeyStoreProviderException {
		KeyStoreProvider.getInstance().putPassword(keystorePasswordAlias, newPassword, keystorePasswordPassword);
	}
	
	public void restoreDefaultPassword() throws KeyStoreProviderException {
		updatePassword(getDefaultPassword());
	}
	
	private String getDefaultPassword() throws KeyStoreProviderException {
		return KeyStoreProvider.getInstance().getPassword(defaultPasswordAlias, defaultPasswordPassword);
	}
	
	public String getPassword() throws KeyStoreProviderException {
		return KeyStoreProvider.getInstance().getPassword(keystorePasswordAlias, keystorePasswordPassword);
	}
	
	public boolean isDefaultPasswordSet() throws KeyStoreProviderException {
		return getPassword().equals(getDefaultPassword());
				
	}

	public void fillHibernateConfiguration(Configuration configuration) throws KeyStoreProviderException {
		configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");

        configuration.setProperty("hibernate.connection.url", getConnectionURL());
        configuration.setProperty("hibernate.connection.username", getLogin());
        configuration.setProperty("hibernate.connection.password", getPassword());

        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.show_sql", "false");
        configuration.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.internal.NoCacheProvider");
        configuration.setProperty("hibernate.connection.pool_size", "5");
        configuration.setProperty("hibernate.current_session_context_class", "thread");
        
        // Update schema based on entities for dev convenience. Remove before release.
        //configuration.setProperty("hibernate.hbm2ddl.auto", "update");
	}
}
