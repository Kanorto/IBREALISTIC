# Связи между компонентами

---

## 1. Клиент-серверный протокол

### Поток данных: Применение режима
```
Игрок входит на трассу
  → TSListener (плагин) детектирует регион START
    → BoatUtilsManager.sendBoatUtilsModePluginMessage()
      → [Стандартный режим] отправляет SET_MODE пакет (ID 8)
      → [Кастомный режим] CustomBoatUtilsMode.applyToPlayer()
        → resetPlayer() — отправляет RESET (ID 0)
        → finallyApplyToPlayer() — серия пакетов для каждой нестандартной настройки
          → sendShortAndFloatPacket() / sendShortAndBooleanPacket() / etc.
            → Player.sendPluginMessage("openboatutils:settings", bytes)

Клиент получает пакет:
  → PluginMessageReceiver (мод) → ClientboundPackets.handlePacket()
    → switch по ordinal пакета
      → вызов соответствующего сеттера в OpenBoatUtils
        → [Если реалистичная физика] OpenBoatUtils.realisticPhysics.setConfig/setEnabled
```

### Поток данных: Физика (каждый тик)
```
BoatMixin.oncePerTick() / AbstractBoatMixin.oncePerTick()
  → Проверка: realisticPhysics.isEnabled()?
    → ДА: realisticPhysics.update(boat, steering, throttle, brake, handbrake)
      → detectSurface(boat) — определение блоков под лодкой
        → SurfaceProperties.getSurfaceForBlock(blockId)
      → [substeps цикл]
        → TireModel.computeSlipAngle() — углы проскальзывания
        → TireModel.computeLateralForce() — боковые силы (Fiala model)
        → TireModel.computeLongitudinalForce() — продольные силы
        → TireModel.applyFrictionCircle() — ограничение сил
        → TireModel.computeEffectiveMu() — эффективный μ
        → Интегрирование vx, vy, yawRate
      → PhysicsResult(velocityX, Y, Z, yawDelta, ...)
    → boat.setVelocity(result.velocityX, result.velocityY, result.velocityZ)
    → boat.setYaw(boat.getYaw() + result.yawDelta)
  → НЕТ: стандартная логика OpenBoatUtils (скользкость, ускорения, прыжки)
```

### Поток данных: Версия клиента
```
Клиент подключается к серверу:
  → OpenBoatUtilsClient.onInitializeClient()
    → ClientPlayConnectionEvents.JOIN
      → OpenBoatUtils.sendVersionPacket()
        → ServerboundPackets.VERSION (ordinal 0)
          → PacketByteBuf: [short: 0, int: VERSION(19)]
            → sendPacketC2S()

Сервер получает:
  → PluginMessageReceiver.onPluginMessageReceived()
    → BoatUtilsManager.pluginMessageListener()
      → Чтение версии клиента
        → TPlayer.setBoatUtilsVersion(version)
```

---

## 2. Таблица Packet ID (мод ↔ плагин)

### Соответствие: ClientboundPackets (мод) ↔ CustomBoatUtilsMode (плагин)

