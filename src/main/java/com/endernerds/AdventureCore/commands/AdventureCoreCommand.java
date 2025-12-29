package com.endernerds.AdventureCore.commands;

import com.endernerds.AdventureCore.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdventureCoreCommand implements CommandExecutor {
    private final Main plugin;

    public AdventureCoreCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "AdventureCore configuration reloaded.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("verbose")) {
            boolean verbose = plugin.toggleVerboseLogging();
            sender.sendMessage(
                ChatColor.AQUA
                    + "[AdventureCore] Verbose logging is now "
                    + (verbose ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED")
            );
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /adventurecore reload | /adventurecore verbose");
        return true;
    }
}
