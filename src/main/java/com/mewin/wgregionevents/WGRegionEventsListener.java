package com.mewin.wgregionevents;

import com.mewin.wgregionevents.events.RegionEnterEvent;
import com.mewin.wgregionevents.events.RegionEnteredEvent;
import com.mewin.wgregionevents.events.RegionLeaveEvent;
import com.mewin.wgregionevents.events.RegionLeftEvent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.*;

/**
 *
 * @author mewin
 */
public class WGRegionEventsListener implements Listener
{
    private WorldGuardPlugin worldGuard;
    private WGRegionEvents plugin;

    private Map<Player, Set<ProtectedRegion>> playerRegions;

    public WGRegionEventsListener(WorldGuardPlugin worldGuard)
    {
        this.plugin = WGRegionEvents.getInstance();
        this.worldGuard = worldGuard;

        playerRegions = new HashMap<>();
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e)
    {
        Set<ProtectedRegion> regions = playerRegions.remove(e.getPlayer());
        if (regions != null)
        {
            for(ProtectedRegion region : regions)
            {
                RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, e.getPlayer(), MovementType.DISCONNECT, e);
                RegionLeftEvent leftEvent = new RegionLeftEvent(region, e.getPlayer(), MovementType.DISCONNECT, e);

                plugin.getServer().getPluginManager().callEvent(leaveEvent);
                plugin.getServer().getPluginManager().callEvent(leftEvent);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e)
    {
        Set<ProtectedRegion> regions = playerRegions.remove(e.getPlayer());
        if (regions != null)
        {
            for(ProtectedRegion region : regions)
            {
                RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, e.getPlayer(), MovementType.DISCONNECT, e);
                RegionLeftEvent leftEvent = new RegionLeftEvent(region, e.getPlayer(), MovementType.DISCONNECT, e);

                plugin.getServer().getPluginManager().callEvent(leaveEvent);
                plugin.getServer().getPluginManager().callEvent(leftEvent);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        e.setCancelled(updateRegions(e.getPlayer(), MovementType.MOVE, e.getTo(), e));
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e)
    {
        e.setCancelled(updateRegions(e.getPlayer(), MovementType.TELEPORT, e.getTo(), e));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        updateRegions(e.getPlayer(), MovementType.SPAWN, e.getPlayer().getLocation(), e);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e)
    {
        updateRegions(e.getPlayer(), MovementType.SPAWN, e.getRespawnLocation(), e);
    }

    private synchronized boolean updateRegions(final Player player, final MovementType movement, Location newLocation, final PlayerEvent event)
    {
        Set<ProtectedRegion> regions;
        Set<ProtectedRegion> oldRegions;

        if (playerRegions.get(player) == null)
        {
            regions = new HashSet<>();
        }
        else
        {
            regions = new HashSet<>(playerRegions.get(player));
        }

        oldRegions = new HashSet<>(regions);

        RegionManager regionManager = worldGuard.getRegionManager(newLocation.getWorld());

        if (regionManager == null)
        {
            return false;
        }

        HashSet<ProtectedRegion> applicableRegions = new HashSet<>(regionManager.getApplicableRegions(newLocation).getRegions());
        ProtectedRegion globalRegion = regionManager.getRegion("__global__");
        if (globalRegion != null) // just to be sure
        {
            applicableRegions.add(globalRegion);
        }

        for (final ProtectedRegion region : applicableRegions)
        {
            if (!regions.contains(region))
            {
                RegionEnterEvent regionEnterEvent = new RegionEnterEvent(region, player, movement, event);

                plugin.getServer().getPluginManager().callEvent(regionEnterEvent);

                if (regionEnterEvent.isCancelled())
                {
                    regions.clear();
                    regions.addAll(oldRegions);

                    return true;
                }
                else
                {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        RegionEnteredEvent regionEnteredEvent = new RegionEnteredEvent(region, player, movement, event);
                        plugin.getServer().getPluginManager().callEvent(regionEnteredEvent);
                    }, 1L);
                    regions.add(region);
                }
            }
        }

        Iterator<ProtectedRegion> itr = regions.iterator();
        while(itr.hasNext())
        {
            final ProtectedRegion region = itr.next();
            if (!applicableRegions.contains(region))
            {
                if (regionManager.getRegion(region.getId()) != region)
                {
                    itr.remove();
                    continue;
                }
                RegionLeaveEvent regionLeaveEvent = new RegionLeaveEvent(region, player, movement, event);

                plugin.getServer().getPluginManager().callEvent(regionLeaveEvent);

                if (regionLeaveEvent.isCancelled())
                {
                    regions.clear();
                    regions.addAll(oldRegions);
                    return true;
                }
                else
                {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        RegionLeftEvent regionLeftEvent = new RegionLeftEvent(region, player, movement, event);
                        plugin.getServer().getPluginManager().callEvent(regionLeftEvent);
                    }, 1L);
                    itr.remove();
                }
            }
        }
        playerRegions.put(player, regions);
        return false;
    }
}
