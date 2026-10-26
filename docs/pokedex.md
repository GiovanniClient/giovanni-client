# Mob Pokédex v1

The Pokédex learns representations of SkyBlock mobs from their nearby entities. Each island has its own JSON file in `config/giovanniclient/pokedex/`; only the current TAB `Area` is loaded. `/gio pokedex` and `/gio dex` are aliases.

## Capture a mob

1. Approach a mob within seven blocks, preferably away from other mobs. Aim at its body or model when possible.
2. Run `/gio dex scan` (default radius: seven blocks), or `/gio dex scan 20` for a larger radius. Green `*` rows show the proposed capture; all listed parts are cached as immutable scan-time data.
3. Run `/gio dex add Rat` (replace `Rat` with your chosen name). The command reports the parts saved.
4. Use `/gio glow Rat` or `/gio hide Rat`.

Names may contain spaces: `/gio dex add Shadow Assassin`. Repeating `add` with the same name learns another representation, such as an armor stand model versus an item display model. An exactly identical variant is ignored.

To explicitly add equipment/model variants to an existing mob, approach each variant and run `/gio dex variant Goblin`. To select exact parts, use `/gio dex variant Goblin 123 124` with IDs from `scan`. Quote multiword names, for example `/gio dex variant "Goblin Warrior" 123 124`. The name must already exist in the current island. The original variants are preserved, duplicates are ignored, and the feedback shows the total variant count.

For six different goblin loadouts, register the first as `Goblin`, add the other five with `variant Goblin`, then run `/gio glow Goblin` once. Every recognised variant shares that same name and glow colour. `/gio hide Goblin` also applies to all variants.

For a crowded scene or an unusual assembly, choose the exact IDs shown by `scan`:

```text
/gio dex capture "Shadow Assassin" 123 124 125
```

This captures exactly the listed IDs from the **latest scan**, including anonymous assemblies with no nametag, equipment or distinctive model. Positions, equipment and signatures come from that snapshot, so capture still works after movement, despawn or walking away. No live entities need to remain nearby. IDs are only selectors for that snapshot and are **not** persisted as identifying data. Real players are excluded. Use `remove` and capture again if the original sample included unwanted parts.

The optional scan radius accepts numbers from 1 to 128 blocks, including decimals, and only searches entities already loaded by the client. A larger scan radius does not change the maximum spacing between parts of one captured mob. Each successful scan replaces the previous snapshot, including an empty scan. Changing island, changing world/server or disconnecting invalidates it; it has no time limit while staying in the same world and island. Capture with IDs requires a scan first and never silently falls back to current live entities. `variant <name> <ids...>` uses the same snapshot; `add` and `variant` without IDs continue to capture live nearby mobs within seven blocks.

## Commands

| Command | Action |
| --- | --- |
| `/gio dex` or `/gio dex help` | Show the full local command guide |
| `/gio dex scan [radius]` | Cache and list loaded entity parts in range (default 7, maximum 128 blocks) |
| `/gio dex add <name>` | Save the aimed-at/nearest proposed group in the current island |
| `/gio dex capture "name" <ids...>` | Save exactly the selected parts from the latest cached scan; quote multiword names |
| `/gio dex variant "name" [ids...]` | Add a variant from cached scan IDs, or omit IDs for automatic live selection |
| `/gio dex list` | List the current island's catalog and recognised live counts |
| `/gio dex info <name>` | Inspect a mob's variants and signatures |
| `/gio dex remove <name>` | Remove that mob and all its variants from the current island |
| `/gio dex reload` | Reload the current island's file after editing or repairing it |

The island must be known from TAB before saving. A damaged or unsupported file produces an error and blocks writes until repaired and reloaded; it is not replaced with an empty catalog. Saves use a temporary file followed by a rename.

