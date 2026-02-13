# Подробный анализ миксинов и архитектуры: IBRealistic vs OpenBoatUtils-main

## Дата
2026-02-13

## Краткое описание
Полная карта функциональности: что за что отвечает, где расположено, как работает в каждом моде. Построчный анализ каждой инъекции с объяснением, можно ли удалить, и как устранить конфликты.

---

## ЧАСТЬ 0: Архитектура — кто что делает

### Общая схема

```
┌──────────────────────────────────────────────────────────────────┐
│                        MINECRAFT VANILLA                          │
│                                                                    │
│  BoatEntity / AbstractBoatEntity (1.21.3+)                        │
│  ├─ tick()           → обновляет позицию, физику                  │
│  ├─ updatePaddles()  → обрабатывает вёсла (accel, yaw)           │
│  ├─ updateVelocity() → обновляет скорость (gravity, decay)       │
│  ├─ checkLocation()  → определяет ON_LAND/IN_WATER/IN_AIR       │
│  ├─ move()           → перемещает лодку с коллизиями             │
│  ├─ getNearbySlipperiness() → скольжение блока под лодкой        │
│  ├─ collidesWith()   → может ли другая сущность столкнуться      │
│  ├─ fall()           → обработка урона от падения                 │
│  ├─ getGravity()     → значение гравитации (>=1.21)              │
│  └─ getPassengerAttachmentPos() → позиция пассажира              │
│                                                                    │
│  Entity                                                            │
│  ├─ getStepHeight()  → высота степпинга (>=1.21)                 │
│  └─ adjustMovementForCollisions() → обработка коллизий           │
│                                                                    │
│  ServerPlayNetworkHandler                                          │
│  ├─ isMovementInvalid() → анти-чит                               │
│  └─ onVehicleMove()     → обработка пакета движения              │
│                                                                    │
│  ClientWorld                                                       │
│  └─ <init>()         → конструктор (загрузка мира)               │
│                                                                    │
│  BoatEntityRenderer / AbstractBoatEntityRenderer (1.21.3+)        │
│  └─ render()         → отрисовка модели лодки                    │
└──────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼                               ▼
┌─────────────────────────┐   ┌─────────────────────────────────┐
│         OBU              │   │         IBRealistic              │
│  (mod id: openboatutils) │   │  (mod id: ibrealistic)          │
│  канал: openboatutils:   │   │  канал: ibrealistic:settings    │
│         settings         │   │                                  │
│                          │   │                                  │
│  Пакеты: ID 0-32        │   │  Пакеты: ID 0-69                │
│  (базовая физика)        │   │  (базовая + реалистичная)        │
│                          │   │                                  │
│  Функционал:             │   │  Функционал OBU (дублирован):    │
│  • Скорости (accel)      │   │  • Скорости (accel) — ДУБЛЬ     │
│  • Гравитация            │   │  • Гравитация — ДУБЛЬ            │
│  • Степпинг              │   │  • Степпинг — ДУБЛЬ             │
│  • Прыжок                │   │  • Прыжок — ДУБЛЬ               │
│  • Скольжение блоков     │   │  • Скольжение блоков — ДУБЛЬ    │
│  • Воздушный контроль    │   │  • Воздушный контроль — ДУБЛЬ   │
│  • Водный контроль       │   │  • Водный контроль — ДУБЛЬ      │
│  • Коллизии              │   │  • Коллизии — ДУБЛЬ             │
│  • Анти-чит отключение   │   │  • Анти-чит — ДУБЛЬ             │
│                          │   │                                  │
│                          │   │  Уникальный функционал:          │
│                          │   │  • 4-колёсная физика (bicycle)   │
│                          │   │  • 17 типов поверхностей         │
│                          │   │  • Визуальные колёса/руль        │
│                          │   │  • Handbrake (дрифт)             │
│                          │   │  • Массообмен (weight transfer)  │
│                          │   │  • Дифференциалы (Open/LSD/Lock) │
│                          │   │  • Аэродинамика (downforce/drag) │
│                          │   │  • Погода (дождь/снег/туман)     │
│                          │   │  • 7 пресетов компонентов        │
│                          │   │  • Обратный отсчёт гонки         │
│                          │   │  • Debug HUD                     │
│                          │   │  • Landing speed preservation    │
│                          │   │  • Визуальный подъём пассажира   │
└─────────────────────────┘   └─────────────────────────────────┘
```

### Файлы OBU — что где

| Файл | Пакет | Назначение |
|------|-------|------------|
| `OpenBoatUtils.java` | `dev.o7moon.openboatutils` | Главный класс. Хранит ВСЕ настройки (enabled, stepSize, jumpForce, etc.). Регистрирует канал `openboatutils:settings`. Метод `resetSettings()` сбрасывает всё к дефолтам. |
| `ClientboundPackets.java` | `dev.o7moon.openboatutils` | Обработка пакетов от сервера. 33 типа пакетов (ID 0-32). Каждый пакет меняет соответствующее поле в OpenBoatUtils. |
| `OpenBoatUtilsClient.java` | `dev.o7moon.openboatutils.client` | Client initializer. Регистрирует обработчики пакетов. |
| `Modes.java` | `dev.o7moon.openboatutils` | Enum режимов (VANILLA, ICE, SPEED, etc.). Каждый режим задаёт набор настроек. |
| `CollisionMode.java` | `dev.o7moon.openboatutils` | Enum режимов коллизий (VANILLA, NO_BOATS_OR_PLAYERS, NO_ENTITIES, ENTITYTYPE_FILTER). |
| `GetStepHeight.java` | `dev.o7moon.openboatutils` | Интерфейс для получения stepHeight из миксина. |

### Файлы IBRealistic (только уникальные, не существующие в OBU)

