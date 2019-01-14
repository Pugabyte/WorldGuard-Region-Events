package com.mewin.worldguardregionapi;

import com.mewin.worldguardregionapi.events.RegionEnteringEvent;
import com.mewin.worldguardregionapi.events.RegionEnteredEvent;
import com.mewin.worldguardregionapi.events.RegionLeavingEvent;
import com.mewin.worldguardregionapi.events.RegionLeftEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * @author mewin
 */
public class PlayerListener implements Listener {
	private WorldGuard worldGuard;
	private WorldGuardRegionAPI plugin;

	public PlayerListener(WorldGuard worldGuard) {
		this.plugin = WorldGuardRegionAPI.getInstance();
		this.worldGuard = worldGuard;
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		clearRegions(event.getPlayer(), MovementType.DISCONNECT, event);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		clearRegions(event.getPlayer(), MovementType.DISCONNECT, event);
	}

	@EventHandler
	public void onPlayerChangeWorlds(PlayerChangedWorldEvent event) {
		clearRegions(event.getPlayer(), MovementType.WORLD_CHANGE, event);
		updateRegions(event.getPlayer(), MovementType.WORLD_CHANGE, event.getPlayer().getLocation(), event);
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		TeleportCause cause = event.getCause();
		MovementType movementType = MovementType.TELEPORT;
		if (cause == TeleportCause.END_PORTAL || cause == TeleportCause.NETHER_PORTAL) {
			clearRegions(event.getPlayer(), MovementType.WORLD_CHANGE, event);
			movementType = MovementType.WORLD_CHANGE;
		}
		updateRegions(event.getPlayer(), movementType, event.getTo(), event);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		event.setCancelled(updateRegions(event.getPlayer(), MovementType.MOVE, event.getTo(), event));
	}

	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		if (event.getVehicle().getPassengers() == null) return;
		List<Entity> passengers = event.getVehicle().getPassengers();
		for (Entity entity : passengers) {
			if (entity instanceof Player) {
				Player player = (Player) entity;
				updateRegions(player, MovementType.RIDE, player.getLocation(), event);
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		updateRegions(event.getPlayer(), MovementType.SPAWN, event.getPlayer().getLocation(), event);
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		updateRegions(event.getPlayer(), MovementType.SPAWN, event.getRespawnLocation(), event);
	}

	private void clearRegions(Player player, MovementType movementType, PlayerEvent event) {
		Set<ProtectedRegion> regions = plugin.getRegions(player);
		plugin.removePlayer(player);
		if (regions != null) {
			for (ProtectedRegion region : regions) {
				RegionLeavingEvent leavingEvent = new RegionLeavingEvent(region, player, movementType, event);
				RegionLeftEvent leftEvent = new RegionLeftEvent(region, player, movementType, event);

				plugin.getServer().getPluginManager().callEvent(leavingEvent);
				plugin.getServer().getPluginManager().callEvent(leftEvent);
			}
		}
	}

	private synchronized boolean updateRegions(final Player player, MovementType movement, Location newLocation, final Event event) {
		Set<ProtectedRegion> regions;
		Set<ProtectedRegion> oldRegions;

		if (plugin.getPlayers().get(player) == null) {
			regions = new HashSet<>();
		} else {
			regions = plugin.getRegions(player);
		}

		oldRegions = new HashSet<>(regions);

		RegionManager regionManager = worldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(newLocation.getWorld()));
		RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();

		if (regionManager == null) {
			return false;
		}

		Set<ProtectedRegion> applicableRegions = query.getApplicableRegions(BukkitAdapter.adapt(newLocation)).getRegions();
		ProtectedRegion globalRegion = regionManager.getRegion("__global__");
		if (globalRegion != null) // just to be sure
		{
			applicableRegions.add(globalRegion);
		}

		for (final ProtectedRegion region : applicableRegions) {
			if (!regions.contains(region)) {
				RegionEnteringEvent regionEnterEvent = new RegionEnteringEvent(region, player, movement, event);

				plugin.getServer().getPluginManager().callEvent(regionEnterEvent);

				if (regionEnterEvent.isCancelled()) {
					regions.clear();
					regions.addAll(oldRegions);

					return true;
				} else {
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						RegionEnteredEvent regionEnteredEvent = new RegionEnteredEvent(region, player, movement, event);
						plugin.getServer().getPluginManager().callEvent(regionEnteredEvent);
					}, 1L);
					regions.add(region);
				}
			}
		}

		Iterator<ProtectedRegion> itr = regions.iterator();
		while (itr.hasNext()) {
			final ProtectedRegion region = itr.next();
			if (!applicableRegions.contains(region)) {
				if (regionManager.getRegion(region.getId()) != region) {
					itr.remove();
					continue;
				}
				RegionLeavingEvent leavingEvent = new RegionLeavingEvent(region, player, movement, event);

				plugin.getServer().getPluginManager().callEvent(leavingEvent);

				if (leavingEvent.isCancelled()) {
					regions.clear();
					regions.addAll(oldRegions);
					return true;
				} else {
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						RegionLeftEvent regionLeftEvent = new RegionLeftEvent(region, player, movement, event);
						plugin.getServer().getPluginManager().callEvent(regionLeftEvent);
					}, 1L);
					itr.remove();
				}
			}
		}
		plugin.setRegions(player, regions);
		return false;
	}
}
