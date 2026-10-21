# Pulse Monitor

[![CI/CD](https://github.com/DoubledDoge/pulsemonitor-library/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/DoubledDoge/pulsemonitor-library/actions/workflows/ci-cd.yml)
[![CodeQL](https://github.com/DoubledDoge/pulsemonitor-library/actions/workflows/codeql.yml/badge.svg)](https://github.com/DoubledDoge/pulsemonitor-library/actions/workflows/codeql.yml)
[![JitPack](https://jitpack.io/v/DoubledDoge/pulsemonitor-library.svg)](https://jitpack.io/#DoubledDoge/pulsemonitor-library)
[![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen)](#requirements)
[![License](https://img.shields.io/badge/license-PolyForm%20Strict%201.0.0-blue)](LICENSE)

A lightweight Android library that displays a floating, draggable overlay with live **CPU**, **memory** and **network** usage for your app. The overlay follows the user across every screen, so you can see the resource cost of each part of your app as you use it.

- One line of setup
- No additional permissions
- Readings update once per second on a background thread

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Metrics](#metrics)
- [API Reference](#api-reference)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Demo App](#demo-app)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **Two overlay states**
    - **Collapsed pill** showing CPU, RAM and network throughput at a glance
    - **Expanded panel** with headline chips and a detailed breakdown per metric
- **Draggable** so it can be moved out of the way of your UI
- **Automatic screen tracking.** A single `install` call attaches the overlay to every Activity, including ones added later.
- **Session-wide readings.** One shared `ProfilerManager` drives every overlay, so totals and peaks carry across screens instead of resetting.
- **Main-thread vs background CPU split** to help separate UI jank from background work
- **Observable data.** Snapshots are exposed as a `StateFlow` for custom UIs or logging.

---

## Requirements

| Requirement | Version |
|---|---|
| Android Min SDK | 24 (Android 7.0) |
| Android Compile SDK | 37 |
| Kotlin | JVM target 11 |
| JDK (to build the project) | 17 or newer (CI uses 25) |

---

## Installation

### JitPack

Add the JitPack repository to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.DoubledDoge:pulsemonitor-library:<version>")
}
```

Replace `<version>` with the latest release tag shown on the JitPack badge above.

### Local module

To work against the source directly, copy the `pulsemonitor` module into your project and add:

```kotlin
// settings.gradle.kts
include(":pulsemonitor")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":pulsemonitor"))
}
```

---

## Usage

Call `install` once from your launcher Activity, after the content has been set:

```kotlin
import io.github.doubleddoge.pulsemonitor.ui.ProfilerOverlay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // App UI
        }

        ProfilerOverlay.install(this)
    }
}
```

Every Activity in the app now shows the overlay. No changes are needed in other screens.

### Single screen

To show the overlay on one Activity only, use `attach` instead of `install`:

```kotlin
ProfilerOverlay.attach(this)
```

### Debug builds only

Pulse Monitor is a development tool. To keep it out of release builds, guard the call:

```kotlin
if (BuildConfig.DEBUG) {
    ProfilerOverlay.install(this)
}
```

`BuildConfig` must be enabled in the app module with `buildFeatures { buildConfig = true }`.

### Network readings

Network values are only produced if your app performs network calls, which requires the standard permission in your app's manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Metrics

All values describe **the host app's own process**. Android does not allow apps to read the resource usage of other apps.

### CPU

| Metric | Description |
|---|---|
| Usage | Share of total device CPU used by the app, across all cores (0 to 100%) |
| Main Thread | Portion of usage on the UI thread. High values indicate likely jank. |
| Background Threads | Portion of usage on all other threads |
| App CPU time (session) | Total CPU time consumed since collection started |

### Memory

| Metric | Description |
|---|---|
| PSS (total) | Proportional Set Size, the app's overall memory footprint |
| Peak this session | Highest PSS recorded since collection started |
| Java/Kotlin Heap | Managed heap in use, useful for spotting leaks |
| Native heap | Memory allocated by native code |
| Heap utilisation | Java heap usage as a percentage of its configured maximum |
| System Available | Free RAM remaining on the device |

### Network

| Metric | Description |
|---|---|
| Throughput | Combined bytes per second sent and received |
| Received | Bytes downloaded since collection started |
| Sent | Bytes uploaded since collection started |

---

## API Reference

### `ProfilerOverlay`

Entry point of the library. Package `io.github.doubleddoge.pulsemonitor.ui`.

| Function | Description |
|---|---|
| `install(activity: Activity)` | Registers activity lifecycle callbacks so the overlay is attached to every Activity when it starts. Safe to call more than once. |
| `attach(activity: Activity)` | Attaches the overlay to a single Activity. Does nothing if that Activity already has one. |

### `ProfilerManager`

The collection engine. Package `io.github.doubleddoge.pulsemonitor.core`.

| Member | Description |
|---|---|
| `performance: StateFlow<PerformanceSnapshot?>` | Latest reading, updated every second. `null` until the first sample. |
| `start()` | Starts the collection loop. Ignored if already running. |
| `stop()` | Stops the collection loop. |
| `collectSnapshot(): PerformanceSnapshot` | Takes a single reading immediately. |

`ProfilerOverlay` creates and manages one shared instance. A separate `ProfilerManager` can be created to consume readings without the overlay:

```kotlin
val manager = ProfilerManager(applicationContext)
manager.start()

lifecycleScope.launch {
    manager.performance.collect { snapshot ->
        snapshot ?: return@collect
        Log.d("PulseMonitor", "CPU ${snapshot.cpu.usagePercent}%  PSS ${snapshot.memory.totalPssMb} MB")
    }
}
```

### Models

Package `io.github.doubleddoge.pulsemonitor.models`.

| Class | Fields |
|---|---|
| `PerformanceSnapshot` | `memory`, `cpu`, `network` |
| `CpuStats` | `usagePercent`, `mainThreadPercent`, `backgroundPercent`, `cpuTimeMs` |
| `MemoryStats` | `totalPssMb`, `sessionPeakPssMB`, `javaHeapUsedMb`, `nativeHeapAllocatedMb`, `javaHeapUtilizationPercent`, `systemAvailableRamMb` |
| `NetworkStats` | `receivedBytes`, `sentBytes`, `bytesPerSecond` |

---

## Architecture

```
                     ProfilerManager  (single shared instance)
       ┌──────────────────────────────────────────────────────────┐
       │   CpuCollector      MemoryCollector      NetworkCollector  │
       │         └───────────────┬────────────────────┘             │
       │                         ▼                                  │
       │                PerformanceSnapshot  (every 1 s)            │
       └─────────────────────────┬────────────────────────────────┘
                                 │ StateFlow
             ┌───────────────────┼───────────────────┐
             ▼                   ▼                   ▼
     ProfilerOverlayView  ProfilerOverlayView  ProfilerOverlayView
        (Activity A)         (Activity B)         (Activity C)
```

1. `install()` registers `Application.ActivityLifecycleCallbacks`.
2. On each `onActivityStarted`, `attach()` adds a `ProfilerOverlayView` to the Activity's content view. A view tag prevents duplicates when returning to an Activity.
3. The first attach creates the shared `ProfilerManager` with the application context and starts it. Later attaches reuse it, and it is injected into each overlay through the constructor.
4. Once per second the manager calls each collector, combines the results into a `PerformanceSnapshot` and publishes it on a `StateFlow`.
5. Each overlay observes the flow while it is attached. Detaching an overlay stops its observation only. The manager keeps running for the rest of the app.

### Design decisions

| Decision | Rationale |
|---|---|
| One shared manager | Avoids duplicate collection loops, keeps readings consistent across screens and reduces the library's own overhead |
| One overlay view per Activity | A view can only belong to one hierarchy. A fresh, lightweight view per Activity avoids leaking the previous Activity. |
| Attach on `onActivityStarted` | At `onActivityCreated` the Activity's content has not been set, so it would be drawn over the overlay |
| Application context for the manager | The manager outlives individual Activities and must not hold a reference to one |
| CPU split by tick ratio | Main-thread share is derived from `/proc/self/task` tick ratios, so device-specific clock tick sizes cancel out |
| `TrafficStats.UNSUPPORTED` handling | Some devices intermittently return `-1`. The last valid value is reused so totals never go negative. |

---

## Project Structure

```
.
├── pulsemonitor/                         Library module
│   └── src/main/java/io/github/doubleddoge/pulsemonitor/
│       ├── core/
│       │   └── ProfilerManager.kt        Collection loop and StateFlow
│       ├── metrics/
│       │   ├── MetricCollector.kt        Collector interface
│       │   ├── cpu/CpuCollector.kt
│       │   ├── memory/MemoryCollector.kt
│       │   └── network/NetworkCollector.kt
│       ├── models/                       CpuStats, MemoryStats, NetworkStats, PerformanceSnapshot
│       └── ui/
│           ├── ProfilerOverlay.kt        Public entry point
│           ├── ProfilerOverlayView.kt    Pill, panel, dragging and rendering
│           └── Palette.kt                Colours and shared styling values
├── demoapp/                              Sample app for manual testing
├── docs/                                 Requirements and technical stack
└── .github/
    ├── workflows/                        CI/CD, CodeQL and release pipelines
    └── PULL_REQUEST_TEMPLATE.md
```

### Adding a metric

1. Add a data class to `models/`.
2. Implement `MetricCollector<T>` in `metrics/<name>/`. For rate or percentage values, store the previous reading and compute the difference in `collect()`.
3. Add the new stats to `PerformanceSnapshot`, call the collector in `ProfilerManager.collectSnapshot()`, and add a render function and rows in `ProfilerOverlayView`.

---

## Demo App

The `demoapp` module contains three Activities, each with controls that deliberately consume a resource so the overlay can be verified.

| Control | Action | Expected change |
|---|---|---|
| Burn CPU (background, 5 s) | Busy loop on a worker thread | CPU and Background Threads increase |
| Burn CPU (main thread, 2 s) | Busy loop on the UI thread (the screen freezes briefly) | CPU and Main Thread increase |
| Allocate 20 MB / Free memory | Retains, then releases, a 20 MB block | PSS and Peak increase, then PSS drops |
| Download 10 MB | Downloads a test file | Throughput and Received increase |
| Upload 5 MB | Uploads generated data | Throughput and Sent increase |
| Next screen | Navigates between Activities | The overlay remains and totals carry over |

Select the `demoapp` run configuration in Android Studio to launch it.

---

## Building from Source

```bash
git clone https://github.com/DoubledDoge/pulsemonitor-library.git
cd pulsemonitor-library

# Build the library
./gradlew :pulsemonitor:assembleRelease

# Run unit tests
./gradlew :pulsemonitor:test

# Install the demo app on a connected device or emulator
./gradlew :demoapp:installDebug
```

Releases are published automatically when a semantic version tag is pushed:

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Contributing

1. Update your local `main`:
   ```bash
   git checkout main
   git pull origin main
   ```
2. Create a branch for the change:
   ```bash
   git checkout -b feature/short-description
   ```
3. Commit with a clear message and push:
   ```bash
   git push -u origin feature/short-description
   ```
4. Open a pull request into `main` and complete the pull request template.

Guidelines:

- Keep each pull request focused on a single feature or fix.
- Use named constants instead of magic numbers.
- Keep platform-specific reading logic inside the relevant collector.
- Pull requests must pass the CI and CodeQL checks before merging.

---

## License

This project is licensed under the [PolyForm Strict License 1.0.0](LICENSE).