| ID | Мод (ClientboundPackets) | Плагин (PACKET_ID_*) | Тип данных |
|----|--------------------------|----------------------|------------|
| 0 | RESET | PACKET_ID_RESET | — |
| 1 | SET_STEP_HEIGHT | PACKET_ID_SET_STEP_HEIGHT | float |
| 2 | SET_DEFAULT_SLIPPERINESS | PACKET_ID_SET_DEFAULT_SLIPPERINESS | float |
| 3 | SET_BLOCKS_SLIPPERINESS | PACKET_ID_SET_BLOCKS_SLIPPERINESS | float + string |
| 4 | SET_BOAT_FALL_DAMAGE | PACKET_ID_SET_BOAT_FALL_DAMAGE | boolean |
| 5 | SET_BOAT_WATER_ELEVATION | PACKET_ID_SET_BOAT_WATER_ELEVATION | boolean |
| 6 | SET_AIR_CONTROL | PACKET_ID_SET_AIR_CONTROL | boolean |
| 7 | SET_BOAT_JUMP_FORCE | PACKET_ID_SET_BOAT_JUMP_FORCE | float |
| 8 | SET_MODE | — (используется BoatUtilsMode ID) | short |
| 9 | SET_GRAVITY | PACKET_ID_SET_GRAVITY | double |
| 10 | SET_YAW_ACCEL | PACKET_ID_SET_YAW_ACCEL | float |
| 11 | SET_FORWARD_ACCEL | PACKET_ID_SET_FORWARD_ACCEL | float |
| 12 | SET_BACKWARD_ACCEL | PACKET_ID_SET_BACKWARD_ACCEL | float |
| 13 | SET_TURN_ACCEL | PACKET_ID_SET_TURN_ACCEL | float |
| 14 | ALLOW_ACCEL_STACKING | PACKET_ID_ALLOW_ACCEL_STACKING | boolean |
| 16 | SET_UNDERWATER_CONTROL | PACKET_ID_SET_UNDERWATER_CONTROL | boolean |
| 17 | SET_SURFACE_WATER_CONTROL | PACKET_ID_SET_SURFACE_WATER_CONTROL | boolean |
| 19 | SET_COYOTE_TIME | PACKET_ID_SET_COYOTE_TIME | int |
| 20 | SET_WATER_JUMPING | PACKET_ID_SET_WATER_JUMPING | boolean |
| 21 | SET_SWIM_FORCE | PACKET_ID_SET_SWIM_FORCE | float |
| 22 | REMOVE_BLOCKS_SLIPPERINESS | PACKET_ID_REMOVE_BLOCKS_SLIPPERINESS | string |
| 23 | CLEAR_SLIPPERINESS | PACKET_ID_CLEAR_SLIPPERINESS | — |
| 26 | SET_PER_BLOCK | PACKET_ID_SET_PER_BLOCK | short+float+string |
| 27 | SET_COLLISION_MODE | PACKET_ID_SET_COLLISION_MODE | short |
| 28 | SET_STEP_WHILE_FALLING | PACKET_ID_SET_STEP_WHILE_FALLING | boolean |
| 29 | SET_INTERPOLATION_COMPAT | PACKET_ID_SET_INTERPOLATION_COMPAT | boolean |
| 30 | SET_COLLISION_RESOLUTION | PACKET_ID_SET_COLLISION_RESOLUTION | byte |
| 33 | SET_REALISTIC_PHYSICS | PACKET_ID_SET_REALISTIC_PHYSICS | boolean |
| 34 | SET_VEHICLE_TYPE | PACKET_ID_SET_VEHICLE_TYPE | short |
| 35 | SET_VEHICLE_MASS | PACKET_ID_SET_VEHICLE_MASS | float |
| 36 | SET_VEHICLE_WHEELBASE | PACKET_ID_SET_VEHICLE_WHEELBASE | float |
| 37 | SET_VEHICLE_CG_HEIGHT | PACKET_ID_SET_VEHICLE_CG_HEIGHT | float |
| 38 | SET_VEHICLE_TRACK_WIDTH | PACKET_ID_SET_VEHICLE_TRACK_WIDTH | float |
| 39 | SET_VEHICLE_MAX_STEERING | PACKET_ID_SET_VEHICLE_MAX_STEERING | float |
| 40 | SET_VEHICLE_STEERING_SPEED | PACKET_ID_SET_VEHICLE_STEERING_SPEED | float |
| 41 | SET_VEHICLE_BRAKING_FORCE | PACKET_ID_SET_VEHICLE_BRAKING_FORCE | float |
| 42 | SET_VEHICLE_ENGINE_FORCE | PACKET_ID_SET_VEHICLE_ENGINE_FORCE | float |
| 43 | SET_VEHICLE_DRAG | PACKET_ID_SET_VEHICLE_DRAG | float |
| 44 | SET_VEHICLE_BRAKE_BIAS | PACKET_ID_SET_VEHICLE_BRAKE_BIAS | float |
| 45 | SET_VEHICLE_SUBSTEPS | PACKET_ID_SET_VEHICLE_SUBSTEPS | int |
| 46 | SET_VEHICLE_FRONT_WEIGHT_BIAS | PACKET_ID_SET_VEHICLE_FRONT_WEIGHT_BIAS | float |
| 47 | SET_BLOCK_SURFACE_TYPE | PACKET_ID_SET_BLOCK_SURFACE_TYPE | string+string |
| 48 | SET_VEHICLE_DRIVETRAIN | PACKET_ID_SET_VEHICLE_DRIVETRAIN | short |
| 49 | SET_DEFAULT_SURFACE_TYPE | PACKET_ID_SET_DEFAULT_SURFACE_TYPE | string |