| Файл | Пакет | Назначение |
|------|-------|------------|
| **Физический движок** | | |
| `FourWheelPhysicsEngine.java` | `dev.o7moon.openboatutils.physics` | 4-колёсная динамика. Независимые нагрузки на каждое колесо, дифференциалы, аэродинамика. Методы: `update()`, `getVx()`, `getYawRate()`, `getCurrentSurface()`. |
| `RealisticPhysicsEngine.java` | `dev.o7moon.openboatutils.physics` | Bicycle model (2 оси). Результат: velocityX/Y/Z, yawDelta, pitchAngle, rollAngle, steeringAngle. |
| `TireModel.java` | `dev.o7moon.openboatutils.physics` | Модель шин Fiala/Brush. Статические методы: `computeSlipAngle()`, `computeLateralForce()`, `computeLongitudinalForce()`, `applyFrictionCircle()`. |
| `VehicleConfig.java` | `dev.o7moon.openboatutils.physics` | Конфигурация машины (40+ параметров). Масса, колёсная база, высота ЦМ, тяга, торможение, etc. |
| `SurfaceProperties.java` | `dev.o7moon.openboatutils.physics` | 17 пресетов поверхностей (ASPHALT_DRY, GRAVEL, MUD, SNOW, ICE, etc.). Параметры: muPeak, muSlide, corneringStiffness, relaxationLength. |
| **Пресеты компонентов** | | |
| `VehicleType.java` | `dev.o7moon.openboatutils.physics` | 5 типов машин: WRC_CAR, GROUP_B, CLASSIC_RALLY, LIGHTWEIGHT, TRUCK. |
| `DrivetrainType.java` | `dev.o7moon.openboatutils.physics` | RWD, FWD, AWD. |
| `DifferentialType.java` | `dev.o7moon.openboatutils.physics` | OPEN, LOCKED, LSD. |
| `WeatherCondition.java` | `dev.o7moon.openboatutils.physics` | CLEAR, RAIN, HEAVY_RAIN, SNOW, FOG. |
| `TirePreset.java` | `dev.o7moon.openboatutils.physics` | 5 пресетов шин с множителями grip/slide. |
| `SuspensionPreset.java` | `dev.o7moon.openboatutils.physics` | 5 пресетов подвески. |
| `EnginePreset.java` | `dev.o7moon.openboatutils.physics` | 5 пресетов двигателя. |
| `BodyPreset.java` | `dev.o7moon.openboatutils.physics` | 5 пресетов кузова. |
| `SteeringPreset.java` | `dev.o7moon.openboatutils.physics` | 5 пресетов рулевого управления. |
| `BrakePreset.java` | `dev.o7moon.openboatutils.physics` | 5 пресетов тормозов. |
| `WeightDistributionPreset.java` | `dev.o7moon.openboatutils.physics` | 4 пресета развесовки. |
| `WheelPosition.java` | `dev.o7moon.openboatutils.physics` | Enum колёс: FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT. |
| **Визуальные рендереры** | | |
| `WheelRenderer.java` | `dev.o7moon.openboatutils.client` | Рендеринг 4 колёс с вращением и поворотом. |
| `SteeringWheelRenderer.java` | `dev.o7moon.openboatutils.client` | Рендеринг рулевого колеса. |
| `RaceCountdownRenderer.java` | `dev.o7moon.openboatutils.client` | Обратный отсчёт гонки (цветные блоки + частицы). |

### Пакеты (IBRealistic-уникальные, ID 33-69)

| ID | Имя | Что делает |
|----|-----|-----------|
| 33 | SET_REALISTIC_PHYSICS | Вкл/выкл реалистичную физику |
| 34 | SET_VEHICLE_TYPE | Задать тип машины (WRC_CAR, GROUP_B, etc.) |
| 35 | SET_VEHICLE_MASS | Масса машины |
| 36 | SET_VEHICLE_WHEELBASE | Колёсная база |
| 37 | SET_VEHICLE_CG_HEIGHT | Высота центра масс |
| 38 | SET_VEHICLE_TRACK_WIDTH | Ширина колеи |
| 39 | SET_VEHICLE_MAX_STEERING | Макс. угол руля |
| 40 | SET_VEHICLE_STEERING_SPEED | Скорость поворота руля |
| 41 | SET_VEHICLE_BRAKING_FORCE | Сила торможения |
| 42 | SET_VEHICLE_ENGINE_FORCE | Сила двигателя |
| 43 | SET_VEHICLE_DRAG | Аэродинамическое сопротивление |
| 44 | SET_VEHICLE_BRAKE_BIAS | Распределение тормозов |
| 45 | SET_VEHICLE_SUBSTEPS | Кол-во подшагов физики |
| 46 | SET_VEHICLE_FRONT_WEIGHT_BIAS | Развесовка |
| 47 | SET_BLOCK_SURFACE_TYPE | Тип поверхности для блока |
| 48 | SET_DEFAULT_SURFACE_TYPE | Поверхность по умолчанию |
| 49 | SET_VEHICLE_DRIVETRAIN | Тип привода (FWD/RWD/AWD) |
| 50 | SET_VEHICLE_SPEED_STEERING | Зависимость руля от скорости |
| 51 | SET_VEHICLE_ENGINE_BRAKING | Торможение двигателем |
| 52 | SET_VEHICLE_ROLL_STIFFNESS | Жёсткость подвески |
| 53 | SET_VEHICLE_CONFIG | Полная конфигурация машины |
| 54 | SET_AWD_FRONT_SPLIT | Распределение тяги AWD |
| 55 | SET_FRONT_DIFFERENTIAL | Передний дифференциал |
| 56 | SET_REAR_DIFFERENTIAL | Задний дифференциал |
| 57 | SET_LSD_LOCKING_COEFF | Коэфф. блокировки LSD |
| 58 | SET_DOWNFORCE_COEFFICIENT | Коэфф. прижимной силы |
| 59 | SET_DOWNFORCE_FRONT_BIAS | Распределение прижимной силы |
| 60 | SET_WEATHER | Погодные условия |
| 61 | SET_STEERING_RETURN_RATE | Скорость возврата руля |
| 62 | SET_TIRE_PRESET | Пресет шин |
| 63 | SET_SUSPENSION_PRESET | Пресет подвески |
| 64 | SET_ENGINE_PRESET | Пресет двигателя |
| 65 | SET_BODY_PRESET | Пресет кузова |
| 66 | SET_STEERING_PRESET | Пресет рулевого |
| 67 | SET_BRAKE_PRESET | Пресет тормозов |
| 68 | SET_WEIGHT_DISTRIBUTION_PRESET | Пресет развесовки |
| 69 | SET_RACE_COUNTDOWN | Обратный отсчёт гонки |

