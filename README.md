# 1MB X-Mas Tree

Upgradeable Christmas trees for Paper servers. Players plant a magic spruce sapling, feed it resources, grow it through several tree levels, and collect presents that appear under the finished tree.

This fork keeps the old X-Mas event data usable for winter 2026 while moving the plugin forward for modern Paper and Java runtimes.

![X-Mas tree preview](http://puu.sh/dKlK1/85c3dad454.jpg)

## Current targets

The Gradle build creates the legacy reference jar and the current Paper 26.1.2 target jar in `build/libs`:

| Jar | Purpose |
| --- | --- |
| `1MB-XMas-2026-v2.0.0-004-v21-1.21.8.jar` | Legacy reference jar copied from the deployed 2025 server jar. |
| `1MB-XMas-2026-v2.0.1-010-v25-26.1.2.jar` | Modern Paper 26.1.2 build, Java 25 bytecode. |

The checked-in source targets Paper 26.1.2. The legacy jar is preserved so the deployed working 2025 behavior can be compared or rolled back during testing.

## Features

- Magic Christmas Crystal item for planting event trees.
- Upgradeable spruce tree levels with configurable material requirements.
- Random present spawning under grown trees.
- Configurable present head skins using Mojang texture URLs.
- Configurable gift table using modern material names or saved exact items.
- MiniMessage support for locale strings, crystal display text, and plugin messages.
- Existing `plugins/X-Mas/trees.yml` data remains the event data source.
- Optional resource refunds when a tree is destroyed or cleaned up after the event.
- Configurable per-stage particles using Paper 26.1.2 particle names.
- `/xmastree debug` pages for status, commands, permissions, placeholders, and global boolean toggles.
- Primary `/xmastree` command with an optional legacy `/xmas` alias.
- Optional PlaceholderAPI placeholders for CMI holograms, ajLeaderboards, scoreboards, and menus.
- Legacy `trees.yml` world-name alias support for renamed destination worlds.

## Installation

1. Stop the Paper server.
2. Back up the existing `plugins/X-Mas` folder and the world folders.
3. Remove the old X-Mas jar from `plugins` so Paper does not load two copies of the same plugin.
4. Copy the jar for your server target into the server `plugins` folder.
5. Start the server with Java 25.
6. Check the console for XMas Tree startup messages, then run `/xmastree` in game or console.

For the 2026 target, use the modern Paper 26.1.2 jar:

- Paper 26.1.2: `1MB-XMas-2026-v2.0.1-010-v25-26.1.2.jar`

## Building

Requirements:

- JDK 25
- Gradle
- The local Paper server folder in `servers/Server-Two-Paper-26.1.2`
- The local PlaceholderAPI jar in `servers/Server-Two-Paper-26.1.2/plugins`
- The deployed legacy jar in `servers/Server-One-Paper-1.21.11/plugins` if you want `legacyJar`

Build the current Paper 26.1.2 jar and the legacy reference jar:

```bash
gradle clean buildAllJars
```

Build only the Paper 26.1.2 jar:

```bash
gradle jar
```

The `paper2612Jar` task is kept as an alias:

```bash
gradle paper2612Jar
```

Copy the deployed legacy jar into the requested legacy filename:

```bash
gradle legacyJar
```

The build compiles against the Paper 26.1.2 API jars found in `servers/Server-Two-Paper-26.1.2`. If that folder is missing or has not been started far enough for Paper to download its libraries, Gradle will not have the Paper API classpath it needs.

## Commands

The primary command is `/xmastree`.

If `core.commands.legacy-command-enabled` is `true`, the legacy `/xmas` alias is also registered.

| Command | Description |
| --- | --- |
| `/xmastree` | Shows plugin version, event status, auto-end status, resource-back status, tree count, and owner count. |
| `/xmastree help` | Shows the command list. |
| `/xmastree give <player>` | Gives an online player a Christmas Crystal. |
| `/xmastree gifts` | Spawns a small batch of presents under every loaded Christmas tree. |
| `/xmastree addhand` | Adds the item in your main hand to the gift list and saves it to `config.yml`. |
| `/xmastree reload` | Reloads config, locale, present heads, gifts, luck settings, command alias settings, and tree level requirements. |
| `/xmastree debug [page]` | Shows paginated status, commands, permissions, placeholders, and toggleable global config keys. |
| `/xmastree debug toggle <key> true\|false` | Toggles supported global boolean config keys and reloads the plugin config. |
| `/xmastree end` | Ends the event and sets `core.plugin-enabled` to `false`. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `xmas.admin` | `op` | Allows use of the `/xmastree` command and all XMas Tree admin subcommands. |

## Player flow

Players can receive a Christmas Crystal from an admin, or craft one with diamonds around an emerald in a cross shape. Right-click a spruce sapling with the crystal to create a magic tree.

After planting, players right-click the tree with the configured level-up materials. The requirement header is short in chat and includes a hover hint that explains the ingredients must be fed into the tree by right-clicking while holding them. When all requirements for a level are complete, right-click the tree again to grow it. Presents spawn around grown trees while the event is enabled.

If `core.holiday-ends.resource-back` is enabled, confirmed tree destruction returns the upgrade materials that were actually spent on the tree. The plugin tries to place a chest first, then a barrel, then the player's inventory, and finally drops any overflow at the tree location.

Ingredient accept sounds can be tuned live in `config.yml` under `core.sounds.grow`. Use `0.0` for silent, `0.1` for quiet, `0.5` for half volume, and `1.0` for full volume. `/xmastree reload` applies the new values without a server restart.

## Configuration

The plugin writes its files to `plugins/X-Mas`:

- `config.yml` controls event timing, locale, tree limits, gift cooldowns, present skins, gift items, and level-up requirements.
- `trees.yml` stores placed tree data and should be kept when upgrading an existing event.
- `locales/*.yml` controls player-facing messages and crystal display text.

Use modern Paper/Bukkit material names such as `GOLD_INGOT`, `SPRUCE_LOG`, and `PLAYER_HEAD`. Legacy numeric IDs and old material names are skipped to avoid modern Paper exceptions.

Gift entries in `xmas.gifts` can be simple material names:

```yaml
- DIAMOND
- EMERALD:3
```

Admins can also hold an item and run `/xmastree addhand`. This saves the exact item as Base64 so custom names, lore, enchantments, and metadata can be used as gifts.

Legacy world-name remapping lives under `migration.world-aliases`. This is useful when an old `trees.yml` was saved in worlds like `general`, `wild`, or `santa`, but the new Paper 26.1.2 server uses different world names:

```yaml
migration:
 world-aliases:
  general: world
  wild: world
  santa: santa_event
```

The saved coordinates are preserved. If the destination world terrain or world border changed, some legacy tree locations may still need manual cleanup.

Present head entries in `xmas.presents` should use `textures.minecraft.net` URLs. Old player-name entries are still accepted for compatibility.

Per-stage particles live under `xmas.tree-lvl.<stage>.particles`. Particle names should come from the Paper 26.1.2 `Particle` enum:

[jd.papermc.io/paper/26.1.2/org/bukkit/Particle.html](https://jd.papermc.io/paper/26.1.2/org/bukkit/Particle.html)

The config currently supports simple particles and `DUST`.

## MiniMessage

Locale messages, crystal names, crystal lore, and command messages support MiniMessage:

```yaml
crystal:
 name: <green>Christmas Crystal</green>
 lore:
  - <gold>Concentrated Christmas Spirit</gold>
  - <gray>Use it on a spruce sapling to fill it with magic!</gray>
```

Legacy `&` color codes are still parsed for compatibility when a message does not contain MiniMessage tags.

## Placeholders

PlaceholderAPI is optional. If PlaceholderAPI is installed, X-Mas registers the `onembxmastree` expansion.

PlaceholderAPI requires an underscore after the expansion identifier, so use:

```text
%onembxmastree_event.active%
```

The dotted key after `onembxmastree_` is supported to keep the placeholders readable and namespaced. Underscore variants also work, for example `%onembxmastree_event_active%`.

| Placeholder | Example output | Description |
| --- | --- | --- |
| `%onembxmastree_event.active%` | `true` | Whether the event is currently active. |
| `%onembxmastree_event.active_text%` | `Active` | Human-readable active/inactive state. |
| `%onembxmastree_event.status%` | `In Progress` | Current event status text. |
| `%onembxmastree_event.starts_at%` | `manual` | Start mode. The plugin currently starts from `core.plugin-enabled`, not a scheduled start date. |
| `%onembxmastree_event.ends_at%` | `10-01-2027 03-33-33` | Configured event end date. |
| `%onembxmastree_event.ends_in%` | `263d 7h` | Approximate time until the configured end date, or `disabled` when auto-end is off. |
| `%onembxmastree_event.ends_timestamp%` | `1799552013000` | Event end timestamp in milliseconds. |
| `%onembxmastree_event.auto_end%` | `true` | Whether automatic event ending is enabled. |
| `%onembxmastree_resource.back%` | `true` | Whether resource refunds are enabled. |
| `%onembxmastree_resource.back_text%` | `Yes` | Human-readable refund state. |
| `%onembxmastree_particles.enabled%` | `true` | Whether X-Mas particles are globally enabled. |
| `%onembxmastree_luck.enabled%` | `false` | Whether gift luck chance is enabled. |
| `%onembxmastree_luck.chance%` | `75` | Gift luck chance as a percent. |
| `%onembxmastree_trees.total%` | `14` | Total loaded X-Mas trees. |
| `%onembxmastree_trees.owners%` | `6` | Number of unique loaded tree owners. |
| `%onembxmastree_player.trees%` | `2` | Number of loaded trees owned by the placeholder player. |
| `%onembxmastree_version%` | `2.0.1-010` | Loaded plugin version. |

CMI hologram example:

```text
&aX-Mas Event: &f%onembxmastree_event.active_text%
&aEnds in: &f%onembxmastree_event.ends_in%
&aResource back: &f%onembxmastree_resource.back_text%
&aTrees planted: &f%onembxmastree_trees.total%
```

ajLeaderboards placeholder examples:

```text
%onembxmastree_trees.total%
%onembxmastree_trees.owners%
%onembxmastree_player.trees%
```

## Compatibility notes

- Back up `plugins/X-Mas/trees.yml` before upgrading a live server.
- Existing tree records are loaded from the same `trees.yml` format.
- When saved world names no longer match the current server world names, `migration.world-aliases` can remap them without rewriting `trees.yml`.
- Existing present head player-name entries are still accepted, but new configs should prefer Mojang texture URLs.
- The modern jars are compiled with Java 25 bytecode and should be run on Java 25.
- The Paper 26.1.2 jar is the intended winter 2026 target. Paper 1.21.11 compatibility is no longer part of the active test path.

## Security notes

- Admin commands are gated by `xmas.admin`.
- Present texture URLs are restricted to `textures.minecraft.net`.
- Gift item Base64 entries are capped before deserialization.
- Config material names are resolved with modern `Material.matchMaterial` and invalid or legacy values are skipped.
- Treat `config.yml` and locale files as trusted admin-controlled files, especially when using MiniMessage click or hover tags.

## Support

Please report bugs, compatibility problems, and upgrade questions in the GitHub issues section:

[github.com/mrfdev/XMasTree/issues](https://github.com/mrfdev/XMasTree/issues)

## Credits

- **MelonCode** - Original developer - [MelonCode](https://github.com/MelonCode)
- **Ghost_chu** - NMS fixes - [Ghost-chu](https://github.com/Ghost-chu)
- **LoneDev6** - Optimization patches - [LoneDev6](https://github.com/LoneDev6)
- **montlikadani** - Hungarian translation - [montlikadani](https://github.com/montlikadani)
- **1MB / mrfdev** - 2026 Paper modernization, Java 25 builds, and XMasTree maintenance

Original SpigotMC listing:

[spigotmc.org/resources/x-mas-upgradeable-christmas-tree-event.2672](https://www.spigotmc.org/resources/x-mas-upgradeable-christmas-tree-event.2672/)