---

## 3. Связи между файлами мода

### RealisticPhysicsEngine
- **Использует:** VehicleConfig, TireModel, SurfaceProperties, BoatEntity
- **Используется в:** BoatMixin.oncePerTick(), AbstractBoatMixin.oncePerTick()
- **Создаётся в:** OpenBoatUtils (static field), OpenBoatUtils.resetSettings()

### VehicleConfig
- **Используется в:** RealisticPhysicsEngine, OpenBoatUtils (сеттеры)
- **Создаётся:** VehicleType.toConfig(), VehicleConfig.createDefault()

### VehicleType
- **Используется в:** Modes.setMode(), OpenBoatUtils.setVehicleType()
- **Связь с плагином:** BoatUtilsMode enum значения REALISTIC_*

### TireModel
- **Используется только в:** RealisticPhysicsEngine.update()
- **Чистые функции:** не имеет состояния

### SurfaceProperties
- **Используется в:** RealisticPhysicsEngine.update(), TireModel (параметры)
- **Настраивается из:** OpenBoatUtils.setBlockSurfaceType(), OpenBoatUtils.setDefaultSurfaceType()

### ClientboundPackets
- **Вызывает:** OpenBoatUtils.set*(), Modes.setMode()
- **Вызывается из:** OpenBoatUtilsClient (registerHandlers)

### Modes
- **Вызывает:** OpenBoatUtils.set*(), VehicleType.toConfig()
- **Вызывается из:** ClientboundPackets.handlePacket()

---

## 4. Связи между файлами плагина

### CustomBoatUtilsMode
- **Использует:** BoatUtilsManager (для отправки), NonDefaultSetting, PerBlockSetting
- **Используется в:** CommandBoatUtilsModeEdit, BoatUtilsManager, Track
- **Хранится в:** JSON (БД), Track.customBoatUtilsModeId

### BoatUtilsManager
- **Использует:** BoatUtilsMode, CustomBoatUtilsMode, Track, TPlayer
- **Вызывается из:** TSListener, PluginMessageReceiver
- **Вызывает:** CustomBoatUtilsMode.applyToPlayer()

### Track → BoatUtilsMode
- Каждая трасса имеет `boatUtilsMode` и необязательный `customBoatUtilsModeId`
- При входе на трассу BoatUtilsManager отправляет соответствующий режим

### Heat → Driver → Lap
- Heat содержит HashMap<UUID, Driver>
- Driver содержит List<Lap>
- Lap содержит ArrayList<Instant> checkpoints

### Event → Round → Heat
- Event содержит EventSchedule с List<Round>
- Round содержит List<Heat>
- Иерархия: Event → Round (Qualification/Final) → Heat → Driver

---

## 5. Связь DRS с BoatUtils

```
DrsListener.onPlayerMove()
  → DrsManager.playerPassedDrsDetect(player, regionIndex)
    → [При совпадении условий]
      → DrsManager.activateDrs(player)
        → DrsManager.sendForwardAccelerationPacket(player, drsForwardAccel)
          → CustomBoatUtilsMode.sendShortAndFloatPacket(player, PACKET_ID_SET_FORWARD_ACCEL, value)

  → [При деактивации]
    → DrsManager.deactivateDrs(player)
      → DrsManager.resetToTrackSettings(player)
```
