# Ядро мода OpenBoatUtilsRealistic

Пакет: `dev.o7moon.openboatutils`

---

## OpenBoatUtils.java

**Назначение:** Главный класс мода. Точка входа (`ModInitializer`). Хранит глобальное состояние всех настроек.

### Константы
| Имя | Тип | Значение | Описание |
|-----|-----|----------|----------|
| `LOG` | `Logger` | — | Логгер мода |
| `VERSION` | `int` | `19` | Версия протокола |
| `settingsChannel` | `Identifier` | `openboatutils:settings` | Канал для пакетов |

### Глобальные поля состояния
| Имя | Тип | Default | Описание |
|-----|-----|---------|----------|
| `enabled` | `boolean` | `false` | Мод активен (сервер отправил пакет) |
| `fallDamage` | `boolean` | `true` | Урон от падения |
| `waterElevation` | `boolean` | `false` | Подъём на воде |
| `airControl` | `boolean` | `false` | Управление в воздухе |
| `defaultSlipperiness` | `float` | `0.6f` | Скользкость по умолчанию |
| `jumpForce` | `float` | `0f` | Сила прыжка |
| `stepSize` | `float` | `0f` | Высота шага |
| `gravityForce` | `double` | `-0.04` | Гравитация |
| `yawAcceleration` | `float` | `1.0f` | Ускорение поворота |
| `forwardsAcceleration` | `float` | `0.04f` | Ускорение вперёд |
| `backwardsAcceleration` | `float` | `0.005f` | Ускорение назад |
| `turningForwardsAcceleration` | `float` | `0.005f` | Ускорение при повороте |
| `allowAccelStacking` | `boolean` | `false` | Суммирование ускорений |
| `underwaterControl` | `boolean` | `false` | Управление под водой |
| `surfaceWaterControl` | `boolean` | `false` | Управление на поверхности воды |
| `coyoteTime` | `int` | `0` | Время coyote (тики) |
| `coyoteTimer` | `int` | `0` | Таймер coyote |
| `waterJumping` | `boolean` | `false` | Прыжок с воды |
| `swimForce` | `float` | `0.0f` | Сила плавания |
| `collision` | `CollisionMode` | `VANILLA` | Режим столкновений |
| `canStepWhileFalling` | `boolean` | `false` | Шаг при падении |
| `realisticPhysics` | `RealisticPhysicsEngine` | `new` | Движок физики |
| `interpolationCompat` | `boolean` | `false` | Совместимость интерполяции |
| `collisionResolution` | `byte` | `1` | Разрешение коллизий |
| `perBlockSettings` | `HashMap<Integer, HashMap<String, Float>>` | — | Настройки по блокам |
| `slipperinessMap` | `HashMap<String, Float>` | — | Карта скользкости |
| `collision_filter` | `ArrayList<String>` | — | Фильтр коллизий |

### Enum: PerBlockSettingType
| Значение | Описание |
|----------|----------|
| `jumpForce` | Сила прыжка на блоке |
| `forwardsAccel` | Ускорение вперёд на блоке |
| `backwardsAccel` | Ускорение назад на блоке |
| `yawAccel` | Ускорение поворота на блоке |
| `turnForwardsAccel` | Ускорение при повороте на блоке |

### Методы — Инициализация и сброс
| Метод | Описание |
|-------|----------|
| `onInitialize()` | Регистрация кодеков, обработчиков, команд |
| `resetAll()` | Полный сброс всего состояния |
| `resetSettings()` | Сброс настроек контекста (без interpolationCompat и collisionResolution) |

### Методы — Скользкость блоков
| Метод | Описание |
|-------|----------|
| `getVanillaSlipperinessMap()` | Ванильная карта скользкости |
| `getSlipperinessMap()` | Текущая карта скользкости |
| `setBlocksSlipperiness(List, float)` | Установка скользкости для списка блоков |
| `setAllBlocksSlipperiness(float)` | Скользкость по умолчанию |
| `setBlockSlipperiness(String, float)` | Скользкость для одного блока |
| `getBlockSlipperiness(String)` | Получение скользкости блока |
| `removeBlockSlipperiness(String)` | Удаление скользкости блока |
| `removeBlocksSlipperiness(List)` | Удаление для списка блоков |
| `clearSlipperinessMap()` | Очистка карты скользкости |
| `breakSlimePlease()` | Удаление скользкости слизи |

### Методы — Per-block настройки
| Метод | Описание |
|-------|----------|
| `settingHasPerBlock(PerBlockSettingType)` | Есть ли per-block для настройки |
| `getPerBlockForBlock(PerBlockSettingType, String)` | Значение per-block для блока |
| `getNearbySetting(BoatEntity, PerBlockSettingType)` | Средневзвешенное по ближайшим блокам |
| `defaultPerBlock(PerBlockSettingType)` | Дефолтное значение per-block |
| `setBlocksSetting(PerBlockSettingType, List, float)` | Установка per-block для блоков |
| `setBlockSetting(PerBlockSettingType, String, float)` | Установка для одного блока |

