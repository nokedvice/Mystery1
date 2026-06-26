package hu.bloodplugin.commands;

import hu.bloodplugin.BloodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class AltarCleanupCommand implements CommandExecutor {

    private final BloodPlugin plugin;
    private final NamespacedKey ALTAR_KEY;
    private final NamespacedKey ALTAR_HOLO_KEY;
    private final NamespacedKey ALTAR_LOC_KEY;

    public AltarCleanupCommand(BloodPlugin plugin) {
        this.plugin         = plugin;
        this.ALTAR_KEY      = new NamespacedKey(plugin, "blood_altar");
        this.ALTAR_HOLO_KEY = new NamespacedKey(plugin, "blood_altar_holo");
        this.ALTAR_LOC_KEY  = new NamespacedKey(plugin, "blood_altar_loc");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bloodplugin.altarspawn")) {
            sender.sendMessage(Component.text("Nincs jogosultságod!", NamedTextColor.RED));
            return true;
        }

        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                // Remove hologram stands
                if (e instanceof ArmorStand as) {
                    if (as.getPersistentDataContainer().has(ALTAR_HOLO_KEY, PersistentDataType.STRING)
                     || as.getPersistentDataContainer().has(ALTAR_KEY, PersistentDataType.BYTE)
                     || as.getPersistentDataContainer().has(ALTAR_LOC_KEY, PersistentDataType.STRING)) {
                        as.remove();
                        removed++;
                    }
                }
                // Remove floating items
                if (e instanceof Item item) {
                    if (item.getPersistentDataContainer().has(ALTAR_KEY, PersistentDataType.BYTE)) {
                        item.remove();
                        removed++;
                    }
                }
            }
        }

        sender.sendMessage(Component.text(
            "✦ " + removed + " altar entity törölve!", NamedTextColor.GOLD));
        return true;
    }
}
