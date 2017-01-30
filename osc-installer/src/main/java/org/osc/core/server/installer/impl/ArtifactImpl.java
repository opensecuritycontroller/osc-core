package org.osc.core.server.installer.impl;

import org.osc.core.server.installer.Artifact;
import org.osc.core.server.installer.Hash;

class ArtifactImpl implements Artifact {

	private final String name;
	private final String location;
	private final Hash hash;

	ArtifactImpl(String name, String location, Hash hash) {
		if (location == null) {
            throw new IllegalArgumentException("Artifact location may not be null");
        }
		this.name = name;
		this.location = location;
		this.hash = hash;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Hash getHash() {
		return this.hash;
	}

	@Override
	public String getLocation() {
		return this.location;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.name).append(":").append(this.hash != null ? this.hash : "<no-hash>").append("@").append(this.location).toString();
	}

}