### Методы — Простые сеттеры (устанавливают `enabled = true`)
| Метод | Параметр | Описание |
|-------|----------|----------|
| `setStepSize(float)` | stepSize | Высота шага |
| `setFallDamage(boolean)` | fallDamage | Урон от падения |
| `setWaterElevation(boolean)` | waterElevation | Подъём на воде |
| `setAirControl(boolean)` | airControl | Управление в воздухе |
| `setJumpForce(float)` | jumpForce | Сила прыжка |
| `setGravityForce(double)` | gravityForce | Гравитация |
| `setYawAcceleration(float)` | yawAcceleration | Ускорение поворота |
| `setForwardsAcceleration(float)` | forwardsAcceleration | Ускорение вперёд |
| `setBackwardsAcceleration(float)` | backwardsAcceleration | Ускорение назад |
| `setTurningForwardsAcceleration(float)` | turningForwardsAcceleration | Ускорение при повороте |
| `setAllowAccelStacking(boolean)` | allowAccelStacking | Суммирование ускорений |
| `setUnderwaterControl(boolean)` | underwaterControl | Управление под водой |
| `setSurfaceWaterControl(boolean)` | surfaceWaterControl | На поверхности воды |
| `setCoyoteTime(int)` | coyoteTime | Coyote time |
| `setWaterJumping(boolean)` | waterJumping | Прыжок с воды |
| `setSwimForce(float)` | swimForce | Сила плавания |
| `setCollisionMode(CollisionMode)` | collision | Режим столкновений |
| `setCanStepWhileFalling(boolean)` | canStepWhileFalling | Шаг при падении |
| `setInterpolationCompat(boolean)` | interpolationCompat | Интерполяция |
| `setCollisionResolution(byte)` | collisionResolution | Разрешение коллизий |

### Методы — Коллизии
| Метод | Описание |
|-------|----------|
| `getCollisionMode()` | Получение режима коллизий |
| `canStepWhileFalling()` | Можно ли шагать при падении |
| `clearCollisionFilter()` | Очистка фильтра |
| `addToCollisionFilter(String)` | Добавление типа сущности |
| `entityIsInCollisionFilter(Entity)` | Проверка сущности в фильтре |

### Методы — Реалистичная физика
| Метод | Описание |
|-------|----------|
| `setRealisticPhysicsEnabled(boolean)` | Включение/выключение |
| `setVehicleType(VehicleType)` | Установка пресета машины |
| `setVehicleConfig(VehicleConfig)` | Установка полной конфигурации |
| `setVehicleMass(float)` | Масса |
| `setVehicleWheelbase(float)` | Колёсная база |
| `setVehicleCgHeight(float)` | Высота ЦМ |
| `setVehicleTrackWidth(float)` | Ширина колеи |
| `setVehicleMaxSteering(float)` | Макс. угол руля |
| `setVehicleSteeringSpeed(float)` | Скорость руления |
| `setVehicleBrakingForce(float)` | Сила торможения |
| `setVehicleEngineForce(float)` | Сила двигателя |
| `setVehicleDragCoefficient(float)` | Аэродинамика |
| `setVehicleBrakeBias(float)` | Распределение тормозов |
| `setVehicleSubsteps(int)` | Подшаги |
| `setVehicleFrontWeightBias(float)` | Развесовка |
| `setBlockSurfaceType(String, String)` | Тип поверхности блока |
| `setVehicleDrivetrain(short)` | Тип привода |
| `setDefaultSurfaceType(String)` | Дефолтная поверхность |
| `resetRealisticPhysics()` | Полный сброс физики |

### Методы — Пакеты
| Метод | Описание |
|-------|----------|
| `sendVersionPacket()` | Отправка версии на сервер |
| `sendPacketC2S(PacketByteBuf)` | Отправка пакета клиент→сервер |
| `sendPacketS2C(ServerPlayerEntity, PacketByteBuf)` | Отправка пакета сервер→клиент |

### Методы — Геттеры с per-block поддержкой
| Метод | Описание |
|-------|----------|
| `GetJumpForce(BoatEntity)` | Сила прыжка (с учётом per-block) |
| `GetYawAccel(BoatEntity)` | Ускорение поворота (с учётом per-block) |
| `GetForwardAccel(BoatEntity)` | Ускорение вперёд (с учётом per-block) |
| `GetBackwardAccel(BoatEntity)` | Ускорение назад (с учётом per-block) |
| `GetTurnForwardAccel(BoatEntity)` | Ускорение при повороте (с учётом per-block) |

---

## ClientboundPackets.java

