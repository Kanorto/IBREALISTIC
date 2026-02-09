# OpenBoatUtilsRealistic — User Guide

A comprehensive guide to the realistic vehicle physics system in OpenBoatUtilsRealistic.

---

## Table of Contents
- [What is this?](#what-is-this)
- [Quick Start](#quick-start)
- [Controls](#controls)
- [Vehicle Types](#vehicle-types)
- [Surface Types](#surface-types)
- [Weather Conditions](#weather-conditions)
- [Drivetrain Types](#drivetrain-types)
- [Differential Types](#differential-types)
- [Vehicle Tuning Guide](#vehicle-tuning-guide)
- [Singleplayer Commands](#singleplayer-commands)
- [For Server Administrators](#for-server-administrators)

---

## What is this?

OpenBoatUtilsRealistic adds a realistic four-wheel vehicle physics simulation to Minecraft boats. When enabled by a server (or in singleplayer via commands), your boat behaves like a real car with:

- **Realistic steering** — The boat turns based on tire slip angles, not just yaw rotation
- **Weight transfer** — Braking shifts weight forward, accelerating shifts it backward, cornering shifts it sideways
- **Surface-dependent grip** — Different blocks have different friction (ice is slippery, concrete has good grip, dirt is loose)
- **Drifting** — Use the handbrake to lock rear wheels and initiate controlled drifts
- **Multiple vehicle types** — From nimble WRC cars to heavy trucks

All features are **server-controlled**. The mod does nothing unless the server activates it via packets.

---

## Quick Start

### In Singleplayer
1. Install the mod
2. Open a singleplayer world
3. Place a boat and sit in it
4. Type `/realisticphysics true` to enable the physics engine
5. Type `/vehicletype WRC_CAR` to set the vehicle type
6. Drive!

### On a Server
The server administrator enables realistic physics via their server plugin. When you join, the physics settings are automatically applied. No configuration needed on your end.

---

## Controls

When realistic physics is active, boat controls change:

| Key | Action |
|-----|--------|
| **W** | Throttle (accelerate forward) |
| **S** | Brake |
| **A** | Steer left |
| **D** | Steer right |
| **Space** | Handbrake (locks rear wheels for drifting) |

### Key Differences from Normal Boats
- **No jumping**: Space is repurposed as handbrake. Boats cannot jump in realistic mode.
- **Steering is speed-dependent**: At high speed, steering becomes less responsive (like a real car).
- **You can drift**: Hold Space while turning to initiate a controlled drift.
- **Braking shifts weight**: When you brake hard, the front tires get more grip and the rear tires get less.
- **Visual tilt**: The boat visually tilts forward when braking and backward when accelerating.

---

## Vehicle Types

Five preset vehicle configurations are available:

### WRC Car (`WRC_CAR`)
Modern World Rally Championship car. The default and most balanced choice.
- **Mass**: 1190 kg
- **Drivetrain**: AWD (All-Wheel Drive)
- **Character**: Balanced, good traction, predictable handling
- **Best for**: General rally driving, beginners

### Group B (`GROUP_B`)
Legendary Group B rally car. High power, rear-wheel drive.
- **Mass**: 1100 kg
- **Drivetrain**: RWD (Rear-Wheel Drive)
- **Character**: Oversteer-prone, spectacular drifts, requires skill
- **Best for**: Experienced drivers who want excitement

### Classic Rally (`CLASSIC_RALLY`)
Classic era rally car. Lower power, higher center of gravity.
- **Mass**: 1000 kg
- **Drivetrain**: RWD
- **Character**: Gentle power delivery, forgiving but slower
- **Best for**: Casual driving, scenic routes

### Lightweight (`LIGHTWEIGHT`)
Small, nimble front-wheel drive car.
- **Mass**: 800 kg
- **Drivetrain**: FWD (Front-Wheel Drive)
- **Character**: Understeer-prone, excellent traction on climbs, very responsive
- **Best for**: Tight tracks, technical courses

### Truck (`TRUCK`)
Heavy all-terrain truck.
- **Mass**: 2000 kg
- **Drivetrain**: AWD
- **Character**: Slow steering, massive braking force, stable at speed
- **Best for**: Off-road courses, obstacles

---

## Surface Types

Different Minecraft blocks give different grip and handling characteristics:

### High Grip Surfaces
| Surface | Blocks | Characteristics |
|---------|--------|-----------------|
| **Asphalt (Dry)** | Stone, Polished stone variants, Obsidian, Quartz, Ores, Sandstone | Best grip (0.85), fast, predictable |
| **Concrete** | All colored concrete blocks | Very good grip (0.80), fast |
| **Brick** | Bricks, Nether bricks, Deepslate bricks, Mud bricks | Good grip (0.75) |

### Medium Grip Surfaces
| Surface | Blocks | Characteristics |
|---------|--------|-----------------|
| **Metal** | Iron, Gold, Copper, Diamond, Emerald blocks | Medium-high grip (0.70), very low rolling resistance |
| **Wool** | All wool and carpet colors, Moss | High grip (0.70) but slow (high rolling resistance) |
| **Asphalt (Wet)** | Cobblestone, Mossy variants, Andesite, Diorite, Granite, Tuff, Prismarine | Reduced grip (0.55), longer braking distances |
| **Terracotta** | All terracotta and glazed terracotta | Medium grip (0.65) |
| **Wood** | All planks, logs, stripped wood, Shulker boxes | Medium grip (0.50), moderate rolling resistance |

### Low Grip Surfaces
| Surface | Blocks | Characteristics |
|---------|--------|-----------------|
| **Gravel** | Gravel | Low grip (0.55), loose, high rolling resistance |
| **Nether** | Netherrack, Basalt, Magma, Glowstone | Medium-low grip (0.50) |
| **Dirt** | Dirt, Grass, Clay, Farmland, Podzol, Packed mud | Low grip (0.45), loose |
| **Vegetation** | Hay, Dried kelp, Sponge, Slime block, Honey block | Soft, very slow |
| **Sand** | Sand, Red sand, Concrete powder | Low grip (0.40), very high rolling resistance |

### Very Low Grip Surfaces
| Surface | Blocks | Characteristics |
|---------|--------|-----------------|
| **Glass** | All glass and glass panes, Sea lantern | Very low grip (0.35), slippery |
| **Snow** | Snow, Snow block, Powder snow | Very low grip (0.30) |
| **Mud** | Mud, Soul sand, Soul soil | Very low grip (0.30), extremely slow |
| **Ice** | Ice, Packed ice, Frosted ice | Minimal grip (0.10), extremely slippery |
| **Blue Ice** | Blue ice | Almost no grip (0.06), fastest sliding |

### Unmapped Blocks
Any block not in the list above uses the **default surface** (Asphalt Dry by default). The server can change this.

---

## Weather Conditions

Weather affects grip globally. The server can change this dynamically.

| Condition | Grip Multiplier | Effect |
|-----------|----------------|--------|
| **Clear** | 100% | Normal conditions |
| **Rain** | 70% | Reduced grip, slower tire response |
| **Heavy Rain** | 50% | Severely reduced grip, much slower tire response |
| **Snow** | 40% | Very low grip globally |
| **Fog** | 95% | Minimal physics effect (mainly visual) |

---

## Drivetrain Types

The drivetrain determines which wheels receive engine power.

### RWD (Rear-Wheel Drive)
- Only rear wheels are powered
- Prone to **oversteer** (rear slides out in corners)
- Great for drifting
- Used by Group B and Classic Rally presets

### FWD (Front-Wheel Drive)
- Only front wheels are powered
- Prone to **understeer** (car goes straight when you turn)
- Good traction going uphill
- Used by Lightweight preset

### AWD (All-Wheel Drive)
- All four wheels are powered
- Configurable front/rear torque split (default 50/50)
- Best overall traction
- Used by WRC Car and Truck presets

---

## Differential Types

Differentials control how torque is distributed between the two wheels on the same axle.

### Open Differential
- Torque goes to the wheel with least resistance
- If one wheel loses grip, that wheel spins while the other gets no power
- Realistic but can cause loss of traction

### Locked Differential
- Both wheels rotate at the same speed
- Maximum traction (both wheels always get power)
- Can cause understeer in corners (fights turning)

### LSD (Limited Slip Differential)
- Partially locks when one wheel spins faster than the other
- Configurable locking coefficient (0.0 = fully open, 1.0 = fully locked)
- Default: 0.3 (moderate locking)
- Best compromise between traction and handling

---

## Vehicle Tuning Guide

For advanced users who want to fine-tune their vehicle.

### Mass
- **Effect**: Heavier = more stable but slower to accelerate/brake
- **Range**: 100 – 5000 kg
- **Default**: 1190 kg (WRC Car)
- **Tip**: Lower mass makes the car more responsive but harder to control

### Wheelbase
- **Effect**: Longer = more stable at speed, slower to turn
- **Range**: 1.0 – 5.0 m
- **Default**: 2.53 m
- **Tip**: Shorter wheelbase = sharper turns, longer = highway stability

### CG Height (Center of Gravity)
- **Effect**: Higher = more weight transfer in corners (more body roll)
- **Range**: 0.1 – 2.0 m
- **Default**: 0.45 m
- **Tip**: Lower CG = less body roll, more stable. Higher CG = more dramatic weight transfer

### Track Width
- **Effect**: Wider = more lateral stability, less body roll
- **Range**: 1.0 – 3.0 m
- **Default**: 1.55 m

### Front Weight Bias
- **Effect**: Higher = more weight on front wheels (better front grip)
- **Range**: 0.3 – 0.7
- **Default**: 0.55 (55% front)
- **Tip**: Front-heavy = understeer tendency, rear-heavy = oversteer tendency

### Max Steering Angle
- **Effect**: Larger = tighter turns possible
- **Range**: 0.1 – 1.2 radians (~6° – 69°)
- **Default**: 0.50 rad (~29°)

### Steering Speed
- **Effect**: Faster = quicker steering response
- **Range**: 0.5 – 10.0
- **Default**: 5.0

### Speed Steering Factor
- **Effect**: Higher = more steering reduction at high speed
- **Range**: 0.0 – 0.1
- **Default**: 0.004
- **Tip**: Increase if the car feels twitchy at high speed

### Engine Force
- **Effect**: More force = faster acceleration
- **Range**: 1000 – 20000 N
- **Default**: 5500 N (WRC Car)

### Braking Force
- **Effect**: More force = shorter braking distance
- **Range**: 1000 – 20000 N
- **Default**: 8000 N

### Brake Bias
- **Effect**: Higher = more braking on front wheels
- **Range**: 0.0 – 1.0
- **Default**: 0.65 (65% front)
- **Tip**: Front-biased braking is more stable. Rear-biased braking helps initiate trail-braking drifts

### Engine Braking
- **Effect**: Simulates engine resistance when not accelerating
- **Range**: 0 – 5000 N
- **Default**: 800 N

### Drag Coefficient
- **Effect**: Higher = more air resistance at high speed
- **Range**: 0.0 – 2.0
- **Default**: 0.35

### Substeps
- **Effect**: More substeps = more accurate physics (but higher CPU cost)
- **Range**: 1 – 10
- **Default**: 4
- **Tip**: 4 is good for most cases. Increase to 6-8 only if you notice physics glitches

### Roll Stiffness Ratio
- **Effect**: How much anti-roll bar stiffness is on the front vs rear
- **Range**: 0.0 – 1.0
- **Default**: 0.55 (55% front)

### Downforce
- **Effect**: Increases grip at high speed (proportional to speed²)
- **Default Coefficient**: 0.5
- **Default Front Bias**: 0.4 (40% front, 60% rear)

### LSD Locking Coefficient
- **Effect**: How aggressively the LSD locks (only matters with LSD differential type)
- **Range**: 0.0 – 1.0
- **Default**: 0.3

---

## Singleplayer Commands

All commands are available in singleplayer for testing. They send packets through the standard server→client pipeline, so behavior is identical to multiplayer.

### Basic Commands
```
/reset                            — Reset all settings to vanilla
/realisticphysics <true/false>    — Enable/disable realistic physics
/vehicletype <type>               — Set vehicle preset (WRC_CAR, GROUP_B, CLASSIC_RALLY, LIGHTWEIGHT, TRUCK)
```

### Vehicle Parameters
```
/vehiclemass <100-5000>           — Set vehicle mass (kg)
/vehiclewheelbase <1-5>           — Set wheelbase (m)
/vehiclecgheight <0.1-2>         — Set center of gravity height (m)
/vehicletrackwidth <1-3>          — Set track width (m)
/vehicleweightbias <0.3-0.7>     — Set front weight bias
/vehiclemaxsteering <0.1-1.2>    — Set max steering angle (rad)
/vehiclesteeringspeed <0.5-10>   — Set steering responsiveness
/vehicleengineforce <1000-20000>  — Set engine power (N)
/vehiclebrakingforce <1000-20000> — Set braking power (N)
/vehiclebrakebias <0-1>          — Set brake bias (0=rear, 1=front)
/vehicleenginebraking <0-5000>   — Set engine braking (N)
/vehicledrag <0-2>               — Set drag coefficient
/vehiclesubsteps <1-10>          — Set physics substeps per tick
/vehiclespeedsteeringfactor <0-0.1> — Set speed-dependent steering reduction
/vehiclerollstiffness <0-1>      — Set front roll stiffness ratio
```

### Drivetrain & Differentials
```
/vehicledrivetrain <RWD/FWD/AWD> — Set drivetrain type
```

> **Note**: The following parameters can only be set by a server plugin (no singleplayer command available):
> AWD front/rear split, front/rear differential type, LSD locking coefficient, downforce coefficient, downforce front bias, weather condition.

### Surface Configuration
```
/setsurfacetype <block> <surface> — Map a block to a surface type
/defaultsurface <surface>         — Set default surface for unmapped blocks
```

Available surface names: `ASPHALT_DRY`, `ASPHALT_WET`, `GRAVEL`, `DIRT`, `MUD`, `SNOW`, `ICE`, `SAND`, `WOOD`, `CONCRETE`, `TERRACOTTA`, `METAL`, `GLASS`, `WOOL`, `BRICK`, `NETHER`, `VEGETATION`

### Standard OpenBoatUtils Commands
All original OpenBoatUtils singleplayer commands also work:
```
/stepsize <size>                  — Set boat step height
/defaultslipperiness <value>      — Set default block slipperiness
/blockslipperiness <value> <blocks> — Set slipperiness for specific blocks
/aircontrol <true/false>          — Enable/disable air control
/waterelevation <true/false>      — Enable/disable water elevation
/falldamage <true/false>          — Enable/disable fall damage
/jumpforce <force>                — Set jump force
/boatmode <mode>                  — Set boat mode (e.g., RALLY, PARKOUR, REALISTIC)
/boatgravity <gravity>            — Set gravity force
/setyawaccel <accel>              — Set yaw acceleration
/setforwardaccel <accel>          — Set forward acceleration
/setbackwardaccel <accel>         — Set backward acceleration
/setturnforwardaccel <accel>      — Set turn-forward acceleration
/allowaccelstacking <true/false>  — Allow acceleration stacking
/underwatercontrol <true/false>   — Enable underwater control
/surfacewatercontrol <true/false> — Enable surface water control
/coyotetime <ticks>               — Set coyote time
/waterjumping <true/false>        — Enable water jumping
/swimforce <force>                — Set swim force
/collisionmode <0-5>              — Set collision mode
/stepwhilefalling <true/false>    — Allow stepping while falling
/setinterpolationten <true/false> — Set interpolation compatibility
/setcollisionresolution <1-50>    — Set collision resolution
```

---

## For Server Administrators

### Enabling Realistic Physics via Plugin

To enable realistic physics for a player, your server plugin needs to send the appropriate packets over the `openboatutils:settings` channel.

#### Quick Setup (Using Mode Packet)
Send packet ID 8 (SET_MODE) with mode ID 25 (REALISTIC) to instantly configure a player with default realistic settings:
```
short packetID = 8;    // SET_MODE
short modeID = 25;     // REALISTIC
```

#### Custom Setup (Individual Packets)
For fine-tuned control, send individual configuration packets:
```
1. Send packet 33 (SET_REALISTIC_PHYSICS) with boolean true
2. Send packet 34 (SET_VEHICLE_TYPE) with short 0 (WRC_CAR)
3. Optionally send packets 35-59 to customize individual parameters
```

#### Reset
Send packet 0 (RESET) to disable all modifications and return to vanilla behavior.

### Version Detection
The Realistic mod sends a version packet with an extra boolean flag set to `true`. Your server plugin can detect this by reading the boolean after the version int if data is available in the buffer.
