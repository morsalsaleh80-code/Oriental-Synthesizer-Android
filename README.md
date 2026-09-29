# Pa4x Arranger Android Prototype

This repository contains a KORG Pa4x-inspired arranger app prototype for Android. It is intentionally built as a high-fidelity UI and real-time music engine foundation for a mobile arranger/synth experience, with an emphasis on Persian/Oriental music and live arrangement workflows.

## Current status

This is an active prototype, not a finished commercial product. The project currently includes:

- KORG-inspired hardware UI and control layout
- Compose-based performance screen
- Sequencer/state logic for arranger variation control
- Basic synthesizer and drum engine
- Model layer for sounds, styles, tuning presets, and chords
- Factory-style arranger set structure

## Important caveat

This project is still in an early engineering phase. To become a true Pa4x-compatible instrument, the following items need deeper implementation:

- Real Pa4x file parsing for SET/STY/PCG/PAD/PCM binaries
- A real low-latency Android audio engine, ideally using Oboe
- Sample library ingestion and mapping
- More advanced mixer, effects, and FX chain
- Complete keyboard splitting, touch control, and live MIDI input behavior
- Full validation and build stabilization across devices

## Repository structure

- app/src/main/java/com/example/...: app logic and Compose UI
- app/src/main/AndroidManifest.xml: Android manifest
- app/build.gradle.kts: app-level Gradle build configuration
- settings.gradle.kts / build.gradle.kts: project Gradle setup

## Getting started

1. Open the project in Android Studio.
2. Ensure Android SDK 35 and Kotlin 1.9.x are available.
3. Sync Gradle.
4. Run the app on a device or emulator with Android API 26+.

## Typical build requirements

- Android Studio Ladybug or newer (recommended)
- JDK 17
- Android SDK Platform 35
- Kotlin plugin compatible with Compose
- Device or emulator with adequate audio support

## Roadmap

- Phase 1: stabilize the project scaffold and build
- Phase 2: real low-latency synth and sampler
- Phase 3: real Pa4x set import and arranger parsing
- Phase 4: effect chain and mixer implementation
- Phase 5: production-level tuning, performance optimization, and polish

## License

This project is currently a personal prototype and is not an official KORG product or a licensed Pa4x implementation.
