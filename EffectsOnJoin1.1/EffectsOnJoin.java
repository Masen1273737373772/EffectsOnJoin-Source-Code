package com.effects.effectsonjoin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectsOnJoin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("effectsreload").setTabCompleter(new EffectsReloadTabCompleter());
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

    private void applyPotionEffectsFromConfig(Player player) {
        if (!player.hasPermission("effectsonjoin.apply")) {
            return;
        }
        ConfigurationSection effectsSection = getConfig().getConfigurationSection("effects");
        if (effectsSection == null) return;

        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effect = effectsSection.getConfigurationSection(key);
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

            int amplifier = effect.getInt("amplifier", 0);
            int duration = effect.getInt("duration", 999999);
            boolean ambient = effect.getBoolean("ambient", false);
            boolean particles = effect.getBoolean("particles", true);

            player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
        }
    }
}
