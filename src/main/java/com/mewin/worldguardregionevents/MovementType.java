package com.mewin.worldguardregionevents;

/**
 * describes the way how a player left/entered a region
 *
 * @author mewin
 */
public enum MovementType {
	MOVE,
	TELEPORT,
	WORLD_CHANGE(false),
	SPAWN(false),
	DISCONNECT(false);

	private boolean cancellable;

	MovementType() {
		this.cancellable = true;
	}

	MovementType(boolean cancellable) {
		this.cancellable = cancellable;
	}

	public boolean isCancellable() {
		return cancellable;
	}
}
