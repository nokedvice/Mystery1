package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.managers.BloodMoonManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class BloodMoonListener implements Listener {

    private final BloodPlugin plugin;
    private final BloodMoonManager manager;

    public BloodMoonListener(BloodPlugin plugin, BloodMoonManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    // Double damage dealt by mobs during Blood Moon
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!manager.isBloodMoonActive()) return;
        if (event.getDamager() instanceof Player) return; // only mob → player
        if (!(event.getEntity() instanceof Player)) return;
        event.setDamage(event.getDamage() * 2.0);
    }

    // Double HP on spawn during Blood Moon
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!manager.isBloodMoonActive()) return;
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        // Delay by 1 tick so attributes are fully initialised
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            manager.applyBloodMoonBuffs(entity);
        }, 1L);
    }

    // Prevent sleeping during Blood Moon
    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!manager.isBloodMoonActive()) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(
            net.kyori.adventure.text.Component.text(
                "Nem lehet aludni Blood Moon alatt!", net.kyori.adventure.text.format.NamedTextColor.RED));
    }
}