### Поля IBRealistic OpenBoatUtils (уникальные, не в OBU)

| Поле | Тип | Назначение |
|------|-----|-----------|
| `fourWheelPhysics` | `FourWheelPhysicsEngine` | Экземпляр физического движка |
| `realisticDebugHud` | `volatile boolean` | Вкл/выкл debug HUD |
| `visualRollAngle` | `volatile float` | Угол крена для рендерера |
| `visualSteeringAngle` | `volatile float` | Угол руля для рендерера |
| `visualHandbrake` | `volatile boolean` | Состояние ручника для рендерера |
| `countdownGoTimeMs` | `volatile long` | Время старта гонки (System.currentTimeMillis) |
| `countdownSeconds` | `volatile int` | Количество секунд обратного отсчёта |
| `countdownActive` | `volatile boolean` | Активен ли обратный отсчёт |

### Поля OBU OpenBoatUtils (общие для обоих модов)

Эти поля есть в обоих модах и управляются пакетами ID 0-32:

| Поле | Тип | Дефолт | Назначение |
|------|-----|--------|-----------|
| `enabled` | `boolean` | `false` | Включён ли мод (устанавливается любым пакетом настроек) |
| `stepSize` | `float` | `0f` | Высота степпинга лодки (для подъёма по блокам) |
| `fallDamage` | `boolean` | `true` | Получает ли лодка урон от падения |
| `waterElevation` | `boolean` | `false` | Поднимает ли лодку при погружении в воду |
| `defaultSlipperiness` | `float` | `0.6f` | Скольжение по умолчанию для блоков |
| `airControl` | `boolean` | `false` | Управление лодкой в воздухе (фейк ON_LAND при IN_AIR) |
| `jumpForce` | `float` | `0f` | Сила прыжка лодки |
| `gravityForce` | `double` | `-0.04` | Сила гравитации |
| `yawAcceleration` | `float` | `1.0f` | Скорость поворота (yaw) |
| `forwardsAcceleration` | `float` | `0.04f` | Ускорение вперёд |
| `backwardsAcceleration` | `float` | `0.005f` | Ускорение назад |
| `turningForwardsAcceleration` | `float` | `0.005f` | Ускорение при повороте |
| `allowAccelStacking` | `boolean` | `false` | Суммируются ли ускорения |
| `underwaterControl` | `boolean` | `false` | Управление под водой |
| `surfaceWaterControl` | `boolean` | `false` | Управление на поверхности воды |
| `coyoteTime` | `int` | `0` | Время после покидания земли, когда ещё можно прыгнуть |
| `coyoteTimer` | `int` | `0` | Текущий таймер coyote time |
| `waterJumping` | `boolean` | `false` | Можно ли прыгать с воды |
| `swimForce` | `float` | `0.0f` | Сила плавания (при нажатии прыжка под водой) |
| `collision` | `CollisionMode` | `VANILLA` | Режим коллизий |
| `canStepWhileFalling` | `boolean` | `false` | Степпинг при падении |
| `interpolationCompat` | `boolean` | `false` | Совместимость интерполяции (10 шагов) |
| `collisionResolution` | `byte` | `0` | Количество sub-moves для коллизий (0 = нет разбиения) |
| `slipperinessMap` | `HashMap<String, Float>` | `{}` | Кастомное скольжение для блоков |
| `perBlockSettings` | `HashMap` | `{}` | Настройки по каждому блоку (accel, jump, etc.) |
| `collision_filter` | `ArrayList<String>` | `[]` | Фильтр типов сущностей для коллизий |

### Методы OBU OpenBoatUtils (используются в миксинах)

Эти методы вызываются из миксинов и являются "API" OBU:

| Метод | Параметры | Что делает |
|-------|-----------|-----------|
| `GetJumpForce(boat)` | `BoatEntity` | Возвращает jumpForce или per-block override |
| `GetYawAccel(boat)` | `BoatEntity` | Возвращает yawAcceleration или per-block override |
| `GetForwardAccel(boat)` | `BoatEntity` | Возвращает forwardsAcceleration или per-block override |
| `GetBackwardAccel(boat)` | `BoatEntity` | Возвращает backwardsAcceleration или per-block override |
| `GetTurnForwardAccel(boat)` | `BoatEntity` | Возвращает turningForwardsAcceleration или per-block override |
| `getBlockSlipperiness(block)` | `String` | Возвращает скольжение блока из slipperinessMap или defaultSlipperiness |
| `getStepSize()` | — | Возвращает stepSize |
| `canStepWhileFalling()` | — | Возвращает canStepWhileFalling |
| `getCollisionMode()` | — | Возвращает collision |
| `entityIsInCollisionFilter(entity)` | `Entity` | Проверяет entity в collision_filter |
| `resetSettings()` | — | Сбрасывает ВСЕ поля к дефолтам |

---

## О краш-логе

**Ошибка:** `@ModifyConstant forwardsAccel(F)F in openboatutils.mixins.json:AbstractBoatMixin from mod openboatutils` — это краш САМОГО OBU на MC 1.21.4 (литерал 0.04f не найден в updatePaddles). Версия 1.21.3 OBU работает и на 1.21.4 когда стоит ОДИН мод. Краш происходит при установке ОБОИХ модов ВМЕСТЕ — потому что два @ModifyConstant на одну и ту же константу.

---

## ЧАСТЬ 1: Конфигурация — что на что целится

### Целевой Minecraft-класс

| MC версия | Ванильный класс | OBU миксин | IBRealistic миксин |
|---|---|---|---|
| 1.20.4 | `BoatEntity` | `BoatMixin.java` → `@Mixin(BoatEntity.class)` | `BoatMixin.java` → `@Mixin(BoatEntity.class)` |
| 1.21 | `BoatEntity` | `BoatMixin.java` → `@Mixin(BoatEntity.class)` | `BoatMixin.java` → `@Mixin(BoatEntity.class)` |
| 1.21.3+ | `AbstractBoatEntity` | **ОТДЕЛЬНЫЙ** `AbstractBoatMixin.java` → `@Mixin(AbstractBoatEntity.class)` | **ТОТ ЖЕ** `BoatMixin.java` с Stonecutter `//? >=1.21.3` → `@Mixin(AbstractBoatEntity.class)` |

