package hu.bloodplugin.managers;

import hu.bloodplugin.BloodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class BloodMoonManager {

    private final BloodPlugin plugin;
    private final Random random = new Random();

    private boolean bloodMoonActive = false;
    private boolean forceNextNight = false;
    private boolean wasNight = false;
    private BukkitTask nightCheckerTask;

    // Night = time 13000–23000 roughly
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END   = 23000L;

    public BloodMoonManager(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    public void startNightChecker() {
        nightCheckerTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    long time = world.getTime();
                    boolean isNight = time >= NIGHT_START && time <= NIGHT_END;

                    if (isNight && !wasNight) {
                        // Night just started
                        onNightStart(world);
                    } else if (!isNight && wasNight) {
                        // Day just started
                        onDayStart(world);
                    }
                    wasNight = isNight;
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // check every 2 seconds
    }

    private void onNightStart(World world) {
        boolean activate = forceNextNight || (random.nextDouble() < plugin.getConfig().getDouble("blood-moon.chance", 0.20));
        forceNextNight = false;

        if (activate) {
            bloodMoonActive = true;
            world.setFullTime(18000L); // midnight
            // Red moon via game rule / resource pack handles visuals
            // Disable sleeping
            world.setGameRule(org.bukkit.GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);

            Component msg = Component.text("☽ BLOOD MOON! ☽", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                    .append(Component.text(" Az éjszaka veszélyesebb – a mobok kétszer erősebbek!", NamedTextColor.RED));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(msg);
                p.sendActionBar(Component.text("☽ BLOOD MOON AKTÍV ☽", NamedTextColor.DARK_RED));
            }
            plugin.getLogger().info("[BloodPlugin] Blood Moon started!");
        }
    }

    private void onDayStart(World world) {
        if (bloodMoonActive) {
            bloodMoonActive = false;
            world.setGameRule(org.bukkit.GameRule.PLAYERS_SLEEPING_PERCENTAGE, 50);

            Component msg = Component.text("A Blood Moon véget ért. Virrad...", NamedTextColor.GOLD);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(msg);
            }
            plugin.getLogger().info("[BloodPlugin] Blood Moon ended.");
        }
    }

    public boolean isBloodMoonActive() {
        return bloodMoonActive;
    }

    public void scheduleForceNextNight() {
        forceNextNight = true;
    }

    /**
     * Apply Blood Moon modifiers to a mob that just spawned or is being damaged.
     * Called from the listener to double HP on spawn.
     */
    public void applyBloodMoonBuffs(LivingEntity entity) {
        if (!bloodMoonActive) return;
        // Double max HP
        var attr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            attr.setBaseValue(base * 2);
            entity.setHealth(attr.getValue());
        }
        // Random armor chance (give some resistance via effect)
        if (random.nextDouble() < 0.40) {
            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        }
    }
}
