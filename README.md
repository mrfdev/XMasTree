# 1MB X-Mas Tree

Upgradeable Christmas trees for Paper servers. Players plant a magic spruce sapling, feed it resources, grow it through several tree levels, and collect presents that appear under the finished tree.

This fork keeps the old X-Mas event data usable for winter 2026 while moving the plugin forward for modern Paper and Java runtimes.

![X-Mas tree preview](http://puu.sh/dKlK1/85c3dad454.jpg)

## Current target

The Gradle build creates the current Paper 26.1.2 target jar in `build/libs`:

| Jar | Purpose |
| --- | --- |
| `1MB-XMas-2026-v2.0.1-023-v25-26.1.2.jar` | Modern Paper 26.1.2 build, Java 25 bytecode. |

The checked-in source compiles against the centralized Paper 26.1.2 cache and declares a plugin compatibility floor of `api-version: 1.21.11` so the same jar can be tested on Paper 1.21.11 and Paper 26.1.2.

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
- `/xmastree debug` sections for `status`, `commands`, `permissions`, `placeholders`, and `config`, plus live global boolean toggles.
- Primary `/xmastree` command with an optional legacy `/xmas` alias.
- Optional PlaceholderAPI placeholders for CMI holograms, ajLeaderboards, scoreboards, and menus.
- Legacy `trees.yml` world-name alias support for renamed destination worlds.

## v2 changelog

- modernize the plugin from the legacy deployed build to an actively maintained Paper 26.1.2 / Java 25 Gradle build
- simplify the build around the active Paper 26.1.2 target and remove the retired 1.21.11 local server dependency
- keep build output clean and predictable in `build/libs` with the 2026 versioned jar naming

- keep legacy tree data compatible by continuing to read `plugins/X-Mas/trees.yml`
- add world alias migration support so old saved trees can survive renamed worlds
- preserve old event data while modernizing the runtime and admin tooling

- make `/xmastree` the primary command
- keep `/xmas` as an optional legacy alias controlled by config
- fix legacy alias reload and unregister behavior so reload no longer crashes when alias settings change
- improve `/xmastree help` output and keep it aligned with the actual command surface

- add granular permissions under `onembxmastree.*`
- replace the old `xmas.admin` permission with `onembxmastree.admin`
- add separate permissions for `status`, `help`, `give`, `gifts`, `addhand`, `reload`, `debug`, `debug.toggle`, `end`, and `tree.override`

- add a modern debug system with named categories: `status`, `commands`, `permissions`, `placeholders`, and `config`
- keep numeric debug pages working as a legacy shortcut
- improve debug output formatting with clearer key/value coloring
- make invalid debug page or section requests return a helpful response instead of silently falling back
- add `/xmastree debug toggle <key> true|false` for live boolean config changes
- keep tab completion focused on named debug categories instead of numeric page suggestions

- add optional PlaceholderAPI support with the `onembxmastree` namespace
- add placeholders for event state, end time, end countdown, auto-end, resource-back, particles, luck, tree totals, owner totals, player tree count, and plugin version
- document placeholders in the README and show them in debug output

- modernize message handling with MiniMessage support while keeping legacy color compatibility
- improve player-facing text, prefixes, debug output, and help text
- change the visible plugin and chat identity toward `XMas Tree` for clearer user-facing output
- make the Christmas Crystal display name non-italic

- fix `resource-back` so destroying a tree returns only the materials actually spent on that tree
- fix the old refund dupe issue where the plugin could return more than the player had used
- improve refund delivery with fallback order: chest, barrel, player inventory, then floor drops

- reduce the loud grow and ingredient sound behavior
- make first-hit and repeat-hit grow sound volumes configurable and reloadable
- support silent, quiet, and loud tuning through config without server restarts

- modernize material and item handling for current Paper names
- use safer material matching and validation to avoid legacy enum failures
- improve displayed item names so materials such as Redstone Dust render properly in requirement output
- add configurable per-stage particle effects using modern Paper particle names

- harden config and item parsing by restricting present texture URLs to `textures.minecraft.net`
- cap Base64 gift payload handling
- skip invalid or legacy material names safely

- update config comments and improve documentation for installation, building, commands, permissions, placeholders, support, and credits
- point support to the GitHub issues page
- refresh `.gitignore` for local dev/test folders and obvious OS/build junk

## Installation

1. Stop the Paper server.
2. Back up the existing `plugins/X-Mas` folder and the world folders.
3. Remove the old X-Mas jar from `plugins` so Paper does not load two copies of the same plugin.
4. Copy the jar for your server target into the server `plugins` folder.
5. Start the server with Java 25.
6. Check the console for XMas Tree startup messages, then run `/xmastree` in game or console.

For the 2026 target, use the modern Paper 26.1.2 jar:

- Paper 26.1.2: `1MB-XMas-2026-v2.0.1-023-v25-26.1.2.jar`

## Building

Requirements:

- JDK 25
- Gradle
- Centralized Paper server cache at `/Users/floris/Projects/Codex/servers/cache/Paper-26.1.2`
- Centralized PlaceholderAPI jar at `/Users/floris/Projects/Codex/servers/cache/Paper-26.1.2/plugins/PlaceholderAPI-2.12.3-DEV-265.jar`

This repo no longer uses a local `servers/` folder for compilation or testing. If a local `servers/` folder still exists here, it is ignored and treated as retired local data.

Build the current Paper 26.1.2 jar:

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

`buildAllJars` now:

- compiles against centralized Paper API `26.1.2.build.20-alpha`
- keeps the Java release target at `25`
- writes the standard jar to `build/libs/`
- copies the same jar into `libs/` for the centralized test runner
- prints the active build config, compile target, and declared plugin API floor

You can also inspect the build metadata directly with:

```bash
gradle printBuildConfig
```

End users do not need any `servers/` folder. The installable jars are written to `build/libs/`, and this project also keeps a local copy in `libs/` for shared test-runner use.

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
| `/xmastree debug` | Opens the `status` debug section by default. |
| `/xmastree debug [section\|page]` | Shows debug output for `status`, `commands`, `permissions`, `placeholders`, or `config`. Numeric pages `1-5` still work as a legacy shortcut. |
| `/xmastree debug toggle <key> true\|false` | Toggles supported global boolean config keys and reloads the plugin config. |
| `/xmastree end` | Ends the event and sets `core.plugin-enabled` to `false`. |

### Debug sections

The preferred debug syntax is category-based:

| Section | Example | Purpose |
| --- | --- | --- |
| `status` | `/xmastree debug status` | Event state, end date, auto-end, refund state, particles, loaded tree count, and owner count. |
| `commands` | `/xmastree debug commands` | Command list, debug syntax, and legacy alias state. |
| `permissions` | `/xmastree debug permissions` | All registered `onembxmastree.*` permissions and what they allow. |
| `placeholders` | `/xmastree debug placeholders` | All built-in PlaceholderAPI placeholders plus their descriptions. |
| `config` | `/xmastree debug config` | The current values of the toggleable global config keys. |

Numeric compatibility remains available for existing habits and old screenshots:

| Page | Section |
| --- | --- |
| `1` | `status` |
| `2` | `commands` |
| `3` | `permissions` |
| `4` | `placeholders` |
| `5` | `config` |

### Debug toggle keys

`/xmastree debug toggle <key> true|false` currently supports:

- `core.commands.legacy-command-enabled`
- `core.plugin-enabled`
- `core.holiday-ends.enabled`
- `core.holiday-ends.resource-back`
- `core.particles-enabled`
- `xmas.luck.enabled`

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `onembxmastree.admin` | `op` | Umbrella permission for all XMas Tree commands and override actions. |
| `onembxmastree.command.status` | `true` | Allows viewing `/xmastree` status output. |
| `onembxmastree.command.help` | `true` | Allows viewing `/xmastree help`. |
| `onembxmastree.command.give` | `op` | Allows `/xmastree give <player>`. |
| `onembxmastree.command.gifts` | `op` | Allows `/xmastree gifts`. |
| `onembxmastree.command.addhand` | `op` | Allows `/xmastree addhand`. |
| `onembxmastree.command.reload` | `op` | Allows `/xmastree reload`. |
| `onembxmastree.command.debug` | `op` | Allows `/xmastree debug [section\|page]`. |
| `onembxmastree.command.debug.toggle` | `op` | Allows `/xmastree debug toggle <key> true\|false`. |
| `onembxmastree.command.end` | `op` | Allows `/xmastree end`. |
| `onembxmastree.tree.override` | `op` | Allows managing other players' trees. |

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
| `%onembxmastree_version%` | `2.0.1-023` | Loaded plugin version. |

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
- The Paper 26.1.2 jar is the intended winter 2026 target, and the same jar now declares `api-version: 1.21.11` so it can be smoke-tested on both Paper 1.21.11 and Paper 26.1.2.

## Security notes

- Admin and staff access is gated by `onembxmastree.*` permissions.
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
- **mrfloris** - 2026 Paper modernization, Java 25 builds, and XMasTree maintenance - [mrfloris](https://github.com/mrfloris)

Original SpigotMC listing:

[spigotmc.org/resources/x-mas-upgradeable-christmas-tree-event.2672](https://www.spigotmc.org/resources/x-mas-upgradeable-christmas-tree-event.2672/)
