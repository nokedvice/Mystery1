package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BloodGemListener implements Listener {

    private final BloodPlugin plugin;

    private final Map<UUID, Integer> shiftTicks = new HashMap<>();
    private final Set<UUID> inInvis             = new HashSet<>();
    private final Map<UUID, Long> cooldowns     = new HashMap<>(); // UUID -> System.currentTimeMillis() when ready

    private static final int REQUIRED_TICKS  = 60;  // 3 seconds
    private static final long COOLDOWN_MS    = 30_000L; // 30 seconds

    public BloodGemListener(BloodPlugin plugin) {
        this.plugin = plugin;
        startShiftChecker();
    }

    private void startShiftChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (inInvis.contains(uuid)) continue;

                    boolean holdingGem = hasGemInHand(player);
                    boolean shifting   = player.isSneaking();

                    if (holdingGem && shifting) {
                        // Show cooldown message if on cooldown
                        Long ready = cooldowns.get(uuid);
                        if (ready != null && System.currentTimeMillis() < ready) {
                            long remaining = (ready - System.currentTimeMillis()) / 1000 + 1;
                            // Only show every 20 ticks (1s)
                            if (shiftTicks.getOrDefault(uuid, 0) % 20 == 0) {
                                player.sendActionBar(Component.text(
                                    "Blood Gem cooldown: " + remaining + "s",
                                    NamedTextColor.DARK_RED));
                            }
                            shiftTicks.put(uuid, shiftTicks.getOrDefault(uuid, 0) + 1);
                            return;
                        }

                        int ticks = shiftTicks.getOrDefault(uuid, 0) + 1;
                        shiftTicks.put(uuid, ticks);

                        // Progress bar in action bar
                        int progress = (int)((ticks / (double) REQUIRED_TICKS) * 20);
                        String bar = "§c" + "█".repeat(progress) + "§8" + "█".repeat(20 - progress);
                        player.sendActionBar(Component.text("Blood Gem: " + bar));

                        if (ticks >= REQUIRED_TICKS) {
                            shiftTicks.remove(uuid);
                            activateGem(player);
                        }
                    } else {
                        shiftTicks.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean hasGemInHand(Player player) {
        return BloodItems.is(player.getInventory().getItemInMainHand(), BloodItems.BLOOD_GEM_KEY)
            || BloodItems.is(player.getInventory().getItemInOffHand(), BloodItems.BLOOD_GEM_KEY);
    }

    private void activateGem(Player player) {
        UUID uuid = player.getUniqueId();
        inInvis.add(uuid);

        // Set cooldown (starts after invis ends = 5s + 30s = 35s from now, but we set from activation)
        cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_MS + 5000L);

        // Full invisibility
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 5, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 2, false, true));

        // Hide from all other players (armor too)
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.equals(player)) other.hidePlayer(plugin, player);
        }

        player.sendMessage(Component.text("☽ Blood Gem aktiválva! (5s invis + Speed III)", NamedTextColor.DARK_RED));

        new BukkitRunnable() {
            @Override
            public void run() {
                inInvis.remove(uuid);
                if (!player.isOnline()) return;

                // Show again
                for (Player other : plugin.getServer().getOnlinePlayers()) {
                    if (!other.equals(player)) other.showPlayer(plugin, player);
                }

                // Debuffs
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 127, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 10, 2, false, true));
                player.sendMessage(Component.text("Blood Gem lejárt. Weakness + Mining Fatigue 10mp. (30s cooldown)", NamedTextColor.GRAY));
            }
        }.runTaskLater(plugin, 20L * 5);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        shiftTicks.remove(uuid);
        inInvis.remove(uuid);
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            other.showPlayer(plugin, event.getPlayer());
        }
    }
}
