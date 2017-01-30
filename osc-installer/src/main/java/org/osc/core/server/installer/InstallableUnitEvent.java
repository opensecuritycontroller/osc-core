package org.osc.core.server.installer;

public final class InstallableUnitEvent {

	private final State newState;
	private final InstallableUnit unit;
	private final State oldState;
	
	public InstallableUnitEvent(State oldState, State newState, InstallableUnit unit) {
		this.oldState = oldState;
		this.newState = newState;
		this.unit = unit;
	}
	
	/**
	 * Get the state of the unit before this event, which may be null if the unit did not previously exist.
	 */
	public State getOldState() {
		return oldState;
	}
	
	/**
	 * Get the new state of the unit.
	 */
	public State getNewState() {
		return newState;
	}
	
	public InstallableUnit getUnit() {
		return unit;
	}
	
}
