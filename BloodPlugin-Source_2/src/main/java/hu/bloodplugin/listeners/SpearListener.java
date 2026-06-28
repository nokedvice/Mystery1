package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpearListener implements Listener {

    private final BloodPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private static final long COOLDOWN_MS = 30_000L;

    // Attribute modifier key to slow attack speed (prevent lunge) during cooldown
    private final NamespacedKey SLOW_ATTACK_KEY;

    public SpearListener(BloodPlugin plugin) {
        this.plugin = plugin;
        this.SLOW_ATTACK_KEY = new NamespacedKey(plugin, "spear_lunge_cd");
    }

    /**
     * Detect jab attack with spear (EntityDamageByEntityEvent with spear in hand).
     * Lunge only triggers on jab attacks (quick left click).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpearAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isSpearWithLunge3(held)) return;
        if (BloodItems.is(held, BloodItems.GOD_SPEAR_KEY)) return; // God Spear nincs cooldown

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long ready = cooldowns.get(uuid);

        if (ready != null && now < ready) {
            // Cooldownon van – a lunge már megtörtént mielőtt megakadályoztuk
            // Következő alkalomra lelassítjuk az attack speedet
            return;
        }

        // Lunge aktiválódott – cooldown beállítása
        cooldowns.put(uuid, now + COOLDOWN_MS);
        applyAttackSlowdown(player);
        startCountdown(player, uuid);
    }

    /**
     * Ha slot vált és nincs kezében spear, vegyük le a slowdownt
     */
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        UUID uuid = player.getUniqueId();
        Long ready = cooldowns.get(uuid);
        if (ready == null || System.currentTimeMillis() >= ready) {
            removeAttackSlowdown(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cooldowns.remove(uuid);
        BukkitTask task = countdownTasks.remove(uuid);
        if (task != null) task.cancel();
        removeAttackSlowdown(event.getPlayer());
    }

    // ─── Slowdown logic ───────────────────────────────────────────
    // Extremely slow attack speed = player cannot jab = cannot lunge
    private void applyAttackSlowdown(Player player) {
        var attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;
        // Remove existing if any
        attr.getModifiers().stream()
            .filter(m -> m.getKey().equals(SLOW_ATTACK_KEY))
            .forEach(attr::removeModifier);
        // Add -1000 modifier (attack speed floored)
        AttributeModifier mod = new AttributeModifier(
            SLOW_ATTACK_KEY, -1000,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.MAINHAND
        );
        attr.addModifier(mod);
    }

    private void removeAttackSlowdown(Player player) {
        var attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;
        attr.getModifiers().stream()
            .filter(m -> m.getKey().equals(SLOW_ATTACK_KEY))
            .forEach(attr::removeModifier);
    }

    // ─── Countdown action bar ─────────────────────────────────────
    private void startCountdown(Player player, UUID uuid) {
        BukkitTask old = countdownTasks.remove(uuid);
        if (old != null) old.cancel();

        BukkitTask task = new BukkitRunnable() {
            int seconds = 30;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Long ready = cooldowns.get(uuid);
                if (ready == null || System.currentTimeMillis() >= ready) {
                    removeAttackSlowdown(player);
                    player.sendActionBar(Component.text("Lunge kész!", NamedTextColor.GREEN));
                    countdownTasks.remove(uuid);
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text(
                    "Lunge cooldown: " + seconds + "s", NamedTextColor.RED));
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(uuid, task);
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private boolean isSpearWithLunge3(ItemStack item) {
        if (item == null) return false;
        Material mat = item.getType();
        // Spear materials in 1.21.11
        boolean isSpear = mat.name().endsWith("_SPEAR");
        if (!isSpear) return false;
        // Lunge III
        org.bukkit.enchantments.Enchantment lunge = org.bukkit.enchantments.Enchantment.getByKey(
            new NamespacedKey("minecraft", "lunge"));
        if (lunge == null) return false;
        return item.getEnchantmentLevel(lunge) >= 3;
    }
}
