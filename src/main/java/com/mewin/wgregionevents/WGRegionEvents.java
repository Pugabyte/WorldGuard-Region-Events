package com.mewin.wgregionevents;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author mewin
 */
public class WGRegionEvents extends JavaPlugin {
	private static WGRegionEvents instance;

	public WGRegionEvents() {
		if (instance == null) {
			instance = this;
		} else {
			throw new IllegalStateException();
		}
	}

	public static WGRegionEvents getInstance() {
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

		WGRegionEventsListener listener = new WGRegionEventsListener(worldGuard);

		getServer().getPluginManager().registerEvents(listener, getInstance());
	}

	private WorldGuardPlugin getWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

		if (plugin instanceof WorldGuardPlugin)
			return (WorldGuardPlugin) plugin;
		return null;
	}
}
