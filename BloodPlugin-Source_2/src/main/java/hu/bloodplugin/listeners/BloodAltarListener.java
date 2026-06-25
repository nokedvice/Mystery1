package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
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
    private final NamespacedKey ALTAR_LOC_KEY;
    private final NamespacedKey ALTAR_HOLO_KEY;
    private final NamespacedKey ALTAR_USED_KEY; // marks altar as used (mace already crafted)

    private final Map<String, Item> altarItems             = new HashMap<>();
    private final Map<String, List<ArmorStand>> altarHolograms = new HashMap<>();
    private final Map<String, BukkitTask> altarTasks       = new HashMap<>();

    // Track whether the server-wide Blood Mace has been crafted
    private boolean maceCrafted = false;

    public BloodAltarListener(BloodPlugin plugin) {
        this.plugin         = plugin;
        this.ALTAR_KEY      = new NamespacedKey(plugin, "blood_altar");
        this.ALTAR_LOC_KEY  = new NamespacedKey(plugin, "blood_altar_loc");
        this.ALTAR_HOLO_KEY = new NamespacedKey(plugin, "blood_altar_holo");
        this.ALTAR_USED_KEY = new NamespacedKey(plugin, "blood_altar_used");
    }

    // ─── /altarspawn ──────────────────────────────────────────────
    public void spawnAltar(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        String key = locKey(loc);
        removeDisplay(key);

        loc.getBlock().setType(Material.BARRIER);

        ArmorStand hitbox = (ArmorStand) world.spawnEntity(
                loc.clone().add(0.5, 0, 0.5), EntityType.ARMOR_STAND);
        hitbox.setVisible(false);
        hitbox.setGravity(false);
        hitbox.setInvulnerable(true);
        hitbox.setSmall(false);
        hitbox.setMarker(false);
        hitbox.setAI(false);
        hitbox.setPersistent(true);
        hitbox.setCollidable(false);
        hitbox.getPersistentDataContainer().set(ALTAR_KEY, PersistentDataType.BYTE, (byte) 1);
        hitbox.getPersistentDataContainer().set(ALTAR_LOC_KEY, PersistentDataType.STRING, key);

        spawnDisplay(key, loc, BloodItems.createBloodMace(), false);
    }

    public boolean isAltar(Entity e) {
        if (!(e instanceof ArmorStand as)) return false;
        return as.getPersistentDataContainer().has(ALTAR_KEY, PersistentDataType.BYTE);
    }

    // ─── Interact with ArmorStand hitbox ──────────────────────────
    @EventHandler
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        if (!isAltar(entity)) return;
        event.setCancelled(true);

        String key = entity.getPersistentDataContainer()
                .get(ALTAR_LOC_KEY, PersistentDataType.STRING);
        if (key == null) return;

        // Check if already used
        if (entity.getPersistentDataContainer().has(ALTAR_USED_KEY, PersistentDataType.BYTE)) {
            event.getPlayer().sendMessage(Component.text(
                "Ez az oltár már fel lett használva. A Blood Mace elkészült.", NamedTextColor.DARK_RED));
            return;
        }

        Location loc = keyToLoc(key, entity.getWorld());
        handleClick(event.getPlayer(), key, loc, entity);
    }

    // ─── Interact with Barrier block ──────────────────────────────
    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;

        Location bLoc = block.getLocation();
        for (Entity e : bLoc.getWorld().getNearbyEntities(bLoc.clone().add(0.5, 0.5, 0.5), 1, 1, 1)) {
            if (!isAltar(e)) continue;
            event.setCancelled(true);

            if (e.getPersistentDataContainer().has(ALTAR_USED_KEY, PersistentDataType.BYTE)) {
                event.getPlayer().sendMessage(Component.text(
                    "Ez az oltár már fel lett használva.", NamedTextColor.DARK_RED));
                return;
            }

            String key = e.getPersistentDataContainer()
                    .get(ALTAR_LOC_KEY, PersistentDataType.STRING);
            if (key == null) return;
            handleClick(event.getPlayer(), key, bLoc, e);
            return;
        }
    }

    private void handleClick(Player player, String key, Location loc, Entity altarEntity) {
        ItemStack held = player.getInventory().getItemInMainHand();

        // Op + Blood Mace in hand → update display
        if (player.isOp() && BloodItems.is(held, BloodItems.BLOOD_MACE_KEY)) {
            removeDisplay(key);
            spawnDisplay(key, loc, held.clone(), false);
            player.sendMessage(Component.text("✦ Altar display frissítve!", NamedTextColor.GOLD));
            return;
        }

        attemptCraft(player, key, loc, altarEntity);
    }

    // ─── Craft – once per server ───────────────────────────────────
    private void attemptCraft(Player player, String key, Location loc, Entity altarEntity) {
        if (maceCrafted) {
            player.sendMessage(Component.text(
                "A Blood Mace már elkészült ezen a szerveren!", NamedTextColor.DARK_RED));
            return;
        }
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
        maceCrafted = true;

        // Mark altar as used
        if (altarEntity instanceof ArmorStand as) {
            as.getPersistentDataContainer().set(ALTAR_USED_KEY, PersistentDataType.BYTE, (byte) 1);
        }

        // Effects
        World w = loc.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.DUST, loc.clone().add(0.5, 1.5, 0.5),
                    80, 0.8, 0.8, 0.8, new Particle.DustOptions(Color.RED, 2f));
            w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
        }

        // Broadcast to whole server
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(Component.text("☽ " + player.getName() +
                " elkészítette a Blood Mace-t! ☽", NamedTextColor.DARK_RED));
        }

        // Remove display – altar is consumed
        removeDisplay(key);

        // Remove the barrier block and the hitbox ArmorStand
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            loc.getBlock().setType(Material.AIR);
            altarEntity.remove();
        }, 40L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (isAltar(event.getEntity())) event.setCancelled(true);
    }

    // ─── Display ──────────────────────────────────────────────────
    private void spawnDisplay(String key, Location blockLoc, ItemStack displayItem, boolean used) {
        World world = blockLoc.getWorld();
        if (world == null) return;

        Item floatingItem = world.dropItem(blockLoc.clone().add(0.5, 1.5, 0.5), displayItem);
        floatingItem.setPickupDelay(Integer.MAX_VALUE);
        floatingItem.setVelocity(new Vector(0, 0, 0));
        floatingItem.setGravity(false);
        floatingItem.setInvulnerable(true);
        floatingItem.setPersistent(true);
        altarItems.put(key, floatingItem);

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
            ArmorStand stand = (ArmorStand) world.spawnEntity(
                    blockLoc.clone().add(0.5, startY - (i * 0.27), 0.5), EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setPersistent(true);
            stand.customName(Component.text(lines[i]));
            stand.setCustomNameVisible(true);
            stand.getPersistentDataContainer().set(ALTAR_HOLO_KEY, PersistentDataType.STRING, key);
            stands.add(stand);
        }
        altarHolograms.put(key, stands);

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

    public void removeDisplay(String key) {
        Item item = altarItems.remove(key);
        if (item != null && !item.isDead()) item.remove();

        List<ArmorStand> stands = altarHolograms.remove(key);
        if (stands != null) stands.forEach(s -> { if (!s.isDead()) s.remove(); });

        BukkitTask task = altarTasks.remove(key);
        if (task != null) task.cancel();
    }

    // Clean up orphaned holograms on startup
    public void cleanupOrphanedHolograms(World world) {
        for (Entity e : world.getEntities()) {
            if (!(e instanceof ArmorStand as)) continue;
            if (as.getPersistentDataContainer().has(ALTAR_HOLO_KEY, PersistentDataType.STRING)) {
                as.remove();
            }
        }
    }

    private String locKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location keyToLoc(String key, World fallback) {
        try {
            String[] p = key.split(",");
            World w = Bukkit.getWorld(p[0]);
            if (w == null) w = fallback;
            return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (Exception e) {
            return fallback.getSpawnLocation();
        }
    }

    private boolean hasIngredients(Player player) {
        int netherite = 0, bloodDrop = 0, mace = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() == Material.NETHERITE_INGOT && !BloodItems.is(item, BloodItems.BLOOD_MACE_KEY))
                netherite += item.getAmount();
            if (BloodItems.is(item, BloodItems.BLOOD_DROP_KEY)) bloodDrop += item.getAmount();
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
