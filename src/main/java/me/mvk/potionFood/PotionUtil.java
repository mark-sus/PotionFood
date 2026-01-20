package me.mvk.potionFood;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.*;

import java.util.ArrayList;
import java.util.List;

public final class PotionUtil {

    public static List<PotionEffect> readPotionEffects(PotionMeta pm) {

        List<PotionEffect> list = new ArrayList<>();

        if (!pm.getCustomEffects().isEmpty()) {
            list.addAll(pm.getCustomEffects());
            return list;
        }

        PotionType base = pm.getBasePotionType();
        if (base == null || base.getEffectType() == null) return list;

        PotionEffectType type = base.getEffectType();

        int duration = switch (type.getName()) {
            case "SPEED", "STRENGTH", "REGENERATION", "POISON" -> 3600;
            case "FIRE_RESISTANCE", "WATER_BREATHING", "INVISIBILITY" -> 3600;
            case "NIGHT_VISION" -> 3600;
            case "INSTANT_HEALTH" -> 800;
            default -> 1200;
        };

        list.add(new PotionEffect(type, duration, 0));
        return list;
    }
}
