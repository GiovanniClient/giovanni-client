# Mob Pokédex

The Pokédex learns how to recognise SkyBlock mobs from nearby entities. `/gio pokedex` and `/gio dex` are aliases. The current area comes from Player Locator: a Catacombs scoreboard location uses one catalog for all dungeon floors, even when TAB has no `Area`; otherwise the TAB area selects the island catalog. On recognised SkyBlock islands outside Catacombs, the shared Fishing, Jerry and Spooky catalogs are active alongside the island catalog.

Each catalog can have bundled definitions and personal captures. Bundled JSON files end in `-jar.json`, live in `src/client/resources/assets/giovanniclient/pokedex/`, and ship inside the mod. Personal files end in `-user.json` and live in `config/giovanniclient/pokedex/`. For example, The End uses `the_end-jar.json` and `the_end-user.json`. The client combines the active catalogs in memory, keeping one copy of an identical profile under the same normalised name while retaining different variants. Updating the mod can add or refresh bundled definitions without rewriting personal captures. No network connection is used to load the bundled catalog.

To update the bundled catalog, use `/gio dex list` in the target island or Catacombs, or the relevant shared command (`/gio dex fishing list`, `/gio dex jerry list`, or `/gio dex spooky list`), to find the personal JSON file. Review the captures and add the definitions you want to ship to the matching `-jar.json` resource, preserving the catalog ID. Keep existing bundled definitions you still want to ship: the personal file only contains personal captures. For special catalogs, the `island` field is one of `scope:catacombs`, `scope:fishing`, `scope:jerry`, or `scope:spooky`.

For development, open `/gio` → **Debug** → **Dex Dev**. Turning it on imports every catalog in the jar into the matching `-user.json` file, preserving personal variants and skipping identical ones. The Pokédex then reads only the personal files. For example, run `/gio dex remove Crypt Ghoul` in the Hub and capture it again, or edit `hub-user.json` and run `/gio dex reload`. Reloads and restarts do not reimport data. Turning Dex Dev off restores the normal jar + user merge and keeps the editable files. Turning it on again explicitly imports the bundled profiles again, including any bundled entries you removed while developing.

## Capture a mob

1. Approach a mob within seven blocks, preferably away from other mobs. Aim at its body or model when possible.
2. Run `/gio dex scan` (default radius: seven blocks), or `/gio dex scan 20` for a larger radius. Green `*` rows show the proposed capture; all listed parts are cached as immutable scan-time data.
3. Run `/gio dex add Rat` (replace `Rat` with your chosen name). The command reports the parts saved.
4. Use `/gio glow Rat` or `/gio hide Rat`.

Names may contain spaces: `/gio dex add Shadow Assassin`. Repeating `add` with the same name learns another representation, such as an armor stand model versus an item display model. An exactly identical variant is ignored.

To explicitly add equipment/model variants to an existing mob, approach each variant and run `/gio dex variant Goblin`. To select exact parts, use `/gio dex variant Goblin 123 124` with IDs from `scan`. Quote multiword names, for example `/gio dex variant "Goblin Warrior" 123 124`. The name must already exist in the destination catalog; a bundled entry also counts. The original variants are preserved, duplicates are ignored, and the feedback shows the destination catalog's total variant count.

For six different goblin loadouts, register the first as `Goblin`, add the other five with `variant Goblin`, then run `/gio glow Goblin` once. Every recognised variant shares that same name and glow colour. `/gio hide Goblin` also applies to all variants.

For a crowded scene or an unusual assembly, choose the exact IDs shown by `scan`:

```text
/gio dex capture "Shadow Assassin" 123 124 125
```

This captures exactly the listed IDs from the **latest scan**, including anonymous assemblies with no nametag, equipment or distinctive model. Positions, equipment and signatures come from that snapshot, so capture still works after movement, despawn or walking away. No live entities need to remain nearby. IDs are only selectors for that snapshot and are **not** persisted as identifying data. Real players are excluded. Use `remove` and capture again if your personal sample included unwanted parts.

The optional scan radius accepts numbers from 1 to 128 blocks, including decimals, and only searches entities already loaded by the client. A larger scan radius does not change the maximum spacing between parts of one captured mob. Each successful scan replaces the previous snapshot, including an empty scan. Changing catalog area, changing world/server or disconnecting invalidates it; it has no time limit while staying in the same world and catalog area. Capture with IDs requires a scan first and never silently falls back to current live entities. `variant <name> <ids...>` uses the same snapshot; `add` and `variant` without IDs continue to capture live nearby mobs within seven blocks.

Use the `base max HP` and `size` values shown by `scan` to distinguish similar mobs. Append `--health` to match the selected body's base maximum health or `--size` to match the standing hitbox width and height in blocks of the selected living parts. For example, `/gio dex capture "Large Slime" 123 --size` saves the size of the selected slime. The flags also work with `variant` when you provide IDs and with the shared catalog commands. You can combine both flags in either order. Each value is used for matching only when its flag is present.

### Shared captures

Use `fishing` for sea creatures, `jerry` for event Jerrys, and `spooky` for Spooky Festival mobs. Each group saves a capture once so it can be recognised across islands:

