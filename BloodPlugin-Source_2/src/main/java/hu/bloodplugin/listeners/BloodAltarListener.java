package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class BloodAltarListener implements Listener {

    private final BloodPlugin plugin;
    private final NamespacedKey ALTAR_KEY;
    private final NamespacedKey ALTAR_UUID_KEY;

    // altar block location (serialized) -> floating item
    private final Map<String, Item> altarItems         = new HashMap<>();
    private final Map<String, List<ArmorStand>> altarHolograms = new HashMap<>();
    private final Map<String, BukkitTask> altarTasks   = new HashMap<>();
    // altar block location -> main ArmorStand UUID
    private final Map<String, UUID> altarStands        = new HashMap<>();

    public BloodAltarListener(BloodPlugin plugin) {
        this.plugin        = plugin;
        this.ALTAR_KEY     = new NamespacedKey(plugin, "blood_altar");
        this.ALTAR_UUID_KEY = new NamespacedKey(plugin, "blood_altar_uuid");
    }

    // ─── /altarspawn ──────────────────────────────────────────────
    public void spawnAltar(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        String key = locKey(loc);

        // Remove old if exists
        removeDisplay(key);

        // Place a barrier block as the "physical" altar
        loc.getBlock().setType(Material.BARRIER);

        // Invisible non-marker ArmorStand so PlayerInteractAtEntityEvent fires
        ArmorStand hitbox = (ArmorStand) world.spawnEntity(
                loc.clone().add(0.5, 0, 0.5), EntityType.ARMOR_STAND);
        hitbox.setVisible(false);
        hitbox.setGravity(false);
        hitbox.setInvulnerable(true);
        hitbox.setSmall(false);
        hitbox.setMarker(false); // NOT marker so interact event fires
        hitbox.setAI(false);
        hitbox.setPersistent(true);
        hitbox.setCollidable(false);
        hitbox.getPersistentDataContainer().set(ALTAR_KEY, PersistentDataType.BYTE, (byte) 1);
        hitbox.getPersistentDataContainer().set(ALTAR_UUID_KEY,
                PersistentDataType.STRING, key);

        altarStands.put(key, hitbox.getUniqueId());
        spawnDisplay(key, loc, BloodItems.createBloodMace());
    }

    public boolean isAltar(Entity e) {
        if (!(e instanceof ArmorStand as)) return false;
        return as.getPersistentDataContainer().has(ALTAR_KEY, PersistentDataType.BYTE);
    }

    // ─── Right click on ArmorStand hitbox ────────────────────────
    @EventHandler
    public void onInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        if (!isAltar(entity)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        String key = entity.getPersistentDataContainer()
                .get(ALTAR_UUID_KEY, PersistentDataType.STRING);
        if (key == null) return;

        Location loc = keyToLoc(key, entity.getWorld());
        handleClick(player, key, loc);
    }

    // ─── Also handle right click on BARRIER block ─────────────────
    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;

        // Check if there's an altar ArmorStand near this barrier
        Location bLoc = block.getLocation();
        for (Entity e : bLoc.getWorld().getNearbyEntities(bLoc.clone().add(0.5,0.5,0.5), 1, 1, 1)) {
            if (!isAltar(e)) continue;
            event.setCancelled(true);
            String key = e.getPersistentDataContainer()
                    .get(ALTAR_UUID_KEY, PersistentDataType.STRING);
            if (key == null) return;
            handleClick(event.getPlayer(), key, bLoc);
            return;
        }
    }

    private void handleClick(Player player, String key, Location loc) {
        ItemStack held = player.getInventory().getItemInMainHand();

        // Op holding Blood Mace → update display
        if (player.isOp() && BloodItems.is(held, BloodItems.BLOOD_MACE_KEY)) {
            removeDisplay(key);
            spawnDisplay(key, loc, held.clone());
            // Respawn hitbox stand
            player.sendMessage(Component.text("✦ Altar display frissítve!", NamedTextColor.GOLD));
            return;
        }

        // Normal craft attempt
        attemptCraft(player, key, loc);
    }

    // ─── Craft ────────────────────────────────────────────────────
    private void attemptCraft(Player player, String key, Location loc) {
        if (hasItem(player, BloodItems.BLOOD_MACE_KEY)) {
            player.sendMessage(Component.text("Már van egy Blood Mace-ed!", NamedTextColor.RED));
            return;
        }
        if (!hasIngredients(player)) {
            player.sendMessage(Component.text(
                "Kell: 8 Netherite Ingot + 4 Vércsepp + 1 Mace", NamedTextColor.DARK_RED));
            return;
        }

        consumeIngredients(player);
        player.getInventory().addItem(BloodItems.createBloodMace());

        World w = loc.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.DUST, loc.clone().add(0.5, 1.5, 0.5),
                    60, 0.6, 0.6, 0.6, new Particle.DustOptions(Color.RED, 2f));
            w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.7f);
        }
        player.sendMessage(Component.text("☽ Blood Mace megalkotva! ☽", NamedTextColor.DARK_RED));
    }

    // ─── Prevent damaging the altar ArmorStand ────────────────────
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (isAltar(event.getEntity())) event.setCancelled(true);
    }

    // ─── Display ─────────────────────────────────────────────────
    private void spawnDisplay(String key, Location blockLoc, ItemStack displayItem) {
        World world = blockLoc.getWorld();
        if (world == null) return;

        // Floating item
        Location itemLoc = blockLoc.clone().add(0.5, 1.5, 0.5);
        Item floatingItem = world.dropItem(itemLoc, displayItem);
        floatingItem.setPickupDelay(Integer.MAX_VALUE);
        floatingItem.setVelocity(new Vector(0, 0, 0));
        floatingItem.setGravity(false);
        floatingItem.setInvulnerable(true);
        floatingItem.setPersistent(true);
        altarItems.put(key, floatingItem);

        // Hologram
        String[] lines = {
            "§4§l✦ Blood Altar ✦",
            "§7Recept:",
            "§6 8x §7Netherite Ingot",
            "§6 4x §7Vércsepp",
            "§6 1x §7Mace",
            "§c§lJobb klikk a crafthoz"
        };

        List<ArmorStand> stands = new ArrayList<>();
        double startY = 2.7;
        for (int i = 0; i < lines.length; i++) {
            Location standLoc = blockLoc.clone().add(0.5, startY - (i * 0.27), 0.5);
            ArmorStand stand = (ArmorStand) world.spawnEntity(standLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setPersistent(true);
            stand.customName(Component.text(lines[i]));
            stand.setCustomNameVisible(true);
            stands.add(stand);
        }
        altarHolograms.put(key, stands);

        // Rotation task
        final double[] angle = {0};
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (floatingItem.isDead()) { cancel(); return; }
                angle[0] = (angle[0] + 4) % 360;
                double rad  = Math.toRadians(angle[0]);
                double bobY = 1.5 + Math.sin(Math.toRadians(angle[0] * 2)) * 0.1;
                floatingItem.teleport(blockLoc.clone().add(
                        0.5 + Math.cos(rad) * 0.3, bobY, 0.5 + Math.sin(rad) * 0.3));
            }
        }.runTaskTimer(plugin, 0L, 2L);
        altarTasks.put(key, task);
    }

    private void removeDisplay(String key) {
        Item item = altarItems.remove(key);
        if (item != null && !item.isDead()) item.remove();

        List<ArmorStand> stands = altarHolograms.remove(key);
        if (stands != null) stands.forEach(s -> { if (!s.isDead()) s.remove(); });

        BukkitTask task = altarTasks.remove(key);
        if (task != null) task.cancel();
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private String locKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location keyToLoc(String key, World fallbackWorld) {
        try {
            String[] parts = key.split(",");
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) w = fallbackWorld;
            return new Location(w, Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return fallbackWorld.getSpawnLocation();
        }
    }

    private boolean hasIngredients(Player player) {
        int netherite = 0, bloodDrop = 0, mace = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() == Material.NETHERITE_INGOT && !BloodItems.is(item, BloodItems.BLOOD_MACE_KEY))
                netherite += item.getAmount();
            if (BloodItems.is(item, BloodItems.BLOOD_DROP_KEY))
                bloodDrop += item.getAmount();
            if (item.getType() == Material.MACE && !BloodItems.is(item, BloodItems.BLOOD_MACE_KEY))
                mace += item.getAmount();
        }
        return netherite >= 8 && bloodDrop >= 4 && mace >= 1;
    }

    private void consumeIngredients(Player player) {
        removeAmount(player, Material.NETHERITE_INGOT, 8, null);
        removeAmount(player, null, 4, BloodItems.BLOOD_DROP_KEY);
        removeAmount(player, Material.MACE, 1, null);
    }

    private void removeAmount(Player player, Material mat, int amount, String bloodKey) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (item == null) continue;
            if (bloodKey != null) {
                if (!BloodItems.is(item, bloodKey)) continue;
            } else {
                if (item.getType() != mat) continue;
                if (BloodItems.is(item, BloodItems.BLOOD_MACE_KEY)) continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
    }

    private boolean hasItem(Player player, String key) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (BloodItems.is(item, key)) return true;
        }
        return false;
    }
}
