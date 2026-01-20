package me.mvk.potionFood;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.TileState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;

public class StationListener implements Listener {

    private final PotionFoodPlugin plugin;
    private final MenuHandler menuHandler;

    public StationListener(PotionFoodPlugin plugin, MenuHandler menuHandler) {
        this.plugin = plugin;
        this.menuHandler = menuHandler;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BREWING_STAND) return;

        Player player = event.getPlayer();
        BlockState state = block.getState();
        if (!(state instanceof BrewingStand brewingStand)) return;

        PersistentDataContainer data = brewingStand.getPersistentDataContainer();

        if (player.isSneaking()) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.GOLDEN_APPLE) {
                if (data.has(PotionFoodPlugin.STATION_KEY, PersistentDataType.BYTE)) {
                    player.sendMessage("§cThis station is already enchanted!");
                    return;
                }
                data.set(PotionFoodPlugin.STATION_KEY, PersistentDataType.BYTE, (byte) 1);
                brewingStand.update();

                createHologram(block.getLocation());
                player.playSound(block.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3);

                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                event.setCancelled(true);
            }
            return;
        }

        if (data.has(PotionFoodPlugin.STATION_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            menuHandler.openStationMenu(player, block);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BREWING_STAND) return;

        if (block.getState() instanceof TileState state) {
            PersistentDataContainer data = state.getPersistentDataContainer();

            if (data.has(PotionFoodPlugin.STATION_KEY, PersistentDataType.BYTE)) {

                removeHologram(block.getLocation());

                if (data.has(MenuHandler.INVENTORY_DATA_KEY, PersistentDataType.STRING)) {
                    String base64 = data.get(MenuHandler.INVENTORY_DATA_KEY, PersistentDataType.STRING);
                    ItemStack[] items = ItemSerializer.fromBase64(base64);

                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.AIR) {
                            block.getWorld().dropItemNaturally(block.getLocation(), item);
                        }
                    }
                }

                menuHandler.removeCachedInventory(block.getLocation());

            }
        }
    }

    private void createHologram(Location blockLoc) {
        Location holoLoc = blockLoc.clone().add(0.5, 1.2, 0.5);
        ArmorStand as = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setMarker(true);
        as.setCustomName("§b*");
        as.setCustomNameVisible(true);
        as.setInvulnerable(true);
        as.getPersistentDataContainer().set(PotionFoodPlugin.HOLOGRAM_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    private void removeHologram(Location blockLoc) {
        Collection<Entity> entities = blockLoc.getWorld().getNearbyEntities(blockLoc.add(0.5, 1.2, 0.5), 1, 1, 1);
        for (Entity en : entities) {
            if (en instanceof ArmorStand) {
                if (en.getPersistentDataContainer().has(PotionFoodPlugin.HOLOGRAM_KEY, PersistentDataType.BYTE)) {
                    en.remove();
                }
            }
        }
    }
}