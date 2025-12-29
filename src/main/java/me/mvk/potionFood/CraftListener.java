package me.mvk.potionFood;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;

import java.util.ArrayList;
import java.util.List;

public class CraftListener implements Listener {

    private final PotionFoodPlugin plugin;

    public CraftListener(PotionFoodPlugin plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        ItemStack food = null;
        List<ItemStack> potions = new ArrayList<>();

        for (ItemStack item : matrix) {
            if (item == null) continue;

            if (item.getType().isEdible()) {
                if (food != null) return;
                food = item;
            }
            else if (item.getItemMeta() instanceof PotionMeta) {
                potions.add(item);
            }
            else {
                return;
            }
        }

        if (food == null || potions.isEmpty()) return;

        ItemStack result = food.clone();
        result.setAmount(1);

        ItemMeta meta = result.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        FileConfiguration cfg = plugin.getConfig();
        List<String> blacklist = cfg.getStringList("potion.blacklist");

        int index = 0;

        for (ItemStack p : potions) {
            PotionMeta pm = (PotionMeta) p.getItemMeta();

            for (PotionEffect e : PotionUtil.readPotionEffects(pm)) {

                String effectName = e.getType().getName();

                if (blacklist.contains(effectName)) {
                    inv.setResult(null);
                    return;
                }

                if (index >= 8) break;

                if (e.getType() == PotionEffectType.POISON) {
                    data.set(
                            PotionFoodPlugin.SPECIAL_POISON_KEY,
                            PersistentDataType.BYTE,
                            (byte) 1
                    );
                    continue;
                }

                String raw = effectName + ":" +
                        e.getAmplifier() + ":" +
                        (e.getDuration() / 2);

                data.set(
                        new NamespacedKey(plugin, "effect_" + index),
                        PersistentDataType.STRING,
                        raw
                );
                index++;
            }
        }

        if (index == 0 &&
                !data.has(PotionFoodPlugin.SPECIAL_POISON_KEY, PersistentDataType.BYTE))
            return;

        data.set(
                PotionFoodPlugin.EFFECT_COUNT,
                PersistentDataType.INTEGER,
                index
        );

        result.setItemMeta(meta);
        inv.setResult(result);
    }

    @EventHandler
    public void onCraftClick(InventoryClickEvent event) {

        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        if (event.getRawSlot() != 0) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        if (!data.has(PotionFoodPlugin.EFFECT_COUNT, PersistentDataType.INTEGER)
                && !data.has(PotionFoodPlugin.SPECIAL_POISON_KEY, PersistentDataType.BYTE))
            return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        int maxUses = plugin.getConfig().getInt("potion.max-uses", 16);

        // 1️⃣ готову їжу — В ІНВЕНТАР
        player.getInventory().addItem(result.clone());

        // 2️⃣ обробляємо матрицю
        ItemStack[] matrix = inv.getMatrix();

        for (ItemStack it : matrix) {
            if (it == null) continue;

            // 🍖 їжа — повертаємо залишок в інвентар
            if (it.getType().isEdible()) {
                it.setAmount(it.getAmount() - 1);
                if (it.getAmount() > 0)
                    player.getInventory().addItem(it.clone());
                continue;
            }

            // 🧪 зілля — зменшуємо uses і повертаємо в інвентар
            if (it.getItemMeta() instanceof PotionMeta pm) {

                PersistentDataContainer pdc = pm.getPersistentDataContainer();

                int uses = pdc.getOrDefault(
                        PotionFoodPlugin.POTION_USES,
                        PersistentDataType.INTEGER,
                        maxUses
                );

                uses--;

                if (uses > 0) {
                    pdc.set(
                            PotionFoodPlugin.POTION_USES,
                            PersistentDataType.INTEGER,
                            uses
                    );
                    updatePotionLore(pm, uses, maxUses);
                    it.setItemMeta(pm);

                    player.getInventory().addItem(it.clone());
                }
            }
        }

        inv.setMatrix(new ItemStack[9]);
        inv.setResult(null);

        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }


    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {

        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Player player = event.getPlayer();

        if (meta instanceof PotionMeta) {

            if (data.has(PotionFoodPlugin.POTION_DRANK, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                player.sendMessage(color(
                        plugin.getConfig().getString("messages.potion-already-drank")
                ));
                return;
            }

            data.set(
                    PotionFoodPlugin.POTION_DRANK,
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            item.setItemMeta(meta);
            return;
        }

        if (!data.has(PotionFoodPlugin.EFFECT_COUNT, PersistentDataType.INTEGER)
                && !data.has(PotionFoodPlugin.SPECIAL_POISON_KEY, PersistentDataType.BYTE))
            return;

        if (data.has(PotionFoodPlugin.SPECIAL_POISON_KEY, PersistentDataType.BYTE)) {

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NAUSEA, 20 * 15, 0
            ));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.POISON, 20 * 10, 1
                ));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, 20 * 2, 0
                ));
            }, 20 * 5);
        }

        int count = data.get(
                PotionFoodPlugin.EFFECT_COUNT,
                PersistentDataType.INTEGER
        );

        for (int i = 0; i < count; i++) {

            String raw = data.get(
                    new NamespacedKey(plugin, "effect_" + i),
                    PersistentDataType.STRING
            );

            if (raw == null) continue;

            String[] s = raw.split(":");
            if (s.length != 3) continue;

            PotionEffectType type = PotionEffectType.getByName(s[0]);
            if (type == null) continue;

            try {
                int amp = Integer.parseInt(s[1]);
                int dur = Integer.parseInt(s[2]);

                player.addPotionEffect(
                        new PotionEffect(type, dur, amp)
                );
            }
            catch (NumberFormatException ignored) {}
        }
    }


    private void updatePotionLore(ItemMeta meta, int uses, int max) {

        String format = plugin.getConfig()
                .getString("potion.lore-format", "&7{%used%/%max%}");

        format = format
                .replace("%used%", String.valueOf(uses))
                .replace("%max%", String.valueOf(max));

        List<String> lore = new ArrayList<>();
        lore.add(color(format));

        meta.setLore(lore);
    }

    private String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }
}
