package me.mvk.potionFood;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ConsumeListener implements Listener {

    private final PotionFoodPlugin plugin;

    public ConsumeListener(PotionFoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {

        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Player player = event.getPlayer();

        if (data.has(PotionFoodPlugin.SUPER_CARROT_KEY, PersistentDataType.BYTE)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 0));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1.5f);
            });
            return;
        }

        if (meta instanceof PotionMeta) {
            if (data.has(PotionFoodPlugin.POTION_DRANK, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                player.sendMessage("§cThis bottle is empty (mechanically).");
                return;
            }
            data.set(PotionFoodPlugin.POTION_DRANK, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
            return;
        }

        if (!data.has(PotionFoodPlugin.EFFECT_COUNT, PersistentDataType.INTEGER)) return;

        int count = data.get(PotionFoodPlugin.EFFECT_COUNT, PersistentDataType.INTEGER);
        boolean hasPoison = false;

        for (int i = 0; i < count; i++) {
            String raw = data.get(new NamespacedKey(plugin, "effect_" + i), PersistentDataType.STRING);
            if (raw == null) continue;

            String[] s = raw.split(":");
            if (s.length != 3) continue;

            PotionEffectType type = PotionEffectType.getByName(s[0]);
            if (type == null) continue;

            if (type.equals(PotionEffectType.POISON)) {
                hasPoison = true;
                continue;
            }

            try {
                int amp = Integer.parseInt(s[1]);
                int dur = Integer.parseInt(s[2]);
                player.addPotionEffect(new PotionEffect(type, dur, amp));
            } catch (NumberFormatException ignored) {}
        }

        if (hasPoison) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 15, 0));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 2, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 10, 1));
            }, 20 * 5);
        }
    }
}