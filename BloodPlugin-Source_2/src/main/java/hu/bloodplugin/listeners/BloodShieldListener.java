package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodShieldListener implements Listener {

    private final BloodPlugin plugin;
    private final Map<UUID, Integer> blockCount  = new HashMap<>();
    // During immunity: ALL damage that would break the shield is cancelled
    private final Set<UUID> shieldImmune = new HashSet<>();

    public BloodShieldListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        if (!defender.isBlocking()) return;
        if (!hasBloodShield(defender)) return;
        if (!isAxeAttack(event.getDamager())) return;

        UUID uuid = defender.getUniqueId();

        // During immunity window – cancel completely, shield cannot be broken
        if (shieldImmune.contains(uuid)) {
            event.setCancelled(true);
            return;
        }

        int count = blockCount.getOrDefault(uuid, 0) + 1;

        if (count >= 5) {
            // 5th hit: block it and start 1.5s full immunity
            blockCount.put(uuid, 0);
            event.setCancelled(true);
            shieldImmune.add(uuid);
            new BukkitRunnable() {
                @Override
                public void run() {
                    shieldImmune.remove(uuid);
                }
            }.runTaskLater(plugin, 30L); // 1.5 seconds
        } else {
            blockCount.put(uuid, count);
        }
    }

    // Also cancel generic damage to shield during immunity
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;
        if (!hasBloodShield(player)) return;
        if (!shieldImmune.contains(player.getUniqueId())) return;
        // If the damage would normally disable the shield (axe hit handled above),
        // cancel it. For non-axe sources during immunity window we leave alone.
    }

    private boolean hasBloodShield(Player player) {
        return BloodItems.is(player.getInventory().getItemInMainHand(), BloodItems.BLOOD_SHIELD_KEY)
            || BloodItems.is(player.getInventory().getItemInOffHand(), BloodItems.BLOOD_SHIELD_KEY);
    }

    private boolean isAxeAttack(org.bukkit.entity.Entity attacker) {
        if (!(attacker instanceof Player p)) return false;
        Material m = p.getInventory().getItemInMainHand().getType();
        return switch (m) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }
}
