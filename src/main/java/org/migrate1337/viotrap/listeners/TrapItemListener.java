package org.migrate1337.viotrap.listeners;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.TrapItem;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, Material>> playerReplacedBlocks = new HashMap<>();
    private final Map<UUID, Long> lastUseTime = new HashMap<>();
    private final Map<String, ProtectedCuboidRegion> activeTraps = new HashMap<>();
    public TrapItemListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && TrapItem.getUniqueId(item) != null &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            long currentTime = System.currentTimeMillis();
            long cooldownMillis = plugin.getPlateCooldown() * 1000L; // Переводим секунды в миллисекунды

            if (lastUseTime.containsKey(playerId)) {
                long lastUsed = lastUseTime.get(playerId);
                long timeRemaining = cooldownMillis - (currentTime - lastUsed);

                if (timeRemaining > 0) {
                    long secondsRemaining = timeRemaining / 1000; // Переводим в секунды для вывода
                    player.sendMessage(plugin.getTrapMessageCooldown().replace("{time}", String.valueOf(secondsRemaining)));
                    return;
                }
            }

            lastUseTime.put(playerId, currentTime);

            Location location = player.getLocation();
            if (isRegionNearby(location, player.getWorld().getName())) {
                player.sendMessage(plugin.getTrapMessageNearby());
                return;
            }
            String sound = plugin.getTrapSoundType();
            player.playSound(location, Sound.valueOf(sound), plugin.getTrapSoundVolume(), plugin.getTrapSoundPitch());
            lastUseTime.put(playerId, currentTime);
            item.setAmount(item.getAmount() - 1);

            try {
                File schematicFile = new File("plugins/WorldEdit/schematics/" + plugin.getTrapSchematic());
                Clipboard clipboard;

                try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                createTrapRegion(player, location);

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                    BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    saveReplacedBlocks(playerId, location, clipboard);

                    ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), editSession, pastePosition);
                    Operations.complete((Operation) copy);
                    int durationTicks = plugin.getTrapDuration() * 20;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        restoreBlocks(playerId);
                        removeTrapRegion(player.getName() + "_trap", location);
                    }, durationTicks);
                }

            } catch (Exception e) {
                player.sendMessage(plugin.getTrapMessageFailed());
                e.printStackTrace();
            }
        }
    }

    private boolean isRegionNearby(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager != null) {
            com.sk89q.worldedit.math.BlockVector3 min = BlockVector3.at(location.getBlockX() - 3, location.getBlockY() - 3, location.getBlockZ() - 3);
            com.sk89q.worldedit.math.BlockVector3 max = BlockVector3.at(location.getBlockX() + 3, location.getBlockY() + 3, location.getBlockZ() + 3);

            ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("checkRegion", min, max);
            return regionManager.getApplicableRegions(checkRegion).size() > 0;
        }
        return false;
    }

    private void saveReplacedBlocks(UUID playerId, Location startLocation, Clipboard clipboard) {
        Map<Location, Material> replacedBlocks = new HashMap<>();
        com.sk89q.worldedit.math.BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        com.sk89q.worldedit.math.BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        int offsetX = -2;
        int offsetY = -1;
        int offsetZ = -2;

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location blockLocation = startLocation.clone().add(
                            x - min.getBlockX() + offsetX,
                            y - min.getBlockY() + offsetY,
                            z - min.getBlockZ() + offsetZ
                    );
                    replacedBlocks.put(blockLocation, blockLocation.getBlock().getType());
                }
            }
        }

        playerReplacedBlocks.put(playerId, replacedBlocks);
    }

    private void restoreBlocks(UUID playerId) {
        Map<Location, Material> replacedBlocks = playerReplacedBlocks.get(playerId);
        if (replacedBlocks != null) {
            for (Map.Entry<Location, Material> entry : replacedBlocks.entrySet()) {
                Location location = entry.getKey();
                Material originalMaterial = entry.getValue();
                location.getBlock().setType(originalMaterial);
            }
            playerReplacedBlocks.remove(playerId);
        }
    }

    public void createTrapRegion(Player player, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) {
            return;
        }

        com.sk89q.worldedit.math.BlockVector3 min = BlockVector3.at(location.getBlockX() - 2, location.getBlockY() - 2, location.getBlockZ() - 2);
        com.sk89q.worldedit.math.BlockVector3 max = BlockVector3.at(location.getBlockX() + 2, location.getBlockY() + 3, location.getBlockZ() + 2);

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(player.getName() + "_trap", min, max);
        DefaultDomain domain = new DefaultDomain();
        domain.addPlayer(player.getUniqueId());
        region.setOwners(domain);

        regionManager.addRegion(region);
        activeTraps.put(player.getName() + "_trap", region);
    }

    public void removeTrapRegion(String regionName, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager != null) {
            regionManager.removeRegion(regionName);
            activeTraps.remove(regionName);
        }
    }
}