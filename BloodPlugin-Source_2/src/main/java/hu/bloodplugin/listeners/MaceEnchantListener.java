package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class MaceEnchantListener implements Listener {

    private final BloodPlugin plugin;

    // Sima Mace limits
    private static final int MACE_MAX_DENSITY = 2;
    private static final int MACE_MAX_BREACH  = 1;
    // Wind Burst: unlimited on normal mace

    // Blood Mace limits
    private static final int BLOOD_MAX_DENSITY    = 3;
    private static final int BLOOD_MAX_WIND_BURST = 2;

    public MaceEnchantListener(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Anvil ────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType() != Material.MACE) return;

        boolean isBlood = BloodItems.is(result, BloodItems.BLOOD_MACE_KEY);
        ItemStack fixed = applyLimits(result.clone(), isBlood);
        if (fixed != null) event.setResult(fixed);
    }

    // ─── Enchanting table ─────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.MACE) return;
        if (BloodItems.is(item, BloodItems.BLOOD_MACE_KEY)) return;

        Map<Enchantment, Integer> added = event.getEnchantsToAdd();
        boolean changed = false;

        if (added.containsKey(Enchantment.DENSITY) && added.get(Enchantment.DENSITY) > MACE_MAX_DENSITY) {
            added.put(Enchantment.DENSITY, MACE_MAX_DENSITY);
            changed = true;
        }
        if (added.containsKey(Enchantment.BREACH) && added.get(Enchantment.BREACH) > MACE_MAX_BREACH) {
            added.put(Enchantment.BREACH, MACE_MAX_BREACH);
            changed = true;
        }
        // Wind Burst: no limit on normal mace

        if (changed) {
            event.getEnchanter().sendMessage(Component.text(
                "Sima Mace: Density max 2, Breach max 1! (Wind Burst szabad)",
                NamedTextColor.RED));
        }
    }

    // ─── Limits logic ─────────────────────────────────────────────
    private ItemStack applyLimits(ItemStack item, boolean isBlood) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        boolean changed = false;

        if (isBlood) {
            // Blood Mace: Density max 3, Wind Burst max 2, always Unbreakable
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
            // Sima Mace: Density max 2, Breach max 1, Wind Burst unlimited
            if (meta.hasEnchant(Enchantment.DENSITY) && meta.getEnchantLevel(Enchantment.DENSITY) > MACE_MAX_DENSITY) {
                meta.removeEnchant(Enchantment.DENSITY);
                meta.addEnchant(Enchantment.DENSITY, MACE_MAX_DENSITY, true);
                changed = true;
            }
            if (meta.hasEnchant(Enchantment.BREACH) && meta.getEnchantLevel(Enchantment.BREACH) > MACE_MAX_BREACH) {
                meta.removeEnchant(Enchantment.BREACH);
                meta.addEnchant(Enchantment.BREACH, MACE_MAX_BREACH, true);
                changed = true;
            }
        }

        if (!changed) return null;
        item.setItemMeta(meta);
        return item;
    }
}
