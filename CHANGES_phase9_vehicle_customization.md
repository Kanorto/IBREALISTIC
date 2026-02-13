# Изменения: Фаза 9 — Система кастомизации машин (Vehicle Customization)

## Дата
2026-02-12

## Краткое описание
Реализована полная система кастомизации машин через компонентные пресеты: 7 типов компонентов (шины, подвеска, двигатель, кузов, руль, тормоза, развесовка), гараж игрока с хранением до 5 машин, магазин пресетов с ценами и уровнями, полная интеграция мод ↔ плагин через пакеты.

## Изменённые файлы

### Мод (OBURealistic)

#### Новые файлы — Пресеты компонентов (physics/)
- `TirePreset.java` — 7 пресетов шин (STANDARD, SOFT, MEDIUM, HARD, RAIN, ICE_SPIKES, RALLY_GRAVEL). Модифицирует gripMultiplier, slideMultiplier, relaxationMultiplier, loadSensitivityMod.
- `SuspensionPreset.java` — 4 пресета подвески (COMFORT, SPORT, RALLY, STIFF). Модифицирует rollStiffnessRatio, cgHeightMultiplier, yawRateDampingMultiplier.
- `EnginePreset.java` — 5 пресетов двигателя (STOCK, SPORT, RALLY, TURBO, MONSTER). Модифицирует engineForceMultiplier, engineBrakingMultiplier, dragMultiplier.
- `BodyPreset.java` — 5 пресетов кузова (STANDARD, LIGHTWEIGHT, AERO, RALLY_SPEC, HEAVY_DUTY). Модифицирует massMultiplier, downforceMultiplier, dragMultiplier.
- `SteeringPreset.java` — 4 пресета руля (STANDARD, QUICK, PROGRESSIVE, DRIFT). Задаёт maxSteeringAngle, steeringSpeed, steeringReturnRate, speedSteeringFactor.
- `BrakePreset.java` — 4 пресета тормозов (STANDARD, SPORT, RACING, ENDURANCE). Модифицирует brakingForceMultiplier, brakeBias.
- `WeightDistributionPreset.java` — 4 пресета развесовки (BALANCED, FRONT_BIASED, REAR_BIASED, MID_ENGINE). Задаёт frontWeightBias.

#### Изменённые файлы
- `VehicleConfig.java` — Добавлены поля пресетов и 13 effective-getter методов (getEffectiveMass(), getEffectiveEngineForce(), и т.д.). Методы getFrontAxleDistance(), getRearAxleDistance(), getStaticFrontLoad(), getStaticRearLoad() теперь используют effective values.
- `FourWheelPhysicsEngine.java` — Все прямые обращения к config.mass, config.engineForce, config.brakingForce и т.д. заменены на effective getters. Добавлена интеграция TirePreset (gripMultiplier, slideMultiplier, loadSensitivity modifier) и SuspensionPreset (yawRateDampingMultiplier).
- `ClientboundPackets.java` — Добавлены 7 новых enum-значений (SET_TIRE_PRESET..SET_WEIGHT_DISTRIBUTION_PRESET) и обработчики пакетов (case 62-68).
- `OpenBoatUtils.java` — Добавлены 7 setter-методов для пресетов и 7 импортов.
- `SingleplayerCommands.java` — Добавлены 7 новых команд (tirepreset, suspensionpreset, enginepreset, bodypreset, steeringpreset, brakepreset, weightdistributionpreset).

### Плагин (TimingSystem)

#### Новые файлы
- `PlayerCar.java` — DTO для машины в гараже (id, ownerUuid, name, 8 preset полей, active флаг). Поддерживает JSON сериализацию.
- `GarageManager.java` — Управление гаражом: CRUD для машин, lookup пресетов по имени/ID, цены, требуемые уровни, применение к CustomBoatUtilsMode.
- `CommandGarage.java` — Команда /garage с подкомандами: default (список), create, select, delete, upgrade, info.
- `CommandShop.java` — Команда /shop для просмотра доступных пресетов по категориям с ценами и уровнями.
- `Version17.java` — Миграция БД v17: создание таблицы `ts_player_garage`.

#### Изменённые файлы
- `CustomBoatUtilsMode.java` — 7 новых PACKET_ID (62-68), 7 новых полей пресетов с @Expose, обновлены resetToVanilla(), applyToPlayer(), applySettingsFrom(), getNonDefaultSettings(), getVersionRequirementFromSettingName(). Добавлены 7 preset name helper методов.
- `CommandBoatUtilsModeEdit.java` — Обновлён @CommandCompletion для 7 новых пресетов. Добавлены case-ы в switch для preset-ов. Добавлен helper метод resolvePresetId().
- `TimingSystem.java` — Регистрация CommandGarage и CommandShop.
- `SQLiteDatabase.java` — DB version 16 → 17, добавлена миграция Version17, создание таблицы ts_player_garage в createTables().
- `MySQLDatabase.java` — DB version 16 → 17, добавлена миграция Version17.
- `Success.java` — 5 новых enum-значений для гаража.
- `Error.java` — 7 новых enum-значений для гаража.
- `Info.java` — 8 новых enum-значений для гаража.
- `en_us.yml` — Переводы для всех новых сообщений.
- `TrackOption.java` — Добавлено значение ALLOW_GARAGE_CARS (id: 10).
- `ApiUtilities.java` — Добавлен метод applyGarageCarPresets() для применения пресетов из гаража при спавне лодки.
- `SpawnManager.java` — Хотбар-предмет показывает имя и характеристики активной машины из гаража.

### Версионирование
- Версии (realistic_version, obu_version) управляются автоматически через CI/CD и НЕ изменяются вручную.

## Новые пакеты
- SET_TIRE_PRESET (ID: 62) — short presetId
- SET_SUSPENSION_PRESET (ID: 63) — short presetId
- SET_ENGINE_PRESET (ID: 64) — short presetId
- SET_BODY_PRESET (ID: 65) — short presetId
- SET_STEERING_PRESET (ID: 66) — short presetId
- SET_BRAKE_PRESET (ID: 67) — short presetId
- SET_WEIGHT_DISTRIBUTION_PRESET (ID: 68) — short presetId

## Изменения VERSION
- Версии управляются автоматически через CI/CD и не изменяются вручную.
- При добавлении новых пакетов VERSION будет автоматически обновлён при следующем релизе.

## Изменения realistic_version
- Версии управляются автоматически через CI/CD и не изменяются вручную.

## Изменения DB version
- Старая версия: 16
- Новая версия: 17
- Причина: таблица ts_player_garage для хранения машин игроков

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4
- [x] Мод собирается успешно на MC 1.21
- [x] Мод собирается успешно на MC 1.21.3
- [x] Плагин собирается успешно (Maven)

## Примечания
- CODEBASE_INDEX.md нужно будет обновить при создании (7 новых файлов пресетов + PlayerCar + GarageManager + Version17 + CommandGarage + CommandShop)