`capture` requires both a name and at least one entity ID: `/gio dex capture Pig` is incomplete. Use `/gio dex add Pig` for automatic selection, or `/gio dex capture Pig 171357` to select an ID from the latest `scan`. The incomplete command now explains this locally.

## Local commands stay local

`/gio`, `/giovanni`, `/giovanniclient` and `/zoo` are reserved client namespaces. A packet-level guard cancels attempts to send these commands to the server, including unknown subcommands, malformed/incomplete arguments, unavailable client registrations and signed command packets. It also blocks server autocomplete requests for these namespaces and slash commands accidentally sent as ordinary chat. Successful client commands still execute through Fabric normally. The guard remains enabled in safe mode.

If Fabric falls back to the server after a parsing failure, the outgoing packet is discarded and a local error points to `/gio help` and `/gio dex help`. Normal server commands, such as `/warp`, are unaffected.

`/gio` opens the configuration. `/gio help` lists the main command groups. For either `glow` or `hide`, use:

| Command | Action |
| --- | --- |
| `/gio glow <name>` / `/gio hide <name>` | Enable the effect for that name |
| `/gio glow list` / `/gio hide list` | List active names |
| `/gio glow remove <name>` / `/gio hide remove <name>` | Disable the effect for that name |
| `/gio glow clear` / `/gio hide clear` | Clear all names for that effect (`off` is an alias) |

Removing a rendering effect does not delete the Pokédex entry. `dex remove` deletes the entry instead. Hide names are persistent. Glow lists are persistent and separate for each island's TAB `Area`, with up to 10 names and distinct colours per island. Names and colours are saved immediately in `config/giovanniclient/config.json` and restored after changing islands, reconnecting or restarting the client. `glow list` shows the current island and its count; `remove`, `clear` and `off` only affect that island. Wait for TAB to identify the island before changing its glow list.

## Recognition and rendering

The shared resolver matches all parts of a recorded representation using entity type, cleaned name, equipment/head texture, item model/custom model data, dye colour, display item or block state, and relative horizontal/vertical offsets. Level and health text are stripped; head textures use their texture URL hash rather than profile UUIDs or timestamps. Actual entity UUIDs and IDs are used only in the temporary live index.

The resolver runs every five client ticks. More complete variants take precedence, each entity can belong to only one assembly, and matched parts stay bound to that anchor's lifetime. A lingering anchor cannot transfer an effect to a replacement body after death. Rejoining, changing island, or explicitly reloading resets live associations.

For an exact Pokédex alias in the current island, hide/glow use the shared assembly and skip their old proximity search for that alias. Other names retain the existing substring-based behaviour. Hide keeps the existing translucent-body behaviour and removes visual accessories and labels; glow outlines the body and visual parts, excluding text labels. Names from the active catalog appear in hide/glow suggestions.

## v1 limits

- The catalog starts empty; no complete external mob-composition database was found. Name parsing, texture identity and grouped entities follow the findings from the SkyHanni/Skyblocker research, with no runtime dependency on those mods or their servers.
- Automatic grouping is a proposal based on proximity and passenger relationships. Check `scan` and use explicit `capture` when mobs overlap or have large/complex models.
- Every recorded part must be present. Different equipment, model states or nametags may need another learned variant. Part offsets allow 0.65 blocks of horizontal error and 0.9 vertically; horizontal distance allows rotation around the anchor.
- Nametags and distinctive models are optional for explicit `capture`: all selected parts are saved, using their types, available characteristics and relative offsets. A single anonymous entity matches all identical entities in the current island. Automatic grouping remains conservative; use explicit IDs for anonymous assemblies.
- Supported parts include living mobs, fake players, armor stands, item/block/text display entities and falling-block entities. Placed world blocks are outside this version. Fake players using real-player-style UUIDs are excluded conservatively.
- In-game validation is needed for each captured mob, particularly animated models and overlapping groups.

## Verification

Build with `./gradlew build`. Verify captures and rendering in game, including persistence after restarting and switching islands.
