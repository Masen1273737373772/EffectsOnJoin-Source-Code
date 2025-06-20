package com.effects.effectsonjoin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class EffectsOnJoin extends JavaPlugin implements Listener {

    // Track last known day/night state per world
    private Map<String, Boolean> worldIsNight = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("effectsreload").setTabCompleter(new EffectsReloadTabCompleter());

        // Schedule repeating task to check world time changes every 5 seconds (100 ticks)
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("effectsreload")) {
            if (!sender.hasPermission("effectsonjoin.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "The EffectsOnJoin config has been reloaded!");

            for (Player player : Bukkit.getOnlinePlayers()) {
                applyPotionEffectsFromConfig(player);
            }
            return true;
        }
        return false;
    }

    // Scheduled method to check day/night changes and update effects accordingly
    private void checkWorldTimesAndUpdateEffects() {
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;

            long time = world.getTime();
            boolean isNight = time >= 12000 && time <= 23999;

            Boolean lastIsNight = worldIsNight.get(world.getName());
            if (lastIsNight == null || lastIsNight != isNight) {
                // Time phase changed in this world, update all players in it
                worldIsNight.put(world.getName(), isNight);
                for (Player player : world.getPlayers()) {
                    applyPotionEffectsFromConfig(player);
                }
            }
        }
    }

    private void applyPotionEffectsFromConfig(Player player) {
        if (!player.hasPermission("effectsonjoin.apply")) {
            return;
        }

        ConfigurationSection effectsSection = getConfig().getConfigurationSection("effects");
        if (effectsSection == null) return;

        String playerWorld = player.getWorld().getName();

        // Prepare conditions
        long time = player.getWorld().getTime();
        boolean isNight = time >= 12000 && time <= 23999;
        boolean isFirstJoin = !player.hasPlayedBefore();

        // Apply global effects first
        ConfigurationSection globalSection = effectsSection.getConfigurationSection("global");
        if (globalSection != null) {
            applyEffectsFromSection(player, globalSection, isNight, isFirstJoin);
        }

        // Apply world-specific effects
        ConfigurationSection worldSection = effectsSection.getConfigurationSection(playerWorld);
        if (worldSection != null) {
            applyEffectsFromSection(player, worldSection, isNight, isFirstJoin);
        }
    }

    private void applyEffectsFromSection(Player player, ConfigurationSection section, boolean isNight, boolean isFirstJoin) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection effect = section.getConfigurationSection(key);
            if (effect == null) continue;

            String typeStr = effect.getString("type", "").toUpperCase();
            PotionEffectType type = PotionEffectType.getByName(typeStr);
            if (type == null) {
                getLogger().warning("Invalid effect type: " + typeStr);
                continue;
            }

            boolean enabled = effect.getBoolean("enabled", true);
            if (!enabled) {
                if (player.hasPotionEffect(type)) {
                    player.removePotionEffect(type);
                }
                continue;
            }

            if (effect.getBoolean("first_join_only", false) && !isFirstJoin) {
                continue;
            }

            String timeCondition = effect.getString("only_at_time", "ANY").toUpperCase();
            if (timeCondition.equals("NIGHT") && !isNight) {
                continue;
            } else if (timeCondition.equals("DAY") && isNight) {
                continue;
            }

            int amplifier = effect.getInt("amplifier", 0);
            int duration = effect.getInt("duration", 999999);
            boolean ambient = effect.getBoolean("ambient", false);
            boolean particles = effect.getBoolean("particles", true);

            player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
        }
    }
}
