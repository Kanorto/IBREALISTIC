# Изменения: Рефакторинг IBRealistic (часть 2) — исправление критических недочётов

## Дата
2026-02-13

## Краткое описание
Исправлены 3 критические проблемы в аддоне IBRealistic: разделение состояния OBU/IBRealistic при сбросе, согласование аэродинамики airborne с наземной, устранение двойного применения физики.

## Выявленные проблемы и исправления

### ПРОБЛЕМА 1 (КРИТИЧЕСКАЯ): Некорректный сброс состояния

**Суть:** `resetSettings()` сбрасывал ВСЕ поля (OBU + IBRealistic) одновременно. При двухканальной архитектуре, когда TimingSystem отправляет RESET на `ibrealistic:settings`, IBRealistic вызывал `resetSettings()`, который сбрасывал `enabled=false`, `stepSize=0`, `gravityForce=-0.04` — уничтожая состояние, которое OBU УЖЕ установил через `openboatutils:settings`.

**Исправление:** Создана `resetRealisticState()` — сбрасывает ТОЛЬКО IBRealistic-специфичные поля:
- `fourWheelPhysics` → новый `FourWheelPhysicsEngine()`
- `SurfaceProperties.resetBlockSurfaceMap()`
- `visualRollAngle`, `visualSteeringAngle`, `visualHandbrake` → 0
- `countdownActive`, `countdownGoTimeMs`, `countdownSeconds` → 0
- `realisticDebugHud` → false

`resetSettings()` оставлена для singleplayer (полный сброс).

### ПРОБЛЕМА 2 (КРИТИЧЕСКАЯ): Несогласованность аэродинамики airborne vs наземной

**Суть:** На земле drag рассчитывается как `config.getEffectiveDragCoefficient() * FRONTAL_AREA * AIR_DENSITY * vx²` — учитывая настройки кузова и двигателя (body/engine presets). В воздухе использовалась ФИКСИРОВАННАЯ `AIR_DRAG_COEFFICIENT = 0.35f`, игнорируя настройки. Это значит:
- Машина с аэро-кузовом (low drag) на земле быстрее, а в прыжке — нет
- Пресет SPORT с `bodyPreset.dragMultiplier = 0.8` не влиял на drag в воздухе
- Разрыв физической модели: одна и та же машина имела разные drag в воздухе и на земле

**Исправление:** Airborne drag теперь использует `config.getEffectiveDragCoefficient()`. Удалена неиспользуемая `AIR_DRAG_COEFFICIENT`.

### ПРОБЛЕМА 3 (КРИТИЧЕСКАЯ): Двойное применение физики

**Суть:** `realisticPhysicsTick` запускается в `@Inject(tick, HEAD)` и устанавливает velocity ПЕРЕД vanilla tick. Затем:
1. `updateVelocity()` — vanilla/OBU устанавливает `velocityDecay`
2. `updatePaddles()` — OBU через @ModifyConstant добавляет acceleration
3. `velocity *= velocityDecay` — vanilla применяет friction

Итог: velocity, рассчитанная IBRealistic (с учётом drag, tire forces, engine braking), получала ДОПОЛНИТЕЛЬНУЮ фрикцию от vanilla и ДОПОЛНИТЕЛЬНОЕ ускорение от OBU.

**Исправление:** Добавлены 2 новых хука:
- `cancelVanillaPaddles` — `@Inject(updatePaddles, HEAD, cancel)` блокирует OBU/vanilla W/S/A/D acceleration
- `cancelVanillaVelocityDecay` — `@Inject(updateVelocity, HEAD, cancel)` + `velocityDecay = 1.0f` нейтрализует vanilla friction

## Описание каждой переменной, настраиваемой по-другому

### OBU-переменные (IDs 0-32) — управляются OBU, IBRealistic НЕ модифицирует