**Факт:** Оба миксина нацелены на один и тот же класс. Два миксина на один класс — это нормально для Fabric Mixin. Конфликт возникает когда два миксина хукают **одно и то же место** несовместимыми способами.

### Mixin JSON: какие классы миксинов зарегистрированы

**OBU** (`openboatutils.mixins.json5` для 1.21.3):
```
server: ServerPlayNetworkHandlerMixin
client: ClientWorldMixin, EntityMixin, AbstractBoatMixin
```

**IBRealistic** (`ibrealistic.mixins.json`):
```
server: ServerPlayNetworkHandlerMixin  
client: BoatMixin, BoatEntityRendererMixin, ClientWorldMixin, EntityMixin
```

**Проблема пакетов:** Оба мода используют `package dev.o7moon.openboatutils.mixin`. Это КРИТИЧНО — Java classloader не может загрузить два класса с одним FQCN из разных JAR. Для аддон-подхода IBRealistic **ОБЯЗАН** сменить пакет.

---

## ЧАСТЬ 2: BoatMixin.java — каждая инъекция по отдельности

### Инъекция 1: `paddleHook` — перехват checkLocation() в getPaddleSoundEvent

**Что делает в ванили:** Метод `getPaddleSoundEvent()` вызывает `checkLocation()` чтобы определить, какой звук вёсел проигрывать (в воде, на земле, etc.).

**Что делает OBU:** `@Redirect` — полностью заменяет вызов `checkLocation()`. Вместо оригинального вызова направляет результат через `hookCheckLocation(instance, false)`, который:
- Устанавливает stepHeight = 0
- Вызывает оригинальный `checkLocation()`
- Если OBU включён и это лодка игрока: обрабатывает waterElevation, airControl, swimForce
- Возвращает модифицированный Location

**Что делает IBRealistic:** `@WrapOperation` — делает то же самое через `hookCheckLocation(instance, false)` (параметр `original` от WrapOperation **игнорируется** — IBRealistic вызывает `this.checkLocation()` напрямую внутри hookCheckLocation). ⚠️ **Это антипаттерн:** при использовании `@WrapOperation` следует вызывать `original.call()` для поддержки цепочки инъекций. Игнорирование original ломает совместимость с другими модами.

**Конфликт при совместной установке:** `@WrapOperation` оборачивает `@Redirect`. Когда OBU ставит свой @Redirect, а IBRealistic ставит @WrapOperation, MixinExtras создаёт цепочку: IBRealistic.WrapOperation → вызов `original.call()` → OBU.Redirect. **НО** IBRealistic НЕ вызывает `original.call()` — он вызывает `this.checkLocation()` напрямую, поэтому OBU.Redirect просто пропускается. Результат: **логика OBU НЕ ВЫПОЛНЯЕТСЯ**, только IBRealistic.

**Можно ли удалить из IBRealistic?** ДА — если IBRealistic станет аддоном, OBU уже обрабатывает waterElevation, airControl, stepHeight. IBRealistic нужно ТОЛЬКО вызов `oncePerTick()` для реалистичной физики, а этот вызов можно сделать через отдельный `@Inject` в `tick()`.

---

### Инъекция 2: `tickHook` — перехват checkLocation() в tick()

**Что делает в ванили:** `tick()` вызывает `checkLocation()` чтобы определить положение лодки (в воде, на суше, в воздухе). Результат влияет на всю последующую физику тика.

**Что делает OBU:** `@Redirect` → `hookCheckLocation(instance, true)`. Тот же hookCheckLocation, но с `is_tick=true`, что дополнительно вызывает `oncePerTick()` (обработка swimForce, coyoteTime, jumpForce).

**Что делает IBRealistic:** `@WrapOperation` → тот же `hookCheckLocation(instance, true)`, но `oncePerTick()` в IBRealistic расширен:
- Та же OBU-логика (swimForce, coyoteTime)
- **НОВОЕ:** Когда реалистичная физика активна, прыжок отключён (spacebar = handbrake)
- **НОВОЕ:** Вызов `FourWheelPhysicsEngine.update()` — основная реалистичная физика
- **НОВОЕ:** Обновление визуальных параметров (roll, pitch, steering)
- **НОВОЕ:** Debug HUD

**Конфликт:** Такой же как у paddleHook — OBU.Redirect пропускается.

**Можно ли удалить?** НЕЛЬЗЯ полностью — но нужно **разделить**:
- ❌ Удалить из IBRealistic всю OBU-логику (hookCheckLocation, waterElevation, airControl, stepHeight)
- ✅ Оставить вызов реалистичной физики, но через **свой отдельный `@Inject(method="tick", at=@At("HEAD"))`** вместо перехвата checkLocation

---

### Инъекция 3: `getFriction` — перехват Block.getSlipperiness() в getNearbySlipperiness

**Что делает в ванили:** `getNearbySlipperiness()` вызывает `block.getSlipperiness()` для блока под лодкой. Возвращает числовое скольжение (0.6 для обычных блоков, 0.98 для льда, etc.).

**Что делает OBU:** `@Redirect` — заменяет вызов. Если OBU включён, возвращает настраиваемое значение из `OpenBoatUtils.getBlockSlipperiness(blockId)`.

**Что делает IBRealistic:** `@WrapOperation` — то же самое. Если `!enabled`, вызывает `original.call(block)`.

**Конфликт:** WrapOperation обернёт Redirect. При `!enabled` IBRealistic вызовет `original.call()` → OBU.Redirect → если OBU.enabled, OBU обработает. Если оба enabled — **двойная подмена**, но поскольку оба мода не могут быть enabled одновременно (разные каналы), фактически проблемы нет.

**Можно ли удалить?** ДА — OBU обработает это. При аддон-подходе IBRealistic не нужен этот хук.

---

### Инъекция 4: `redirectYawVelocityIncrement` — перехват присвоения yawVelocity в updatePaddles

**Что делает в ванили:** `updatePaddles()` прибавляет к `yawVelocity` значение ±1 (ванильная скорость поворота).

