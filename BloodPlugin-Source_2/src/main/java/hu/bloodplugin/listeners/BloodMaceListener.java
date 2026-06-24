package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BloodMaceListener implements Listener {

    private final BloodPlugin plugin;

    // Players currently in flight from Blood Mace launch
    private final Set<UUID> inLaunch = new HashSet<>();

    // Cooldown set so you can't spam launch
    private final Set<UUID> onCooldown = new HashSet<>();

    public BloodMaceListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!BloodItems.is(player.getInventory().getItemInMainHand(), BloodItems.BLOOD_MACE_KEY)) return;

        UUID uuid = player.getUniqueId();
        if (onCooldown.contains(uuid)) return;

        event.setCancelled(true);

        // Launch player upward 15 blocks
        // Approximate velocity: sqrt(2 * g * h) where g ≈ 0.08 blocks/tick²
        // v = sqrt(2 * 0.08 * 15) ≈ 1.549 blocks/tick → in Bukkit units (blocks/tick)
        Vector vel = new Vector(0, 1.55, 0);
        // Add slight current horizontal momentum
        Vector horiz = player.getVelocity().clone();
        horiz.setY(0);
        vel.add(horiz.multiply(0.4));
        player.setVelocity(vel);

        // Mark as launched (fall damage immunity)
        inLaunch.add(uuid);
        onCooldown.add(uuid);

        // Remove cooldown after 5 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> onCooldown.remove(uuid), 100L);

        // Remove fall immunity once player lands (ground check)
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) { inLaunch.remove(uuid); task.cancel(); return; }
            if (player.isOnGround() && player.getVelocity().getY() <= 0) {
                inLaunch.remove(uuid);
                task.cancel();
            }
        }, 10L, 2L);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (inLaunch.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
