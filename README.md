# Streamline

**More FPS by rendering smarter, not less.** *(client-side — NeoForge 1.21.1)*

Sightline's companion: it raises frame rates by controlling *when* and *how hard* the game renders,
without hiding any geometry. It caps frames when the window is idle, minimized or unfocused, paces
the frame limiter precisely, and trims per-frame busywork like texture animation, lightmap rebuilds
and particle physics. Includes an FPS meter; options live in Sodium's video settings.

## Building

Requires JDK 21 (Gradle auto-provisions the toolchain).

```bash
./gradlew streamlineJar     # builds the jar -> build/libs/streamline-<version>.jar
./gradlew runClient    # launches a dev client (put bridged mods in run/mods/)
```

The mods this bridges are compile-only and **not** bundled — they're provided at runtime by your modpack.
To build, drop the matching jars into `libs/` (see [libs/README.md](libs/README.md)); they are gitignored
and never redistributed here.

## Compatibility model

Every patch **self-gates**: it activates only when the mods it bridges are installed, and stays dormant
(and harmless) otherwise. Safe to keep loaded with any subset of the target mods.

## License

[MIT](LICENSE) © leon.raineri
