package hu.bloodplugin.commands;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.listeners.BloodAltarListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SetLegendaryCommand implements CommandExecutor, TabCompleter {

    private final BloodPlugin plugin;
    private final BloodAltarListener altarListener;

    public SetLegendaryCommand(BloodPlugin plugin, BloodAltarListener altarListener) {
        this.plugin        = plugin;
        this.altarListener = altarListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bloodplugin.legendary")) {
            sender.sendMessage(Component.text("Nincs jogosultságod!", NamedTextColor.RED));
            return true;
        }

        // /setlegendary status <name> <true|false>
        if (args.length < 3 || !args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(Component.text(
                "Használat: /setlegendary status <blood_mace> <true|false>", NamedTextColor.RED));
            return true;
        }

        String name   = args[1].toLowerCase();
        String value  = args[2].toLowerCase();

        if (!value.equals("true") && !value.equals("false")) {
            sender.sendMessage(Component.text("Az érték csak true vagy false lehet!", NamedTextColor.RED));
            return true;
        }

        boolean crafted = value.equals("true");

        switch (name) {
            case "blood_mace" -> {
                if (crafted) {
                    altarListener.setMaceCrafted(true);
                    sender.sendMessage(Component.text(
                        "✦ Blood Mace státusz: §4CRAFTOLVA §r(true) – az altar nem használható!", NamedTextColor.GOLD));
                } else {
                    altarListener.setMaceCrafted(false);
                    sender.sendMessage(Component.text(
                        "✦ Blood Mace státusz: §aELÉRHETŐ §r(false) – az altar újra craftolható!", NamedTextColor.GOLD));
                }
            }
            default -> sender.sendMessage(Component.text(
                "Ismeretlen legendary: " + name + ". Jelenleg csak: blood_mace", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("status");
        if (args.length == 2) return List.of("blood_mace");
        if (args.length == 3) return List.of("true", "false");
        return List.of();
    }
}
