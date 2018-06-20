package com.mewin.worldguardregionapi;

import com.mewin.worldguardregionapi.events.RegionEnterEvent;
import com.mewin.worldguardregionapi.events.RegionEnteredEvent;
import com.mewin.worldguardregionapi.events.RegionLeaveEvent;
import com.mewin.worldguardregionapi.events.RegionLeftEvent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * @author mewin
 */
public class PlayerListener implements Listener {
	private WorldGuardPlugin worldGuard;
	private WorldGuardRegionAPI plugin;

	public PlayerListener(WorldGuardPlugin worldGuard) {
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
				RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, player, movementType, event);
				RegionLeftEvent leftEvent = new RegionLeftEvent(region, player, movementType, event);

				plugin.getServer().getPluginManager().callEvent(leaveEvent);
				plugin.getServer().getPluginManager().callEvent(leftEvent);
			}
		}
	}

	private synchronized boolean updateRegions(final Player player, MovementType movement, Location newLocation, final PlayerEvent event) {
		Set<ProtectedRegion> regions;
		Set<ProtectedRegion> oldRegions;

		if (plugin.getPlayers().get(player) == null) {
			regions = new HashSet<>();
		} else {
			regions = plugin.getRegions(player);
		}

		oldRegions = new HashSet<>(regions);

		RegionManager regionManager = worldGuard.getRegionManager(newLocation.getWorld());

		if (regionManager == null) {
			return false;
		}

		HashSet<ProtectedRegion> applicableRegions = new HashSet<>(regionManager.getApplicableRegions(newLocation).getRegions());
		ProtectedRegion globalRegion = regionManager.getRegion("__global__");
		if (globalRegion != null) // just to be sure
		{
			applicableRegions.add(globalRegion);
		}

		for (final ProtectedRegion region : applicableRegions) {
			if (!regions.contains(region)) {
				RegionEnterEvent regionEnterEvent = new RegionEnterEvent(region, player, movement, event);

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
				RegionLeaveEvent regionLeaveEvent = new RegionLeaveEvent(region, player, movement, event);

				plugin.getServer().getPluginManager().callEvent(regionLeaveEvent);

				if (regionLeaveEvent.isCancelled()) {
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
