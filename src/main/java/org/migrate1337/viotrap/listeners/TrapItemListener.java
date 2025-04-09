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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class TrapItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, BlockData>> playerReplacedBlocks = new HashMap<>();
    private final Map<String, ProtectedCuboidRegion> activeTraps = new HashMap<>();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    private final Set<UUID> playersInTrapRegions = new HashSet<>();

    public TrapItemListener(VioTrap plugin) {
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle();
        this.plugin = plugin;

    }

    @EventHandler
    public void onPlayerUseTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !TrapItem.isTrapItem(item)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (player.hasCooldown(item.getType())) {
            player.sendMessage(plugin.getTrapMessageCooldown());
            return;
        }

        Location location = player.getLocation();
        saveTrapToConfig(player, location);
        String skin = TrapItem.getSkin(item);
        String schematic = plugin.getSkinSchematic(skin);
        if (!plugin.getConfig().contains("skins." + skin)) {
            player.sendMessage("§cСкин не найден в конфигурации.");
            return;
        }

        if (isInBannedRegion(location, location.getWorld().getName()) || hasBannedRegionFlags(location, location.getWorld().getName())) {
            player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            return;
        }
        if (isRegionNearby(location, location.getWorld().getName())) {
            player.sendMessage(plugin.getTrapMessageNearby());
            return;
        }
        if(plugin.getConfig().getString("trap.enable-pvp") == "true"){
            if (combatLogXHandler.isCombatLogXEnabled()) {
                combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.ATTACKER);
                player.sendMessage(plugin.getConfig().getString("trap.messages.pvp-enabled"));
            }
            if (pvpManagerHandler.isPvPManagerEnabled()) {
                pvpManagerHandler.tagPlayerForPvP(player);
                player.sendMessage(plugin.getConfig().getString("trap.messages.pvp-enabled"));
            }
        }

        String soundType = plugin.getConfig().getString("skins." + skin + ".sound.type", plugin.getTrapSoundType());
        float soundVolume = (float) plugin.getConfig().getDouble("skins." + skin + ".sound.volume", plugin.getTrapSoundVolume());
        float soundPitch = (float) plugin.getConfig().getDouble("skins." + skin + ".sound.pitch", plugin.getTrapSoundPitch());

        player.playSound(location, Sound.valueOf(soundType), soundVolume, soundPitch);
        player.setCooldown(item.getType(), plugin.getTrapCooldown() * 20);
        try {
            File schematicFile = new File("plugins/WorldEdit/schematics/" + schematic);
            try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                BlockVector3 max = clipboard.getRegion().getMaximumPoint();

                double sizeX = max.getBlockX() - min.getBlockX() + 1;
                double sizeY = max.getBlockY() - min.getBlockY() + 1;
                double sizeZ = max.getBlockZ() - min.getBlockZ() + 1;

                applyEffects(player, "skins." + skin + ".effects.player");
                player.sendMessage(plugin.getConfig().getString("trap.messages.success_used"));
                location.getWorld().getNearbyEntities(location, sizeX - 3, sizeY, sizeZ - 3, entity -> entity instanceof Player && !entity.equals(player))
                        .forEach(entity -> {
                            if (entity instanceof Player opponent) {
                                applyEffects(opponent, "skins." + skin + ".effects.opponents");

                                enablePvpForPlayer(opponent);

                            }
                        });
                createTrapRegion(player, location, sizeX, sizeY, sizeZ);
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                    BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    saveReplacedBlocks(player.getUniqueId(), location, clipboard);

                    ClipboardHolder holder = new ClipboardHolder(clipboard);

                    Operations.complete(
                            holder
                                    .createPaste(editSession)
                                    .to(pastePosition)
                                    .ignoreAirBlocks(true)
                                    .build()
                    );


                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            restoreBlocks(player.getUniqueId());
                        }, 2L);

                        removeTrapRegion(player.getName() + "_trap", location);
                        removeTrapFromFile(location);
                        String soundTypeEnded = plugin.getConfig().getString("skins." + skin + ".sound.type-ended");
                        float soundVolumeEnded = (float) plugin.getConfig().getDouble("skins." + skin + ".sound.volume-ended");
                        float soundPitchEnded = (float) plugin.getConfig().getDouble("skins." + skin + ".sound.pitch-ended");
                        player.playSound(location, Sound.valueOf(soundTypeEnded), soundVolumeEnded, soundPitchEnded);
                    }, plugin.getTrapDuration() * 20);
                }

                item.setAmount(item.getAmount() - 1);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getTrapMessageFailed());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Location location = event.getTo();
        boolean wasInTrapRegion = playersInTrapRegions.contains(player.getUniqueId());
        boolean isNowInTrapRegion = isInAnyTrapRegion(location);

        if (!wasInTrapRegion && isNowInTrapRegion) {
            playersInTrapRegions.add(player.getUniqueId());
            enablePvpForPlayer(player);
            player.setAllowFlight(false);
        }
        else if (wasInTrapRegion && !isNowInTrapRegion) {
            playersInTrapRegions.remove(player.getUniqueId());
        }
    }

    private boolean isInAnyTrapRegion(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) {
            return false;
        }

        BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(vector).getRegions();

        for (ProtectedRegion region : regions) {
            if (region.getId().endsWith("_trap")) {
                return true;
            }
        }

        return false;
    }

    private void enablePvpForPlayer(Player player) {
        if (combatLogXHandler.isCombatLogXEnabled()) {
            combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.ATTACKER);
            player.sendMessage(plugin.getConfig().getString("trap.messages.pvp-enabled", "§cВы вошли в зону ловушки! Режим PVP активирован."));
        }

        if (pvpManagerHandler.isPvPManagerEnabled()) {
            pvpManagerHandler.tagPlayerForPvP(player);
            player.sendMessage(plugin.getConfig().getString("trap.messages.pvp-enabled", "§cВы вошли в зону ловушки! Режим PVP активирован."));
        }
    }

    private void removeTrapFromFile(Location location) {
        String path = "traps." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        plugin.getTrapsConfig().set(path, null);
        plugin.saveTrapsConfig();
    }
    private void saveTrapToConfig(Player player, Location location) {
        String path = "traps." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        plugin.getTrapsConfig().set(path + ".player", player.getUniqueId().toString());
        plugin.getTrapsConfig().set(path + ".world", location.getWorld().getName());
        plugin.getTrapsConfig().set(path + ".x", location.getBlockX());
        plugin.getTrapsConfig().set(path + ".y", location.getBlockY());
        plugin.getTrapsConfig().set(path + ".z", location.getBlockZ());
        plugin.saveTrapsConfig();
    }


    private void applyEffects(Player player, String configPath) {
        if (!plugin.getConfig().contains(configPath)) {
            return;
        }

        plugin.getConfig().getConfigurationSection(configPath).getKeys(false).forEach(effectName -> {
            try {
                int duration = plugin.getTrapDuration() * 20;
                int amplifier = plugin.getConfig().getInt(configPath + "." + effectName + ".amplifier");
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(effectName), duration, amplifier));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка применения эффекта: " + effectName);
            }
        });
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (plugin.getConfig().getBoolean("trap.disabled_all_regions", false)) {
            return regionManager != null && regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .getRegions().stream().anyMatch(region -> !"__default__".equals(region.getId()));
        }

        if (regionManager == null) {
            return false;
        }

        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("trap.banned_regions");
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

        ConfigurationSection bannedFlagsSection = plugin.getConfig().getConfigurationSection("trap.banned_region_flags");
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

    private void saveReplacedBlocks(UUID playerId, Location startLocation, Clipboard clipboard) {
        Map<Location, BlockData> replacedBlocks = new HashMap<>();
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        int sizeX = max.getBlockX() - min.getBlockX() + 1;
        int sizeY = max.getBlockY() - min.getBlockY() + 1;
        int sizeZ = max.getBlockZ() - min.getBlockZ() + 1;

        int offsetX = -(sizeX / 2);
        int offsetY = -(sizeY / 2) + 1;
        int offsetZ = -(sizeZ / 2);

        if (sizeX == 5 && sizeY == 5 && sizeZ == 5) {
            offsetY = -(sizeY / 2) + 1;
        } else if (sizeX == 7 && sizeY == 7 && sizeZ == 7) {
            offsetY = -(sizeY / 2) + 2;
        } else if (sizeX == 9 && sizeY == 9 && sizeZ == 9) {
            offsetY = -(sizeY / 2) + 2;
        }

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location blockLocation = startLocation.clone().add(
                            x - min.getBlockX() + offsetX,
                            y - min.getBlockY() + offsetY,
                            z - min.getBlockZ() + offsetZ
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


    public void restoreAllBlocks() {
        Bukkit.getLogger().info("[VioTrap] Вызван restoreAllBlocks()!");

        if (playerReplacedBlocks.isEmpty()) {
            Bukkit.getLogger().info("[VioTrap] playerReplacedBlocks пуст, загружаем данные из конфига...");
            loadTrapsFromConfig();
        }

        for (UUID playerId : new HashSet<>(playerReplacedBlocks.keySet())) {
            restoreBlocks(playerId);
        }

        Bukkit.getLogger().info("[VioTrap] Все блоки успешно восстановлены.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        restoreBlocks(playerId);
        if (playerReplacedBlocks.containsKey(playerId)) {
            playerReplacedBlocks.remove(playerId);
        }
        playersInTrapRegions.remove(playerId);
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

                if (block.getState() instanceof CreatureSpawner spawner) {
                    if (blockData.getSpawnedType() != null && !blockData.getSpawnedType().equals("UNKNOWN")) {
                        spawner.setSpawnedType(EntityType.valueOf(blockData.getSpawnedType()));
                        spawner.update();
                    }
                }
            }

            playerReplacedBlocks.remove(playerId);
        }
    }

    private void loadTrapsFromConfig() {
        Bukkit.getLogger().info("[VioTrap] Загружаем ловушки из traps.yml...");
        if (!plugin.getTrapsConfig().contains("traps")) return;

        ConfigurationSection trapsSection = plugin.getTrapsConfig().getConfigurationSection("traps");
        for (String worldName : trapsSection.getKeys(false)) {
            ConfigurationSection worldSection = trapsSection.getConfigurationSection(worldName);
            if (worldSection == null) continue;

            for (String trapKey : worldSection.getKeys(false)) {
                ConfigurationSection trapSection = worldSection.getConfigurationSection(trapKey);
                if (trapSection == null) continue;

                UUID playerId = UUID.fromString(trapSection.getString("player"));
                String world = trapSection.getString("world");
                int x = trapSection.getInt("x");
                int y = trapSection.getInt("y");
                int z = trapSection.getInt("z");

                Location location = new Location(Bukkit.getWorld(world), x, y, z);

                playerReplacedBlocks.putIfAbsent(playerId, new HashMap<>());

                Block block = location.getBlock();
                BlockData blockData = new BlockData(block.getType(), block.getBlockData(), null, null);
                playerReplacedBlocks.get(playerId).put(location, blockData);
            }
        }
        Bukkit.getLogger().info("[VioTrap] Загружено " + playerReplacedBlocks.size() + " активных ловушек.");
    }

    public void createTrapRegion(Player player, Location location, double sizeX, double sizeY, double sizeZ) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        BlockVector3 min = BlockVector3.at(location.getBlockX() - (sizeX / 2), location.getBlockY() - (sizeY / 2) + 2, location.getBlockZ() - (sizeZ / 2));
        BlockVector3 max = BlockVector3.at(location.getBlockX() + (sizeX / 2), location.getBlockY() + (sizeY / 2) + 1, location.getBlockZ() + (sizeZ / 2));
        if (regionManager == null) {
            return;
        }
        if(sizeX == 5 && sizeY == 5 && sizeZ == 5) {
            min = BlockVector3.at(location.getBlockX() - (sizeX / 2), location.getBlockY() - (sizeY / 2) + 2, location.getBlockZ() - (sizeZ / 2));
            max = BlockVector3.at(location.getBlockX() + (sizeX / 2), location.getBlockY() + (sizeY / 2) + 1, location.getBlockZ() + (sizeZ / 2));
        } else if (sizeX == 7 && sizeY == 7 && sizeZ == 7) {
            min = BlockVector3.at(location.getBlockX() - (sizeX / 2), location.getBlockY() - (sizeY / 2) + 3, location.getBlockZ() - (sizeZ / 2));
            max = BlockVector3.at(location.getBlockX() + (sizeX / 2), location.getBlockY() + (sizeY / 2) + 2, location.getBlockZ() + (sizeZ / 2));
        } else if (sizeX == 9 && sizeY == 9 && sizeZ == 9) {
            min = BlockVector3.at(location.getBlockX() - (sizeX / 2), location.getBlockY() - (sizeY / 2) + 4, location.getBlockZ() - (sizeZ / 2));
            max = BlockVector3.at(location.getBlockX() + (sizeX / 2), location.getBlockY() + (sizeY / 2) + 3, location.getBlockZ() + (sizeZ / 2));
        }
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(player.getName() + "_trap", min, max);

        ConfigurationSection flagsSection = plugin.getConfig().getConfigurationSection("trap.flags");
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
        region.setPriority(52);
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
    public void removeAllTraps() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.endsWith("_trap")) {
                        regionManager.removeRegion(regionName);
                    }
                }
            }
        }
        restoreAllBlocks();
        activeTraps.clear();
        playersInTrapRegions.clear();
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