**Назначение:** Enum обработки пакетов от сервера к клиенту. 50 типов пакетов.

### Пакеты (по ordinal)
| # | Имя | Описание |
|---|-----|----------|
| 0 | `RESET` | Сброс всех настроек |
| 1 | `SET_STEP_HEIGHT` | Высота шага |
| 2 | `SET_DEFAULT_SLIPPERINESS` | Скользкость по умолчанию |
| 3 | `SET_BLOCKS_SLIPPERINESS` | Скользкость блоков |
| 4 | `SET_BOAT_FALL_DAMAGE` | Урон от падения |
| 5 | `SET_BOAT_WATER_ELEVATION` | Подъём на воде |
| 6 | `SET_AIR_CONTROL` | Управление в воздухе |
| 7 | `SET_BOAT_JUMP_FORCE` | Сила прыжка |
| 8 | `SET_MODE` | Установка режима |
| 9 | `SET_GRAVITY` | Гравитация |
| 10 | `SET_YAW_ACCEL` | Ускорение поворота |
| 11 | `SET_FORWARD_ACCEL` | Ускорение вперёд |
| 12 | `SET_BACKWARD_ACCEL` | Ускорение назад |
| 13 | `SET_TURN_ACCEL` | Ускорение при повороте |
| 14 | `ALLOW_ACCEL_STACKING` | Суммирование ускорений |
| 15 | `RESEND_VERSION` | Перезапрос версии |
| 16 | `SET_UNDERWATER_CONTROL` | Управление под водой |
| 17 | `SET_SURFACE_WATER_CONTROL` | На поверхности воды |
| 18 | `SET_EXCLUSIVE_MODE` | Эксклюзивный режим |
| 19 | `SET_COYOTE_TIME` | Coyote time |
| 20 | `SET_WATER_JUMPING` | Прыжок с воды |
| 21 | `SET_SWIM_FORCE` | Сила плавания |
| 22 | `REMOVE_BLOCKS_SLIPPERINESS` | Удаление скользкости |
| 23 | `CLEAR_SLIPPERINESS` | Очистка скользкости |
| 24 | `MODE_SERIES` | Серия режимов |
| 25 | `EXCLUSIVE_MODE_SERIES` | Серия эксклюзивных режимов |
| 26 | `SET_PER_BLOCK` | Per-block настройка |
| 27 | `SET_COLLISION_MODE` | Режим коллизий |
| 28 | `SET_STEP_WHILE_FALLING` | Шаг при падении |
| 29 | `SET_INTERPOLATION_COMPAT` | Интерполяция |
| 30 | `SET_COLLISION_RESOLUTION` | Разрешение коллизий |
| 31 | `ADD_COLLISION_ENTITYTYPE_FILTER` | Добавление в фильтр |
| 32 | `CLEAR_COLLISION_ENTITYTYPE_FILTER` | Очистка фильтра |
| 33 | `SET_REALISTIC_PHYSICS` | Вкл/выкл реалистичной физики |
| 34 | `SET_VEHICLE_TYPE` | Пресет машины |
| 35 | `SET_VEHICLE_MASS` | Масса машины |
| 36 | `SET_VEHICLE_WHEELBASE` | Колёсная база |
| 37 | `SET_VEHICLE_CG_HEIGHT` | Высота ЦМ |
| 38 | `SET_VEHICLE_TRACK_WIDTH` | Ширина колеи |
| 39 | `SET_VEHICLE_MAX_STEERING` | Макс. руль |
| 40 | `SET_VEHICLE_STEERING_SPEED` | Скорость руления |
| 41 | `SET_VEHICLE_BRAKING_FORCE` | Тормозное усилие |
| 42 | `SET_VEHICLE_ENGINE_FORCE` | Тяга двигателя |
| 43 | `SET_VEHICLE_DRAG` | Аэродинамика |
| 44 | `SET_VEHICLE_BRAKE_BIAS` | Распределение тормозов |
| 45 | `SET_VEHICLE_SUBSTEPS` | Подшаги физики |
| 46 | `SET_VEHICLE_FRONT_WEIGHT_BIAS` | Развесовка |
| 47 | `SET_BLOCK_SURFACE_TYPE` | Тип поверхности блока |
| 48 | `SET_VEHICLE_DRIVETRAIN` | Тип привода |
| 49 | `SET_DEFAULT_SURFACE_TYPE` | Дефолтная поверхность |

### Методы
| Метод | Описание |
|-------|----------|
| `registerCodecs()` | Регистрация кодеков пакетов |
| `registerHandlers()` | Регистрация обработчиков |
| `handlePacket(PacketByteBuf)` | Обработка входящего пакета по ordinal |

---

## ServerboundPackets.java

**Назначение:** Пакеты от клиента к серверу.

### Пакеты
| # | Имя | Описание |
|---|-----|----------|
| 0 | `VERSION` | Версия клиента |