| Переменная | Тип | Дефолт | Как настраивается | Где используется IBRealistic |
|---|---|---|---|---|
| `enabled` | boolean | false | OBU устанавливает при получении любого пакета | BoatMixin: НЕ проверяет (использует `fourWheelPhysics.isEnabled()`) |
| `stepSize` | float | 0f | OBU packet 1 | НЕ используется IBRealistic (OBU EntityMixin) |
| `defaultSlipperiness` | float | 0.6f | OBU packet 2 | НЕ используется IBRealistic (OBU getFriction hook) |
| `slipperinessMap` | HashMap | vanilla | OBU packets 3,22,23 | НЕ используется IBRealistic (OBU getFriction hook) |
| `fallDamage` | boolean | true | OBU packet 4 | НЕ используется IBRealistic (OBU fallHook) |
| `waterElevation` | boolean | false | OBU packet 5 | НЕ используется IBRealistic (OBU hookCheckLocation) |
| `airControl` | boolean | false | OBU packet 6 | realisticPhysicsTick: determines if physics runs in air |
| `jumpForce` | float | 0f | OBU packet 7 | НЕ используется (handbrake blocks jump via coyoteTimer=-1) |
| `gravityForce` | double | -0.04 | OBU packet 9 | НЕ используется IBRealistic (OBU gravity hooks) |
| `yawAcceleration` | float | 1.0f | OBU packet 10 | НЕ используется (cancelVanillaPaddles blocks) |
| `forwardsAcceleration` | float | 0.04f | OBU packet 11 | НЕ используется (cancelVanillaPaddles blocks) |
| `backwardsAcceleration` | float | 0.005f | OBU packet 12 | НЕ используется (cancelVanillaPaddles blocks) |
| `turningForwardsAcceleration` | float | 0.005f | OBU packet 13 | НЕ используется (cancelVanillaPaddles blocks) |
| `allowAccelStacking` | boolean | false | OBU packet 14 | НЕ используется (cancelVanillaPaddles blocks) |
| `underwaterControl` | boolean | false | OBU packet 16 | НЕ используется (cancelVanillaVelocityDecay blocks) |
| `surfaceWaterControl` | boolean | false | OBU packet 17 | НЕ используется (cancelVanillaVelocityDecay blocks) |
| `coyoteTime` | int | 0 | OBU packet 19 | coyoteTimer set to -1 when handbrake pressed |
| `waterJumping` | boolean | false | OBU packet 20 | НЕ используется IBRealistic |
| `swimForce` | float | 0f | OBU packet 21 | НЕ используется IBRealistic |
| `collision` | CollisionMode | VANILLA | OBU packet 27 | НЕ используется (OBU canCollideHook) |
| `canStepWhileFalling` | boolean | false | OBU packet 28 | НЕ используется (OBU EntityMixin) |
| `interpolationCompat` | boolean | false | OBU packet 29 | НЕ используется IBRealistic |
| `collisionResolution` | byte | 1 | OBU packet 30 | НЕ используется (OBU moveHook handles) |

### IBRealistic-переменные (IDs 33-69) — управляются IBRealistic

