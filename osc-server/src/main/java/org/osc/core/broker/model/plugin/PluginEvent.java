package org.osc.core.broker.model.plugin;

public final class PluginEvent<T> {

	public static enum Type { ADDING, MODIFIED, REMOVED }
	
	private final Type type;
	private final Plugin<T> plugin;
	
	public PluginEvent(Type type, Plugin<T> plugin) {
		this.type = type;
		this.plugin = plugin;
	}
	
	public Type getType() {
		return type;
	}
	
	public Plugin<T> getPlugin() {
		return plugin;
	}
	
}
