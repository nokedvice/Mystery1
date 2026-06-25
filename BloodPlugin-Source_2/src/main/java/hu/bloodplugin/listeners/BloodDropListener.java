package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BloodDropListener implements Listener {

    private final BloodPlugin plugin;
    private final Random random = new Random();
    // Players who have already died at least once by another player's hand
    private final Set<UUID> hasHadFirstDeath = new HashSet<>();

    public BloodDropListener(BloodPlugin plugin) {
        this.plugin = plugin;
        BloodItems.init(plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Only count deaths caused by another player
        if (!(victim.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent dmg)) return;
        if (!(dmg.getDamager() instanceof Player)) return;

        UUID uuid = victim.getUniqueId();

        if (!hasHadFirstDeath.contains(uuid)) {
            // First death by a player – guaranteed blood drop
            hasHadFirstDeath.add(uuid);
            event.getDrops().add(BloodItems.createBloodDrop());
        } else {
            // Subsequent deaths – 20% chance
            if (random.nextDouble() < plugin.getConfig().getDouble("blood-drop.death-chance", 0.20)) {
                event.getDrops().add(BloodItems.createBloodDrop());
            }
        }
    }
}
