🔮 EffectsOnJoin
EffectsOnJoin is a lightweight, highly customizable plugin that automatically applies potion effects to players when they join, respawn, switch worlds, or when the world transitions between day and night.

Perfect for RPG servers, lobby effects, gamemode-specific challenges, or immersive world environments.

✨ Features

Automatically apply potion effects when players:

Join the server

Respawn after dying

Switch to a different world

Experience a time change (Day ↔ Night)

World-Specific and Global Config Support
Configure potion effects per world or use global settings for all players.

Gamemode-Specific Conditions
Apply effects only to players in specific gamemodes: Survival, Creative, Adventure, Spectator, or All.

First-Join Effects
Trigger effects only the first time a player joins the server.

Time-Based Triggers
Choose whether effects apply during the day, night, or all the time.

Live Configuration Reload
Use /effectsreload to reload the config without restarting the server.

Effect Removal
Disabled effects are automatically removed from players when specified in the config.

Compatible With:

Spigot

Paper

Purpur

Any Bukkit-based fork

🔐 Permissions

effectsonjoin.apply – Allows the player to receive effects (Default: ✅ Enabled)

effectsonjoin.reload – Allows the use of /effectsreload (Default: ❌ OP only)

📘 Configuration Notes

Potion effects are applied based on:

World

Gamemode

Time of day (DAY/NIGHT/ANY)

First-time join status

Effects can be fully customized, disabled, or removed dynamically.

Example setting:

Give players Night Vision only at night and only in Survival mode

Apply Speed in all worlds and all gamemodes