```text
/gio dex scan 20
/gio dex fishing capture "Sea Walker" 123 124
/gio dex jerry capture "Jerry" 123 124
/gio dex spooky capture "Spooky Mob" 123 124
```

Use the name and IDs from your own scan; the three capture commands above are alternatives for different mobs. The command group selects the destination catalog. The scan still describes the world where you captured it, and scan IDs cannot be reused after changing worlds or areas. Use ordinary `add` or `capture` for mobs that should remain specific to an island. Shared catalogs are available outside their events too, so a profile based only on a generic vanilla entity type may also match unrelated mobs on other islands.

## Commands

| Command | Action |
| --- | --- |
| `/gio dex` or `/gio dex help` | Show the full local command guide |
| `/gio dex scan [radius]` | Cache and list loaded entity parts in range (default 7, maximum 128 blocks) |
| `/gio dex add <name>` | Save the aimed-at/nearest proposed group in the current island or Catacombs catalog |
| `/gio dex capture "name" <ids...> [--health] [--size]` | Save selected parts from the latest scan. Quote multiword names |
| `/gio dex variant "name" [ids... [--health] [--size]]` | Add a variant from cached scan IDs, or omit IDs for automatic live selection |
| `/gio dex list` | List active entries, including available shared catalogs, and recognised live counts |
| `/gio dex info <name>` | Inspect variants and signatures in the active catalog |
| `/gio dex remove <name>` | Remove personal captures of that mob from the current island or Catacombs catalog |
| `/gio dex fishing`, `/gio dex jerry`, `/gio dex spooky` | Show help for the selected shared catalog |
| Shared commands `add`, `capture`, `variant`, `list`, `info`, `remove` | Use the same arguments as the base commands, after the catalog name, for example `/gio dex jerry list` |
| `/gio dex reload` | Reload the active catalogs after editing or repairing a personal file |

The current area must be recognised before saving. Root capture commands write to the current island or Catacombs catalog; `fishing`, `jerry` and `spooky` explicitly select a shared personal catalog. `remove` removes only personal data. If the same name has a bundled definition, it remains available and the command says so. A damaged or unsupported personal file produces an error and blocks writes to that catalog until repaired and reloaded; it is not replaced with an empty catalog. Saves use a temporary file followed by a rename.

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

Removing a rendering effect does not delete a Pokédex entry. Hide names are persistent. Glow lists are persistent and separate per island, with one list shared across Catacombs floors and up to 10 names and distinct colours per area. Names and colours are saved in `config/giovanniclient/config.json` and restored after changing areas, reconnecting or restarting. `glow list` shows the current area and its count; `remove`, `clear` and `off` affect only that area's glow list. Shared entries can be recognised across islands, but glow still has to be enabled in each island's list.

## Recognition and rendering

The shared resolver matches all parts of a recorded representation using entity type, cleaned name, equipment/head texture, item model/custom model data, dye colour, display item or block state, and relative horizontal/vertical offsets. Level and health text are stripped; head textures use their texture URL hash rather than profile UUIDs or timestamps. Actual entity UUIDs and IDs are used only in the temporary live index.

The resolver starts a refresh at most once every five client ticks. It samples loaded entities in batches of at most 128 and aims to spend at most one millisecond per tick on sampling. Matching runs on a background thread. More complete variants take precedence, each entity can belong to only one assembly, and matched parts stay bound to that anchor's lifetime. A lingering anchor cannot transfer an effect to a replacement body after death. Rejoining, changing catalog area, or explicitly reloading resets live associations.

For an exact alias in the active Pokédex, hide/glow use the recognised assembly and skip their old proximity search for that alias. Other names retain the existing substring-based behaviour. Hide keeps the existing translucent-body behaviour and removes visual accessories and labels; glow outlines the body and visual parts, excluding text labels. Names from the active catalog appear in hide/glow suggestions.

## Limits

- Bundled definitions cover only the mobs included in the release. Name parsing, texture identity and grouped entities have no runtime dependency on other SkyBlock mods or their servers.
- Automatic grouping is a proposal based on proximity and passenger relationships. Check `scan` and use explicit `capture` when mobs overlap or have large/complex models.
- Every recorded part must be present. Different equipment, model states or nametags may need another learned variant. Part offsets allow 0.65 blocks of horizontal error and 0.9 vertically; horizontal distance allows rotation around the anchor.
- Nametags and distinctive models are optional for explicit `capture`: all selected parts are saved, using their types, available characteristics and relative offsets. A single anonymous entity matches all identical entities where its catalog is active. Automatic grouping remains conservative; use explicit IDs for anonymous assemblies.
- Supported parts include living mobs, fake players, armor stands, item/block/text display entities and falling-block entities. Placed world blocks are outside this version. Fake players using real-player-style UUIDs are excluded conservatively.
- In-game validation is needed for each captured mob, particularly animated models and overlapping groups.

## Verification

After building, check that the jar includes `assets/giovanniclient/pokedex/`. In game, check that bundled and personal variants appear together, Catacombs works without a TAB area, and a personal capture in each shared catalog appears on another island. Check that removing a personal capture does not remove any bundled definition and that old scan IDs cannot be reused after changing worlds.
