package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class FinisherPotionListener implements Listener {

    private final BloodPlugin plugin;

    public FinisherPotionListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player thrower)) return;

        ItemStack thrown = event.getPotion().getItem();
        if (!BloodItems.is(thrown, BloodItems.FINISHER_POTION_KEY)) return;

        // Cancel normal splash effect (affects nobody else)
        event.setCancelled(true);

        // Apply effects to the thrower only
        applyFinisherEffects(thrower);
    }

    private void applyFinisherEffects(Player player) {
        // -2 hearts (4 damage)
        player.damage(4.0);

        // Strength III for 15s
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 15, 2, false, true));
        // Hunger I for 25s
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 25, 0, false, true));
        // Slowness I for 30s
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 30, 0, false, true));
        // Glowing (red outline)
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 30, 0, false, false));

        // Red particle aura for 30 seconds
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 600 || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        player.getLocation().add(0, 1, 0),
                        12,
                        0.4, 0.6, 0.4,
                        new Particle.DustOptions(org.bukkit.Color.RED, 1.2f)
                );
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
