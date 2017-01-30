package org.osc.core.broker.model.plugin;

@FunctionalInterface
public interface PluginTrackerCustomizer<T> {
	
	void pluginEvent(PluginEvent<T> event);
	
}
