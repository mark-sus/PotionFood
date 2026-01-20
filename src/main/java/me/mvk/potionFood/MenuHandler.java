package me.mvk.potionFood;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuHandler implements Listener {

    private final PotionFoodPlugin plugin;
    private final String GUI_TITLE = "§8Potion Food Station";
    private final String RECIPE_TITLE = "§8Список рецептів";

    private final Map<Location, Inventory> activeMenus = new HashMap<>();
    public static final NamespacedKey INVENTORY_DATA_KEY = new NamespacedKey("potionfood", "station_items");

    private final int SLOT_INPUT = 19;
    private final int SLOT_RESULT = 25;
    private final int[] SLOTS_GRID = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private final int[] SLOTS_ARROWS = {20, 24};

    private final int SLOT_BLACKLIST_INFO = 49; // Бар'єр
    private final int SLOT_RECIPES_BOOK = 50;   // Верстак з рецептами

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


    public void openRecipeMenu(Player player, Block block) {
        Inventory inv = Bukkit.createInventory(new RecipeHolder(block), 45, RECIPE_TITLE);

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, FILLER_ITEM);

        inv.setItem(10, createDisplayPotion(PotionType.SWIFTNESS, Color.AQUA, "Зілля швидкості"));
        inv.setItem(11, new ItemStack(Material.GOLDEN_PICKAXE));
        inv.setItem(12, ARROW_ITEM);
        inv.setItem(13, createPotion("Зілля квапливості", Color.YELLOW, PotionEffectType.HASTE, 3*60*20, 0, false));

        ItemStack hasteMods = new ItemStack(Material.PAPER);
        ItemMeta hmMeta = hasteMods.getItemMeta();
        hmMeta.setDisplayName("§eПокращення зілля квапливості");
        List<String> hLore = new ArrayList<>();
        hLore.add("§7Додайте до зілля квапливості:");
        hLore.add("§cРедстоуновий пил §7-> Збільшує час зілля до (4:30)");
        hLore.add("§eАбо Світлопил §7-> Покращує зілля до II рівня на (2:00)");
        hLore.add("§8Порох §7-> Робить зілля вибуховим (2:30)");
        hmMeta.setLore(hLore);
        hasteMods.setItemMeta(hmMeta);
        inv.setItem(14, hasteMods);

        inv.setItem(19, createDisplayPotion(PotionType.NIGHT_VISION, Color.NAVY, "Зілля нічного бачення"));
        inv.setItem(20, new ItemStack(Material.SPIDER_EYE));
        inv.setItem(21, ARROW_ITEM);
        inv.setItem(22, createPotion("Зілля сліпоти", Color.BLACK, PotionEffectType.BLINDNESS, 3*60*20, 0, false));

        ItemStack blindMods = new ItemStack(Material.PAPER);
        ItemMeta bmMeta = blindMods.getItemMeta();
        bmMeta.setDisplayName("§8Покращення зілля сліпоти");
        List<String> bLore = new ArrayList<>();
        bLore.add("§7Додайте до Зілля сліпоти:");
        bLore.add("§cРедстоуновий пил §7-> Збільшує час зілля до (4:30)");
        bLore.add("§8Порох §7-> Робить зілля вибуховим");
        bLore.add("§mСвітлопил §c(Не доступний)");
        bmMeta.setLore(bLore);
        blindMods.setItemMeta(bmMeta);
        inv.setItem(23, blindMods);

        inv.setItem(28, new ItemStack(Material.GOLDEN_CARROT));
        inv.setItem(29, new ItemStack(Material.GOLDEN_CARROT));
        inv.setItem(30, ARROW_ITEM);
        inv.setItem(31, createSuperCarrot());

        ItemStack carrotInfo = new ItemStack(Material.PAPER);
        ItemMeta cMeta = carrotInfo.getItemMeta();
        cMeta.setDisplayName("§6Ефекти їжі");
        List<String> cLore = new ArrayList<>();
        cLore.add("§7При з'їданні:");
        cLore.add("§a+20 Голоду §7(100%)");
        cLore.add("§e+20 Насичення §7(Максимум)");
        cLore.add("§dЕфект Сліпота");
        cMeta.setLore(cLore);
        carrotInfo.setItemMeta(cMeta);
        inv.setItem(32, carrotInfo);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§a« Повернутися до крафту");
        back.setItemMeta(backMeta);
        inv.setItem(40, back);

        player.openInventory(inv);
    }

    // --- GUI EVENTS ---

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        Player player = (Player) event.getWhoClicked();

        if (holder instanceof RecipeHolder recipeHolder) {
            event.setCancelled(true);

            if (event.getRawSlot() == 40) {
                if (recipeHolder.block.getType() == Material.BREWING_STAND) {
                    openStationMenu(player, recipeHolder.block);
                } else {
                    player.closeInventory();
                }
            }
            return;
        }

        if (!(holder instanceof StationHolder) && !(holder instanceof VirtualHolder)) return;

        int slot = event.getRawSlot();
        boolean isTopInventory = slot < inv.getSize();

        if (isTopInventory) {
            if (slot == SLOT_RECIPES_BOOK) {
                event.setCancelled(true);
                if (holder instanceof StationHolder station) {
                    saveItemsToBlock(inv, station.block);
                    openRecipeMenu(player, station.block);
                } else {
                    player.sendMessage("§cRecipes are only available at physical stations.");
                }
                return;
            }

            if (slot == SLOT_INPUT || contains(SLOTS_GRID, slot)) {

            }
            else if (slot == SLOT_RESULT) {
                event.setCancelled(true);
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() != Material.AIR) {
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage("§cInventory full!");
                        return;
                    }
                    if (isBrewingRecipe(inv)) {
                        processBrewing(inv, player, current);
                    } else {
                        processFoodCraft(inv, player, current);
                    }
                    if (holder instanceof StationHolder station) saveItemsToBlock(inv, station.block);
                }
                return;
            }
            else {
                event.setCancelled(true);
                return;
            }
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null) return;

                if (clickedItem.getType().isEdible() || isPotion(clickedItem)) {
                    if (tryMoveItem(inv, clickedItem, SLOT_INPUT)) event.setCurrentItem(null);
                } else {
                    for (int gridSlot : SLOTS_GRID) {
                        if (tryMoveItem(inv, clickedItem, gridSlot)) {
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

    // --- GUI FILLING ---

    private void fillGuiBase(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, FILLER_ITEM);
        inv.setItem(SLOT_INPUT, null);
        inv.setItem(SLOT_RESULT, null);
        for (int slot : SLOTS_GRID) inv.setItem(slot, null);
        for (int slot : SLOTS_ARROWS) inv.setItem(slot, ARROW_ITEM);

        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName("§c§lБлекліст зілль");
        List<String> bl = plugin.getConfig().getStringList("blacklist");
        if (bl.isEmpty()) bl = plugin.getConfig().getStringList("potion.blacklist");
        meta.setLore(bl);
        barrier.setItemMeta(meta);
        inv.setItem(SLOT_BLACKLIST_INFO, barrier);

        ItemStack recipes = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta rMeta = recipes.getItemMeta();
        rMeta.setDisplayName("§e§lРецепти крафтів");
        List<String> rLore = new ArrayList<>();
        rLore.add("§7Натисніть щоб побачити всі рецепти");
        rMeta.setLore(rLore);
        recipes.setItemMeta(rMeta);
        inv.setItem(SLOT_RECIPES_BOOK, recipes);
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
            if (items.length > 0 && items[0] != null) inv.setItem(SLOT_INPUT, items[0]);
            for (int i = 0; i < SLOTS_GRID.length; i++) {
                if (i + 1 < items.length && items[i + 1] != null) {
                    inv.setItem(SLOTS_GRID[i], items[i + 1]);
                }
            }
        }
    }

    public void saveItemsToBlock(Inventory inv, Block block) {
        if (!(block.getState() instanceof TileState state)) return;
        ItemStack[] itemsToSave = new ItemStack[10];
        itemsToSave[0] = inv.getItem(SLOT_INPUT);
        for (int i = 0; i < SLOTS_GRID.length; i++) {
            itemsToSave[i + 1] = inv.getItem(SLOTS_GRID[i]);
        }
        String base64 = ItemSerializer.toBase64(itemsToSave);
        state.getPersistentDataContainer().set(INVENTORY_DATA_KEY, PersistentDataType.STRING, base64);
        state.update();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof StationHolder) && !(holder instanceof VirtualHolder)) return;

        boolean involvesInput = false;
        for (int slot : event.getRawSlots()) {
            if (slot == SLOT_INPUT || contains(SLOTS_GRID, slot)) involvesInput = true;
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

        if (holder instanceof RecipeHolder) return;

        if (holder instanceof VirtualHolder) {
            returnItem((Player) event.getPlayer(), inv, SLOT_INPUT);
            for (int slot : SLOTS_GRID) returnItem((Player) event.getPlayer(), inv, slot);
        } else if (holder instanceof StationHolder station) {
            saveItemsToBlock(inv, station.block);
            // Видаляємо з кешу тільки якщо ніхто не дивиться
            if (inv.getViewers().size() <= 1) {
                activeMenus.remove(station.block.getLocation());
            }
        }
    }

    // --- CRAFTING LOGIC ---

    private void updateResult(Inventory inv) {
        ItemStack input = inv.getItem(SLOT_INPUT);
        if (input == null || input.getType() == Material.AIR) {
            inv.setItem(SLOT_RESULT, null);
            return;
        }

        if (isPotion(input)) {
            ItemStack result = checkCustomBrewing(input, inv);
            inv.setItem(SLOT_RESULT, result);
            return;
        }

        ItemStack specialFood = checkSpecialFood(input, inv);
        if (specialFood != null) {
            inv.setItem(SLOT_RESULT, specialFood);
            return;
        }

        if (input.getType().isEdible()) {
            updateFoodResult(inv, input);
            return;
        }
        inv.setItem(SLOT_RESULT, null);
    }

    // --- LOGIC: CUSTOM BREWING ---

    private ItemStack checkSpecialFood(ItemStack input, Inventory inv) {

        if (input.getType() != Material.GOLDEN_CARROT) return null;

        ItemStack ingredient = null;
        int ingredientCount = 0;

        for (int i = 0; i < SLOTS_GRID.length; i++) {
            ItemStack item = inv.getItem(SLOTS_GRID[i]);
            if (item != null && item.getType() != Material.AIR) {
                ingredient = item;
                ingredientCount++;
            }
        }

        if (ingredientCount != 1 || ingredient == null) return null;

        if (ingredient.getType() == Material.GOLDEN_CARROT) {
            return createSuperCarrot();
        }

        return null;
    }

    private ItemStack createSuperCarrot() {
        ItemStack item = new ItemStack(Material.GOLDEN_CARROT);
        ItemMeta meta = item.getItemMeta();

        String name = net.md_5.bungee.api.ChatColor.of("#FF4500") + "П"
                + net.md_5.bungee.api.ChatColor.of("#FF5900") + "о"
                + net.md_5.bungee.api.ChatColor.of("#FF6D00") + "с"
                + net.md_5.bungee.api.ChatColor.of("#FF8200") + "и"
                + net.md_5.bungee.api.ChatColor.of("#FF9600") + "л"
                + net.md_5.bungee.api.ChatColor.of("#FFAA00") + "е"
                + net.md_5.bungee.api.ChatColor.of("#FFBF00") + "н"
                + net.md_5.bungee.api.ChatColor.of("#FFD300") + "а "
                + net.md_5.bungee.api.ChatColor.of("#FFE700") + "З"
                + net.md_5.bungee.api.ChatColor.of("#FFD700") + "о"
                + net.md_5.bungee.api.ChatColor.of("#FFC700") + "л"
                + net.md_5.bungee.api.ChatColor.of("#FFB700") + "о"
                + net.md_5.bungee.api.ChatColor.of("#FFA700") + "т"
                + net.md_5.bungee.api.ChatColor.of("#FF9700") + "а "
                + net.md_5.bungee.api.ChatColor.of("#FF8700") + "М"
                + net.md_5.bungee.api.ChatColor.of("#FF7700") + "о"
                + net.md_5.bungee.api.ChatColor.of("#FF6700") + "р"
                + net.md_5.bungee.api.ChatColor.of("#FF5700") + "к"
                + net.md_5.bungee.api.ChatColor.of("#FF4700") + "в"
                + net.md_5.bungee.api.ChatColor.of("#FF3700") + "а";

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Насичує вас енергією");
        lore.add("§7до максимуму!");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.FORTUNE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(PotionFoodPlugin.SUPER_CARROT_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack checkCustomBrewing(ItemStack basePotion, Inventory inv) {
        ItemStack ingredient = null;
        int ingredientCount = 0;

        for (int i = 0; i < SLOTS_GRID.length; i++) {
            ItemStack item = inv.getItem(SLOTS_GRID[i]);
            if (item != null && item.getType() != Material.AIR) {
                ingredient = item;
                ingredientCount++;
            }
        }

        if (ingredientCount != 1 || ingredient == null) return null;

        PotionMeta pm = (PotionMeta) basePotion.getItemMeta();
        PotionType type = pm.getBasePotionType();
        boolean isSplash = basePotion.getType() == Material.SPLASH_POTION;

        boolean hasHaste = pm.hasCustomEffect(PotionEffectType.HASTE);
        boolean hasBlindness = pm.hasCustomEffect(PotionEffectType.BLINDNESS);

        int currentAmp = 0;
        int currentDur = 0;

        if (hasHaste) {
            PotionEffect eff = pm.getCustomEffects().stream().filter(e -> e.getType().equals(PotionEffectType.HASTE)).findFirst().orElse(null);
            if (eff != null) { currentAmp = eff.getAmplifier(); currentDur = eff.getDuration(); }
        } else if (hasBlindness) {
            PotionEffect eff = pm.getCustomEffects().stream().filter(e -> e.getType().equals(PotionEffectType.BLINDNESS)).findFirst().orElse(null);
            if (eff != null) { currentAmp = eff.getAmplifier(); currentDur = eff.getDuration(); }
        }

        // HASTE RECIPES
        if (!hasHaste && type == PotionType.SWIFTNESS && ingredient.getType() == Material.GOLDEN_PICKAXE) {
            return createPotion("Зілля квапливості", Color.YELLOW, PotionEffectType.HASTE, 3 * 60 * 20, 0, false);
        }
        if (hasHaste && !isSplash && ingredient.getType() == Material.GUNPOWDER) {
            return createPotion("Вибухове зілля квапливості", Color.YELLOW, PotionEffectType.HASTE, (int)(2.5 * 60 * 20), currentAmp, true);
        }
        if (hasHaste && ingredient.getType() == Material.REDSTONE) {
            if (currentDur < 4.5 * 60 * 20) {
                return createPotion(isSplash ? "Вибухове зілля квапливості" : "Зілля квапливості", Color.YELLOW, PotionEffectType.HASTE, (int)(4.5 * 60 * 20), currentAmp, isSplash);
            }
        }
        if (hasHaste && ingredient.getType() == Material.GLOWSTONE_DUST) {
            if (currentAmp == 0) {
                return createPotion(isSplash ? "Вибухове зілля квапливості II" : "Зілля квапливості II", Color.YELLOW, PotionEffectType.HASTE, 2 * 60 * 20, 1, isSplash);
            }
        }

        // BLINDNESS RECIPES
        if (!hasBlindness && type == PotionType.NIGHT_VISION && ingredient.getType() == Material.SPIDER_EYE) {
            return createPotion("Зілля сліпоти", Color.BLACK, PotionEffectType.BLINDNESS, 3 * 60 * 20, 0, false);
        }
        if (hasBlindness && !isSplash && ingredient.getType() == Material.GUNPOWDER) {
            return createPotion("Вибухове зілля сліпоти", Color.BLACK, PotionEffectType.BLINDNESS, currentDur, currentAmp, true);
        }
        if (hasBlindness && ingredient.getType() == Material.REDSTONE) {
            if (currentDur < 4.5 * 60 * 20) {
                return createPotion(isSplash ? "Вибухове зілля сліпоти" : "Зілля сліпоти", Color.BLACK, PotionEffectType.BLINDNESS, (int)(4.5 * 60 * 20), currentAmp, isSplash);
            }
        }
        return null;
    }

    private void processBrewing(Inventory inv, Player player, ItemStack result) {
        player.getInventory().addItem(result);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);

        ItemStack input = inv.getItem(SLOT_INPUT);
        if (input != null) {
            input.setAmount(input.getAmount() - 1);
            if (input.getAmount() <= 0) inv.setItem(SLOT_INPUT, new ItemStack(Material.GLASS_BOTTLE));
            else inv.setItem(SLOT_INPUT, input);
        }

        for (int i = 0; i < SLOTS_GRID.length; i++) {
            ItemStack item = inv.getItem(SLOTS_GRID[i]);
            if (item != null && item.getType() != Material.AIR) {
                item.setAmount(item.getAmount() - 1);
                inv.setItem(SLOTS_GRID[i], item.getAmount() > 0 ? item : null);
                break;
            }
        }
        updateResult(inv);
    }


    private void updateFoodResult(Inventory inv, ItemStack food) {
        List<PotionEffect> finalEffects = new ArrayList<>();
        int potionsFound = 0;

        for (int slot : SLOTS_GRID) {
            ItemStack potion = inv.getItem(slot);
            if (potion != null && potion.getItemMeta() instanceof PotionMeta pm) {
                List<PotionEffect> effects = PotionUtil.readPotionEffects(pm);

                for (PotionEffect pe : effects) {
                    if (isBlacklisted(pe.getType())) {
                        inv.setItem(SLOT_RESULT, null);
                        return;
                    }
                    PotionEffect fixedEffect = new PotionEffect(pe.getType(), 200, pe.getAmplifier());
                    finalEffects.add(fixedEffect);
                }

                if (!effects.isEmpty()) {
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

    private void processFoodCraft(Inventory inv, Player player, ItemStack result) {
        player.getInventory().addItem(result);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);

        ItemStack food = inv.getItem(SLOT_INPUT);
        if (food != null) {
            food.setAmount(food.getAmount() - 1);
            inv.setItem(SLOT_INPUT, food.getAmount() > 0 ? food : null);
        }

        int maxUses = plugin.getConfig().getInt("potion.max-uses", 16);

        for (int slot : SLOTS_GRID) {
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


    private ItemStack createPotion(String name, Color color, PotionEffectType type, int duration, int amplifier, boolean isSplash) {
        ItemStack item = new ItemStack(isSplash ? Material.SPLASH_POTION : Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName("§r" + name);
        meta.setColor(color);
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        meta.setBasePotionType(PotionType.WATER);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        List<String> lore = new ArrayList<>();
        String typeName = type.getName().equals("FAST_DIGGING") ? "Haste" : "Blindness";
        String ampStr = (amplifier > 0) ? "II" : "";
        int min = duration / 20 / 60;
        int sec = (duration / 20) % 60;
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDisplayPotion(PotionType base, Color color, String name) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(base);
        meta.setDisplayName("§f" + name);
        if (color != null) meta.setColor(color);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isBrewingRecipe(Inventory inv) {
        ItemStack input = inv.getItem(SLOT_INPUT);
        return isPotion(input) && inv.getItem(SLOT_RESULT) != null;
    }
    private boolean isPotion(ItemStack item) {
        return item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION);
    }
    private boolean isBlacklisted(PotionEffectType type) {
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.isEmpty()) blacklist = plugin.getConfig().getStringList("potion.blacklist");
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

    public static class StationHolder implements InventoryHolder {
        public final Block block;
        public StationHolder(Block block) { this.block = block; }
        @Override public Inventory getInventory() { return null; }
    }
    public static class VirtualHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public static class RecipeHolder implements InventoryHolder {
        public final Block block;
        public RecipeHolder(Block block) { this.block = block; }
        @Override public Inventory getInventory() { return null; }
    }
}