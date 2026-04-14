# Temporature

A temperature mod for Minecraft Fabric.

## Features

- **Dual-scale temperature** — ambient *world temp* and personal *core temp* tracked separately. Core rises or falls when outside a habitable band; damage at extreme thresholds (hypothermia / heatstroke).
- **Layered world-temp system** — biome, elevation, nearby blocks, weather, dimension, structures, and wetness each contribute through pluggable `WorldTemperatureLayer`s. Third-party mods can register their own.
- **Data-driven sources** — biome, dimension, structure, and block temperatures are dynamic registries synced from datapacks. Custom biomes get sensible defaults via formula; override with one JSON.
- **Biome adaptation** — players gradually acclimatize to the biome they live in, shifting their habitable band. Desert dwellers tolerate heat better but chill easily, and vice versa.
- **Wetness & water temperature** — getting wet accumulates a water-temp value that blends into what your skin feels. Hitbox-aware submersion (tip-toe vs. waist vs. fully under) scales soak rate, and deeper water is felt as colder.
- **Rain vs. swim** — rain carries its own temperature (tracked separately from standing water) and caps at a configurable wetness level.
- **Residual drift** — water clinging to a wet player drifts toward ambient over time, faster when hot, slower when cold.
- **HUD gauge** — smooth sliding world-temp meter with optional per-tick audio cue (rising pitch for warming, lower for cooling). Framerate-independent, pixel-exact.
- **YACL config + server sync** — every tunable is live-editable and mismatches are surfaced in a diff screen before a client joins.

## Credit

Core temperature algorithm derived from [Cold Sweat](https://github.com/Momo-Softworks/Cold-Sweat) by MikulDev (GPL 3.0).
