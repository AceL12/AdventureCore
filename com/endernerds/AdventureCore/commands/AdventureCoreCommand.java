package com.endernerds.AdventureCore.commands;

import com.endernerds.AdventureCore.Authorization;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdventureCoreCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
         Authorization.loadServerConfig();
         sender.sendMessage(ChatColor.GREEN + "AdventureCore configuration reloaded.");
         return true;
      } else if (args.length == 1 && args[0].equalsIgnoreCase("verbose")) {
         Authorization.toggleVerboseLogging();
         sender.sendMessage(
            ChatColor.AQUA
               + "[AdventureCore] Verbose logging is now "
               + (Authorization.isVerboseLogging() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED")
         );
         return true;
      } else {
         sender.sendMessage(ChatColor.YELLOW + "Usage: /adventurecore reload | /adventurecore verbose");
         return true;
      }
   }
}
