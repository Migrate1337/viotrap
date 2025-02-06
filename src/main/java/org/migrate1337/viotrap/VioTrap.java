package org.migrate1337.viotrap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.migrate1337.viotrap.commands.ApplySkinCommand;
import org.migrate1337.viotrap.commands.CreateSkinCommand;
import org.migrate1337.viotrap.commands.GiveItemCommand;
import org.migrate1337.viotrap.gui.SkinCreationMenu;
import org.migrate1337.viotrap.listeners.*;
import org.migrate1337.viotrap.listeners.ChatListener;
import org.migrate1337.viotrap.utils.GiveItemTabCompleter;
import org.migrate1337.viotrap.utils.ChatInputHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class VioTrap extends JavaPlugin {

    private static VioTrap plugin;
    private static FileConfiguration config;

    private String trapDisplayName;
    private List<String> trapDescription;
    private String trapType;
    private int trapCooldown;
    private int trapDuration;

    private String trapSchematic;
    private String trapMessageNearby;
    private String trapMessageCooldown;
    private String trapMessageFailed;
    private String trapSoundType;
    private float trapSoundVolume;
    private float trapSoundPitch;
    private String plateDisplayName;
    private List<String> plateDescription;
    private String plateType;
    private int plateCooldown;
    private int plateDuration;
    private String plateMessageNearby;
    private String plateMessageCooldown;
    private String plateMessageFailed;
    private String plateSoundType;
    private float plateSoundVolume;
    private float plateSoundPitch;

    // Добавленные переменные для разных направлений схем
    private String plateForwardSchematic;
    private String plateForwardLeftSchematic;
    private String plateForwardRightSchematic;
    private String plateBackwardSchematic;
    private String plateBackwardLeftSchematic;
    private String plateBackwardRightSchematic;
    private String plateLeftSchematic;
    private String plateRightSchematic;
    private String plateUpSchematic;
    private String plateDownSchematic;

    private String revealItemType;

    private String revealItemDisplayName;

    private List<String> revealItemDescription;

    private int revealItemCooldown;

    private int revealItemRadius;

    private String revealItemSoundType;

    private float revealItemSoundVolume;

    private float revealItemSoundPitch;

    private String revealItemParticleType;

    private int revealItemGlowDuration;

    private String divineAuraItemName;
    private Material divineAuraItemMaterial;
    private List<String> divineAuraItemDescription;
    private int divineAuraItemCooldown;
    private String divineAuraParticleType;
    private String divineAuraSoundType;
    private float divineAuraSoundVolume;
    private float divineAuraSoundPitch;
    private TrapItemListener trapItemListener;
    private PlateItemListener plateItemListener;
    private ChatInputHandler chatInputHandler;
    private Map<String, String> tempSkinData;
    private File trapsFile;
    private File platesFile;
    private FileConfiguration trapsConfig;
    private FileConfiguration platesConfig;
    @Override
    public void onEnable() {
        plugin = this;
        config = getConfig();
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        loadTrapConfig();
        loadPlateConfig();
        loadRevealItemConfig();
        loadDivineAuraItemConfig();
        loadTrapsConfig();
        loadPlatesConfig();
        getCommand("viotrap").setExecutor(new GiveItemCommand());
        getCommand("viotrap").setTabCompleter(new GiveItemTabCompleter());

        trapItemListener = new TrapItemListener(this);
        getServer().getPluginManager().registerEvents(trapItemListener, this);
        plateItemListener = new PlateItemListener(this);
        getServer().getPluginManager().registerEvents(plateItemListener, this);
        getServer().getPluginManager().registerEvents(new RevealItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlateItemListener(this), this);
        getServer().getPluginManager().registerEvents(new DisorientItemListener(this), this);
        getServer().getPluginManager().registerEvents(new DivineAuraItemListener(this), this);
        getServer().getPluginManager().registerEvents(new SkinCreationMenu(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new FirestormItemListener(this), this);
        getCommand("createskin").setExecutor(new CreateSkinCommand(this));
        getCommand("applyskin").setExecutor(new ApplySkinCommand(this));

        chatInputHandler = new ChatInputHandler();
        tempSkinData = new HashMap<>();

        Bukkit.getLogger().info("[VioTrap] Плагин успешно загружен!");
    }

    public void onDisable() {
        if (trapItemListener != null) {
            Bukkit.getLogger().info("[VioTrap] Выключение сервера, восстанавливаем блоки...");
            trapItemListener.removeAllTraps();
        } else {
            Bukkit.getLogger().warning("[VioTrap] trapItemListener == null, restoreAllBlocks() не вызван!");
        }
        if (plateItemListener != null) {
            Bukkit.getLogger().info("[VioTrap] Выключение сервера, восстанавливаем блоки...");
            plateItemListener.removeAllPlates();
        } else {
            Bukkit.getLogger().warning("[VioTrap] trapItemListener == null, restoreAllBlocks() не вызван!");
        }
        Bukkit.getLogger().info("[VioTrap] Плагин VioTrap успешно отключен.");
    }
    public void loadTrapsConfig() {
        trapsFile = new File(getDataFolder(), "traps.yml");

        if (!trapsFile.exists()) {
            try {
                trapsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать traps.yml!");
                e.printStackTrace();
            }
        }

        trapsConfig = YamlConfiguration.loadConfiguration(trapsFile);

        trapsConfig.set("traps", null);
        saveTrapsConfig();
    }


    public void saveTrapsConfig() {
        try {
            trapsConfig.save(trapsFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить traps.yml!");
            e.printStackTrace();
        }
    }
    public void loadPlatesConfig() {
        platesFile = new File(plugin.getDataFolder(), "plats.yml");
        if (!platesFile.exists()) {
            try {
                platesFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Не удалось создать plats.yml!");
                e.printStackTrace();
            }
        }
        platesConfig = YamlConfiguration.loadConfiguration(platesFile);
    }

    public void savePlatesConfig() {
        try {
            platesConfig.save(platesFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Не удалось сохранить plats.yml!");
            e.printStackTrace();
        }
    }
    public FileConfiguration getTrapsConfig() {
        return trapsConfig;
    }

    public FileConfiguration getPlatesConfig() {
        return platesConfig;
    }

    public void loadTrapConfig() {
        trapDisplayName = config.getString("trap.display_name");
        trapDescription = config.getStringList("trap.description");
        trapType = config.getString("trap.type");
        trapCooldown = config.getInt("trap.cooldown");
        trapDuration = config.getInt("trap.duration");
        trapMessageNearby = config.getString("trap.messages.already_nearby");
        trapMessageCooldown = config.getString("trap.messages.cooldown_message");
        trapMessageFailed = config.getString("trap.messages.trap_failed");
        trapSchematic = config.getString("trap.schematic");
        trapSoundType = config.getString("trap.sound.type", "BLOCK_PISTON_CONTRACT");
        trapSoundVolume = (float) config.getDouble("trap.sound.volume", 10.0);
        trapSoundPitch =  plateSoundPitch = (float) config.getDouble("trap.sound.pitch", 1.0);
    }

    public void loadPlateConfig() {
        plateDisplayName = config.getString("plate.display_name", "§6Пласт");
        plateDescription = config.getStringList("plate.description");
        plateType = config.getString("plate.type");
        plateCooldown = config.getInt("plate.cooldown", 1);
        plateDuration = config.getInt("plate.duration", 5);
        plateMessageNearby = config.getString("plate.messages.already_nearby");
        plateMessageCooldown = config.getString("plate.messages.cooldown_message");
        plateMessageFailed = config.getString("plate.messages.placement_failed");
        plateSoundType = config.getString("plate.sound.type", "BLOCK_ANVIL_PLACE");
        plateSoundVolume = (float) config.getDouble("plate.sound.volume", 10.0);
        plateSoundPitch = (float) config.getDouble("plate.sound.pitch", 1.0);
        plateForwardSchematic = config.getString("plate.forward_schematic", "plate_forward.schem");
        plateForwardLeftSchematic = config.getString("plate.forward_left_schematic", "plate_forward_left.schem");
        plateForwardRightSchematic = config.getString("plate.forward_right_schematic", "plate_forward_right.schem");
        plateBackwardSchematic = config.getString("plate.backward_schematic", "plate_backward.schem");
        plateBackwardLeftSchematic = config.getString("plate.backward_left_schematic", "plate_backward_left.schem");
        plateBackwardRightSchematic = config.getString("plate.backward_right_schematic", "plate_backward_right.schem");
        plateLeftSchematic = config.getString("plate.left_schematic", "plate_left.schem");
        plateRightSchematic = config.getString("plate.right_schematic", "plate_right.schem");
        plateUpSchematic = config.getString("plate.up_schematic", "plate_up.schem");
        plateDownSchematic = config.getString("plate.down_schematic", "plate.schem");
    }
    public void loadRevealItemConfig() {
        revealItemType = config.getString("reveal_item.type");
        revealItemDisplayName = config.getString("reveal_item.display_name");
        revealItemDescription = config.getStringList("disorient_item.description");
        revealItemCooldown = config.getInt("reveal_item.cooldown");
        revealItemRadius = config.getInt("reveal_item.radius");
        revealItemSoundType = config.getString("reveal_item.sound.type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        revealItemSoundVolume = (float) config.getDouble("reveal_item.sound.volume", 1.0);
        revealItemSoundPitch = (float) config.getDouble("reveal_item.sound.pitch", 1.0);
        revealItemParticleType = config.getString("reveal_item.particle_type", "ENDROD");
        revealItemGlowDuration = config.getInt("reveal_item.glow_duration");
    }

    public void loadDivineAuraItemConfig() {
        divineAuraItemName = config.getString("divine_aura.name", "Божья Аура");
        divineAuraItemMaterial = Material.getMaterial(config.getString("divine_aura.material", "GHAST_TEAR"));
        divineAuraItemDescription = config.getStringList("divine_aura.description");
        divineAuraItemCooldown = config.getInt("divine_aura.cooldown", 10);
        divineAuraParticleType = config.getString("divine_aura.particle_type", "VILLAGER_HAPPY");
        divineAuraSoundType = config.getString("divine_aura.sound.type", "ENTITY_PLAYER_LEVELUP");
        divineAuraSoundVolume = (float) config.getDouble("divine_aura.sound.volume", 1.0);
        divineAuraSoundPitch = (float) config.getDouble("divine_aura.sound.pitch", 1.0);
    }


    public static VioTrap getPlugin() {
        return plugin;
    }

    public String getTrapDisplayName() {
        return trapDisplayName;
    }
    public String getTrapSchematic() {
        return trapSchematic;
    }
    public List<String> getTrapDescription() {
        return trapDescription;
    }

    public String getTrapType() {
        return trapType;
    }

    public int getTrapCooldown() {
        return trapCooldown;
    }

    public int getTrapDuration() {
        return trapDuration;
    }

    public String getTrapMessageNearby() {
        return trapMessageNearby;
    }

    public String getTrapMessageCooldown() { return trapMessageCooldown; }

    public String getTrapMessageFailed() {
        return trapMessageFailed;
    }

    public String getTrapSoundType() {
        return trapSoundType;
    }

    public float getTrapSoundVolume() { return trapSoundVolume; }

    public float getTrapSoundPitch() {
        return trapSoundPitch;
    }

    public String getPlateDisplayName() {
        return plateDisplayName;
    }

    public List<String> getPlateDescription() {
        return plateDescription;
    }

    public String getPlateType() {
        return plateType;
    }

    public int getPlateCooldown() {
        return plateCooldown;
    }

    public int getPlateDuration() { return plateDuration; }

    public String getPlateMessageNearby() {
        return plateMessageNearby;
    }

    public String getPlateMessageCooldown() {
        return plateMessageCooldown;
    }

    public String getPlateMessageFailed() {
        return plateMessageFailed;
    }

    public String getPlateSoundType() {
        return plateSoundType;
    }

    public float getPlateSoundVolume() { return plateSoundVolume; }

    public float getPlateSoundPitch() {
        return plateSoundPitch;
    }
    public String getPlateForwardSchematic() {
        return plateForwardSchematic;
    }

    public String getPlateForwardLeftSchematic() {
        return plateForwardLeftSchematic;
    }

    public String getPlateForwardRightSchematic() {
        return plateForwardRightSchematic;
    }

    public String getPlateBackwardSchematic() {
        return plateBackwardSchematic;
    }

    public String getPlateBackwardLeftSchematic() {
        return plateBackwardLeftSchematic;
    }

    public String getPlateBackwardRightSchematic() {
        return plateBackwardRightSchematic;
    }

    public String getPlateLeftSchematic() {
        return plateLeftSchematic;
    }

    public String getPlateRightSchematic() { return plateRightSchematic; }

    public String getPlateUpSchematic() {
        return plateUpSchematic;
    }

    public String getPlateDownSchematic() {
        return plateDownSchematic;
    }

    public String getRevealItemType() {
        return revealItemType;
    }

    public String getRevealItemDisplayName() {
        return revealItemDisplayName;
    }

    public List<String> getRevealItemDescription() {
        return revealItemDescription;
    }

    public int getRevealItemCooldown() {
        return revealItemCooldown;
    }

    public int getRevealItemRadius() {
        return revealItemRadius;
    }

    public String getRevealItemSoundType() {
        return revealItemSoundType;
    }

    public float getRevealItemSoundVolume() {
        return revealItemSoundVolume;
    }

    public float getRevealItemSoundPitch() {
        return revealItemSoundPitch;
    }

    public String getRevealItemParticleType() { return revealItemParticleType; }

    public int getRevealItemGlowDuration() { return revealItemGlowDuration;}
    public int getDisorientItemCooldown() {
        return getConfig().getInt("disorient_item.cooldown", 10);
    }

    public int getDisorientItemEffectDuration() {
        return getConfig().getInt("disorient_item.effect_duration", 5);
    }

    public int getDisorientItemRadius() {
        return getConfig().getInt("disorient_item.radius", 10);
    }

    public String getDisorientItemSoundType() {
        return getConfig().getString("disorient_item.sound.type", "ENTITY_WITHER_AMBIENT");
    }

    public float getDisorientItemSoundVolume() {
        return (float) getConfig().getDouble("disorient_item.sound.volume", 1.0f);
    }

    public float getDisorientItemSoundPitch() {
        return (float) getConfig().getDouble("disorient_item.sound.pitch", 1.0f);
    }

    public String getDisorientItemParticleType() {
        return getConfig().getString("disorient_item.particle_type", "SMOKE_LARGE");
    }
    public String getDisorientItemName() {
        return config.getString("disorient_item.display_name", "Дезориентация");
    }

    public String getDisorientItemType() {
        return config.getString("disorient_item.type", "ENDER_EYE");
    }
    public List<String> getDisorientItemDescription() {
        return config.getStringList("disorient_item.description");
    }

    public String getDivineAuraItemName() {
        return divineAuraItemName;
    }

    public Material getDivineAuraItemMaterial() {
        return divineAuraItemMaterial;
    }

    public List<String> getDivineAuraItemDescription() {
        return divineAuraItemDescription;
    }

    public int getDivineAuraItemCooldown() {
        return divineAuraItemCooldown;
    }

    public String getDivineAuraItemParticleType() {
        return divineAuraParticleType;
    }

    public String getDivineAuraItemSoundType() {
        return divineAuraSoundType;
    }

    public float getDivineAuraItemSoundVolume() {
        return divineAuraSoundVolume;
    }

    public float getDivineAuraItemSoundPitch() {
        return divineAuraSoundPitch;
    }

    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }

    public Map<String, String> getTempSkinData() {
        return tempSkinData;
    }
    public String getSkinSchematic(String skinName) {
        return config.getString("skins." + skinName + ".schem", getTrapSchematic());
    }

    public String getSkinDescription(String skinName) {
        return config.getString("skins." + skinName + ".desc_for_trap", "Описание не найдено.");
    }
    public List<String> getSkinNames() {
        ConfigurationSection skinsSection = config.getConfigurationSection("skins");
        if (skinsSection != null) {
            return new ArrayList<>(skinsSection.getKeys(false));
        }
        return Collections.emptyList();
    }
    public TrapItemListener getTrapItemListener() {
        return trapItemListener;
    }
    public int getFirestormItemCooldown() {
        return getConfig().getInt("firestorm_item.cooldown", 10);
    }

    public int getFirestormItemRadius() {
        return getConfig().getInt("firestorm_item.radius", 5);
    }

    public int getFirestormItemFireDuration() {
        return getConfig().getInt("firestorm_item.fire_duration", 5);
    }

    public String getFirestormItemSoundType() {
        return getConfig().getString("firestorm_item.sound.type", "ENTITY_BLAZE_SHOOT");
    }

    public float getFirestormItemSoundVolume() {
        return (float) getConfig().getDouble("firestorm_item.sound.volume", 1.0f);
    }

    public float getFirestormItemSoundPitch() {
        return (float) getConfig().getDouble("firestorm_item.sound.pitch", 1.0f);
    }

    public String getFirestormItemName() {
        return getConfig().getString("firestorm_item.name", "§cОгненный смерч");
    }

    public String getFirestormItemType() {
        return getConfig().getString("firestorm_item.type", "BLAZE_ROD");
    }

    public List<String> getFirestormItemDescription() {
        return getConfig().getStringList("firestorm_item.description");
    }

}
