package org.migrate1337.viotrap.actions;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;

public class EffectCustomAction implements CustomAction {
    private final String target;
    private final String effectName;
    private final int amplifier;
    private final int duration;

    public EffectCustomAction(String target, String effectName, int amplifier, int duration) {
        this.target = target.toLowerCase();
        this.effectName = effectName.toUpperCase();
        this.amplifier = amplifier;
        this.duration = duration;
    }

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        PotionEffectType effectType;
        try {
            effectType = PotionEffectType.getByName(effectName);
            if (effectType == null) {
                plugin.getLogger().warning("Некорректный эффект: " + effectName);
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении эффекта: " + effectName);
            return;
        }

        switch (target) {
            case "p":
            case "player":
                applyEffect(player, effectType);
                break;
            case "o":
                for (Player opponent : opponents) {
                    applyEffect(opponent, effectType);
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                applyEffect(randomPlayer, effectType);
                break;
            default:
                plugin.getLogger().warning("Некорректный таргет в EffectCustomAction: " + target);
        }
    }

    private void applyEffect(Player player, PotionEffectType effectType) {
        if (player != null && player.isOnline()) {
            player.addPotionEffect(new PotionEffect(effectType, duration * 20, amplifier));
        }
    }
}