# Интеграция плагина с BoatUtils

Пакет: `me.makkuusen.timing.system.boatutils`

---

## CustomBoatUtilsMode.java

**Назначение:** DTO кастомных режимов BoatUtils. Хранит все настройки, сериализуется в JSON, отправляет пакеты игрокам.
**Аннотации:** `@Getter`, `@Setter`

### Константы Packet ID
| Имя | Значение | Описание |
|-----|----------|----------|
| `PACKET_ID_RESET` | 0 | Сброс |
| `PACKET_ID_SET_STEP_HEIGHT` | 1 | Высота шага |
| `PACKET_ID_SET_DEFAULT_SLIPPERINESS` | 2 | Скользкость по умолчанию |
| `PACKET_ID_SET_BLOCKS_SLIPPERINESS` | 3 | Скользкость блоков |
| `PACKET_ID_SET_BOAT_FALL_DAMAGE` | 4 | Урон от падения |
| `PACKET_ID_SET_BOAT_WATER_ELEVATION` | 5 | Подъём на воде |
| `PACKET_ID_SET_AIR_CONTROL` | 6 | Воздушное управление |
| `PACKET_ID_SET_BOAT_JUMP_FORCE` | 7 | Сила прыжка |
| `PACKET_ID_SET_GRAVITY` | 9 | Гравитация |
| `PACKET_ID_SET_YAW_ACCEL` | 10 | Ускорение поворота |
| `PACKET_ID_SET_FORWARD_ACCEL` | 11 | Ускорение вперёд |
| `PACKET_ID_SET_BACKWARD_ACCEL` | 12 | Ускорение назад |
| `PACKET_ID_SET_TURN_ACCEL` | 13 | Ускорение при повороте |
| `PACKET_ID_ALLOW_ACCEL_STACKING` | 14 | Суммирование ускорений |
| `PACKET_ID_SET_UNDERWATER_CONTROL` | 16 | Управление под водой |
| `PACKET_ID_SET_SURFACE_WATER_CONTROL` | 17 | На поверхности воды |
| `PACKET_ID_SET_COYOTE_TIME` | 19 | Coyote time |
| `PACKET_ID_SET_WATER_JUMPING` | 20 | Прыжок с воды |
| `PACKET_ID_SET_SWIM_FORCE` | 21 | Сила плавания |
| `PACKET_ID_REMOVE_BLOCKS_SLIPPERINESS` | 22 | Удаление скользкости |
| `PACKET_ID_CLEAR_SLIPPERINESS` | 23 | Очистка скользкости |
| `PACKET_ID_SET_PER_BLOCK` | 26 | Per-block настройка |
| `PACKET_ID_SET_COLLISION_MODE` | 27 | Режим коллизий |
| `PACKET_ID_SET_STEP_WHILE_FALLING` | 28 | Шаг при падении |
| `PACKET_ID_SET_INTERPOLATION_COMPAT` | 29 | Интерполяция |
| `PACKET_ID_SET_COLLISION_RESOLUTION` | 30 | Разрешение коллизий |
| `PACKET_ID_SET_REALISTIC_PHYSICS` | 33 | Реалистичная физика |
| `PACKET_ID_SET_VEHICLE_TYPE` | 34 | Тип машины |
| `PACKET_ID_SET_VEHICLE_MASS` | 35 | Масса |
| `PACKET_ID_SET_VEHICLE_WHEELBASE` | 36 | Колёсная база |
| `PACKET_ID_SET_VEHICLE_CG_HEIGHT` | 37 | Высота ЦМ |
| `PACKET_ID_SET_VEHICLE_TRACK_WIDTH` | 38 | Ширина колеи |
| `PACKET_ID_SET_VEHICLE_MAX_STEERING` | 39 | Макс. руль |
| `PACKET_ID_SET_VEHICLE_STEERING_SPEED` | 40 | Скорость руления |
| `PACKET_ID_SET_VEHICLE_BRAKING_FORCE` | 41 | Тормозное усилие |
| `PACKET_ID_SET_VEHICLE_ENGINE_FORCE` | 42 | Тяга двигателя |
| `PACKET_ID_SET_VEHICLE_DRAG` | 43 | Аэродинамика |
| `PACKET_ID_SET_VEHICLE_BRAKE_BIAS` | 44 | Распределение тормозов |
| `PACKET_ID_SET_VEHICLE_SUBSTEPS` | 45 | Подшаги |
| `PACKET_ID_SET_VEHICLE_FRONT_WEIGHT_BIAS` | 46 | Развесовка |
| `PACKET_ID_SET_BLOCK_SURFACE_TYPE` | 47 | Тип поверхности блока |
| `PACKET_ID_SET_VEHICLE_DRIVETRAIN` | 48 | Тип привода |
| `PACKET_ID_SET_DEFAULT_SURFACE_TYPE` | 49 | Дефолтная поверхность |

