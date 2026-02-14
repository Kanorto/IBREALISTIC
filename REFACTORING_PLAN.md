# Детальный план рефакторинга IBRealistic

## Приоритет: ВЫСШИЙ (выше PLAN.md)

## Дата: 2026-02-13

## Статус выполнения (обновлено 2026-02-14)

### ✅ ВЫПОЛНЕНО:
- **Фаза 1 (частично)**: Полная карта переменных — использована для рефакторинга
- **Фаза 2**: Двухканальная архитектура — реализована в плагине (уже было) и в моде (сделано)
- **Фаза 3.3**: Разделение пакетов — ClientboundPackets.java теперь обрабатывает только 33-69
- **Фаза 3.4**: Разделение IBRealistic.java — OBU-поля удалены, используется `dev.o7moon.openboatutils.OpenBoatUtils`
- **Фаза 3.5**: Зависимость от OBU — добавлена в fabric.mod.json и build.gradle
- **Фаза 4**: Плагин TimingSystem — уже имел двухканальную маршрутизацию
- **Пакет Java**: Изменён на `dev.kanorto.ibrealistic` (ранее)

### 🔲 ОСТАЛОСЬ:
- **Фаза 3.1**: Удаление дублирующих хуков — BoatMixin уже имеет только 5 уникальных хуков (сделано ранее)
- **Фаза 3.2**: EntityMixin и ServerPlayNetworkHandlerMixin — уже удалены (сделано ранее)
- **Фаза 5**: Тестирование в игре на всех MC версиях
- **Фаза 6**: Пограничные случаи — нуждаются в тестировании

---

## Краткое описание проблем

IBRealistic (мод) и TimingSystem (плагин) сейчас работают как **самостоятельный замена OBU**, а не как аддон. Это создаёт:

1. **Критические крашы** при установке рядом с OBU — дублирующие @ModifyConstant инъекции
2. **Дублирование кода** — 15 из 23 хуков в BoatMixin идентичны OBU
3. **Одноканальная архитектура** — ВСЕ пакеты (базовые 0-32 и реалистичные 33-69) идут через один канал `ibrealistic:settings`
4. **Конфликт Java-пакетов** — оба мода используют `dev.o7moon.openboatutils`

---

## ФАЗА 1: Полная карта переменных и их источников

### 1.1 Переменные OBU (базовые, ID 0-32)

Эти переменные **дублированы** в IBRealistic из OBU. Сейчас IBRealistic обрабатывает их самостоятельно, но должен делегировать OBU.

