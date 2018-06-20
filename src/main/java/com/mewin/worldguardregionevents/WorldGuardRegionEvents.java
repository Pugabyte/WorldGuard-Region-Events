package com.mewin.worldguardregionevents;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author mewin
 */
public class WorldGuardRegionEvents extends JavaPlugin {
	private static WorldGuardRegionEvents instance;

	public WorldGuardRegionEvents() {
		if (instance == null) {
			instance = this;
		} else {
			throw new IllegalStateException();
		}
	}

	public static WorldGuardRegionEvents getInstance() {
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
}
