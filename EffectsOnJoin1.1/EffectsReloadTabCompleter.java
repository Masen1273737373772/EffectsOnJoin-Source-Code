package com.effects.effectsonjoin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class EffectsReloadTabCompleter implements TabCompleter{
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    return Collections.emptyList();
    }
}