### Поля настроек (все с `@Expose`)
| Имя | Тип | Default | Описание |
|-----|-----|---------|----------|
| `name` | `String` | `""` | Имя режима |
| `stepHeight` | `float` | `0f` | Высота шага |
| `defaultSlipperiness` | `float` | `0.6f` | Скользкость по умолчанию |
| `blocksSlipperiness` | `Map<String, Float>` | `{}` | Скользкость блоков |
| `boatFallDamage` | `boolean` | `true` | Урон от падения |
| `boatWaterElevation` | `boolean` | `false` | Подъём на воде |
| `boatAirControl` | `boolean` | `false` | Управление в воздухе |
| `airStepping` | `boolean` | `false` | Шаг при падении |
| `boatJumpForce` | `float` | `0f` | Сила прыжка |
| `gravity` | `double` | `-0.04` | Гравитация |
| `yawAcceleration` | `float` | `1.0f` | Ускорение поворота |
| `forwardAcceleration` | `float` | `0.04f` | Ускорение вперёд |
| `backwardAcceleration` | `float` | `0.005f` | Ускорение назад |
| `turningForwardAcceleration` | `float` | `0.005f` | Ускорение при повороте |
| `allowAccelerationStacking` | `boolean` | `false` | Суммирование ускорений |
| `underwaterControl` | `boolean` | `false` | Управление под водой |
| `surfaceWaterControl` | `boolean` | `false` | На поверхности воды |
| `coyoteTime` | `int` | `0` | Coyote time |
| `waterJumping` | `boolean` | `false` | Прыжок с воды |
| `swimForce` | `float` | `0f` | Сила плавания |
| `perBlockSettings` | `Map<String, PerBlockSetting>` | `{}` | Per-block настройки |
| `realisticPhysics` | `boolean` | `false` | Реалистичная физика |
| `vehicleType` | `short` | `-1` | Тип машины |
| `vehicleMass` | `float` | `1190f` | Масса |
| `vehicleWheelbase` | `float` | `2.53f` | Колёсная база |
| `vehicleCgHeight` | `float` | `0.45f` | Высота ЦМ |
| `vehicleTrackWidth` | `float` | `1.55f` | Ширина колеи |
| `vehicleMaxSteering` | `float` | `0.6f` | Макс. руль |
| `vehicleSteeringSpeed` | `float` | `2.5f` | Скорость руления |
| `vehicleBrakingForce` | `float` | `8000f` | Тормозное усилие |
| `vehicleEngineForce` | `float` | `5500f` | Тяга двигателя |
| `vehicleDrag` | `float` | `0.35f` | Аэродинамика |
| `vehicleBrakeBias` | `float` | `0.65f` | Распределение тормозов |
| `vehicleSubsteps` | `int` | `4` | Подшаги |
| `vehicleFrontWeightBias` | `float` | `0.55f` | Развесовка |
| `blockSurfaceTypes` | `Map<String, String>` | `{}` | Тип поверхности блока |
| `vehicleDrivetrain` | `short` | `2` | Тип привода |
| `defaultSurfaceType` | `String` | `""` | Дефолтная поверхность |

### Публичные методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `resetToVanilla()` | `void` | Сброс всех полей к ванильным значениям |
| `applyToPlayer(Player)` | `boolean` | Отправка всех пакетов игроку |
| `setBlocksSlipperiness(float, String)` | `void` | Установка скользкости для блоков |
| `clearBlocksSlipperiness(String)` | `void` | Очистка скользкости блоков |
| `clearAllSlipperiness()` | `void` | Полная очистка скользкости |
| `setPerBlockSetting(short, Object, String)` | `void` | Per-block настройка |
| `clearPerBlockSettings(String)` | `void` | Очистка per-block для блоков |
| `clearAllPerBlockSettings()` | `void` | Полная очистка per-block |
| `addBlockSurfaceType(String, String)` | `void` | Добавление типа поверхности |
| `clearBlockSurfaceTypes()` | `void` | Очистка типов поверхности |
| `toJson()` | `String` | Сериализация в JSON |
| `fromJson(String)` | `CustomBoatUtilsMode` | (static) Десериализация из JSON |
| `getNonDefaultSettings()` | `Map<String, List<NonDefaultSetting>>` | Список нестандартных настроек |
| `applySettingsFrom(CustomBoatUtilsMode)` | `void` | Копирование настроек из другого режима |
| `getRequiredVersion()` | `int` | Минимальная версия клиента |
| `playerHasCorrectVersion(Player)` | `boolean` | Проверка версии клиента |
| `writeString(DataOutputStream, String)` | `void` | (static) Запись строки в поток |

