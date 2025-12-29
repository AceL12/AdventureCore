package com.endernerds.AdventureCore;

import com.endernerds.AdventureCore.commands.AdventureCoreCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
   public void onEnable() {
      this.getCommand("adventurecore").setExecutor(new AdventureCoreCommand());
   }

   public void onDisable() {
   }
}
