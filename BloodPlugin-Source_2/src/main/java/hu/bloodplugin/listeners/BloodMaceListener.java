package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodMaceListener implements Listener {

    private final BloodPlugin plugin;
    private final Set<UUID> inLaunch   = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // UUID -> ready time ms

    private static final long COOLDOWN_MS = 45_000L; // 45 seconds

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

        // Check cooldown
        long now = System.currentTimeMillis();
        Long ready = cooldowns.get(uuid);
        if (ready != null && now < ready) {
            long remaining = (ready - now) / 1000 + 1;
            player.sendActionBar(Component.text(
                "Blood Mace cooldown: " + remaining + "s", NamedTextColor.DARK_RED));
            return;
        }

        event.setCancelled(true);

        // Launch
        Vector vel = new Vector(0, 1.55, 0);
        Vector horiz = player.getVelocity().clone();
        horiz.setY(0);
        vel.add(horiz.multiply(0.4));
        player.setVelocity(vel);

        // Set cooldown
        cooldowns.put(uuid, now + COOLDOWN_MS);
        inLaunch.add(uuid);

        player.sendActionBar(Component.text("☽ Blood Mace feldobás! (45s cooldown)", NamedTextColor.DARK_RED));

        // Remove fall immunity once landed
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
