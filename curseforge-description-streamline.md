# Streamline

**Raise FPS by controlling *when* and *how hard* the game renders — without culling a single thing.**
Streamline is a client-side performance mod for NeoForge 1.21.1, built to sit alongside Sodium and Iris. It is
the companion to [Sightline](https://www.curseforge.com/minecraft/mc-mods/sightline): Sightline decides *what*
gets drawn, Streamline decides *when* frames are delivered and *how much* per-frame busywork the game does.

**Short summary (for the CurseForge "Summary" field):**
> Client-side FPS mod that governs frame delivery instead of geometry: caps frames nobody is watching, paces the
> frame limiter precisely, and trims per-frame busywork (texture animation, lightmap rebuilds, particle physics,
> HUD flushes, F3 rebuilds). Culls nothing — that's Sightline's job. Options live in Sodium's video settings.

---

## Frames nobody watches shouldn't cost anything.

Every feature is client-side, opt-in where it could ever be noticed, and self-reports in the debug overlay.
**Culls no geometry** — what gets rendered is Sightline's job. Every hook is vanilla-targeted and composes with
Sodium's, Iris's, [Fovea's](https://www.curseforge.com/minecraft/mc-mods/fovea) and Sightline's.

## Features

### FPS governor
Frame caps for window states where you aren't looking: **unfocused (15 fps)**, **minimized (3 fps)** and
**idle (30 fps after 5 minutes without input)**. Frames nobody watches stop burning the GPU. Any input or
refocus lifts the cap on the very next frame; loading screens are never throttled, and multiplayer stays in
sync (packets drain fully every tick regardless of frame rate).

### Precise frame limiter *(opt-in)*
Vanilla's FPS cap overshoots its wait by a millisecond or more, so capped gameplay has a visible cadence wobble.
Streamline's wait-then-spin pacing lands frames within microseconds of the target for a steady, even cadence.

### Adaptive VSync *(opt-in)*
When a frame runs late, tear for one frame instead of stalling for a whole refresh interval
(`swap_control_tear`, where the driver supports it) — smoother than hard VSync, less tearing than none.

### Texture animation control
Freeze animated sprites per category — **water, lava, fire, portal, other**. Every animated sprite re-uploads
its pixels to the GPU each animation frame; Sodium already skips the off-screen ones, and Streamline can stop
the visible ones too.

### Steady lightmap
Vanilla rebuilds and re-uploads the lightmap texture every tick just to animate torch flicker. Streamline pins
the flicker and skips any rebuild whose inputs didn't change. Night vision, darkness, gamma, storms and
dimension effects still apply instantly.

### Block ambience density
Scale vanilla's 667-samples-per-tick ambient effects loop (torch flames, drips, cave crackles) down to taste.

### Particle physics fast path
Particles surrounded by air skip vanilla's allocation-heavy block-collision machinery — **bit-identical
outcomes**, a big win in storms where thousands of rain particles would otherwise collision-test every tick.

### HUD draw batching
Vanilla flushes the GUI buffer after *every single* HUD element; Streamline batches per vanilla layer,
collapsing dozens of flushes into a few. Modded HUD layers render exactly as before.

### Fast F3
The debug columns rebuild at most 10×/second instead of every frame (vanilla runs dozens of `String.format`
calls per frame while F3 is open).

### Detail toggles
Turn off the **enchantment glint** pass (enchanted crowds otherwise draw everything twice) and the
**vignette**.

### FPS meter
A compact FPS / frame-time / 1%-low readout that also shows the active governor cap.

---

## Compatibility

- **Client-only.** Safe on any server; nothing touches gameplay, world data or the network.
- **Sodium** *(optional)* — when present, every option appears in Sodium's video settings (Reese's Sodium
  Options compatible). The texture-animation gate composes with Sodium's "animate only visible textures."
- **Iris** *(optional)* — the lightmap memo never skips a rebuild Iris's darkness-uniform capture needs, and no
  hook touches the shader pipeline.
- **Sightline** *(optional)* — the culling half of the pair. Both work alone; best together.

**Requires:** NeoForge 1.21.1. **Recommended alongside:** Sodium, Iris, Sightline.
