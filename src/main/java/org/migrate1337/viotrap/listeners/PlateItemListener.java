package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class PlateItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, BlockData>> playerReplacedBlocks = new HashMap<>(); // Изменено на BlockData
    private final Map<String, ProtectedCuboidRegion> activePlates = new HashMap<>();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;

    public PlateItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle();
    }

    @EventHandler
    public void onPlayerUsePlate(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && PlateItem.getUniqueId(item) != null &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            if (plugin.getConfig().getString("plate.enable-pvp") == "true") {
                if (combatLogXHandler.isCombatLogXEnabled()) {
                    combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.ATTACKER);
                    player.sendMessage(plugin.getConfig().getString("plate.messages.pvp-enabled"));
                }
                if (pvpManagerHandler.isPvPManagerEnabled()) {
                    pvpManagerHandler.tagPlayerForPvP(player);
                    player.sendMessage(plugin.getConfig().getString("plate.messages.pvp-enabled"));
                }
            }

            Location location = player.getLocation();
            String worldName = location.getWorld().getName();
            savePlateToConfig(player, location);
            if (isInBannedRegion(location, location.getWorld().getName()) || hasBannedRegionFlags(location, location.getWorld().getName())) {
                player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
                return;
            }

            if (player.hasCooldown(item.getType())) {
                player.sendMessage(plugin.getPlateMessageCooldown());
                return;
            }

            int cooldownTicks = plugin.getPlateCooldown() * 20;
            player.setCooldown(item.getType(), cooldownTicks);

            if (isRegionNearby(location, player.getWorld().getName())) {
                player.sendMessage(plugin.getPlateMessageNearby());
                return;
            }

            String sound = plugin.getPlateSoundType();
            player.playSound(location, Sound.valueOf(sound), plugin.getPlateSoundVolume(), plugin.getPlateSoundPitch());
            item.setAmount(item.getAmount() - 1);

            try {
                DirectionInfo directionInfo = getOffsetsAndSchematic(player);

                File schematicFile = new File("plugins/WorldEdit/schematics/" + directionInfo.schematicName);
                plugin.getLogger().info(directionInfo.schematicName);
                Clipboard clipboard;

                try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                    BlockVector3 pastePosition = BlockVector3.at(
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ()
                    );

                    ClipboardHolder holder = new ClipboardHolder(clipboard);

                    Operations.complete(
                            holder
                                    .createPaste(editSession)
                                    .to(pastePosition)
                                    .ignoreAirBlocks(true)
                                    .build()
                    );
                    saveReplacedBlocks(player.getUniqueId(), location, clipboard, directionInfo.angX, directionInfo.angY, directionInfo.angZ);
                    createPlateRegion(player, location, directionInfo.pos1X, directionInfo.pos1Y, directionInfo.pos1Z, directionInfo.pos2X, directionInfo.pos2Y, directionInfo.pos2Z);
                    player.sendMessage(plugin.getConfig().getString("plate.messages.success_used"));
                    int durationTicks = plugin.getPlateDuration() * 20;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        restoreBlocks(player.getUniqueId());
                        removePlateRegion(player, location);
                        removePlateFromFile(location);
                        String soundEnded = plugin.getPlateSoundTypeEnded();
                        player.playSound(location, Sound.valueOf(soundEnded), plugin.getPlateSoundVolumeEnded(), plugin.getPlateSoundPitchEnded());
                    }, durationTicks);
                }

            } catch (Exception e) {
                player.sendMessage(plugin.getPlateMessageFailed());
                e.printStackTrace();
            }
        }
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (plugin.getConfig().getBoolean("plate.disabled_all_regions", false)) {
            return regionManager != null && regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .getRegions().stream().anyMatch(region -> !"__default__".equals(region.getId()));
        }

        if (regionManager == null) {
            return false;
        }

        List<String> bannedRegions = plugin.getConfig().getStringList("plate.banned_regions");
        BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        return regionManager.getApplicableRegions(vector).getRegions()
                .stream()
                .anyMatch(region -> bannedRegions.contains(region.getId()));
    }

    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager == null) {
            return false;
        }

        ConfigurationSection bannedFlagsSection = plugin.getConfig().getConfigurationSection("plate.banned_region_flags");
        if (bannedFlagsSection == null) {
            return false;
        }

        BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(vector).getRegions();

        for (ProtectedRegion region : regions) {
            for (String flagName : bannedFlagsSection.getKeys(false)) {
                StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                if (flag == null) {
                    continue;
                }

                if (region.getFlag(flag) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removeAllPlates() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.endsWith("plate_")) {
                        regionManager.removeRegion(regionName);
                    }
                }
            }
        }
        restoreAllBlocks();
        activePlates.clear();
    }

    private boolean isRegionNearby(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager != null) {
            BlockVector3 min = BlockVector3.at(location.getBlockX() - 3, location.getBlockY() - 3, location.getBlockZ() - 3);
            BlockVector3 max = BlockVector3.at(location.getBlockX() + 3, location.getBlockY() + 3, location.getBlockZ() + 3);

            ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("checkRegion", min, max);

            return regionManager.getApplicableRegions(checkRegion).getRegions().stream()
                    .anyMatch(region -> region.getId().endsWith("_trap") || region.getId().startsWith("plate_"));
        }
        return false;
    }

    private DirectionInfo getOffsetsAndSchematic(Player player) {
        float pitch = player.getLocation().getPitch();
        float yaw = player.getLocation().getYaw() % 360;
        if (yaw > 180) yaw -= 360;
        if (yaw < -180) yaw += 360;

        if (pitch < -45) {
            return new DirectionInfo(0, 0, 0, -2, 3, -2, 2, -4, 2, 2, 3, 2, plugin.getPlateUpSchematic());
        } else if (pitch > 45) {
            return new DirectionInfo(0, 0, 0, -2, -2, -2, -2, 1, 2, -2, -2, 2, plugin.getPlateDownSchematic());
        } else {
            if (yaw >= -22.5 && yaw <= 22.5) {
                return new DirectionInfo(0, 0, 0, -2, 0, 3, -2, 0, -4, -2, 5, 3, plugin.getPlateForwardSchematic());
            } else if (yaw > 22.5 && yaw <= 67.5) {
                return new DirectionInfo(0, 0, 0, -4, 0, 0, 4, 0, -4, 0, 5, 0, plugin.getPlateForwardLeftSchematic());
            } else if (yaw > -67.5 && yaw <= -22.5) {
                return new DirectionInfo(0, 0, 0, 0, 0, 0, -4, 0, -4, 0, 5, 0, plugin.getPlateForwardRightSchematic());
            } else if (yaw > 67.5 && yaw <= 112.5) {
                return new DirectionInfo(0, 0, 0, -4, 0, -2, 3, 0, -2, -4, 5, -2, plugin.getPlateLeftSchematic());
            } else if (yaw > 112.5 && yaw <= 157.5) {
                return new DirectionInfo(0, 0, 0, -4, 0, -4, 4, 0, 4, 0, 5, 0, plugin.getPlateBackwardLeftSchematic());
            } else if (yaw < -112.5 && yaw >= -157.5) {
                return new DirectionInfo(0, 0, 0, 0, 0, -4, -4, 0, 4, 0, 5, 0, plugin.getPlateBackwardRightSchematic());
            } else if ((yaw > 157.5 && yaw <= 180) || (yaw < -157.5 && yaw >= -180)) {
                return new DirectionInfo(0, 0, 0, -2, 0, -4, 2, 0, 3, 2, 5, -4, plugin.getPlateBackwardSchematic());
            } else if (yaw < -67.5 && yaw >= -112.5) {
                return new DirectionInfo(0, 0, 0, 3, 0, -2, -4, 0, 2, 3, 5, 2, plugin.getPlateRightSchematic());
            } else {
                return new DirectionInfo(0, 0, 0, -2, 0, -4, 2, 0, 3, 2, 5, -4, plugin.getPlateBackwardSchematic());
            }
        }
    }

    private void saveReplacedBlocks(UUID playerId, Location startLocation, Clipboard clipboard, int angX, int angY, int angZ) {
        Map<Location, BlockData> replacedBlocks = new HashMap<>();
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location blockLocation = startLocation.clone().add(
                            x - min.getBlockX() + angX,
                            y - min.getBlockY() + angY,
                            z - min.getBlockZ() + angZ
                    );

                    Block block = blockLocation.getBlock();
                    BlockState state = block.getState();

                    BlockData blockData = new BlockData(
                            block.getType(),
                            block.getBlockData(),
                            null,
                            null
                    );

                    if (state instanceof Container container) {
                        Inventory inventory = container.getInventory();

                        if (inventory.getHolder() instanceof DoubleChest doubleChest) {
                            blockData.setDoubleChest(true);
                            InventoryHolder leftHolder = doubleChest.getLeftSide();
                            InventoryHolder rightHolder = doubleChest.getRightSide();

                            if (leftHolder instanceof Chest leftChest &&
                                    rightHolder instanceof Chest rightChest) {
                                if (blockLocation.equals(leftChest.getLocation())) {
                                    blockData.setContents(inventory.getContents().clone());
                                    blockData.setPairedChestLocation(rightChest.getLocation().clone());
                                }
                            }
                        } else {
                            blockData.setContents(inventory.getContents().clone());
                        }

                        container.update();
                    }

                    if (state instanceof CreatureSpawner spawner) {
                        try {
                            EntityType spawnedType = spawner.getSpawnedType();
                            if (spawnedType != null) {
                                blockData.setSpawnedType(spawnedType.name());
                            }
                            spawner.update();
                        } catch (Exception e) {
                            plugin.getLogger().warning("[VioTrap] Ошибка при сохранении данных спавнера на " +
                                    blockLocation.toString() + ": " + e.getMessage());
                        }
                    }

                    if (state instanceof TileState tileState) {
                        tileState.update();
                    }

                    replacedBlocks.put(blockLocation.clone(), blockData);
                }
            }
        }

        playerReplacedBlocks.put(playerId, replacedBlocks);
    }

    private void createPlateRegion(Player player, Location location, int pos1X, int pos1Y, int pos1Z, int pos2X, int pos2Y, int pos2Z) {
        String regionName = "plate_" + player.getName();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, BlockVector3.at(location.getBlockX() - pos1X, location.getBlockY() - pos1Y, location.getBlockZ() - pos1Z),
                BlockVector3.at(location.getBlockX() + pos2X, location.getBlockY() + pos2Y, location.getBlockZ() + pos2Z));

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        region.setPriority(52);
        if (regionManager != null) {
            regionManager.addRegion(region);
            activePlates.put(regionName, region);
        }
        ConfigurationSection flagsSection = plugin.getConfig().getConfigurationSection("plate.flags");
        if (flagsSection != null) {
            for (String flagName : flagsSection.getKeys(false)) {
                try {
                    StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                    if (flag != null) {
                        String value = flagsSection.getString(flagName);
                        StateFlag.State state = StateFlag.State.valueOf(value.toUpperCase());
                        region.setFlag(flag, state);
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cНекорректное значение для флага " + flagName + " в конфиге.");
                }
            }
        }
    }

    private void removePlateRegion(Player player, Location location) {
        String regionName = "plate_" + player.getName();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager != null) {
            regionManager.removeRegion(regionName);
            activePlates.remove(regionName);
        }
    }

    public void restoreAllBlocks() {
        Bukkit.getLogger().info("[VioTrap] Вызван restoreAllBlocks() для пластов!");

        if (playerReplacedBlocks.isEmpty()) {
            Bukkit.getLogger().info("[VioTrap] playerReplacedBlocks пуст, загружаем данные из конфига...");
            loadPlatesFromConfig();
        }

        for (UUID playerId : new HashSet<>(playerReplacedBlocks.keySet())) {
            restoreBlocks(playerId);
        }

        Bukkit.getLogger().info("[VioTrap] Все пласты успешно восстановлены.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        restoreBlocks(playerId); // Восстанавливаем блоки при выходе
        playerReplacedBlocks.remove(playerId);
    }

    private void restoreBlocks(UUID playerId) {
        Map<Location, BlockData> replacedBlocks = playerReplacedBlocks.get(playerId);
        if (replacedBlocks != null) {
            for (Map.Entry<Location, BlockData> entry : replacedBlocks.entrySet()) {
                Location location = entry.getKey();
                BlockData blockData = entry.getValue();
                Block block = location.getBlock();
                block.setType(blockData.getMaterial());
                block.setBlockData(blockData.getBlockData());

                // Восстановление содержимого сундуков
                if (block.getState() instanceof Container container) {
                    if (blockData.isDoubleChest() && blockData.getContents() != null) {
                        container.getInventory().setContents(blockData.getContents());
                        Location pairedLocation = blockData.getPairedChestLocation();
                        if (pairedLocation != null) {
                            Block pairedBlock = pairedLocation.getBlock();
                            pairedBlock.setType(blockData.getMaterial());
                            pairedBlock.setBlockData(blockData.getBlockData());
                        }
                    } else if (blockData.getContents() != null) {
                        container.getInventory().setContents(blockData.getContents());
                    }
                }

                // Восстановление спавнеров
                if (block.getState() instanceof CreatureSpawner spawner) {
                    if (blockData.getSpawnedType() != null && !blockData.getSpawnedType().equals("UNKNOWN")) {
                        spawner.setSpawnedType(EntityType.valueOf(blockData.getSpawnedType()));
                        spawner.update();
                    }
                }

                removePlateFromFile(location);
            }
            playerReplacedBlocks.remove(playerId);
        }
    }

    private void loadPlatesFromConfig() {
        Bukkit.getLogger().info("[VioTrap] Загружаем пласты из plats.yml...");
        if (!plugin.getPlatesConfig().contains("plates")) return;

        ConfigurationSection platesSection = plugin.getPlatesConfig().getConfigurationSection("plates");
        for (String worldName : platesSection.getKeys(false)) {
            ConfigurationSection worldSection = platesSection.getConfigurationSection(worldName);
            if (worldSection == null) continue;

            for (String plateKey : worldSection.getKeys(false)) {
                ConfigurationSection plateSection = worldSection.getConfigurationSection(plateKey);
                if (plateSection == null) continue;

                UUID playerId = UUID.fromString(plateSection.getString("player"));
                String world = plateSection.getString("world");
                int x = plateSection.getInt("x");
                int y = plateSection.getInt("y");
                int z = plateSection.getInt("z");

                Location location = new Location(Bukkit.getWorld(world), x, y, z);
                Block block = location.getBlock();
                BlockData blockData = new BlockData(block.getType(), block.getBlockData(), null, null);

                playerReplacedBlocks.putIfAbsent(playerId, new HashMap<>());
                playerReplacedBlocks.get(playerId).put(location, blockData);
            }
        }
        Bukkit.getLogger().info("[VioTrap] Загружено " + playerReplacedBlocks.size() + " активных пластов.");
    }

    private void savePlateToConfig(Player player, Location location) {
        String path = "plates." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        plugin.getPlatesConfig().set(path + ".player", player.getUniqueId().toString());
        plugin.getPlatesConfig().set(path + ".world", location.getWorld().getName());
        plugin.getPlatesConfig().set(path + ".x", location.getBlockX());
        plugin.getPlatesConfig().set(path + ".y", location.getBlockY());
        plugin.getPlatesConfig().set(path + ".z", location.getBlockZ());
        plugin.savePlatesConfig();
    }

    private void removePlateFromFile(Location location) {
        String path = "plates." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        plugin.getPlatesConfig().set(path, null);
        plugin.savePlatesConfig();
    }

    private static class DirectionInfo {
        final int offsetX, offsetY, offsetZ;
        final int angX, angY, angZ;
        final int pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z;
        final String schematicName;

        DirectionInfo(int offsetX, int offsetY, int offsetZ, int angX, int angY, int angZ,
                      int pos1X, int pos1Y, int pos1Z, int pos2X, int pos2Y, int pos2Z,
                      String schematicName) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.angX = angX;
            this.angY = angY;
            this.angZ = angZ;
            this.pos1X = pos1X;
            this.pos1Y = pos1Y;
            this.pos1Z = pos1Z;
            this.pos2X = pos2X;
            this.pos2Y = pos2Y;
            this.pos2Z = pos2Z;
            this.schematicName = schematicName;
        }
    }

    private static class BlockData {
        private Material material;
        private org.bukkit.block.data.BlockData blockData;
        private ItemStack[] contents;
        private String spawnedType;
        private boolean isDoubleChest;
        private Location pairedChestLocation;

        public BlockData(Material material, org.bukkit.block.data.BlockData blockData, ItemStack[] contents, String spawnedType) {
            this.material = material;
            this.blockData = blockData;
            this.contents = contents;
            this.spawnedType = spawnedType;
            this.isDoubleChest = false;
            this.pairedChestLocation = null;
        }

        public Material getMaterial() {
            return material;
        }

        public org.bukkit.block.data.BlockData getBlockData() {
            return blockData;
        }

        public ItemStack[] getContents() {
            return contents;
        }

        public void setContents(ItemStack[] contents) {
            this.contents = contents;
        }

        public String getSpawnedType() {
            return spawnedType;
        }

        public void setSpawnedType(String spawnedType) {
            this.spawnedType = spawnedType;
        }

        public boolean isDoubleChest() {
            return isDoubleChest;
        }

        public void setDoubleChest(boolean isDoubleChest) {
            this.isDoubleChest = isDoubleChest;
        }

        public Location getPairedChestLocation() {
            return pairedChestLocation;
        }

        public void setPairedChestLocation(Location pairedChestLocation) {
            this.pairedChestLocation = pairedChestLocation;
        }
    }
}