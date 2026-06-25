package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodShieldListener implements Listener {

    private final BloodPlugin plugin;
    // How many axe hits have been blocked
    private final Map<UUID, Integer> blockCount = new HashMap<>();
    // Full immunity for 1.5s after 5th block
    private final Set<UUID> immune = new HashSet<>();

    public BloodShieldListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        if (!defender.isBlocking()) return;
        if (!hasBloodShield(defender)) return;
        if (!isAxeAttack(event.getDamager())) return;

        UUID uuid = defender.getUniqueId();

        // During immunity – cancel completely, shield cannot break
        if (immune.contains(uuid)) {
            event.setCancelled(true);
            return;
        }

        int count = blockCount.getOrDefault(uuid, 0) + 1;
        blockCount.put(uuid, count);

        if (count >= 5) {
            // 5th block – cancel damage and start 1.5s immunity
            blockCount.put(uuid, 0);
            event.setCancelled(true);
            immune.add(uuid);
            new BukkitRunnable() {
                @Override
                public void run() {
                    immune.remove(uuid);
                }
            }.runTaskLater(plugin, 30L); // 1.5 seconds
        }
        // For hits 1-4: let damage through normally (shield blocks it naturally)
    }

    private boolean hasBloodShield(Player player) {
        return BloodItems.is(player.getInventory().getItemInMainHand(), BloodItems.BLOOD_SHIELD_KEY)
            || BloodItems.is(player.getInventory().getItemInOffHand(), BloodItems.BLOOD_SHIELD_KEY);
    }

    private boolean isAxeAttack(org.bukkit.entity.Entity attacker) {
        if (!(attacker instanceof Player p)) return false;
        return switch (p.getInventory().getItemInMainHand().getType()) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }
}
