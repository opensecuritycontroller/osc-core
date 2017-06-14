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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osc.core.broker.service.api.plugin.PluginApi;
import org.osc.core.broker.service.api.plugin.PluginType;
import org.osc.core.server.installer.InstallableUnit;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;

public final class Plugin<T> implements PluginApi {

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

	@Override
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

	@Override
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
		org.osc.core.broker.service.api.plugin.PluginApi.State oldState = this.state;

		if (this.error != null) {
            this.state = org.osc.core.broker.service.api.plugin.PluginApi.State.ERROR;
        } else if (this.installUnit != null && !this.services.isEmpty()) {
            this.state = org.osc.core.broker.service.api.plugin.PluginApi.State.READY;
        } else {
            this.state = org.osc.core.broker.service.api.plugin.PluginApi.State.INSTALL_WAIT;
        }

		return !this.state.equals(oldState);
	}

    @Override
    public int getServiceCount() {
        return getServices().size();
    }

    @Override
    public PluginType getType() {
        if(ApplianceManagerApi.class.equals(this.pluginClass)) {
            return PluginType.MANAGER;
        }

        if(SdnControllerApi.class.equals(this.pluginClass)) {
            return PluginType.SDN;
        }

        throw new IllegalArgumentException("The plugin class " + this.pluginClass + " is not recognised");
    }

    @Override
    public String getSymbolicName() {
        InstallableUnit unit = getInstallUnit();
        return unit == null ? null : unit.getSymbolicName();
    }

    @Override
    public String getVersion() {
        InstallableUnit unit = getInstallUnit();
        return unit == null ? null : unit.getVersion();
    }

    @Override
    public String getName() {
        InstallableUnit unit = getInstallUnit();
        return unit == null ? null : unit.getName();
    }

    @Override
    public File getOrigin() {
        InstallableUnit unit = getInstallUnit();
        return unit == null ? null : unit.getOrigin();
    }

}