**Что делает OBU:** `@Redirect` на PUTFIELD yawVelocity. Если OBU включён, вместо ванильного `this.yawVelocity = newValue` вычисляет дельту и применяет настраиваемый `OpenBoatUtils.GetYawAccel()`.

**Что делает IBRealistic:** `@WrapOperation` — аналогично. Если `!enabled`, вызывает `original.call()` (что делегирует OBU).

**Конфликт:** При совместной работе: IBRealistic.WrapOperation оборачивает OBU.Redirect. Если IBRealistic enabled → обрабатывает сам (original не вызывается → OBU пропускается). Если IBRealistic !enabled → original.call() → OBU.Redirect → OBU обработает.

**Можно ли удалить?** ДА — OBU уже обрабатывает yawAcceleration. При аддон-подходе IBRealistic не нужен этот хук (реалистичная физика управляет yaw через свой движок в `oncePerTick`).

---

### Инъекция 5-7: `forwardsAccel`, `turnAccel`, `backwardsAccel` — @ModifyConstant на updatePaddles

**Что делает в ванили:** `updatePaddles()` содержит литералы: 
- `0.04f` — ускорение вперёд
- `0.005f` (ordinal 0) — ускорение при повороте
- `0.005f` (ordinal 1) — ускорение назад

**Что делает OBU:** `@ModifyConstant` — подменяет каждый литерал на значение из настроек (`GetForwardAccel()`, `GetTurnForwardAccel()`, `GetBackwardAccel()`).

**Что делает IBRealistic:** Точно такой же `@ModifyConstant` с теми же сигнатурами.

**Конфликт: ‼️ КРИТИЧЕСКИЙ ‼️** Два `@ModifyConstant` на одну и ту же константу — **это основная причина краша!** Mixin framework не может применить два @ModifyConstant к одному литералу. При совместной установке: `InjectionError: Critical injection failure`.

**Можно ли удалить?** ДА, ОБЯЗАТЕЛЬНО — OBU уже обрабатывает эти константы. При аддон-подходе IBRealistic не трогает updatePaddles (реалистичная физика не использует ванильное ускорение — она задаёт velocity напрямую через FourWheelPhysicsEngine).

---

### Инъекция 8-9: `pressingForwardHook`, `pressingBackHook` — перехват чтения pressingForward/pressingBack в updatePaddles

**Что делает в ванили:** `updatePaddles()` проверяет `if (this.pressingForward)` и `if (this.pressingBack)` для определения направления ускорения.

**Что делает OBU:** `@Redirect` на GETFIELD. Если `allowAccelStacking` включён, возвращает `false` (заставляет все ветви ускорения работать одновременно).

**Что делает IBRealistic:** `@WrapOperation` — аналогично, но при `!enabled` вызывает `original.call()`.

**Конфликт:** WrapOperation обернёт Redirect. Безопасно, но дублирует логику.

**Можно ли удалить?** ДА — OBU обрабатывает. IBRealistic не нужен этот хук.

---

### Инъекция 10-12: `velocityDecayHook1/2/3` — перехват присвоения velocityDecay в updateVelocity

**Что делает в ванили:** `updateVelocity()` устанавливает `velocityDecay` в зависимости от положения лодки:
- ordinal 1: IN_WATER → `velocityDecay = 0.9f`
- ordinal 2: UNDER_FLOWING_WATER → `velocityDecay = 0.45f`
- ordinal 3: UNDER_WATER → `velocityDecay = 0.45f`

**Что делает OBU:** `@Redirect` на PUTFIELD. Если underwaterControl/surfaceWaterControl включены, заменяет значение на `getBlockSlipperiness("minecraft:water")`.

**Что делает IBRealistic:** `@WrapOperation` — аналогично, но при `!enabled` вызывает `original.call()`.

**Конфликт:** WrapOperation обернёт Redirect. Безопасно.

**Можно ли удалить?** ДА — OBU обрабатывает.

---

### Инъекция 13: `canCollideHook` — @Inject на collidesWith HEAD

**Что делает в ванили:** `collidesWith(Entity)` определяет, может ли другая сущность столкнуться с лодкой.

**Что делает OBU:** `@Inject(HEAD)` — проверяет CollisionMode и отменяет столкновение если настроено (NO_BOATS_OR_PLAYERS, NO_ENTITIES, ENTITYTYPE_FILTER).

**Что делает IBRealistic:** Точно такой же `@Inject(HEAD)` с идентичной логикой.

**Конфликт:** Два `@Inject` на HEAD — оба вызовутся. Первый может `ci.cancel()`, второй тоже попытается. Результат: **логика дублируется**, но функционально одинаковый результат. **Не критично**, но избыточно.

**Можно ли удалить?** ДА — OBU обрабатывает.

---

### Инъекция 14: `fallHook` — @Inject на fall HEAD

**Что делает в ванили:** `fall()` вызывается при падении лодки (для урона от падения).

**Что делает OBU:** `@Inject(HEAD)` — если `!fallDamage`, отменяет урон.

**Что делает IBRealistic:** Идентично.

**Конфликт:** Два @Inject — оба выполнятся, оба отменят. Безопасно.

**Можно ли удалить?** ДА.

---

### Инъекция 15: `updateVelocityHook` (<=1.20.4) — @ModifyVariable на updateVelocity

**Что делает в ванили:** В 1.20.4 `updateVelocity()` содержит локальную переменную `e` (ordinal 1) — значение гравитации.

**Что делает OBU:** `@ModifyVariable` — заменяет значение на `OpenBoatUtils.gravityForce`.

**Что делает IBRealistic:** Идентично.

**Конфликт:** Два @ModifyVariable на одну переменную — **КРИТИЧЕСКИЙ!** Оба модифицируют одну переменную, результат непредсказуем.

**Можно ли удалить?** ДА — OBU обрабатывает.

---

### Инъекция 16: `onGetGravity` (>=1.21) — @Inject на getGravity HEAD

**Что делает в ванили:** `getGravity()` возвращает значение гравитации для лодки.

**Что делает OBU:** `@Inject(HEAD)` — подменяет возвращаемое значение на `-OpenBoatUtils.gravityForce`.

