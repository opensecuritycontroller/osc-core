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

import org.osc.core.server.Server;
import org.osc.core.util.FileUtil;
import org.osc.core.util.KeyStoreProvider;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;
import org.osgi.service.component.annotations.Component;

@Component(service=DBConnectionParameters.class)
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
		this.url = properties.getProperty("db.connection.url", "");
		this.extraArgs = properties.getProperty("db.connection.url.extraArgs", "");
		this.login = properties.getProperty("db.connection.login", "");
		this.keystorePasswordAlias = properties.getProperty("db.connection.password.keystore.alias", "");
		this.keystorePasswordPassword = properties.getProperty("db.connection.password.keystore.password", "");
		this.defaultPasswordAlias = properties.getProperty("db.connection.default.password.keystore.alias", "");
		this.defaultPasswordPassword = properties.getProperty("db.connection.default.password.keystore.password", "");
	}

	public String getConnectionURL() {
		if(this.customConnectionURL != null) {
			return this.customConnectionURL;
		}

		return this.url + this.extraArgs;
	}

	public void setConnectionURL(String connectionUrl) {
		this.customConnectionURL = connectionUrl;

	}

	public String getLogin() {
		return this.login;
	}

	/**
	 * Updates the new DB password in keystore
	 * @param newPassword new password
	 * @throws KeyStoreProviderException
	 */
	public void updatePassword(String newPassword) throws KeyStoreProviderException {
		KeyStoreProvider.getInstance().putPassword(this.keystorePasswordAlias, newPassword, this.keystorePasswordPassword);
	}

	public void restoreDefaultPassword() throws KeyStoreProviderException {
		updatePassword(getDefaultPassword());
	}

	private String getDefaultPassword() throws KeyStoreProviderException {
		return KeyStoreProvider.getInstance().getPassword(this.defaultPasswordAlias, this.defaultPasswordPassword);
	}

	public String getPassword() throws KeyStoreProviderException {
		return KeyStoreProvider.getInstance().getPassword(this.keystorePasswordAlias, this.keystorePasswordPassword);
	}

	public boolean isDefaultPasswordSet() throws KeyStoreProviderException {
		return getPassword().equals(getDefaultPassword());

	}
}
