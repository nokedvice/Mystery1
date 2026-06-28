package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class MaceEnchantListener implements Listener {

    private final BloodPlugin plugin;

    // Sima mace limits
    private static final int MAX_DENSITY = 2;
    private static final int MAX_BREACH   = 1;

    // Blood Mace limits
    private static final int BLOOD_MAX_DENSITY   = 3;
    private static final int BLOOD_MAX_WIND_BURST = 2;

    public MaceEnchantListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Anvil result check ───────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType() != Material.MACE) return;

        boolean isBloodMace = BloodItems.is(result, BloodItems.BLOOD_MACE_KEY);
        ItemStack fixed = fixEnchants(result, isBloodMace);
        if (fixed != null) event.setResult(fixed);
    }

    // ─── Enchanting table ─────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.MACE) return;
        if (BloodItems.is(item, BloodItems.BLOOD_MACE_KEY)) return;

        // Check added enchants
        Map<Enchantment, Integer> added = event.getEnchantsToAdd();
        boolean modified = false;

        if (added.containsKey(Enchantment.DENSITY) && added.get(Enchantment.DENSITY) > MAX_DENSITY) {
            added.put(Enchantment.DENSITY, MAX_DENSITY);
            modified = true;
        }
        if (added.containsKey(Enchantment.BREACH) && added.get(Enchantment.BREACH) > MAX_BREACH) {
            added.put(Enchantment.BREACH, MAX_BREACH);
            modified = true;
        }
        // Wind Burst is NOT limited on normal mace
        if (modified && event.getEnchanter() instanceof Player p) {
            p.sendMessage(net.kyori.adventure.text.Component.text(
                "A sima Mace-en Density max 2, Breach max 1 lehet! (Wind Burst szabad)",
                net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    // ─── Inventory click – catch /give or other methods ───────────
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        checkAndFix(cursor, player);
        checkAndFix(current, player);
    }

    private void checkAndFix(ItemStack item, Player player) {
        if (item == null || item.getType() != Material.MACE) return;
        boolean isBlood = BloodItems.is(item, BloodItems.BLOOD_MACE_KEY);
        ItemStack fixed = fixEnchants(item, isBlood);
        if (fixed != null) {
            item.setItemMeta(fixed.getItemMeta());
            player.sendMessage(net.kyori.adventure.text.Component.text(
                isBlood ? "Blood Mace enchantok limitálva (Density 3, Wind Burst 2)!"
                        : "Sima Mace enchantok limitálva (Density 2, Breach 1)!",
                net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    /**
     * Returns a corrected copy if any enchant exceeded limits, null if all OK.
     */
    private ItemStack fixEnchants(ItemStack item, boolean isBloodMace) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        boolean changed = false;

        if (isBloodMace) {
            // Blood Mace: Density max 3, Wind Burst max 2, keep unbreakable
            if (meta.hasEnchant(Enchantment.DENSITY) && meta.getEnchantLevel(Enchantment.DENSITY) > BLOOD_MAX_DENSITY) {
                meta.removeEnchant(Enchantment.DENSITY);
                meta.addEnchant(Enchantment.DENSITY, BLOOD_MAX_DENSITY, true);
                changed = true;
            }
            if (meta.hasEnchant(Enchantment.WIND_BURST) && meta.getEnchantLevel(Enchantment.WIND_BURST) > BLOOD_MAX_WIND_BURST) {
                meta.removeEnchant(Enchantment.WIND_BURST);
                meta.addEnchant(Enchantment.WIND_BURST, BLOOD_MAX_WIND_BURST, true);
                changed = true;
            }
            if (!meta.isUnbreakable()) {
                meta.setUnbreakable(true);
                changed = true;
            }
        } else {
            // Sima Mace: Density max 2, Breach max 1
            if (meta.hasEnchant(Enchantment.DENSITY) && meta.getEnchantLevel(Enchantment.DENSITY) > MAX_DENSITY) {
                meta.removeEnchant(Enchantment.DENSITY);
                meta.addEnchant(Enchantment.DENSITY, MAX_DENSITY, true);
                changed = true;
            }
            if (meta.hasEnchant(Enchantment.BREACH) && meta.getEnchantLevel(Enchantment.BREACH) > MAX_BREACH) {
                meta.removeEnchant(Enchantment.BREACH);
                meta.addEnchant(Enchantment.BREACH, MAX_BREACH, true);
                changed = true;
            }
        }

        if (!changed) return null;
        ItemStack copy = item.clone();
        copy.setItemMeta(meta);
        return copy;
    }
}
