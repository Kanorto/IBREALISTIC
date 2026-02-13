# OBURealistic — Changelog

All changes made on top of the original [OpenBoatUtils](https://github.com/o7Moon/OpenBoatUtils) (from commit `38e3097`, after CONTRIBUTING.md merge).

---

## Overview

OBURealistic is a fork of OpenBoatUtils that adds realistic four-wheel vehicle physics simulation to Minecraft boats. All new features are **server-driven** — the client does nothing unless the server sends the appropriate configuration packets. Vanilla behavior is preserved when no server packets are received.

---

## Version 1.4 (Current)

### New Features

#### Collision-Aware Velocity Initialization (`physics/FourWheelPhysicsEngine.java`)
When Minecraft's `move()` clips velocity during wall/block collisions, the world-frame velocity changes abruptly. Previously, naively converting to local frame created false lateral velocity (vy) that threw the vehicle sideways. Now the engine:

- **Tracks expected world velocity** (`expectedWorldVx`, `expectedWorldVz`) between ticks
- **Detects collisions** by comparing actual entity velocity with expected velocity
- **Limits lateral injection** — caps vy change to `MAX_COLLISION_LATERAL_INJECTION` (1.5 m/s) during collisions
- **Damps lateral forces** — reduces accumulated tire forces by `COLLISION_LATERAL_FORCE_DAMPING` (0.5) on collision

#### Landing Inertia Preservation (`physics/FourWheelPhysicsEngine.java`)
When landing from flight, block collisions clip world velocity. Previously this could cause abrupt stops. Now:

- **Lateral velocity damped** by `LANDING_LATERAL_DAMPING` (0.5) on air-to-ground transition
- **Lateral tire forces damped** to prevent sudden sideways forces after landing
- **Yaw rate damped** by `LANDING_YAW_RATE_DAMPING` (0.8) for smoother landing

#### Realistic Steering Speeds (`physics/VehicleType.java`, `physics/VehicleConfig.java`)
Steering speeds updated based on real WRC data (with +30% for gameplay comfort):

| Vehicle | Before (rad/s) | After (rad/s) | Lock-to-lock time |
|---------|----------------|----------------|--------------------|
| WRC_CAR | 5.0 | 1.4 | ~0.7s |
| GROUP_B | 4.5 | 1.2 | ~0.8s |
| CLASSIC_RALLY | 4.0 | 1.0 | ~0.9s |
| LIGHTWEIGHT | 6.0 | 1.6 | ~0.7s |
| TRUCK | 3.0 | 0.8 | ~0.9s |

- `steeringReturnRate` reduced from 3.0 to 1.5 rad/s (realistic self-aligning torque)

#### Steering Return Rate Packet & Command
- New packet `SET_STEERING_RETURN_RATE` (ID: 60) — `float` steering return rate in rad/s (0 = disabled)
- New singleplayer command `/steeringreturnrate <0-20>` — set steering return rate

### Improved

#### Wheel Rendering (`client/WheelRenderer.java`)
- **Wheel radius** increased from 2.0 to 2.5 blocks for better visual proportions

#### Steering Wheel Rendering (`client/SteeringWheelRenderer.java`)
- **Visual radius** increased from 0.25 to 2.5 blocks (visible from cockpit)
- **Position** moved to boat protrusion: X: 0.35→0.7, Y: -0.15→-0.55
- Steering wheel is now clearly visible to the player in first-person view

### Changed Constants
| Constant | Before | After | Location |
|----------|--------|-------|----------|
| `WHEEL_RADIUS` | 2.0f | 2.5f | WheelRenderer |
| `WHEEL_VISUAL_RADIUS` | 0.25f | 2.5f | SteeringWheelRenderer |
| `WHEEL_X_OFFSET` | 0.35f | 0.7f | SteeringWheelRenderer |
| `WHEEL_Y_OFFSET` | -0.15f | -0.55f | SteeringWheelRenderer |
| `steeringSpeed` (default) | 5.0f | 1.4f | VehicleConfig |
| `steeringReturnRate` | 3.0f | 1.5f | VehicleConfig |

### New Constants
| Constant | Value | Location |
|----------|-------|----------|
| `COLLISION_SPEED_LOSS_THRESHOLD` | 0.3f | FourWheelPhysicsEngine |
| `MAX_COLLISION_LATERAL_INJECTION` | 1.5f | FourWheelPhysicsEngine |
| `COLLISION_LATERAL_FORCE_DAMPING` | 0.5f | FourWheelPhysicsEngine |
| `LANDING_LATERAL_DAMPING` | 0.5f | FourWheelPhysicsEngine |
| `LANDING_YAW_RATE_DAMPING` | 0.8f | FourWheelPhysicsEngine |

### Changed Files
| File | Changes |
|------|---------|
| `physics/FourWheelPhysicsEngine.java` | Collision detection, landing inertia, expected velocity tracking |
| `physics/VehicleConfig.java` | Reduced steeringSpeed and steeringReturnRate defaults |
| `physics/VehicleType.java` | Reduced steeringSpeed for all vehicle types |
| `client/WheelRenderer.java` | Increased WHEEL_RADIUS to 2.5 |
| `client/SteeringWheelRenderer.java` | Increased size to 2.5, moved to boat protrusion |
| `ClientboundPackets.java` | Added SET_STEERING_RETURN_RATE (ID: 60) |
| `SingleplayerCommands.java` | Added `/steeringreturnrate` command |
| `OpenBoatUtils.java` | Added `setSteeringReturnRate()` method |

---

## Version 1.3

### Improved

#### Wheel Rendering Overhaul (`client/WheelRenderer.java`)
Complete visual rework of the wheel rendering system for more realistic and proportionate wheels:

- **Increased wheel size**: Radius increased from 0.25 to 0.4 blocks (+60%), making wheels proportionate to the boat body. Previous wheels were disproportionately small ("micro-wheels").

- **Octagonal tire profile**: Instead of a single cuboid, each tire now uses two overlapping cuboids rotated 45° apart (`tire_0` at 0° and `tire_45` at 45° around X axis), creating an 8-sided approximation of a circular cross-section.

- **Separate rim/hub**: Added a central hub disc (`hub` part) rendered in lighter gray (0x595959 / RGB 0.35) using `gray_concrete` texture, distinct from the dark tire (0x1F1F1F / RGB 0.12) using `black_concrete` texture. Hub is ~56% of tire face size.

- **Wider track width**: Lateral offset increased from 0.55 to 0.70 blocks, making wheels visibly extend beyond the boat body width for a more aggressive rally car stance.

- **Extended wheelbase**: Front/rear axle offsets increased from ±0.55 to ±0.60 blocks for better proportions with larger wheels.

- **Separate texture identifiers**: `TIRE_TEXTURE` and `HUB_TEXTURE` constants for clean texture management instead of inline `Identifier.of()` calls.

**Model structure (before → after):**
| Part | Before | After |
|------|--------|-------|
| Tire | Single cuboid `wheel` (4×6×6) | Two cuboids `tire_0` + `tire_45` (3×10×10 each, 45° apart) |
| Hub/Rim | None | Cuboid `hub` (3.6×5.6×5.6) in lighter gray |

**Dimensions in blocks:**
| Component | Width (X) | Height (Y) | Depth (Z) |
|-----------|-----------|------------|-----------|
| Tire (each) | 0.24 | 0.80 | 0.80 |
| Hub | 0.29 | 0.45 | 0.45 |

#### Visual Boat Lift (`mixin/BoatEntityRendererMixin.java`)
Added a visual vertical offset to raise the boat model when realistic physics is active:

- **New constant `VISUAL_LIFT = 0.25f`**: Boat is raised by 0.25 blocks visually to appear as if it's sitting on its wheels.

- **Hitbox unchanged**: Only the rendered model is shifted — the entity's collision box and position remain the same.

- **Version-correct Y translation**: 
  - For MC ≤1.21: `translate(0, -VISUAL_LIFT, 0)` — injection point is AFTER `scale(-1,-1,1)`, so Y axis is inverted (negative = up)
  - For MC ≥1.21.3: `translate(0, +VISUAL_LIFT, 0)` — injection point is BEFORE `scale(-1,-1,1)`, so Y axis is standard (positive = up)
  - Both produce the same visual effect: boat moves upward.

- **Conditional activation**: Lift only applies when `isPlayerBoat()` (≤1.21) or `fourWheelPhysics.isEnabled()` (≥1.21.3) returns true. Vanilla boats render normally.

### Changed Files
| File | Changes |
|------|---------|
| `client/WheelRenderer.java` | New compound wheel model (tire_0 + tire_45 + hub), increased dimensions, separate textures, dual VertexConsumer rendering |
| `mixin/BoatEntityRendererMixin.java` | Added `VISUAL_LIFT` constant and Y-translate in both version-specific `applyRealisticRoll` injections |

### Changed Constants
| Constant | Before | After | Location |
|----------|--------|-------|----------|
| `WHEEL_RADIUS` | 0.25f | 0.4f | WheelRenderer |
| `WHEEL_Y_OFFSET` | 0.3f | 0.35f | WheelRenderer |
| `FRONT_X_OFFSET` | 0.55f | 0.6f | WheelRenderer |
| `REAR_X_OFFSET` | -0.55f | -0.6f | WheelRenderer |
| `LATERAL_OFFSET` | 0.55f | 0.7f | WheelRenderer |
| `WHEEL_WIDTH` | 0.15f | *(removed)* | WheelRenderer |

### New Constants
| Constant | Value | Location |
|----------|-------|----------|
| `TIRE_HALF_WIDTH` | 1.5f | WheelRenderer |
| `TIRE_HALF_SIZE` | 5f | WheelRenderer |
| `HUB_HALF_WIDTH` | 1.8f | WheelRenderer |
| `HUB_HALF_SIZE` | 2.8f | WheelRenderer |
| `TIRE_TEXTURE` | `minecraft:textures/block/black_concrete.png` | WheelRenderer |
| `HUB_TEXTURE` | `minecraft:textures/block/gray_concrete.png` | WheelRenderer |
| `VISUAL_LIFT` | 0.25f | BoatEntityRendererMixin |

---

## Version 1.2

### Improved

#### Physics System Audit & Optimizations
A comprehensive audit and fix of the physics system, addressing performance, thread safety, correctness, and dead code:

- **Immutable surface presets**: All `SurfaceProperties` fields are now `final`, and all surface presets (`ASPHALT_DRY`, `ICE`, `BLUE_ICE`, etc.) are now `public static final`, preventing accidental mutation that could affect all players on a server. `BLUE_ICE` is now a proper preset (μ=0.06, even less grip than ICE).

- **Thread-safe block surface map**: `blockSurfaceMap` now uses `volatile` + double-checked locking for initialization, `ConcurrentHashMap` for thread-safe reads/writes, and `volatile` on `defaultSurface`. `resetBlockSurfaceMap()` is also `synchronized`.

- **GC optimization (SurfaceAccumulator)**: Introduced `SurfaceProperties.SurfaceAccumulator` — a reusable per-engine object that eliminates per-tick `new SurfaceProperties(...)` allocation in `detectSurface()`. When all sampled blocks resolve to the same preset instance (uniform surface), the accumulator returns the preset directly with zero allocation. Each physics engine instance holds its own accumulator.

- **Fixed yaw moment signs**: Corrected inverted signs in the longitudinal force yaw moment calculation in `FourWheelPhysicsEngine`. Left wheel forward force now correctly creates positive yaw (counterclockwise from above), and right wheel creates negative yaw. Changed from `(-fxWheel[0] + fxWheel[1])` to `(fxWheel[0] - fxWheel[1])` for both front and rear axles.

- **Removed dead code (RealisticPhysicsEngine)**: The old `RealisticPhysicsEngine` (bicycle model) was being kept in sync with `FourWheelPhysicsEngine` through ~20 setter methods in `OpenBoatUtils.java`, but was never actually used for physics computation (only `fourWheelPhysics` is referenced in `BoatMixin`). All redundant `realisticPhysics.*` calls have been removed. The `RealisticPhysicsEngine.java` class file is retained for potential future use.

### Changed Files
- `physics/SurfaceProperties.java` — `final` fields, `final` presets, `BLUE_ICE` preset, `SurfaceAccumulator`, thread-safe init
- `physics/FourWheelPhysicsEngine.java` — Uses `SurfaceAccumulator`, fixed yaw moment signs
- `physics/RealisticPhysicsEngine.java` — Uses `SurfaceAccumulator`, removed unused `Block` import
- `OpenBoatUtils.java` — Removed `realisticPhysics` field and all ~20 redundant setter calls

---

## Version 1.1

### Added

#### Realistic Physics Engine (`physics/` package)
A complete vehicle dynamics simulation with the following new source files:

- **`RealisticPhysicsEngine.java`** (598 lines) — Bicycle-model physics engine. Computes longitudinal/lateral tire forces, weight transfer, steering dynamics, surface detection, and airborne physics. Uses Fiala/Brush tire model for realistic slip angle behavior.

- **`FourWheelPhysicsEngine.java`** (653 lines) — Four-wheel (four-corner) physics engine that replaces the bicycle model. Each wheel (FL, FR, RL, RR) has independent:
  - Vertical load (weight transfer from acceleration/braking/cornering)
  - Slip angle and lateral force
  - Longitudinal force (drive/brake)
  - Friction circle constraint
  Supports differential types, AWD torque split, aerodynamic downforce, and weather-dependent grip.

- **`TireModel.java`** (136 lines) — Fiala/Brush tire model implementation. Computes:
  - Slip angles
  - Lateral forces (linear region + saturation + progressive falloff)
  - Longitudinal forces with friction circle constraint
  - Load sensitivity (grip reduction under high load)
  - Force relaxation (smooth tire response)

- **`SurfaceProperties.java`** (522 lines) — Surface friction model with:
  - 18 surface presets: `ASPHALT_DRY`, `ASPHALT_WET`, `GRAVEL`, `DIRT`, `MUD`, `SNOW`, `ICE`, `BLUE_ICE`, `SAND`, `WOOD`, `CONCRETE`, `TERRACOTTA`, `METAL`, `GLASS`, `WOOL`, `BRICK`, `NETHER`, `VEGETATION`
  - Automatic block → surface mapping for 300+ Minecraft blocks
  - Per-surface parameters: peak friction (`muPeak`), sliding friction (`muSlide`), cornering stiffness, relaxation length, rolling resistance, peak slip angle, slip angle falloff, load sensitivity
  - Server-configurable block surface type overrides
  - Server-configurable default surface type

- **`VehicleConfig.java`** (62 lines) — Vehicle configuration with all tunable parameters:
  - Mass, wheelbase, CG height, track width, front weight bias
  - Max steering angle, steering speed, speed-dependent steering reduction
  - Engine force, braking force, brake bias, engine braking
  - Drag coefficient, rolling resistance
  - Drivetrain type, substep count
  - AWD front/rear torque split
  - Front/rear differential type, LSD locking coefficient
  - Downforce coefficient and front/rear distribution
  - Roll stiffness ratio

- **`VehicleType.java`** (60 lines) — 5 preset vehicle types:
  - `WRC_CAR` — Modern WRC rally car (AWD, 1190kg, high power)
  - `GROUP_B` — Group B rally car (RWD, 1100kg, very high power)
  - `CLASSIC_RALLY` — Classic rally car (RWD, 1000kg, moderate power)
  - `LIGHTWEIGHT` — Lightweight car (FWD, 800kg, nimble)
  - `TRUCK` — Heavy truck (AWD, 2000kg, high torque)

- **`DrivetrainType.java`** (29 lines) — Drivetrain enumeration:
  - `RWD` — Rear-wheel drive
  - `FWD` — Front-wheel drive
  - `AWD` — All-wheel drive (configurable front/rear split)

- **`DifferentialType.java`** (23 lines) — Differential types per axle:
  - `OPEN` — Torque goes to wheel with least resistance
  - `LOCKED` — Both wheels rotate at same speed
  - `LSD` — Limited Slip Differential with configurable locking coefficient

- **`WeatherCondition.java`** (32 lines) — Weather grip modifiers:
  - `CLEAR` — 100% grip
  - `RAIN` — 70% grip, slower tire response
  - `HEAVY_RAIN` — 50% grip, much slower tire response
  - `SNOW` — 40% grip
  - `FOG` — 95% grip (visual only, minimal physics effect)

- **`WheelPosition.java`** (25 lines) — Wheel position enumeration (FL, FR, RL, RR).

#### Visual Wheel Rendering (`client/WheelRenderer.java`)
A visual rendering system for vehicle wheels, displayed when realistic physics is active:

- **`WheelRenderer.java`** (161 lines) — Renders four wheels on the boat entity:
  - Positioned at four corners: front-left, front-right, rear-left, rear-right
  - Front wheels rotate with steering input (Y axis rotation)
  - All wheels spin based on forward velocity (X axis rotation)
  - Tick-based spin with frame-rate-independent interpolation via `tickDelta`
  - Uses `ModelPartBuilder` cuboid geometry with entity solid render layer
  - Thread-safe: `volatile` fields for cross-thread spin angle and speed sharing (game tick → render thread)

#### Visual Effects Rendering (`mixin/BoatEntityRendererMixin.java`)
A mixin for the boat entity renderer that adds visual effects when realistic physics is active:

- **`BoatEntityRendererMixin.java`** (98 lines) — Injects into `BoatEntityRenderer` (≤1.21) / `AbstractBoatEntityRenderer` (≥1.21.3):
  - **Roll visualization**: Applies Z-axis rotation based on `visualRollAngle` (computed from lateral acceleration in physics engine)
  - **Wheel rendering**: Calls `WheelRenderer.renderWheels()` before matrix stack pop, passing steering angle and forward speed
  - **Player-only filtering** (≤1.21): Only renders effects on the boat the local player is riding, via `isPlayerBoat()` check
  - **Limitation** (≥1.21.3): `BoatEntityRenderState` does not provide entity reference, so effects apply to all boats when enabled

#### Visual State Fields (`OpenBoatUtils.java`)
- **`visualRollAngle`** (`volatile float`) — Current body roll angle in degrees, set from `FourWheelPhysicsEngine` lateral forces
- **`visualSteeringAngle`** (`volatile float`) — Current steering wheel angle in radians, set from player input processing in `BoatMixin`
- Both fields use `volatile` for thread-safe sharing between game tick thread and render thread

#### New Game Modes (`Modes.java`)
7 new modes added to the `Modes` enum:
- `REALISTIC` (25) — Default realistic mode with WRC car, rally settings, increased backward acceleration
- `REALISTIC_WRC` (26) — WRC rally car configuration
- `REALISTIC_GROUP_B` (27) — Group B rally car configuration
- `REALISTIC_CLASSIC` (28) — Classic rally car configuration
- `REALISTIC_LIGHTWEIGHT` (29) — Lightweight car configuration
- `REALISTIC_TRUCK` (30) — Truck configuration
- `REALISTIC_ALLTERRAIN` (31) — WRC car with step-while-falling enabled for terrain traversal

All realistic modes set: `allBlocksSlipperiness(0.98)`, `fallDamage(false)`, `airControl(true)`, `stepSize(1.25)`, plus the corresponding vehicle type.

#### New Network Packets (`ClientboundPackets.java`)
27 new server-to-client packets (IDs 33–59):
| ID | Packet | Data | Description |
|----|--------|------|-------------|
| 33 | `SET_REALISTIC_PHYSICS` | `boolean` | Enable/disable realistic physics engine |
| 34 | `SET_VEHICLE_TYPE` | `short` | Set vehicle preset (0-4, see VehicleType enum) |
| 35 | `SET_VEHICLE_MASS` | `float` | Vehicle mass in kg |
| 36 | `SET_VEHICLE_WHEELBASE` | `float` | Distance between axles in meters |
| 37 | `SET_VEHICLE_CG_HEIGHT` | `float` | Center of gravity height in meters |
| 38 | `SET_VEHICLE_TRACK_WIDTH` | `float` | Distance between left and right wheels |
| 39 | `SET_VEHICLE_MAX_STEERING` | `float` | Maximum steering angle in radians |
| 40 | `SET_VEHICLE_STEERING_SPEED` | `float` | How fast steering reacts |
| 41 | `SET_VEHICLE_BRAKING_FORCE` | `float` | Maximum braking force in Newtons |
| 42 | `SET_VEHICLE_ENGINE_FORCE` | `float` | Maximum engine force in Newtons |
| 43 | `SET_VEHICLE_DRAG` | `float` | Aerodynamic drag coefficient |
| 44 | `SET_VEHICLE_BRAKE_BIAS` | `float` | Front/rear brake distribution (0=rear, 1=front) |
| 45 | `SET_VEHICLE_SUBSTEPS` | `int` | Physics substeps per tick (1-10) |
| 46 | `SET_VEHICLE_FRONT_WEIGHT_BIAS` | `float` | Static weight distribution |
| 47 | `SET_BLOCK_SURFACE_TYPE` | `string, string` | Map block ID to surface type |
| 48 | `SET_VEHICLE_DRIVETRAIN` | `short` | Drivetrain type (0=RWD, 1=FWD, 2=AWD) |
| 49 | `SET_DEFAULT_SURFACE_TYPE` | `string` | Default surface for unmapped blocks |
| 50 | `SET_VEHICLE_SPEED_STEERING_FACTOR` | `float` | Speed-dependent steering reduction |
| 51 | `SET_VEHICLE_ENGINE_BRAKING` | `float` | Engine braking force |
| 52 | `SET_VEHICLE_ROLL_STIFFNESS_RATIO` | `float` | Front anti-roll bar stiffness ratio |
| 53 | `SET_AWD_FRONT_SPLIT` | `float` | AWD front/rear torque split |
| 54 | `SET_FRONT_DIFFERENTIAL` | `short` | Front axle differential type |
| 55 | `SET_REAR_DIFFERENTIAL` | `short` | Rear axle differential type |
| 56 | `SET_LSD_LOCKING_COEFF` | `float` | LSD locking coefficient |
| 57 | `SET_DOWNFORCE_COEFFICIENT` | `float` | Aerodynamic downforce |
| 58 | `SET_DOWNFORCE_FRONT_BIAS` | `float` | Downforce front/rear distribution |
| 59 | `SET_WEATHER_CONDITION` | `short` | Weather condition (0-4) |

#### Singleplayer Commands (`SingleplayerCommands.java`)
Extended from original 490 lines to 756 lines (+266 lines). All new commands send packets through the same server→client pipeline, ensuring identical behavior to multiplayer. New commands added:
- `/realisticphysics <true/false>` — Enable/disable realistic physics
- `/vehicletype <type>` — Set vehicle preset
- `/vehiclemass <mass>` — Set vehicle mass
- `/vehiclewheelbase <wheelbase>` — Set wheelbase
- `/vehiclecgheight <height>` — Set CG height
- `/vehiclemaxsteering <angle>` — Set max steering angle
- `/vehiclesteeringspeed <speed>` — Set steering responsiveness
- `/vehicleengineforce <force>` — Set engine power
- `/vehiclebrakingforce <force>` — Set braking power
- `/vehiclebrakebias <bias>` — Set brake bias
- `/vehiclesubsteps <steps>` — Set physics substeps
- `/vehicleweightbias <bias>` — Set front weight bias
- `/setsurfacetype <block> <surface>` — Map block to surface
- `/vehicledrivetrain <RWD/FWD/AWD>` — Set drivetrain
- `/defaultsurface <surface>` — Set default surface
- `/vehicletrackwidth <width>` — Set track width
- `/vehicledrag <drag>` — Set drag coefficient
- `/vehiclespeedsteeringfactor <factor>` — Set speed steering factor
- `/vehicleenginebraking <braking>` — Set engine braking
- `/vehiclerollstiffness <ratio>` — Set roll stiffness

Note: The following four-wheel model parameters have no singleplayer command and can only be set via server packets: AWD front split (53), front/rear differential (54, 55), LSD locking coefficient (56), downforce coefficient/bias (57, 58), weather condition (59).

#### CI/CD Workflows (`.github/workflows/`)
3 new workflow files:
- **`ci.yml`** — Runs `chiseledBuild` on every push/PR to `main` branch. Validates that the mod compiles for all supported Minecraft versions.
- **`prerelease-build.yml`** — Triggered on pre-release creation. Builds mod and uploads JAR artifacts to the GitHub release.
- **`release-build.yml`** — Triggered on full release creation. Builds mod, uploads artifacts, and attaches JARs to the release.

### Modified

#### `OpenBoatUtils.java`
- **VERSION**: Kept at `18` (matching base OpenBoatUtils protocol; new packets extend beyond the base range)
- **`sendVersionPacket()`**: Added `writeBoolean(true)` after version int to identify this mod as the Realistic variant to compatible server plugins
- **`resetSettings()`**: Now also resets `fourWheelPhysics` and `SurfaceProperties.resetBlockSurfaceMap()`
- **New static field**: `fourWheelPhysics` (FourWheelPhysicsEngine instance)
- **New static fields**: `visualRollAngle` and `visualSteeringAngle` (`volatile float`) for thread-safe sharing of visual state between game tick and render thread
- **New methods** for setting all realistic physics parameters: `setRealisticPhysicsEnabled()`, `setVehicleType()`, `setVehicleConfig()`, `setVehicleMass()`, `setVehicleWheelbase()`, `setVehicleCgHeight()`, `setVehicleTrackWidth()`, `setVehicleMaxSteering()`, `setVehicleSteeringSpeed()`, `setVehicleBrakingForce()`, `setVehicleEngineForce()`, `setVehicleDragCoefficient()`, `setVehicleBrakeBias()`, `setVehicleSubsteps()`, `setVehicleFrontWeightBias()`, `setBlockSurfaceType()`, `setVehicleDrivetrain()`, `setDefaultSurfaceType()`, `setVehicleSpeedSteeringFactor()`, `setVehicleEngineBraking()`, `setVehicleRollStiffnessRatio()`, `resetRealisticPhysics()`, `setAwdFrontSplit()`, `setFrontDifferential()`, `setRearDifferential()`, `setLsdLockingCoeff()`, `setDownforceCoefficient()`, `setDownforceFrontBias()`, `setWeatherCondition()`
- **New imports**: physics package classes

#### `BoatMixin.java`
- **Merged with AbstractBoatMixin**: Now handles both `BoatEntity` (<=1.21) and `AbstractBoatEntity` (>=1.21.3) using Stonecutter conditional comments and the Stonecutter `/*$ boat >>*/` swap directive, eliminating the need for a separate `AbstractBoatMixin.java` in the 1.21.3 versions folder
- **`@Mixin` target**: Now conditionally targets `BoatEntity` (<=1.21) or `AbstractBoatEntity` (>=1.21.3) using Stonecutter
- **Shadow fields/methods**: All shadow declarations use Stonecutter conditions for the correct type per MC version
- **All hook methods**: `oncePerTick()`, `paddleHook()`, `tickHook()`, `hookCheckLocation()`, and all `@Redirect`/`@ModifyConstant` methods have version-conditional signatures for both `BoatEntity` and `AbstractBoatEntity`
- **`oncePerTick()`**: Added realistic physics engine integration:
  - When `fourWheelPhysics.isEnabled()` and boat is on ground (or in air with airControl), computes full physics update each tick
  - Reads player keyboard input (left/right for steering, forward for throttle, back for braking, jump for handbrake)
  - Applies computed velocity and yaw delta to the boat entity
  - Computes visual pitch (nose dips when braking, rises when accelerating) and roll contribution
  - Jump key is repurposed as handbrake when realistic physics is active
  - Original jump behavior is suppressed when realistic physics is enabled
- **`interpolationStepsHook()`**: Moved from the deleted `AbstractBoatMixin.java` into `BoatMixin.java` with `>=1.21.3` Stonecutter guard. Sets interpolation steps to 10 when `interpolationCompat` is enabled
- **New import**: `FourWheelPhysicsEngine`

#### `ClientboundPackets.java`
- **27 new enum values** for realistic physics packets (SET_REALISTIC_PHYSICS through SET_WEATHER_CONDITION)
- **`handlePacket()`**: 27 new switch cases (33–59) handling all new packet types

#### `Modes.java`
- **7 new enum values**: `REALISTIC` (25) through `REALISTIC_ALLTERRAIN` (31)
- **New import**: `VehicleType`
- **`setMode()`**: 7 new switch cases configuring realistic physics with appropriate vehicle types

#### `ServerPlayNetworkHandlerMixin.java`
- **Added `remap = false`** to the `@Redirect` annotation on `preventMovedWronglyLog`. This is a correctness fix — `Logger.warn()` is not a Minecraft method and should not be remapped by the mixin processor.

#### `ServerboundPackets.java`
- **`handlePacket()`**: VERSION packet handler now reads the optional `boolean` realistic mod identifier if present in the buffer, using `buf.isReadable()` to maintain backward compatibility with standard OpenBoatUtils clients

#### `openboatutils.mixins.json5`
- **Removed Stonecutter conditionals** around `BoatMixin` — it is now included unconditionally for all versions since the version-specific logic was moved inside the mixin itself using Stonecutter comments
- **Removed `AbstractBoatMixin`** reference — no longer needed since `BoatMixin` handles all versions
- **Added `BoatEntityRendererMixin`** — client-side mixin for rendering visual effects (roll, wheels)

### Removed

#### `versions/1.21.3/src/main/java/dev/o7moon/openboatutils/mixin/AbstractBoatMixin.java`
Deleted (263 lines). This was a separate mixin file for Minecraft 1.21.3+ that mixed into `AbstractBoatEntity` (Mojang renamed `BoatEntity` to `AbstractBoatEntity` in 1.21.3). Its functionality was merged into the main `BoatMixin.java` using Stonecutter conditional comments, keeping all version-specific logic in one file.

---

## Multiplayer Compatibility

### Server Plugin Compatibility
All new features follow the OpenBoatUtils protocol design:
- **No modifications without server packet**: The realistic physics engine is disabled by default. It only activates when the server sends packet 33 (`SET_REALISTIC_PHYSICS`) or packet 8 (`SET_MODE`) with a realistic mode.
- **`enabled` flag**: All setter methods set `OpenBoatUtils.enabled = true`. The mixin checks this flag before applying any modifications.
- **Reset on disconnect**: `resetSettings()` resets all physics state including the new engines, ensuring no state persists across server connections.

### Version Identification
- The version packet now includes an optional boolean to identify the Realistic variant
- Standard OpenBoatUtils servers will simply ignore the extra byte
- Compatible server plugins can read the boolean to detect the Realistic mod variant

### Protocol Extension
Packets 0–32 remain identical to the original OpenBoatUtils protocol. New packets 33–59 extend the protocol without breaking existing functionality.