**Что делает IBRealistic:** Идентично.

**Конфликт:** Два @Inject — первый отменит, второй может вызвать проблемы т.к. CallbackInfoReturnable уже cancelled.

**Можно ли удалить?** ДА.

---

### Инъекция 17: `interpolationStepsHook` (>=1.21.3) — @ModifyVariable на updateTrackedPositionAndAngles

**Что делает в ванили:** Параметр `interpolationSteps` определяет плавность интерполяции позиции лодки.

**Что делает OBU:** `@ModifyVariable` — если `interpolationCompat`, устанавливает steps = 10.

**Что делает IBRealistic:** Идентично.

**Конфликт:** Два @ModifyVariable — дублирование.

**Можно ли удалить?** ДА.

---

### Инъекция 18: `moveHook` — перехват move() в tick()

**Что делает в ванили:** `tick()` вызывает `move(MovementType, Vec3d)` для перемещения лодки на основе её скорости.

**Что делает OBU:** `@Redirect` — если `collisionResolution > 0`, разбивает один move() на несколько sub-move шагов для лучшей обработки столкновений со стенами.

**Что делает IBRealistic:** `@WrapOperation` — то же разбиение на sub-moves, **ПЛЮС** уникальная логика Landing Speed Preservation:
- Сохраняет горизонтальную скорость перед move()
- После move() проверяет: лодка приземлилась? Скорость упала? Это стена или земля?
- Если земля — восстанавливает 90% горизонтальной скорости (предотвращает внезапную остановку при приземлении)

**Конфликт:** WrapOperation обернёт Redirect. IBRealistic при `!enabled` вызывает `original.call()` → OBU.Redirect → OBU обработает.

**Можно ли удалить?** НЕЛЬЗЯ полностью — Landing Speed Preservation уникальна для IBRealistic. **НО** логику collisionResolution нужно удалить (OBU делает это). IBRealistic moveHook должен:
- При `!enabled` → `original.call()` (делегировать OBU)
- При enabled → использовать collisionResolution ОТ OBU (через `original.call()` цепочку), а потом ДОПОЛНИТЕЛЬНО применить landing speed preservation

---

### Инъекция 19: `liftPassenger` — @Inject на getPassengerAttachmentPos RETURN (УНИКАЛЬНАЯ)

**Что делает в ванили:** `getPassengerAttachmentPos()` определяет позицию пассажира на лодке.

**Что делает OBU:** ❌ Не трогает.

**Что делает IBRealistic:** `@Inject(RETURN)` — если реалистичная физика активна, поднимает позицию пассажира на `PASSENGER_LIFT = 0.25f` (чтобы игрок сидел внутри приподнятой лодки, а не ниже).

**Конфликт:** Нет — OBU не трогает этот метод.

**Можно ли удалить?** НЕТ — уникальная функциональность IBRealistic.

---

## ЧАСТЬ 3: Остальные миксин-файлы

### ClientWorldMixin.java — ПОЛНОСТЬЮ ИДЕНТИЧЕН

**OBU (строки 10-16):**
```java
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void postWorldLoad(CallbackInfo ci){
        OpenBoatUtils.resetSettings();
    }
}
```

**IBRealistic (строки 10-16):** Байт-в-байт идентично.

**Что делает:** При загрузке нового мира сбрасывает все настройки на дефолтные.

**Конфликт:** Оба `@Inject` вызовутся. Каждый вызовет `resetSettings()` **СВОЕГО** класса OpenBoatUtils. Проблема: оба мода имеют класс `dev.o7moon.openboatutils.OpenBoatUtils` — Java загрузит один из них (undefined behavior).

**Можно ли удалить из IBRealistic?** ДА — при аддон-подходе OBU сбросит свои настройки. IBRealistic должен сбрасывать только свои уникальные поля (fourWheelPhysics, visualRollAngle, etc.) через свой ClientWorldMixin со своим пакетом.

---

### EntityMixin.java — ПОЛНОСТЬЮ ИДЕНТИЧЕН

**Что делает (инъекция 1, >=1.21):** `@Inject` на `getStepHeight()` → если это лодка, возвращает `openboatutils_step_height` (значение, установленное в hookCheckLocation).

**Что делает (инъекция 2):** `@ModifyVariable` на `adjustMovementForCollisions()` → если `canStepWhileFalling()`, разрешает степпинг в воздухе (обычно лодка не степает при падении).

**Конфликт:** 
- getStepHeight: два @Inject на HEAD — первый отменит, второй попытается опять. Безопасно, но избыточно.
- hookStepHeightOnGroundCheck: два @ModifyVariable на одну переменную — непредсказуемый порядок.

**Можно ли удалить?** ДА — OBU полностью обрабатывает stepHeight.

---

### ServerPlayNetworkHandlerMixin.java — ПОЧТИ ИДЕНТИЧЕН

**Инъекция 1** (`isMovementInvalid`): Одинаковая. `@Inject(HEAD)` → отключает проверку движения.

**Инъекция 2** (`onVehicleMove_WronglyFlag`): Одинаковая. `@ModifyVariable` → форсит moved-wrongly flag в false.

**Инъекция 3** (`preventMovedWronglyLog`):
- **OBU:** `@Redirect` на `Logger.warn()` — полностью заменяет, пустое тело.
- **IBRealistic:** `@WrapOperation` на `Logger.warn()` — оборачивает, `original.call()` не вызывается.

**Конфликт:** 
- isMovementInvalid: два @Inject — безопасно.
- WronglyFlag: два @ModifyVariable — непредсказуемый порядок.
- Logger.warn: WrapOperation обернёт Redirect — безопасно.

**Можно ли удалить?** ДА полностью — OBU уже отключает все проверки.

---

### BoatEntityRendererMixin.java — УНИКАЛЕН ДЛЯ IBREALISTIC

**Что делает (инъекция 1, `applyRealisticRoll`):** `@Inject` в `render()` перед `interpolateBubbleWobble()` — применяет визуальный крен (roll) и подъём лодки (VISUAL_LIFT = 0.25f).

**Что делает (инъекция 2, `renderWheels`):** `@Inject` в `render()` перед `pop()` — рисует визуальные колёса и рулевое колесо.

