package com.mewin.worldguardregionevents.events;

import com.mewin.worldguardregionevents.MovementType;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.*;

/**
 * @author mewin
 */
public abstract class RegionEvent extends PlayerEvent {

	private static final HandlerList handlerList = new HandlerList();

	private ProtectedRegion region;
	private MovementType movement;
	private PlayerEvent parentEvent;

	public RegionEvent(ProtectedRegion region, Player player, MovementType movement, PlayerEvent parent) {
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

	/**
	 * retrieves the event that has been used to create this event
	 *
	 * @return
	 * @see PlayerMoveEvent
	 * @see PlayerTeleportEvent
	 * @see PlayerQuitEvent
	 * @see PlayerKickEvent
	 * @see PlayerJoinEvent
	 * @see PlayerRespawnEvent
	 */
	public PlayerEvent getParentEvent() {
		return parentEvent;
	}
}
