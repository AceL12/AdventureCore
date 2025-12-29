package com.endernerds.AdventureCore;

import com.endernerds.AdventureCore.commands.AdventureCoreCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private boolean verboseLogging;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadFlags();
        getCommand("adventurecore").setExecutor(new AdventureCoreCommand(this));
        getLogger().info("AdventureCore v" + getDescription().getVersion() + " loaded without authentication checks.");
    }

    @Override
    public void onDisable() {
        // Nothing to clean up
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadFlags();
    }

    public boolean toggleVerboseLogging() {
        verboseLogging = !verboseLogging;
        getConfig().set("verbose-logging", verboseLogging);
        saveConfig();
        return verboseLogging;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    private void loadFlags() {
        verboseLogging = getConfig().getBoolean("verbose-logging", false);
    }
}