**Конфликт:** Нет — OBU не имеет этого миксина.

**Можно ли удалить?** НЕТ — уникальная визуальная функциональность IBRealistic.

---

## ЧАСТЬ 4: Как `@WrapOperation` взаимодействует с `@Redirect`

Это ключевое знание для понимания конфликтов:

```
Порядок приоритетов MixinExtras:
1. @WrapOperation (высший приоритет, оборачивает всё ниже)
2. @Redirect (средний приоритет)
3. Оригинальный код (низший)
```

**Когда OBU ставит @Redirect, а IBRealistic ставит @WrapOperation на ту же точку:**

```
Ванильный код: instance.checkLocation()
        ↓
OBU @Redirect: заменяет на hookCheckLocation()
        ↓ (обёрнут)
IBRealistic @WrapOperation(original):
    original.call() → вызывает OBU.Redirect → hookCheckLocation()
```

**Вывод:** @WrapOperation от IBRealistic при вызове `original.call()` вызывает OBU.Redirect. Это БЕЗОПАСНО, если IBRealistic корректно делегирует при `!enabled`. IBRealistic сейчас делает так:
- При `!enabled`: `original.call()` → OBU обработает ✅
- При `enabled`: напрямую обращается к полям, OBU пропускается ✅

**НО для @ModifyConstant:**

```
@ModifyConstant НЕ поддерживает цепочки.
Два @ModifyConstant на одну константу = CRASH.
```

Вот почему `forwardsAccel`, `turnAccel`, `backwardsAccel` — **КРИТИЧЕСКИЕ** конфликты.

---

## ЧАСТЬ 5: Свод — можно ли удалить каждый элемент

### BoatMixin.java — подробная таблица

| # | Хук | Аннотация | Цель | OBU делает то же? | IBRealistic уникальное? | Вердикт |
|---|------|-----------|------|---|---|---|
| 1 | `paddleHook` | @WrapOperation | checkLocation в getPaddleSoundEvent | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 2 | `tickHook` | @WrapOperation | checkLocation в tick | ✅ Да (@Redirect) | ⚠️ Частично (oncePerTick) | ⚠️ **ПЕРЕДЕЛАТЬ** — вместо WrapOperation использовать @Inject(tick, HEAD) только для вызова реалистичной физики |
| 3 | `hookCheckLocation` | (helper) | — | ✅ Да | ⚠️ Содержит oncePerTick | ⚠️ **ПЕРЕДЕЛАТЬ** — вынести oncePerTick в отдельный @Inject |
| 4 | `getFriction` | @WrapOperation | Block.getSlipperiness в getNearbySlipperiness | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 5 | `redirectYawVelocityIncrement` | @WrapOperation | yawVelocity PUTFIELD в updatePaddles | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 6 | `forwardsAccel` | @ModifyConstant | 0.04f в updatePaddles | ✅ Да (@ModifyConstant) | ❌ Нет | ❌ **УДАЛИТЬ** ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 7 | `turnAccel` | @ModifyConstant | 0.005f ordinal 0 в updatePaddles | ✅ Да (@ModifyConstant) | ❌ Нет | ❌ **УДАЛИТЬ** ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 8 | `backwardsAccel` | @ModifyConstant | 0.005f ordinal 1 в updatePaddles | ✅ Да (@ModifyConstant) | ❌ Нет | ❌ **УДАЛИТЬ** ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 9 | `pressingForwardHook` | @WrapOperation | pressingForward GETFIELD в updatePaddles | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 10 | `pressingBackHook` | @WrapOperation | pressingBack GETFIELD в updatePaddles | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 11 | `velocityDecayHook1` | @WrapOperation | velocityDecay PUTFIELD ordinal 2 | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 12 | `velocityDecayHook2` | @WrapOperation | velocityDecay PUTFIELD ordinal 3 | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 13 | `velocityDecayHook3` | @WrapOperation | velocityDecay PUTFIELD ordinal 1 | ✅ Да (@Redirect) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 14 | `canCollideHook` | @Inject | collidesWith HEAD | ✅ Да (@Inject) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 15 | `fallHook` | @Inject | fall HEAD | ✅ Да (@Inject) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 16 | `updateVelocityHook` | @ModifyVariable | updateVelocity ordinal 1 (<=1.20.4) | ✅ Да (@ModifyVariable) | ❌ Нет | ❌ **УДАЛИТЬ** ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ |
| 17 | `onGetGravity` | @Inject | getGravity HEAD (>=1.21) | ✅ Да (@Inject) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 18 | `interpolationStepsHook` | @ModifyVariable | updateTrackedPositionAndAngles (>=1.21.3) | ✅ Да (@ModifyVariable) | ❌ Нет | ❌ **УДАЛИТЬ** |
| 19 | `moveHook` | @WrapOperation | move() в tick | ✅ Частично (@Redirect, только collisionResolution) | ✅ Да (Landing Speed Preservation) | ⚠️ **ПЕРЕДЕЛАТЬ** — убрать collisionResolution (OBU), оставить landing speed |
| 20 | `liftPassenger` | @Inject | getPassengerAttachmentPos RETURN | ❌ Нет | ✅ Да | ✅ **ОСТАВИТЬ** |
| 21 | `set_step_height` / `getStepHeight` | @Shadow/@Unique | BoatEntity поля | ✅ Да | ❌ Нет | ❌ **УДАЛИТЬ** (OBU обрабатывает) |
| 22 | `oncePerTick` (helper) | (no annotation) | — | ⚠️ Частично (swim/jump) | ✅ Да (physics engine) | ⚠️ **ПЕРЕДЕЛАТЬ** — вынести только уникальную часть |
| 23 | SURFACE_NAMES static map | @Unique | — | ❌ Нет | ✅ Да | ✅ **ОСТАВИТЬ** (для debug) |

### Остальные файлы

