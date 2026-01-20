package me.mvk.potionFood;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuHandler implements Listener {

    private final PotionFoodPlugin plugin;
    private final String GUI_TITLE = "§8Potion Food Station";

    private final Map<Location, Inventory> activeMenus = new HashMap<>();
    public static final NamespacedKey INVENTORY_DATA_KEY = new NamespacedKey("potionfood", "station_items");

    private final int SLOT_FOOD = 19;
    private final int SLOT_RESULT = 25;
    private final int[] SLOTS_POTIONS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private final int SLOT_BLACKLIST_INFO = 49;
    private final int[] SLOTS_ARROWS = {20, 24};
    private final ItemStack FILLER_ITEM;
    private final ItemStack ARROW_ITEM;

    public MenuHandler(PotionFoodPlugin plugin) {
        this.plugin = plugin;

        FILLER_ITEM = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = FILLER_ITEM.getItemMeta();
        meta.setDisplayName(" ");
        FILLER_ITEM.setItemMeta(meta);

        ARROW_ITEM = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta arrowMeta = ARROW_ITEM.getItemMeta();
        arrowMeta.setDisplayName("§a->");
        ARROW_ITEM.setItemMeta(arrowMeta);
    }

    public void openStationMenu(Player player, Block block) {
        Location loc = block.getLocation();
        if (activeMenus.containsKey(loc)) {
            player.openInventory(activeMenus.get(loc));
            return;
        }
        StationHolder holder = new StationHolder(block);
        Inventory inv = Bukkit.createInventory(holder, 54, GUI_TITLE);
        fillGuiBase(inv);
        loadItemsFromBlock(block, inv);
        updateResult(inv);
        activeMenus.put(loc, inv);
        player.openInventory(inv);
    }

    public void openCraftMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new VirtualHolder(), 54, GUI_TITLE);
        fillGuiBase(inv);
        player.openInventory(inv);
    }

    public void removeCachedInventory(Location loc) {
        activeMenus.remove(loc);
    }

    private void loadItemsFromBlock(Block block, Inventory inv) {
        if (!(block.getState() instanceof TileState state)) return;
        PersistentDataContainer data = state.getPersistentDataContainer();
        if (data.has(INVENTORY_DATA_KEY, PersistentDataType.STRING)) {
            String base64 = data.get(INVENTORY_DATA_KEY, PersistentDataType.STRING);
            ItemStack[] items = ItemSerializer.fromBase64(base64);
            if (items.length > 0 && items[0] != null) inv.setItem(SLOT_FOOD, items[0]);
            for (int i = 0; i < SLOTS_POTIONS.length; i++) {
                if (i + 1 < items.length && items[i + 1] != null) {
                    inv.setItem(SLOTS_POTIONS[i], items[i + 1]);
                }
            }
        }
    }

    public void saveItemsToBlock(Inventory inv, Block block) {
        if (!(block.getState() instanceof TileState state)) return;
        ItemStack[] itemsToSave = new ItemStack[10];
        itemsToSave[0] = inv.getItem(SLOT_FOOD);
        for (int i = 0; i < SLOTS_POTIONS.length; i++) {
            itemsToSave[i + 1] = inv.getItem(SLOTS_POTIONS[i]);
        }
        String base64 = ItemSerializer.toBase64(itemsToSave);
        state.getPersistentDataContainer().set(INVENTORY_DATA_KEY, PersistentDataType.STRING, base64);
        state.update();
    }


    private void fillGuiBase(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, FILLER_ITEM);
        inv.setItem(SLOT_FOOD, null);
        inv.setItem(SLOT_RESULT, null);
        for (int slot : SLOTS_POTIONS) inv.setItem(slot, null);
        for (int slot : SLOTS_ARROWS) inv.setItem(slot, ARROW_ITEM);

        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName("§c§lRestricted Potions");
        meta.setLore(plugin.getConfig().getStringList("blacklist"));
        barrier.setItemMeta(meta);
        inv.setItem(SLOT_BLACKLIST_INFO, barrier);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();

        if (!(holder instanceof StationHolder) && !(holder instanceof VirtualHolder)) return;

        int slot = event.getRawSlot();
        boolean isTopInventory = slot < inv.getSize();

        if (isTopInventory) {
            if (slot == SLOT_FOOD || contains(SLOTS_POTIONS, slot)) {

            }
            else if (slot == SLOT_RESULT) {
                event.setCancelled(true);
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() != Material.AIR) {
                    Player player = (Player) event.getWhoClicked();
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage("§cInventory full!");
                        return;
                    }
                    processCraft(inv, player, current);
                    if (holder instanceof StationHolder station) saveItemsToBlock(inv, station.block);
                }
                return;
            }
            else {
                event.setCancelled(true);
                return;
            }
        }
        else {
            if (event.isShiftClick()) {
                event.setCancelled(true);

                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                if (clickedItem.getType().isEdible()) {
                    if (tryMoveItem(inv, clickedItem, SLOT_FOOD)) {
                        event.setCurrentItem(null);
                    }
                }
                else if (clickedItem.hasItemMeta() && clickedItem.getItemMeta() instanceof PotionMeta) {
                    for (int potionSlot : SLOTS_POTIONS) {
                        if (tryMoveItem(inv, clickedItem, potionSlot)) {
                            event.setCurrentItem(null);
                            break;
                        }
                    }
                }
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            updateResult(inv);
            if (holder instanceof StationHolder station) saveItemsToBlock(inv, station.block);
        });
    }

    private boolean tryMoveItem(Inventory targetInv, ItemStack itemFromPlayer, int targetSlot) {
        ItemStack itemInSlot = targetInv.getItem(targetSlot);
        if (itemInSlot == null || itemInSlot.getType() == Material.AIR) {
            targetInv.setItem(targetSlot, itemFromPlayer.clone());
            return true;
        }
        if (itemInSlot.isSimilar(itemFromPlayer)) {
            int space = itemInSlot.getMaxStackSize() - itemInSlot.getAmount();
            if (space > 0) {
                int toAdd = Math.min(space, itemFromPlayer.getAmount());
                itemInSlot.setAmount(itemInSlot.getAmount() + toAdd);
                itemFromPlayer.setAmount(itemFromPlayer.getAmount() - toAdd);

                targetInv.setItem(targetSlot, itemInSlot);
                return itemFromPlayer.getAmount() <= 0;
            }
        }
        return false;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof StationHolder) && !(holder instanceof VirtualHolder)) return;

        boolean involvesInput = false;
        for (int slot : event.getRawSlots()) {
            if (slot == SLOT_FOOD || contains(SLOTS_POTIONS, slot)) involvesInput = true;
            else if (slot < event.getInventory().getSize()) event.setCancelled(true);
        }

        if (involvesInput) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                updateResult(event.getInventory());
                if (holder instanceof StationHolder station) saveItemsToBlock(event.getInventory(), station.block);
            });
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();

        if (holder instanceof VirtualHolder) {
            returnItem((Player) event.getPlayer(), inv, SLOT_FOOD);
            for (int slot : SLOTS_POTIONS) returnItem((Player) event.getPlayer(), inv, slot);
        } else if (holder instanceof StationHolder station) {
            saveItemsToBlock(inv, station.block);
            if (inv.getViewers().size() <= 1) {
                activeMenus.remove(station.block.getLocation());
            }
        }
    }

    private void updateResult(Inventory inv) {
        ItemStack food = inv.getItem(SLOT_FOOD);
        if (food == null || !food.getType().isEdible()) {
            inv.setItem(SLOT_RESULT, null);
            return;
        }

        List<PotionEffect> finalEffects = new ArrayList<>();
        int potionsFound = 0;
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");

        for (int slot : SLOTS_POTIONS) {
            ItemStack potion = inv.getItem(slot);
            if (potion != null && potion.getItemMeta() instanceof PotionMeta pm) {
                List<PotionEffect> effects = PotionUtil.readPotionEffects(pm);
                for (PotionEffect pe : effects) {
                    if (blacklist.contains(pe.getType().getName())) {
                        inv.setItem(SLOT_RESULT, null);
                        return;
                    }
                }
                if (!effects.isEmpty()) {
                    finalEffects.addAll(effects);
                    potionsFound++;
                }
            }
        }

        if (potionsFound == 0) {
            inv.setItem(SLOT_RESULT, null);
            return;
        }

        ItemStack result = food.clone();
        result.setAmount(1);
        ItemMeta meta = result.getItemMeta();

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(PotionFoodPlugin.EFFECT_COUNT, PersistentDataType.INTEGER, finalEffects.size());

        for (int i = 0; i < finalEffects.size(); i++) {
            PotionEffect eff = finalEffects.get(i);
            String val = eff.getType().getName() + ":" + eff.getAmplifier() + ":" + eff.getDuration();
            data.set(new NamespacedKey(plugin, "effect_" + i), PersistentDataType.STRING, val);
        }


        result.setItemMeta(meta);
        inv.setItem(SLOT_RESULT, result);
    }

    private boolean isBlacklisted(PotionEffectType type) {
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.isEmpty()) {
            blacklist = plugin.getConfig().getStringList("potion.blacklist");
        }

        String internalName = type.getName();

        for (String s : blacklist) {
            if (s.equalsIgnoreCase(internalName)) return true;
            if (s.equalsIgnoreCase("INSTANT_HEALTH") && internalName.equals("HEAL")) return true;
            if (s.equalsIgnoreCase("INSTANT_DAMAGE") && internalName.equals("HARM")) return true;
            if (s.equalsIgnoreCase("STRENGTH") && internalName.equals("INCREASE_DAMAGE")) return true;
            if (s.equalsIgnoreCase("JUMP_BOOST") && internalName.equals("JUMP")) return true;
            if (s.equalsIgnoreCase("SLOWNESS") && internalName.equals("SLOW")) return true;
        }
        return false;
    }

    private void processCraft(Inventory inv, Player player, ItemStack resultItem) {
        player.getInventory().addItem(resultItem);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);

        ItemStack food = inv.getItem(SLOT_FOOD);
        if (food != null) {
            food.setAmount(food.getAmount() - 1);
            inv.setItem(SLOT_FOOD, food.getAmount() > 0 ? food : null);
        }

        int maxUses = plugin.getConfig().getInt("potion.max-uses", 16);

        for (int slot : SLOTS_POTIONS) {
            ItemStack potion = inv.getItem(slot);
            if (potion == null || !(potion.getItemMeta() instanceof PotionMeta)) continue;

            ItemMeta meta = potion.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            int currentUses = data.getOrDefault(PotionFoodPlugin.POTION_USES, PersistentDataType.INTEGER, maxUses);
            currentUses--;

            if (currentUses <= 0) {
                inv.setItem(slot, new ItemStack(Material.GLASS_BOTTLE));
            } else {
                data.set(PotionFoodPlugin.POTION_USES, PersistentDataType.INTEGER, currentUses);
                updatePotionLore(meta, currentUses, maxUses);
                potion.setItemMeta(meta);
                inv.setItem(slot, potion);
            }
        }
        updateResult(inv);
    }

    private void updatePotionLore(ItemMeta meta, int uses, int max) {
        String format = plugin.getConfig().getString("potion.lore-format", "&7{%used%/%max%}");
        format = format.replace("%used%", String.valueOf(uses)).replace("%max%", String.valueOf(max));
        List<String> lore = new ArrayList<>();
        lore.add(format.replace("&", "§"));
        meta.setLore(lore);
    }

    private void returnItem(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item).values().forEach(l -> player.getWorld().dropItem(player.getLocation(), l));
        }
    }
    private boolean contains(int[] arr, int val) { for (int i : arr) if (i == val) return true; return false; }

    public static class StationHolder implements InventoryHolder {
        public final Block block;
        public StationHolder(Block block) { this.block = block; }
        @Override public Inventory getInventory() { return null; }
    }
    public static class VirtualHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}