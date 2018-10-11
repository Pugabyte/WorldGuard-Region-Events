package com.mewin.worldguardregionapi;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mewin
 */
public class WorldGuardRegionAPI extends JavaPlugin {
	private static WorldGuardRegionAPI instance;
	private Map<Player, Set<ProtectedRegion>> players = new HashMap<>();

	public WorldGuardRegionAPI() {
		if (instance == null) {
			instance = this;
		} else {
			throw new IllegalStateException();
		}
	}

	public static WorldGuardRegionAPI getInstance() {
		if (instance == null) {
			throw new IllegalStateException();
		}
		return instance;
	}

	@Override
	public void onEnable() {
		WorldGuardPlugin worldGuard = getWorldGuard();
		if (worldGuard == null) {
			getLogger().severe("Could not find WorldGuard, disabling.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		PlayerListener listener = new PlayerListener(worldGuard);

		getServer().getPluginManager().registerEvents(listener, getInstance());
	}

	private WorldGuardPlugin getWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

		if (plugin instanceof WorldGuardPlugin)
			return (WorldGuardPlugin) plugin;
		return null;
	}

	public Map<Player, Set<ProtectedRegion>> getPlayers() {
		return players;
	}

	protected void setPlayers(Map<Player, Set<ProtectedRegion>> players) {
		this.players = players;
	}

	protected void removePlayer(Player player) {
		players.remove(player);
	}

	public Set<ProtectedRegion> getRegions(Player player) {
		return players.get(player);
	}

	public Set<String> getRegionNames(Player player) {
		Set<ProtectedRegion> regions = players.get(player);
		Set<String> regionNames = regions.stream().map(ProtectedRegion::getId).collect(Collectors.toSet());
		return regionNames;
	}

	protected void setRegions(Player player, Set<ProtectedRegion> regions) {
		this.players.put(player, regions);
	}

	protected void addRegion(Player player, ProtectedRegion region) {
		Set<ProtectedRegion> regions = getRegions(player);
		regions.add(region);
		this.players.put(player, regions);
	}

	protected void removeRegion(Player player, ProtectedRegion region) {
		Set<ProtectedRegion> regions = getRegions(player);
		regions.remove(region);
		this.players.put(player, regions);
	}
}