### Методы
| Метод | Описание |
|-------|----------|
| `registerCodecs()` | Регистрация кодеков |
| `registerHandlers()` | Регистрация обработчиков |
| `handlePacket(ByteBuf)` | Обработка пакета |

---

## Modes.java

**Назначение:** Enum предустановленных режимов (31 шт.). Каждый режим устанавливает комбинацию настроек.

### Режимы
| # | Имя | Описание |
|---|-----|----------|
| 0-7 | `BROKEN_SLIME_*` | Режимы с broken slime (rally, parkour, BA) |
| 8-15 | `RALLY`, `BA`, `PARKOUR` и варианты | Стандартные режимы |
| 16-17 | `JUMP_BLOCKS`, `BOOSTER_BLOCKS` | Специальные блоки |
| 18-19 | `DEFAULT_ICE`, `DEFAULT_NINE_EIGHT_FIVE` | Дефолтные поверхности |
| 20-21 | `NOCOL_*` | Режимы без коллизий |
| 22-23 | `BA_JANKLESS`, `BA_BLUE_JANKLESS` | BA без jank |
| 24 | `DEFAULT_BLUE_ICE` | Blue ice по умолчанию |
| 25 | `REALISTIC` | Реалистичная физика (WRC_CAR) |
| 26 | `REALISTIC_WRC` | WRC_CAR |
| 27 | `REALISTIC_GROUP_B` | GROUP_B |
| 28 | `REALISTIC_CLASSIC` | CLASSIC_RALLY |
| 29 | `REALISTIC_LIGHTWEIGHT` | LIGHTWEIGHT |
| 30 | `REALISTIC_TRUCK` | TRUCK |

### Методы
| Метод | Описание |
|-------|----------|
| `setMode(Modes)` | (static) Применение режима (вызывает соответствующие сеттеры) |

---

## CollisionMode.java

**Назначение:** Enum режимов столкновений лодок.

| Значение | Описание |
|----------|----------|
| `VANILLA` | Ванильные коллизии |
| `NO_BOATS_OR_PLAYERS` | Без коллизий с лодками и игроками |
| `NO_ENTITIES` | Без коллизий со всеми сущностями |
| `ENTITYTYPE_FILTER` | Фильтр по типу сущности |
| `NO_BOATS_OR_PLAYERS_PLUS_FILTER` | Без лодок/игроков + фильтр |

---

## GetStepHeight.java

**Назначение:** Интерфейс для получения высоты шага из миксина.

| Метод | Возврат | Описание |
|-------|---------|----------|
| `getStepHeight()` | `float` | Высота шага лодки |

---

## ISettingContext.java

**Назначение:** Интерфейс контекста настроек. Определяет все методы управления настройками лодки. Все методы дублируют API из `OpenBoatUtils`, но через интерфейс.

Содержит **40+ абстрактных методов** — полный набор сеттеров и геттеров для всех настроек.

---

## SingleplayerCommands.java

**Назначение:** Регистрация команд для одиночной игры (тестирование без сервера).

### Метод `registerCommands()`
Регистрирует **50+ команд** для тестирования всех настроек:
- Стандартные: `stepsize`, `reset`, `defaultslipperiness`, `blockslipperiness`, `aircontrol`, `waterelevation`, `falldamage`, `jumpforce`, `boatmode`, `boatgravity`
- Ускорения: `setyawaccel`, `setforwardaccel`, `setbackwardaccel`, `setturnforwardaccel`, `allowaccelstacking`
- Вода: `underwatercontrol`, `surfacewatercontrol`, `waterjumping`, `swimforce`, `coyotetime`
- Коллизии: `collisionmode`, `stepwhilefalling`, `clearcollisionfilter`, `addcollisionfilter`, `setcollisionresolution`
- Скользкость: `removeblockslipperiness`, `clearslipperiness`
- Серии: `modeseries`, `exclusivemodeseries`
- Реалистичная физика: `realisticphysics`, `vehicletype`, `vehiclemass`, `vehiclewheelbase`, `vehiclecgheight`, `vehiclemaxsteering`, `vehiclesteeringspeed`, `vehicleengineforce`, `vehiclebrakingforce`, `vehiclebrakebias`, `vehiclesubsteps`, `vehicleweightbias`, `setsurfacetype`, `vehicledrivetrain`, `defaultsurface`
- Прочее: `setinterpolationten`, `setblocksetting`, `sendversionpacket`

---

## OpenBoatUtilsClient.java

**Пакет:** `dev.o7moon.openboatutils.client`

**Назначение:** Клиентская точка входа (`ClientModInitializer`).

| Метод | Описание |
|-------|----------|
| `onInitializeClient()` | Регистрация обработчиков пакетов и события подключения |

При подключении к серверу вызывает `OpenBoatUtils.sendVersionPacket()`.
