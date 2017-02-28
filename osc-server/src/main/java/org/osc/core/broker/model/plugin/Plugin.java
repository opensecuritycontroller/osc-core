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
package org.osc.core.broker.model.plugin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osc.core.server.installer.InstallableUnit;

public final class Plugin<T> {

	public static enum State {
		/**
		 * Plugin installed but no service published yet.
		 */
		INSTALL_WAIT,

		/**
		 * Plugin installed and service published.
		 */
		READY,

		/**
		 * Plugin not installed or ready due to an error.
		 */
		ERROR;
	}

	private final Class<T> pluginClass;
	private final List<T> services = new LinkedList<>();

	private InstallableUnit installUnit;
	private State state = State.INSTALL_WAIT;
	private String error = null;

	Plugin(Class<T> pluginClass) {
		this.pluginClass = pluginClass;
	}

	public Class<T> getPluginClass() {
		return this.pluginClass;
	}

	public synchronized State getState() {
		return this.state;
	}

	/**
	 * Return a snapshot of the Plugin API services provided by the plugin.
	 */
	public synchronized List<T> getServices() {
		return new ArrayList<>(this.services);
	}

	/**
	 * Get the installable unit: NB this can be {@code null} for short times.
	 */
	public synchronized InstallableUnit getInstallUnit() {
		return this.installUnit;
	}

	public synchronized String getError() {
		return this.error;
	}

	/**
	 * @param error
	 * @return
	 */
	synchronized boolean setError(String newError) {
		boolean changedError;
		if (this.error == null) {
            changedError = newError != null;
        } else {
            changedError = !this.error.equals(newError);
        }
		this.error = newError;
		return updateState() || changedError;
	}

	/**
	 * Set the installable unit
	 * @return Whether the Plugin has been modified.
	 */
	synchronized boolean setUnit(InstallableUnit unit) {
		boolean changedUnit = (this.installUnit != unit);
		this.installUnit = unit;
		return updateState() || changedUnit;
	}

	/**
	 * Add a service.
	 * @return Whether the Plugin has been modified
	 */
	synchronized boolean addService(T service) {
		boolean changedServices = this.services.add(service);
		return updateState() || changedServices;
	}

	/**
	 * Remove a service
	 * @return Whether the Plugin state has been modified
	 */
	synchronized boolean removeService(T service) {
		boolean changedServices = this.services.remove(service);
		return updateState() || changedServices;
	}

	private synchronized boolean updateState() {
		State oldState = this.state;

		if (this.error != null) {
            this.state = State.ERROR;
        } else if (this.installUnit != null && !this.services.isEmpty()) {
            this.state = State.READY;
        } else {
            this.state = State.INSTALL_WAIT;
        }

		return !this.state.equals(oldState);
	}

}