### Приватные методы отправки пакетов
| Метод | Описание |
|-------|----------|
| `finallyApplyToPlayer(Player)` | Отправка всех нестандартных настроек |
| `resetPlayer(Player)` | (static) Отправка RESET пакета |
| `sendPacket(Player, byte[])` | Отправка сырого пакета |
| `sendShortAndBooleanPacket(Player, short, boolean)` | Short + Boolean |
| `sendShortAndFloatPacket(Player, short, float)` | Short + Float |
| `sendShortAndDoublePacket(Player, short, double)` | Short + Double |
| `sendShortAndIntPacket(Player, short, int)` | Short + Int |
| `sendShortAndShortPacket(Player, short, short)` | Short + Short |
| `sendShortAndStringPacket(Player, short, String)` | Short + String |
| `sendShortAndTwoStringsPacket(Player, short, String, String)` | Short + 2 Strings |
| `sendShortAndFloatAndStringPacket(Player, short, float, String)` | Short + Float + String |
| `sendShortAndShortAndFloatAndStringPacket(Player, short, short, float, String)` | Short + Short + Float + String |

---

## BoatUtilsMode.java

**Назначение:** Enum стандартных предустановленных режимов.

### Значения (31 шт.)
| Имя | ID | Версия | Описание |
|-----|----|--------|----------|
| `VANILLA` | -1 | 0 | Ванильный режим |
| `BROKEN_SLIME_RALLY` | 0 | 0 | Rally с broken slime |
| `RALLY` | 8 | 0 | Стандартный Rally |
| `REALISTIC` | 25 | 19 | Реалистичная физика |
| `REALISTIC_WRC` | 26 | 19 | WRC_CAR |
| `REALISTIC_GROUP_B` | 27 | 19 | GROUP_B |
| `REALISTIC_CLASSIC` | 28 | 19 | CLASSIC_RALLY |
| `REALISTIC_LIGHTWEIGHT` | 29 | 19 | LIGHTWEIGHT |
| `REALISTIC_TRUCK` | 30 | 19 | TRUCK |
| ... | ... | ... | (и 22 других) |

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `getId()` | `short` | ID режима |
| `getRequiredVersion()` | `short` | Минимальная версия |
| `getMode(int)` | `BoatUtilsMode` | (static) Получение по ID |

---

## BoatUtilsManager.java

**Назначение:** Менеджер режимов BoatUtils для игроков. Обработка plugin messages.

### Статические поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `playerBoatUtilsMode` | `Map<UUID, BoatUtilsMode>` | Текущий режим каждого игрока |
| `playerCustomBoatUtilsModeId` | `Map<UUID, Integer>` | ID кастомного режима |

### Методы
| Метод | Описание |
|-------|----------|
| `pluginMessageListener(String, Player, byte[])` | Обработка plugin message (версия клиента) |
| `sendBoatUtilsModePluginMessage(Player, BoatUtilsMode, Track, boolean)` | Отправка режима игроку |
| `clearPlayerModes(UUID)` | Очистка режимов |
| `isPlayerUsingCorrectMode(Player, Track)` | Проверка режима для трассы |
| `getAvailableModes(int)` | Доступные режимы для версии |

---

## NocolManager.java

**Назначение:** Менеджер режимов коллизий (nocol).

### Константы
| Имя | Значение | Описание |
|-----|----------|----------|
| `PACKET_ID_NOCOL` | 27 | ID пакета коллизий |
| `PACKET_ID_COLLISION_FILTER` | 31 | ID пакета фильтра |
| `NOCOL_MODE_VANILLA` | 0 | Ванильные коллизии |
| `NOCOL_MODE_NO_COLLISION_BOATS_PLAYERS` | 1 | Без лодок/игроков |
| `NOCOL_MODE_NO_COLLISION_ANY` | 2 | Без коллизий |
| `NOCOL_MODE_FILTERED_COLLISION` | 3 | Фильтрованные |
| `NOCOL_MODE_FILTERED_COLLISION_PLUS` | 4 | Фильтрованные + без лодок |

### Методы
| Метод | Описание |
|-------|----------|
| `playerCanUseNocol(Player)` | Проверка разрешения nocol |
| `playerCanUseFilteredCollision(Player)` | Проверка разрешения фильтра |
| `setCollisionMode(Player, boolean)` | Установка режима коллизий |
| `setLowCollisionMode(Player)` | Установка базового nocol |

---

## NonDefaultSetting.java

**Назначение:** Record для отображения нестандартных настроек.

| Компонент | Тип | Описание |
|-----------|-----|----------|
| `name` | `String` | Имя настройки |
| `currentValue` | `Object` | Текущее значение |
| `defaultValue` | `@Nullable Object` | Значение по умолчанию |

---

## PerBlockSetting.java

**Назначение:** Настройка на уровне блоков.
**Аннотации:** `@Getter`

| Поле | Тип | Описание |
|------|-----|----------|
| `type` | `short` | Тип настройки (ordinal PerBlockSettingType) |
| `value` | `Object` | Значение |
| `blockId` | `String` | ID блока Minecraft |

| Метод | Описание |
|-------|----------|
| `getAsFloat()` | Значение как float |
