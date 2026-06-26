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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemLimitListener implements Listener {

    private final BloodPlugin plugin;
    // Cooldown map to prevent spam messages (UUID -> last message time ms)
    private final Map<UUID, Long> messageCooldown = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000L; // 3 seconds between messages

    public ItemLimitListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (isLimited(item) && alreadyHas(player, getKey(item))) {
            event.setCancelled(true);
            sendLimitMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) return;
        if (isLimited(cursor) && alreadyHas(player, getKey(cursor))) {
            event.setCancelled(true);
            sendLimitMessage(player);
        }
    }

    private void sendLimitMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = messageCooldown.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_COOLDOWN_MS) return; // suppress spam
        messageCooldown.put(player.getUniqueId(), now);
        player.sendMessage(Component.text(
            "Már van nálad ebből az itemből, csak 1 lehet!", NamedTextColor.RED));
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
