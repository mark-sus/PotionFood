package me.mvk.potionFood;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class PotionFoodPlugin extends JavaPlugin {

    public static NamespacedKey EFFECT_COUNT;
    public static NamespacedKey SPECIAL_POISON_KEY;
    public static NamespacedKey POTION_USES;
    public static NamespacedKey POTION_DRANK;

    public static NamespacedKey STATION_KEY;
    public static NamespacedKey HOLOGRAM_KEY;
    public static NamespacedKey INVENTORY_DATA_KEY;

    public static NamespacedKey SUPER_CARROT_KEY;

    private MenuHandler menuHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EFFECT_COUNT = new NamespacedKey(this, "effect_count");
        SPECIAL_POISON_KEY = new NamespacedKey(this, "poison_special");
        POTION_USES = new NamespacedKey(this, "potion_uses");
        POTION_DRANK = new NamespacedKey(this, "potion_drank");

        STATION_KEY = new NamespacedKey(this, "station_active");
        HOLOGRAM_KEY = new NamespacedKey(this, "station_hologram");
        SUPER_CARROT_KEY = new NamespacedKey(this, "super_carrot");

        menuHandler = new MenuHandler(this);
        getServer().getPluginManager().registerEvents(menuHandler, this);
        getServer().getPluginManager().registerEvents(new ConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new StationListener(this, menuHandler), this);

        getCommand("potionfood").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage("§aPotionFood config reloaded");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("craft")) {
                if (sender instanceof org.bukkit.entity.Player player) {
                    menuHandler.openCraftMenu(player);
                }
                return true;
            }
            sender.sendMessage("§cUsage: /potionfood reload");
            return true;
        });
    }

    public MenuHandler getMenuHandler() {
        return menuHandler;
    }
}