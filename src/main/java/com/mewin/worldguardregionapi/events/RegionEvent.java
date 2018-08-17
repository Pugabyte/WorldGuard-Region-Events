package com.mewin.worldguardregionapi.events;

import com.mewin.worldguardregionapi.MovementType;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.*;

/**
 * @author mewin
 */
public abstract class RegionEvent extends PlayerEvent {

	private static final HandlerList handlerList = new HandlerList();

	private ProtectedRegion region;
	private MovementType movement;
	private Event parentEvent;

	public RegionEvent(ProtectedRegion region, Player player, MovementType movement, Event parent) {
		super(player);
		this.region = region;
		this.movement = movement;
		this.parentEvent = parent;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	public ProtectedRegion getRegion() {
		return region;
	}

	public MovementType getMovementType() {
		return this.movement;
	}

	public Event getParentEvent() {
		return parentEvent;
	}
}