| Переменная | Тип | Дефолт | Пакет ID | Кто должен управлять | Текущее состояние |
|---|---|---|---|---|---|
| `enabled` | boolean | false | (любой пакет) | OBU | ⚠️ Дублируется — IBRealistic и OBU оба устанавливают |
| `stepSize` | float | 0f | 1 (SET_STEP_HEIGHT) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `defaultSlipperiness` | float | 0.6f | 2 (SET_DEFAULT_SLIPPERINESS) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `slipperinessMap` | HashMap | {} | 3 (SET_BLOCKS_SLIPPERINESS) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `fallDamage` | boolean | true | 4 (SET_BOAT_FALL_DAMAGE) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `waterElevation` | boolean | false | 5 (SET_BOAT_WATER_ELEVATION) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `airControl` | boolean | false | 6 (SET_AIR_CONTROL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `jumpForce` | float | 0f | 7 (SET_BOAT_JUMP_FORCE) | OBU → openboatutils:settings | ⚠️ Дублируется |
| MODE (составной) | — | — | 8 (SET_MODE) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `gravityForce` | double | -0.04 | 9 (SET_GRAVITY) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `yawAcceleration` | float | 1.0f | 10 (SET_YAW_ACCEL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `forwardsAcceleration` | float | 0.04f | 11 (SET_FORWARD_ACCEL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `backwardsAcceleration` | float | 0.005f | 12 (SET_BACKWARD_ACCEL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `turningForwardsAcceleration` | float | 0.005f | 13 (SET_TURN_ACCEL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `allowAccelStacking` | boolean | false | 14 (ALLOW_ACCEL_STACKING) | OBU → openboatutils:settings | ⚠️ Дублируется |
| (RESEND_VERSION) | — | — | 15 | OBU → openboatutils:settings | ⚠️ Дублируется |
| `underwaterControl` | boolean | false | 16 (SET_UNDERWATER_CONTROL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `surfaceWaterControl` | boolean | false | 17 (SET_SURFACE_WATER_CONTROL) | OBU → openboatutils:settings | ⚠️ Дублируется |
| SET_EXCLUSIVE_MODE | — | — | 18 | OBU → openboatutils:settings | ⚠️ Дублируется |
| `coyoteTime` | int | 0 | 19 (SET_COYOTE_TIME) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `waterJumping` | boolean | false | 20 (SET_WATER_JUMPING) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `swimForce` | float | 0f | 21 (SET_SWIM_FORCE) | OBU → openboatutils:settings | ⚠️ Дублируется |
| REMOVE_BLOCKS_SLIPPERINESS | — | — | 22 | OBU → openboatutils:settings | ⚠️ Дублируется |
| CLEAR_SLIPPERINESS | — | — | 23 | OBU → openboatutils:settings | ⚠️ Дублируется |
| MODE_SERIES | — | — | 24 | OBU → openboatutils:settings | ⚠️ Дублируется |
| EXCLUSIVE_MODE_SERIES | — | — | 25 | OBU → openboatutils:settings | ⚠️ Дублируется |
| `perBlockSettings` | HashMap | {} | 26 (SET_PER_BLOCK) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `collision` | CollisionMode | VANILLA | 27 (SET_COLLISION_MODE) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `canStepWhileFalling` | boolean | false | 28 (SET_STEP_WHILE_FALLING) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `interpolationCompat` | boolean | false | 29 (SET_INTERPOLATION_COMPAT) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `collisionResolution` | byte | 1 | 30 (SET_COLLISION_RESOLUTION) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `collision_filter` (add) | ArrayList | [] | 31 (ADD_COLLISION_ENTITYTYPE_FILTER) | OBU → openboatutils:settings | ⚠️ Дублируется |
| `collision_filter` (clear) | — | — | 32 (CLEAR_COLLISION_ENTITYTYPE_FILTER) | OBU → openboatutils:settings | ⚠️ Дублируется |

**Итого: 33 пакета дублируется**

### 1.2 Переменные IBRealistic (уникальные, ID 33-69)

Эти переменные **УНИКАЛЬНЫ** для IBRealistic и должны передаваться только через `ibrealistic:settings`.

| Переменная | Тип | Дефолт | Пакет ID | Назначение |
|---|---|---|---|---|
| `fourWheelPhysics.enabled` | boolean | false | 33 (SET_REALISTIC_PHYSICS) | Вкл/выкл 4-колёсную физику |
| `fourWheelPhysics.config (by type)` | VehicleConfig | WRC_CAR | 34 (SET_VEHICLE_TYPE) | Установить тип машины из пресета |
| `fourWheelPhysics.config.mass` | float | 1350f | 35 (SET_VEHICLE_MASS) | Масса машины в кг |
| `fourWheelPhysics.config.wheelbase` | float | 2.55f | 36 (SET_VEHICLE_WHEELBASE) | Колёсная база в метрах |
| `fourWheelPhysics.config.cgHeight` | float | 0.5f | 37 (SET_VEHICLE_CG_HEIGHT) | Высота центра масс |
| `fourWheelPhysics.config.trackWidth` | float | 1.5f | 38 (SET_VEHICLE_TRACK_WIDTH) | Ширина колеи |
| `fourWheelPhysics.config.maxSteeringAngle` | float | 0.60f | 39 (SET_VEHICLE_MAX_STEERING) | Макс. угол поворота руля (рад) |
| `fourWheelPhysics.config.steeringSpeed` | float | 0.08f | 40 (SET_VEHICLE_STEERING_SPEED) | Скорость поворота руля |
| `fourWheelPhysics.config.brakingForce` | float | 25000f | 41 (SET_VEHICLE_BRAKING_FORCE) | Сила торможения (Н) |
| `fourWheelPhysics.config.engineForce` | float | 8000f | 42 (SET_VEHICLE_ENGINE_FORCE) | Сила двигателя (Н) |
| `fourWheelPhysics.config.dragCoefficient` | float | 0.35f | 43 (SET_VEHICLE_DRAG) | Аэродин. сопротивление (Cd) |
| `fourWheelPhysics.config.brakeBias` | float | 0.60f | 44 (SET_VEHICLE_BRAKE_BIAS) | Распределение тормозов (перед/зад) |
| `fourWheelPhysics.config.substeps` | int | 4 | 45 (SET_VEHICLE_SUBSTEPS) | Количество подшагов физики (1-10) |
| `fourWheelPhysics.config.frontWeightBias` | float | 0.55f | 46 (SET_VEHICLE_FRONT_WEIGHT_BIAS) | Развесовка (доля массы спереди) |
| blockSurfaceMap (per-block) | — | — | 47 (SET_BLOCK_SURFACE_TYPE) | Тип поверхности для конкретного блока |
| defaultSurface | SurfaceProperties | ASPHALT_DRY | 48 (SET_DEFAULT_SURFACE_TYPE) | Поверхность по умолчанию |
| `fourWheelPhysics.config.drivetrain` | DrivetrainType | AWD | 49 (SET_VEHICLE_DRIVETRAIN) | Тип привода (FWD/RWD/AWD) |
| `fourWheelPhysics.config.speedSteeringFactor` | float | 0.5f | 50 (SET_VEHICLE_SPEED_STEERING) | Зависимость руля от скорости |
| `fourWheelPhysics.config.engineBraking` | float | 2000f | 51 (SET_VEHICLE_ENGINE_BRAKING) | Торможение двигателем (Н) |
| `fourWheelPhysics.config.rollStiffnessRatioFront` | float | 0.55f | 52 (SET_VEHICLE_ROLL_STIFFNESS) | Жёсткость подвески на крен |
| `fourWheelPhysics.config (full)` | VehicleConfig | — | 53 (SET_VEHICLE_CONFIG) | Полная конфигурация (бинарная) |
| `fourWheelPhysics.config.awdFrontSplit` | float | 0.5f | 54 (SET_AWD_FRONT_SPLIT) | Распределение тяги AWD (0=зад, 1=перед) |
| `fourWheelPhysics.config.frontDifferential` | DifferentialType | OPEN | 55 (SET_FRONT_DIFFERENTIAL) | Тип переднего дифференциала |
| `fourWheelPhysics.config.rearDifferential` | DifferentialType | OPEN | 56 (SET_REAR_DIFFERENTIAL) | Тип заднего дифференциала |
| `fourWheelPhysics.config.lsdLockingCoeff` | float | 0.3f | 57 (SET_LSD_LOCKING_COEFF) | Коэффициент блокировки LSD (0-1) |
| `fourWheelPhysics.config.downforceCoefficient` | float | 0.0f | 58 (SET_DOWNFORCE_COEFFICIENT) | Прижимная сила (коэфф.) |
| `fourWheelPhysics.config.downforceFrontBias` | float | 0.5f | 59 (SET_DOWNFORCE_FRONT_BIAS) | Распределение прижимной силы |
| weather | WeatherCondition | CLEAR | 60 (SET_WEATHER) | Текущие погодные условия |
| `fourWheelPhysics.config.steeringReturnRate` | float | 0.15f | 61 (SET_STEERING_RETURN_RATE) | Скорость возврата руля в центр |
| (server info) | — | — | 61 (REALISTIC_SERVER_INFO) | Версия, features, название сервера |
| `fourWheelPhysics.config.tirePreset` | TirePreset | STANDARD | 62 (SET_TIRE_PRESET) | Пресет шин |
| `fourWheelPhysics.config.suspensionPreset` | SuspensionPreset | RALLY | 63 (SET_SUSPENSION_PRESET) | Пресет подвески |
| `fourWheelPhysics.config.enginePreset` | EnginePreset | TURBO_WRC | 64 (SET_ENGINE_PRESET) | Пресет двигателя |
| `fourWheelPhysics.config.bodyPreset` | BodyPreset | SPORT | 65 (SET_BODY_PRESET) | Пресет кузова |
| `fourWheelPhysics.config.steeringPreset` | SteeringPreset | SPORT | 66 (SET_STEERING_PRESET) | Пресет рулевого управления |
| `fourWheelPhysics.config.brakePreset` | BrakePreset | SPORT | 67 (SET_BRAKE_PRESET) | Пресет тормозов |
| `fourWheelPhysics.config.weightDistributionPreset` | WeightDistributionPreset | BALANCED | 68 (SET_WEIGHT_DISTRIBUTION_PRESET) | Пресет развесовки |
| countdown (goTimeMs + seconds) | long + int | 0 | 69 (SET_RACE_COUNTDOWN) | Обратный отсчёт гонки |

### 1.3 Переменные IBRealistic (визуальные, без пакетов)

Эти переменные не имеют пакетов — они рассчитываются на клиенте:

| Переменная | Тип | Назначение | Откуда берётся |
|---|---|---|---|
| `visualRollAngle` | float | Угол крена для рендерера | FourWheelPhysicsEngine.getRollAngle() |
| `visualSteeringAngle` | float | Угол руля для рендерера | FourWheelPhysicsEngine.getSteeringAngle() |
| `visualHandbrake` | boolean | Состояние ручника | Клавиша Space в реалистичном режиме |
| `realisticDebugHud` | boolean | Показать debug HUD | Команда /ibrealistic debug |

---

## ФАЗА 2: Архитектура двух каналов

### 2.1 Текущая архитектура (ПРОБЛЕМА)

```
TimingSystem (плагин)
  │
  ├─ ВСЕ пакеты (0-69) ──► ibrealistic:settings ──► IBRealistic (мод)
  │                                                    ├─ Обрабатывает OBU-пакеты (0-32) САМОСТОЯТЕЛЬНО
  │                                                    └─ Обрабатывает реалистичные (33-69)
  │
  └─ openboatutils:settings: НЕ ИСПОЛЬЗУЕТСЯ
```

### 2.2 Целевая архитектура (РЕШЕНИЕ)

```
TimingSystem (плагин)
  │
  ├─ OBU-пакеты (0-32) ──► openboatutils:settings ──► OBU (мод)
  │                                                     ├─ Обрабатывает ВСЮ базовую физику
  │                                                     └─ Хуки через @Redirect
  │
  └─ Реалистичные (33-69) ──► ibrealistic:settings ──► IBRealistic (аддон к OBU)
                                                        ├─ НЕ дублирует OBU-хуки
                                                        ├─ Читает OBU-поля напрямую
                                                        └─ Добавляет 4-колёсную физику
```

### 2.3 Что это даёт

1. **Нет конфликтов** — IBRealistic не хукает те же точки что OBU
2. **Совместимость** — можно использовать OBU без IBRealistic (базовый TimingSystem)
3. **Чистая архитектура** — каждый мод отвечает только за свой функционал
4. **Дефолтное ванильное поведение** — когда реалистичная физика не активна, IBRealistic вообще не вмешивается

---

## ФАЗА 3: Рефакторинг мода IBRealistic

### 3.1 Удаление дублирующих хуков из BoatMixin.java

**УДАЛИТЬ (15 хуков):**

| # | Хук | Аннотация | Почему удаляется |
|---|------|-----------|------------------|
| 1 | `paddleHook` | @WrapOperation | OBU @Redirect делает то же |
| 2 | `tickHook` (OBU-часть) | @WrapOperation | OBU @Redirect делает то же |
| 3 | `getFriction` | @WrapOperation | OBU @Redirect делает то же |
| 4 | `redirectYawVelocityIncrement` | @WrapOperation | OBU @Redirect делает то же |
| 5 | `forwardsAccel` | @ModifyConstant | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 6 | `turnAccel` | @ModifyConstant | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 7 | `backwardsAccel` | @ModifyConstant | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 8 | `pressingForwardHook` | @WrapOperation | OBU @Redirect делает то же |
| 9 | `pressingBackHook` | @WrapOperation | OBU @Redirect делает то же |
| 10 | `velocityDecayHook1` | @WrapOperation | OBU @Redirect делает то же |
| 11 | `velocityDecayHook2` | @WrapOperation | OBU @Redirect делает то же |
| 12 | `velocityDecayHook3` | @WrapOperation | OBU @Redirect делает то же |
| 13 | `canCollideHook` | @Inject | OBU @Inject делает то же |
| 14 | `fallHook` | @Inject | OBU @Inject делает то же |
| 15 | `updateVelocityHook` | @ModifyVariable | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 16 | `onGetGravity` | @Inject | OBU @Inject делает то же |
| 17 | `interpolationStepsHook` | @ModifyVariable | OBU @ModifyVariable делает то же |

**ПЕРЕДЕЛАТЬ (3 хука):**

| # | Хук | Что делать |
|---|------|-----------|
| 1 | `tickHook/hookCheckLocation/oncePerTick` | Заменить на `@Inject(method="tick", at=@At("HEAD"))` → вызов только реалистичной физики (FourWheelPhysicsEngine, visual state, debug HUD). Удалить всю OBU-логику (stepHeight, waterElevation, airControl, swimForce, coyoteTime, jumpForce) |
| 2 | `moveHook` | Убрать collisionResolution (OBU обрабатывает). Оставить ТОЛЬКО landing speed preservation. При `!enabled` → `original.call()` для делегации OBU |
| 3 | `hookCheckLocation (helper)` | Полностью удалить — OBU делает всё что нужно |

**ОСТАВИТЬ (3 хука):**

| # | Хук | Почему |
|---|------|--------|
| 1 | `liftPassenger` | Уникальная функциональность IBRealistic |
| 2 | `BoatEntityRendererMixin` (весь файл) | Визуальные колёса и руль |
| 3 | `SURFACE_NAMES` + debug | Уникальный debug HUD (вынести из миксина в утилитный класс) |

### 3.2 Удаление дублирующих миксин-файлов

| Файл | Действие |
|------|----------|
| `ClientWorldMixin.java` | **ПЕРЕДЕЛАТЬ** — сбрасывать только IBRealistic-поля (fourWheelPhysics, visual*, countdown*), НЕ вызывать OpenBoatUtils.resetSettings() (OBU делает это сам) |
| `EntityMixin.java` | **УДАЛИТЬ** — OBU обрабатывает stepHeight полностью |
| `ServerPlayNetworkHandlerMixin.java` | **УДАЛИТЬ** — OBU отключает анти-чит проверки |

### 3.3 Разделение пакетов на два канала (ClientboundPackets.java)

**Текущее:** Один обработчик на `ibrealistic:settings` для ВСЕХ 70 пакетов

**Целевое:** 
- OBU-пакеты (0-32) → НЕ обрабатываются IBRealistic (OBU обрабатывает на своём канале `openboatutils:settings`)
- Реалистичные пакеты (33-69) → обрабатываются IBRealistic на `ibrealistic:settings`

**Важно:** IBRealistic НЕ регистрирует обработчик на `openboatutils:settings`. OBU сам слушает свой канал. IBRealistic только читает поля OBU (OpenBoatUtils.enabled, OpenBoatUtils.airControl, etc.) когда нужно.

### 3.4 Разделение OpenBoatUtils.java

**Текущее:** Один монолитный класс с OBU-полями И реалистичными полями

**Целевое:**
- OBU-поля → **читаются из OBU JAR** (dev.o7moon.openboatutils.OpenBoatUtils) — IBRealistic импортирует этот класс
- IBRealistic-поля → **новый класс** `IBRealisticState.java` в пакете `dev.o7moon.openboatutils` (или `dev.o7moon.ibrealistic`)

**ПРИМЕЧАНИЕ о пакетах Java:** Идеально сменить пакет на `dev.o7moon.ibrealistic`, но это ОГРОМНОЕ изменение (все import-ы, все ссылки). **Компромисс на первом этапе:** оставить пакет `dev.o7moon.openboatutils`, но вынести IBRealistic-специфичные поля в `IBRealisticState.java` чтобы не конфликтовать с `OpenBoatUtils.java` из OBU JAR.

⚠️ **ПРОБЛЕМА:** Сейчас IBRealistic имеет СВОЙ `OpenBoatUtils.java` с тем же FQCN что у OBU. Если оба JAR загружены — classloader выберет один произвольно. **РЕШЕНИЕ:** IBRealistic НЕ ДОЛЖЕН иметь свой `OpenBoatUtils.java`. Он должен использовать OBU-шный.

### 3.5 Зависимость от OBU в fabric.mod.json

```json
{
  "depends": {
    "openboatutils": ">=0.4.10"
  }
}
```

Это заставит Fabric загружать OBU перед IBRealistic.

---

## ФАЗА 4: Рефакторинг плагина TimingSystem

### 4.1 Добавление второго канала

**Текущее:** Только `ibrealistic:settings`

**Целевое:**
```java
// TimingSystem.java onEnable()
Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "openboatutils:settings");
Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "openboatutils:settings", new PluginMessageReceiver());
Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "ibrealistic:settings");
Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "ibrealistic:settings", new PluginMessageReceiver());
```

### 4.2 Разделение пакетов по каналам (CustomBoatUtilsMode.java)

**Правило:** Пакеты OBU (0-32) → `openboatutils:settings`, реалистичные (33-69) → `ibrealistic:settings`

Это означает:
1. Все методы `sendShortAndFloatPacket()` и т.д. должны принимать параметр канала ИЛИ иметь два варианта
2. `finallyApplyToPlayer()` отправляет пакеты в правильный канал в зависимости от ID
3. `resetPlayer()` отправляет RESET (ID 0) в оба канала

### 4.3 Определение клиента

**Текущее:** Проверяет `isRealistic` boolean в VERSION пакете на канале `ibrealistic:settings`

**Целевое:**
- Если клиент подключился по `openboatutils:settings` — это OBU (базовый)
- Если клиент подключился И по `ibrealistic:settings` — это IBRealistic (полный)
- Проверить оба канала для определения возможностей клиента

### 4.4 Поведение при OBU-only клиенте

Когда клиент имеет только OBU (без IBRealistic):
- Отправлять ТОЛЬКО пакеты 0-32 через `openboatutils:settings`
- НЕ отправлять реалистичные пакеты (33-69)
- Режимы REALISTIC_* недоступны для этого клиента

---

## ФАЗА 5: Конкретный план работ (чек-лист)

### Мод (IBRealistic)

- [ ] **5.1** Создать `IBRealisticState.java` — вынести уникальные поля из OpenBoatUtils.java:
  - fourWheelPhysics, realisticDebugHud
  - visualRollAngle, visualSteeringAngle, visualHandbrake
  - countdownGoTimeMs, countdownSeconds, countdownActive
  - serverRealisticVersion, serverFeatures, serverName
  - BUILD_HASH, computeJarHash()
  - Все методы set* для реалистичной физики
  - resetRealisticState() вместо части resetSettings()
  - getClientRealisticVersion(), sendRealisticClientInfoPacket()

- [ ] **5.2** Переделать `OpenBoatUtils.java` — убрать все IBRealistic-специфичные поля:
  - Оставить ТОЛЬКО OBU-совместимые поля (которые есть в OBU OpenBoatUtils.java)
  - Или: вообще удалить OpenBoatUtils.java и использовать из OBU JAR напрямую

- [ ] **5.3** Переделать `ClientboundPackets.java`:
  - Убрать обработку пакетов 0-32 (OBU обрабатывает на своём канале)
  - Оставить ТОЛЬКО пакеты 33-69 на канале `ibrealistic:settings`
  - Регистрация: `ClientPlayNetworking.registerGlobalReceiver("ibrealistic:settings", ...)`

- [ ] **5.4** Переделать `BoatMixin.java`:
  - Удалить 15 дублирующих хуков (см. список выше)
  - Переделать `tickHook` → `@Inject(method="tick", at=@At("HEAD"))` только для реалистичной физики
  - Переделать `moveHook` → только landing speed preservation
  - Удалить `hookCheckLocation` полностью
  - Удалить все @Shadow полей OBU (pressingForward, pressingBack, yawVelocity, velocityDecay)
  - Вынести `SURFACE_NAMES` в утилитный класс
  - Оставить: liftPassenger, set_step_height (для реалистичного подъёма)

- [ ] **5.5** Удалить `EntityMixin.java`

- [ ] **5.6** Удалить `ServerPlayNetworkHandlerMixin.java`

- [ ] **5.7** Переделать `ClientWorldMixin.java`:
  - Вызывать `IBRealisticState.resetState()` вместо `OpenBoatUtils.resetSettings()`
  - НЕ сбрасывать OBU-поля (OBU делает это сам)

- [ ] **5.8** Переделать `ServerboundPackets.java`:
  - Отправлять VERSION через `ibrealistic:settings` (как сейчас)
  - Это позволяет серверу определить что клиент — IBRealistic

- [ ] **5.9** Обновить `fabric.mod.json`:
  - Добавить `"openboatutils": ">=0.4.10"` в depends
  - Убрать дублирующие миксины из конфигурации

- [ ] **5.10** Обновить `ibrealistic.mixins.json`:
  - Убрать EntityMixin, ServerPlayNetworkHandlerMixin
  - Оставить: BoatMixin, BoatEntityRendererMixin, ClientWorldMixin

### Плагин (TimingSystem)

- [ ] **5.11** Зарегистрировать второй канал `openboatutils:settings` в TimingSystem.java

- [ ] **5.12** Переделать `CustomBoatUtilsMode.java`:
  - OBU-пакеты (0-32) → sendPluginMessage через `openboatutils:settings`
  - Реалистичные пакеты (33-69) → sendPluginMessage через `ibrealistic:settings`
  - Добавить вспомогательные методы для отправки по правильному каналу

- [ ] **5.13** Переделать `BoatUtilsManager.java`:
  - Слушать ВЕРСИЮ на обоих каналах
  - Определять тип клиента: OBU-only или IBRealistic
  - При OBU-only: отправлять только пакеты 0-32

- [ ] **5.14** Обновить `plugin.yml` — зарегистрировать оба канала

### Тестирование

- [ ] **5.15** Проверка сборки мода (Gradle, все MC версии)
- [ ] **5.16** Проверка сборки плагина (Maven)
- [ ] **5.17** Проверка что удалённые хуки не ломают функционал

### Документация

- [ ] **5.18** Создать CHANGES_refactoring.md
- [ ] **5.19** Обновить DOCS_REALISTIC_PHYSICS.md (архитектура каналов)

---

## ФАЗА 6: Переменные, которые должны настраиваться по-другому

### 6.1 enabled (OBU vs IBRealistic)

**Текущее:** Одна переменная `enabled` управляет ВСЕМИ хуками.

**Проблема:** Когда IBRealistic активирует реалистичную физику, OBU тоже должен быть enabled (для stepHeight, collisions, etc.), но с другими значениями некоторых полей.

**Решение:** IBRealistic не трогает `OpenBoatUtils.enabled`. OBU управляет этим через свои пакеты. IBRealistic имеет свой `IBRealisticState.realisticEnabled` для реалистичной физики.

### 6.2 jumpForce (конфликт с handbrake)

**Текущее:** jumpForce из OBU используется для прыжка лодки. В реалистичном режиме Space = handbrake.

**Решение:** Когда `IBRealisticState.realisticEnabled = true`:
- IBRealistic перехватывает Space и использует его как handbrake
- OBU-шный jumpForce по-прежнему устанавливается сервером, но IBRealistic игнорирует его для Space
- Прыжок недоступен в реалистичном режиме

### 6.3 yawAcceleration, forwardsAcceleration, etc. (физика vs OBU)

**Текущее:** IBRealistic полностью переопределяет эти значения в updatePaddles через @ModifyConstant.

**Решение:** В реалистичном режиме FourWheelPhysicsEngine.update() устанавливает velocity и yaw НАПРЯМУЮ, минуя updatePaddles. OBU-значения не мешают, т.к. реалистичная физика перезаписывает результат после updatePaddles.

### 6.4 collisionResolution (OBU vs IBRealistic)

**Текущее:** IBRealistic дублирует moveHook для collisionResolution И добавляет landing speed preservation.

**Решение:** collisionResolution → OBU обрабатывает (через свой @Redirect на move()). IBRealistic использует @WrapOperation для ДОПОЛНИТЕЛЬНОЙ логики landing speed preservation ПОВЕРХ OBU.

### 6.5 stepHeight (OBU vs реалистичная физика)

**Текущее:** IBRealistic дублирует EntityMixin для stepHeight.

**Решение:** OBU устанавливает stepHeight через свой EntityMixin. IBRealistic не трогает stepHeight — OBU API достаточно.

---

## Риски и ограничения

1. **Пакет Java** — в идеале надо менять на `dev.o7moon.ibrealistic`, но это слишком масштабное изменение для первой итерации. Оставляем `dev.o7moon.openboatutils` для файлов, которые НЕ конфликтуют с OBU (physics/, client/, миксины). Для `OpenBoatUtils.java` — ОБЯЗАТЕЛЬНО решить конфликт FQCN.

2. **OBU как зависимость** — IBRealistic должен компилироваться с OBU JAR в classpath. Нужно добавить OBU как зависимость в build.gradle.

3. **Совместимость версий** — OBU VERSION = 18, IBRealistic VERSION = 18. При изменении протокола нужна координация.

4. **Stonecutter** — изменения в BoatMixin.java должны учитывать версионные ветки (1.20.4, 1.21, 1.21.3).

---

## Порядок выполнения

1. Сначала рефакторинг **мода** (удалить дубликаты, разделить поля)
2. Затем рефакторинг **плагина** (два канала)
3. Проверка сборки обоих
4. Документация
