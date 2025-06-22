package com.effects.effectsonjoin;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EffectsOnJoin extends JavaPlugin implements Listener {

    private final Map<String, Boolean> worldIsNight = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("effectsreload") != null) {
            getCommand("effectsreload").setTabCompleter(new EffectsReloadTabCompleter());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                checkWorldTimesAndUpdateEffects();
            }
        }.runTaskTimer(this, 0L, 100L);

        getLogger().info("The EffectsOnJoin plugin has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("The EffectsOnJoin plugin has been disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyPotionEffectsFromConfig(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            applyPotionEffectsFromConfig(event.getPlayer());
        }, 1L);
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        applyPotionEffectsFromConfig(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        GameMode newGameMode = event.getNewGameMode();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            applyPotionEffectsFromConfig(player, newGameMode);
        }, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("effectsreload")) {
            if (!sender.hasPermission("effectsonjoin.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            try {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "The EffectsOnJoin config has been reloaded!");

                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyPotionEffectsFromConfig(player);
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error while reloading config. Check console for details.");
                getLogger().severe("Config reload failed: " + e.getMessage());
            }

            return true;
        }
        return false;
    }

    private void checkWorldTimesAndUpdateEffects() {
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;

            long time = world.getTime();
            boolean isNight = time >= 12000 && time <= 23999;

            Boolean lastIsNight = worldIsNight.get(world.getName());
            if (lastIsNight == null || lastIsNight != isNight) {
                worldIsNight.put(world.getName(), isNight);
                for (Player player : world.getPlayers()) {
                    applyPotionEffectsFromConfig(player);
                }
            }
        }
    }

    private void applyPotionEffectsFromConfig(Player player) {
        applyPotionEffectsFromConfig(player, player.getGameMode());
    }

    private void applyPotionEffectsFromConfig(Player player, GameMode forcedGamemode) {
        if (!player.hasPermission("effectsonjoin.apply")) return;

        ConfigurationSection effectsSection = getConfig().getConfigurationSection("effects");
        if (effectsSection == null) return;

        String worldName = player.getWorld().getName();

        long time = player.getWorld().getTime();
        boolean isNight = time >= 12000 && time <= 23999;
        boolean isFirstJoin = !player.hasPlayedBefore();

        // Global effects
        ConfigurationSection global = effectsSection.getConfigurationSection("global");
        if (global != null) {
            applyEffectsFromSection(player, global, isNight, isFirstJoin, forcedGamemode);
        }

        // World-specific effects
        ConfigurationSection worldSection = effectsSection.getConfigurationSection(worldName);
        if (worldSection != null) {
            applyEffectsFromSection(player, worldSection, isNight, isFirstJoin, forcedGamemode);
        }
    }

    private void applyEffectsFromSection(Player player, ConfigurationSection section, boolean isNight, boolean isFirstJoin, GameMode mode) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection effect = section.getConfigurationSection(key);
            if (effect == null) continue;

            String typeStr = effect.getString("type", "").toUpperCase();
            PotionEffectType type = PotionEffectType.getByName(typeStr);
            if (type == null) {
                getLogger().warning("Invalid effect type: " + typeStr);
                continue;
            }

            if (!effect.getBoolean("enabled", true)) {
                player.removePotionEffect(type);
                continue;
            }

            if (effect.getBoolean("first_join_only", false) && !isFirstJoin) continue;

            String timeCondition = effect.getString("only_at_time", "ANY").toUpperCase();
            if ((timeCondition.equals("NIGHT") && !isNight) || (timeCondition.equals("DAY") && isNight)) continue;

            String gamemodeCondition = effect.getString("gamemode", "ALL").toUpperCase();
            if (!gamemodeCondition.equals("ALL")) {
                // Check if player's gamemode is listed (comma-separated)
                String[] allowedModes = gamemodeCondition.split(",");
                boolean allowed = false;
                for (String allowedMode : allowedModes) {
                    if (allowedMode.trim().equalsIgnoreCase(mode.name())) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) continue;
            }

            int amplifier = effect.getInt("amplifier", 0);
            int duration = effect.getInt("duration", 999999);
            boolean ambient = effect.getBoolean("ambient", false);
            boolean particles = effect.getBoolean("particles", true);

            player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
        }
    }
}
