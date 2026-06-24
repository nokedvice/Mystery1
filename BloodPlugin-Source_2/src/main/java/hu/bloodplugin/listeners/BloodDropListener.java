package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class BloodDropListener implements Listener {

    private final BloodPlugin plugin;
    private final Random random = new Random();

    public BloodDropListener(BloodPlugin plugin) {
        this.plugin = plugin;
        BloodItems.init(plugin);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Only non-player mobs
        if (entity instanceof Player) return;

        double chance = plugin.getConfig().getDouble("blood-drop.death-chance", 0.20);
        if (random.nextDouble() < chance) {
            event.getDrops().add(BloodItems.createBloodDrop());
        }
    }
}