| Переменная | Тип | Дефолт | Packet ID | Описание |
|---|---|---|---|---|
| `fourWheelPhysics.enabled` | boolean | false | 33 | Включает 4-колёсный движок физики |
| `fourWheelPhysics.config.mass` | float | 1190f | 35 | Масса машины (кг) |
| `fourWheelPhysics.config.wheelbase` | float | 2.53f | 36 | Колёсная база (м) |
| `fourWheelPhysics.config.cgHeight` | float | 0.45f | 37 | Высота ЦМ (м) |
| `fourWheelPhysics.config.trackWidth` | float | 1.55f | 38 | Ширина колеи (м) |
| `fourWheelPhysics.config.maxSteeringAngle` | float | 0.50f rad | 39 | Макс. угол руля |
| `fourWheelPhysics.config.steeringSpeed` | float | 1.4f | 40 | Скорость поворота руля |
| `fourWheelPhysics.config.brakingForce` | float | 8000f | 41 | Сила тормозов (Н) |
| `fourWheelPhysics.config.engineForce` | float | 5500f | 42 | Сила двигателя (Н) |
| `fourWheelPhysics.config.dragCoefficient` | float | 0.35f | 43 | Коэфф. аэро сопротивления |
| `fourWheelPhysics.config.brakeBias` | float | 0.65f | 44 | Распределение тормозов перед/зад |
| `fourWheelPhysics.config.substeps` | int | 4 | 45 | Подшаги физики за тик |
| `fourWheelPhysics.config.frontWeightBias` | float | 0.55f | 46 | Развесовка перед/зад |
| SurfaceProperties map | Map | auto-detect | 47 | Тип поверхности для блока |
| `fourWheelPhysics.config.drivetrain` | DrivetrainType | AWD | 48 | Тип привода |
| Default surface | SurfaceProperties | ASPHALT_DRY | 49 | Поверхность по умолчанию |
| `fourWheelPhysics.config.speedSteeringFactor` | float | 0.004f | 50 | Ограничение руля на скорости |
| `fourWheelPhysics.config.engineBraking` | float | 800f | 51 | Торможение двигателем (Н) |
| `fourWheelPhysics.config.rollStiffnessRatioFront` | float | 0.55f | 52 | Жёсткость стабилизатора |
| `fourWheelPhysics.config.awdFrontSplit` | float | 0.5f | 53 | Распределение тяги AWD |
| `fourWheelPhysics.config.frontDifferential` | DifferentialType | OPEN | 54 | Тип дифференциала спереди |
| `fourWheelPhysics.config.rearDifferential` | DifferentialType | OPEN | 55 | Тип дифференциала сзади |
| `fourWheelPhysics.config.lsdLockingCoeff` | float | 0.3f | 56 | Коэфф. блокировки LSD |
| `fourWheelPhysics.config.downforceCoefficient` | float | 0.5f | 57 | Коэфф. прижимной силы |
| `fourWheelPhysics.config.downforceFrontBias` | float | 0.4f | 58 | Распределение прижима перед/зад |
| `fourWheelPhysics.weather` | WeatherCondition | CLEAR | 59 | Погодные условия |
| `fourWheelPhysics.config.steeringReturnRate` | float | 1.5f | 60 | Скорость возврата руля в центр |
| server info | String+int+String | null | 61 | Версия сервера, feature flags |
| `fourWheelPhysics.config.tirePreset` | TirePreset | STANDARD | 62 | Пресет шин |
| `fourWheelPhysics.config.suspensionPreset` | SuspensionPreset | COMFORT | 63 | Пресет подвески |
| `fourWheelPhysics.config.enginePreset` | EnginePreset | STOCK | 64 | Пресет двигателя |
| `fourWheelPhysics.config.bodyPreset` | BodyPreset | STANDARD | 65 | Пресет кузова |
| `fourWheelPhysics.config.steeringPreset` | SteeringPreset | STANDARD | 66 | Пресет рулевого управления |
| `fourWheelPhysics.config.brakePreset` | BrakePreset | STANDARD | 67 | Пресет тормозов |
| `fourWheelPhysics.config.weightDistributionPreset` | WeightDistributionPreset | BALANCED | 68 | Пресет развесовки |
| countdown | long+int | 0 | 69 | Синхронизация обратного отсчёта |

### Визуальные переменные (без пакетов)
| Переменная | Тип | Описание |
|---|---|---|
| `visualRollAngle` | float | Крен кузова (для рендерера) |
| `visualSteeringAngle` | float | Угол руля (для рендерера) |
| `visualHandbrake` | boolean | Состояние ручника (для рендерера) |
| `realisticDebugHud` | boolean | Debug HUD overlay |

## Изменённые файлы

### Мод (IBRealistic)
- `OpenBoatUtils.java` — разделена `resetSettings()` + новая `resetRealisticState()`
- `ClientboundPackets.java` — case 0 использует `resetRealisticState()`
- `ClientWorldMixin.java` — использует `resetRealisticState()`
- `BoatMixin.java` — добавлены `cancelVanillaPaddles`, `cancelVanillaVelocityDecay`, @Shadow velocityDecay
- `FourWheelPhysicsEngine.java` — airborne drag использует `config.getEffectiveDragCoefficient()`

## Текущее состояние BoatMixin: 5 хуков
1. `liftPassenger` — @Inject getPassengerAttachmentPos RETURN
2. `realisticPhysicsTick` — @Inject tick HEAD (основная физика)
3. `cancelVanillaPaddles` — @Inject updatePaddles HEAD cancel
4. `cancelVanillaVelocityDecay` — @Inject updateVelocity HEAD cancel + velocityDecay=1
5. `moveHook` — @WrapOperation move() (landing speed preservation)

## Тестирование
- [x] Мод собирается на MC 1.20.4
- [x] Мод собирается на MC 1.21
- [x] Мод собирается на MC 1.21.3
- [x] Плагин собирается (Maven)

## Примечание
Файл CODEBASE_INDEX.md необходимо обновить.
