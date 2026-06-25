package hu.bloodplugin.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BloodItems {

    public static final String BLOOD_DROP_KEY      = "blood_drop";
    public static final String BLOOD_SHARD_KEY     = "blood_shard";
    public static final String BLOOD_GEM_KEY       = "blood_gem";
    public static final String FINISHER_POTION_KEY = "finisher_potion";
    public static final String BLOOD_MACE_KEY      = "blood_mace";
    public static final String BLOOD_SHIELD_KEY    = "blood_shield";
    public static final String BLOOD_ALTAR_KEY     = "blood_altar_item";

    private static JavaPlugin plugin;

    public static void init(JavaPlugin p) { plugin = p; }

    private static NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }

    public static boolean is(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(key(id), PersistentDataType.BYTE);
    }

    public static ItemStack createBloodDrop() {
        ItemStack item = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Vércsepp", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Egy friss vércsepp...", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)));
        meta.setCustomModelData(1001);
        meta.getPersistentDataContainer().set(key(BLOOD_DROP_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBloodShard() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Vér Shard", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("9 darab = 1 Vércsepp", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
        meta.setCustomModelData(1002);
        meta.getPersistentDataContainer().set(key(BLOOD_SHARD_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBloodGem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blood Gem", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Shift 3mp: Invis + Speed III (5mp)", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false),
            Component.text("Utána: Weakness + Mining Fatigue (10mp)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("30s cooldown", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setCustomModelData(1003);
        meta.getPersistentDataContainer().set(key(BLOOD_GEM_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFinisherPotion() {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Finisher Potion", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Csak rád hat!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
            Component.text("Strength III (15s) | Hunger I (25s) | Slowness I (30s)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("-2 szív, vörös aura + Glowing", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setCustomModelData(1004);
        meta.getPersistentDataContainer().set(key(FINISHER_POTION_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBloodMace() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blood Mace", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Density VI | Fire Aspect III | Wind Burst II", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
            Component.text("Shift + Jobb klikk: 15 blokk feldobás (45s cd)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setCustomModelData(1005);
        meta.setUnbreakable(true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.DENSITY, 6, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 3, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.WIND_BURST, 2, true);
        meta.getPersistentDataContainer().set(key(BLOOD_MACE_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBloodShield() {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blood Shield", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Minden 5. blokkolt fejsze ütést megvéd", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false),
            Component.text("1.5s immunitás az 5. blokk után", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setCustomModelData(1006);
        meta.getPersistentDataContainer().set(key(BLOOD_SHIELD_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBloodAltarItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blood Altar", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
        meta.setCustomModelData(1007);
        meta.getPersistentDataContainer().set(key(BLOOD_ALTAR_KEY), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