| Файл | Вердикт | Причина |
|------|---------|---------|
| `ClientWorldMixin.java` | ❌ **УДАЛИТЬ** | Идентичен OBU. Для IBRealistic — свой ClientWorldMixin в своём пакете для сброса fourWheelPhysics и визуальных полей |
| `EntityMixin.java` | ❌ **УДАЛИТЬ** | Идентичен OBU |
| `ServerPlayNetworkHandlerMixin.java` | ❌ **УДАЛИТЬ** | Почти идентичен OBU |
| `BoatEntityRendererMixin.java` | ✅ **ОСТАВИТЬ** | Уникален для IBRealistic (колёса, руль, крен) |

---

## ЧАСТЬ 6: Дополнительные проблемы совместимости

### Проблема 1: Одинаковый Java пакет

Оба мода используют `dev.o7moon.openboatutils` и `dev.o7moon.openboatutils.mixin`. 

**Решение:** IBRealistic ДОЛЖЕН сменить пакет. Например:
- `dev.o7moon.ibrealistic` — основные классы
- `dev.o7moon.ibrealistic.mixin` — миксины
- `dev.o7moon.ibrealistic.physics` — физика
- `dev.o7moon.ibrealistic.client` — рендеринг

### Проблема 2: IBRealistic использует поля OBU напрямую

IBRealistic обращается к `OpenBoatUtils.enabled`, `OpenBoatUtils.airControl`, `OpenBoatUtils.getBlockSlipperiness()` и т.д. Если IBRealistic станет аддоном:
- Он будет **импортировать** `dev.o7moon.openboatutils.OpenBoatUtils` из OBU JAR
- Его собственные уникальные поля (`fourWheelPhysics`, `visualRollAngle`, etc.) будут в своём классе (например `IBRealisticState`)
- `resetSettings()` IBRealistic будет сбрасывать только свои поля

### Проблема 3: hookCheckLocation содержит смешанную логику

Метод `hookCheckLocation` (~80 строк) содержит:
- **OBU-логику** (строки 264-344): stepHeight, waterElevation, airControl, swimForce — ЭТО ДЕЛАЕТ OBU
- **IBRealistic-логику** (в oncePerTick, строки 134-204): FourWheelPhysicsEngine, handbrake, debug HUD

**Решение:** Весь hookCheckLocation нужно заменить на простой `@Inject(method="tick", at=@At("HEAD"))` который:
1. Проверяет что это лодка игрока
2. Вызывает FourWheelPhysicsEngine.update()
3. Обновляет визуальные параметры

### Проблема 4: static initializer block в BoatMixin

Строки 207-228 содержат `static { ... }` блок для `SURFACE_NAMES`. По документации Mixin, **static initializer blocks запрещены в миксинах**. Это может вызвать проблемы. Сейчас это **латентная проблема** — она не вызывает краш благодаря тому, что `SURFACE_NAMES` содержит только ссылки на существующие объекты (не создаёт Minecraft-зависимые объекты), но при определённых условиях загрузки классов это может привести к `clinit` ошибке.

**Решение:** Перенести SURFACE_NAMES и getSurfaceName() в отдельный утилитный класс (не миксин).

---

## ЧАСТЬ 7: План реализации аддона

### Шаг 1: Создать новый пакет
```
dev.o7moon.ibrealistic/
├── IBRealistic.java          ← главный класс (mod initializer)
├── IBRealisticState.java     ← fourWheelPhysics, visualRollAngle, etc.
├── client/
│   ├── IBRealisticClient.java ← client initializer
│   ├── WheelRenderer.java
│   └── SteeringWheelRenderer.java
├── physics/
│   ├── FourWheelPhysicsEngine.java  (уже есть, переименовать пакет)
│   └── ...
└── mixin/
    ├── BoatTickMixin.java       ← ТОЛЬКО @Inject(tick, HEAD) для реалистичной физики
    ├── BoatRendererMixin.java   ← визуалка (колёса, крен)
    ├── BoatPassengerMixin.java  ← liftPassenger
    ├── BoatMoveMixin.java       ← landing speed preservation (WrapOperation на move)
    └── ClientWorldMixin.java    ← сброс IBRealistic-specific полей
```

### Шаг 2: fabric.mod.json
```json
{
  "id": "ibrealistic",
  "depends": {
    "openboatutils": ">=0.4.10",  ← зависимость от OBU
    "fabricloader": "...",
    "minecraft": "..."
  }
}
```

### Шаг 3: Новый BoatTickMixin (вместо 20+ хуков)
```java
@Mixin(BoatEntity.class)  // или AbstractBoatEntity для 1.21.3+
public class BoatTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 1. Проверить что это лодка игрока
        // 2. Проверить что реалистичная физика активна
        // 3. Считать вводы (steering, throttle, brake, handbrake)
        // 4. Вызвать FourWheelPhysicsEngine.update()
        // 5. Применить velocity + yaw
        // 6. Обновить визуальные параметры
    }
}
```

### Шаг 4: Удалить из ibrealistic.mixins.json
- ❌ EntityMixin
- ❌ ServerPlayNetworkHandlerMixin
- Переименовать остальные

---

## Заключение

### Что удалять (15 хуков из 23):
`paddleHook`, `tickHook`, `hookCheckLocation` (OBU-часть), `getFriction`, `redirectYawVelocityIncrement`, `forwardsAccel`, `turnAccel`, `backwardsAccel`, `pressingForwardHook`, `pressingBackHook`, `velocityDecayHook1/2/3`, `canCollideHook`, `fallHook`, `updateVelocityHook`, `onGetGravity`, `interpolationStepsHook`, `set_step_height/getStepHeight`

### Что оставить/переделать (5 хуков):
1. **oncePerTick (реалистичная физика)** → переделать в `@Inject(tick, HEAD)`
2. **moveHook (landing speed preservation)** → переделать: убрать collisionResolution, оставить только landing speed, использовать `original.call()` для делегации OBU
3. **liftPassenger** → оставить без изменений
4. **BoatEntityRendererMixin** → оставить без изменений
5. **ClientWorldMixin** → переделать: сбрасывать только IBRealistic-поля, в своём пакете

### Критические действия:
1. ‼️ Сменить Java пакет
2. ‼️ Удалить @ModifyConstant (forwardsAccel, turnAccel, backwardsAccel)
3. ‼️ Удалить @ModifyVariable (updateVelocityHook)
4. ‼️ Добавить OBU как зависимость
5. ‼️ Убрать static initializer block из миксина
