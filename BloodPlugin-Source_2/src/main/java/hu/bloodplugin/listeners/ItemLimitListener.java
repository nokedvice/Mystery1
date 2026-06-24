package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Ensures a player cannot hold more than 1 of:
 *  - Blood Gem
 *  - Finisher Potion
 *  - Blood Mace
 */
public class ItemLimitListener implements Listener {

    private final BloodPlugin plugin;

    public ItemLimitListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (isLimited(item) && alreadyHas(player, getKey(item))) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Már van nálad ebből az itemből!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) return;
        if (isLimited(cursor) && alreadyHas(player, getKey(cursor))) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Már van nálad ebből az itemből!", NamedTextColor.RED));
        }
    }

    private boolean isLimited(ItemStack item) {
        return BloodItems.is(item, BloodItems.BLOOD_GEM_KEY)
            || BloodItems.is(item, BloodItems.FINISHER_POTION_KEY)
            || BloodItems.is(item, BloodItems.BLOOD_MACE_KEY);
    }

    private String getKey(ItemStack item) {
        if (BloodItems.is(item, BloodItems.BLOOD_GEM_KEY))       return BloodItems.BLOOD_GEM_KEY;
        if (BloodItems.is(item, BloodItems.FINISHER_POTION_KEY)) return BloodItems.FINISHER_POTION_KEY;
        if (BloodItems.is(item, BloodItems.BLOOD_MACE_KEY))      return BloodItems.BLOOD_MACE_KEY;
        return "";
    }

    private boolean alreadyHas(Player player, String key) {
        if (key.isEmpty()) return false;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (BloodItems.is(inv, key)) return true;
        }
        return false;
    }
}
