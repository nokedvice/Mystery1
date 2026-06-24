package hu.bloodplugin.commands;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.listeners.BloodAltarListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AltarSpawnCommand implements CommandExecutor {

    private final BloodPlugin plugin;
    private final BloodAltarListener altarListener;

    public AltarSpawnCommand(BloodPlugin plugin, BloodAltarListener altarListener) {
        this.plugin        = plugin;
        this.altarListener = altarListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bloodplugin.altarspawn")) {
            sender.sendMessage(Component.text("Nincs jogosultságod!", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Csak játékos használhatja!", NamedTextColor.RED));
            return true;
        }

        altarListener.spawnAltar(player.getLocation().getBlock().getLocation());
        player.sendMessage(Component.text("✦ Blood Altar lerakva!", NamedTextColor.DARK_RED));
        return true;
    }
}
