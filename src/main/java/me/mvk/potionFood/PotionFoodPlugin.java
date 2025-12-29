package me.mvk.potionFood;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class PotionFoodPlugin extends JavaPlugin {

    public static NamespacedKey EFFECT_COUNT;
    public static NamespacedKey SPECIAL_POISON_KEY;
    public static NamespacedKey POTION_USES;
    public static NamespacedKey POTION_DRANK;



    @Override
    public void onEnable() {

        saveDefaultConfig();

        EFFECT_COUNT = new NamespacedKey(this, "effect_count");
        SPECIAL_POISON_KEY = new NamespacedKey(this, "poison_special");
        POTION_USES = new NamespacedKey(this, "potion_uses");
        POTION_DRANK = new NamespacedKey(this, "potion_drank");

        getServer().getPluginManager().registerEvents(
                new CraftListener(this), this
        );

        getCommand("potionfood").setExecutor((sender, cmd, label, args) -> {

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage("§aPotionFood config reloaded");
                return true;
            }

            sender.sendMessage("§cUsage: /potionfood reload");
            return true;
        });
    }

}
