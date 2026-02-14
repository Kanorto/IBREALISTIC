# 🏎️ Реалистичная физика ралли — Документация

## Содержание
- [Обзор системы](#обзор-системы)
- [Архитектура клиент-сервер](#архитектура-клиент-сервер)
- [Типы машин](#типы-машин)
- [Типы привода](#типы-привода)
- [Поверхности](#поверхности)
- [Визуальная физика](#визуальная-физика)
- [Команды мода (Singleplayer)](#команды-мода-singleplayer)
- [Команды плагина (TimingSystem)](#команды-плагина-timingsystem)
- [Режимы (Modes)](#режимы-modes)
- [Сетевые пакеты](#сетевые-пакеты)
- [Физическая модель](#физическая-модель)
- [Примеры настройки](#примеры-настройки)

---

## Обзор системы

Система реалистичной физики заменяет линейную модель управления лодкой на полноценную **четырёхколёсную модель (four-wheel model)** с:

- **Модель шин Fiala/Brush** — нелинейная зависимость боковой силы от угла скольжения
- **Фрикционный круг** — ограничение `√(Fx² + Fy²) ≤ μ·Fz`
- **Массообмен** — перераспределение нагрузки между осями при торможении/разгоне/повороте
- **Чувствительность к нагрузке** — μ уменьшается при увеличении вертикальной нагрузки
- **Релаксация** — задержка нарастания боковых сил (реалистичное нарастание сцепления)
- **Привод** — FWD / RWD / AWD с распределением тяги
- **18 типов поверхностей** — от сухого асфальта до льда, включая дерево, бетон, шерсть и другие
- **Визуальный наклон** — лодка наклоняется при торможении/разгоне/в полёте
- **Вертикальная физика** — реалистичное поведение на кочках, потеря/восстановление сцепления при приземлении

### Поддерживаемые версии Minecraft
- **1.20.4** ✅
- **1.21** ✅
- **1.21.3** ✅

---

## Архитектура клиент-сервер

### Двухканальная система пакетов

IBRealistic использует **двухканальную архитектуру** для разделения базовых и реалистичных настроек:

```
TimingSystem (Плагин)
  │
  ├─ Базовые пакеты (IDs 0-32) ──► openboatutils:settings ──► IBRealistic (Мод)
  │                                                             ├─ Обрабатывает базовую физику
  │                                                             └─ stepHeight, gravity, collision
  │
  └─ Реалистичные (IDs 33-69) ──► ibrealistic:settings ──────► IBRealistic (Мод)
                                                                 ├─ 4-колёсная физика
                                                                 ├─ Настройки машины
                                                                 └─ Поверхности, погода

Клиент (IBRealistic)
  │
  ├─ ClientboundPackets.java — обработчик входящих пакетов
  │   ├─ Канал openboatutils:settings → базовые пакеты (0-32)
  │   └─ Канал ibrealistic:settings → реалистичные (33-69)
  │
  └─ ServerboundPackets.java — отправка VERSION пакета на сервер
      └─ Позволяет серверу определить версию и возможности клиента
```

### Таблица пакетов

#### Базовые пакеты (IDs 0-32) — канал `openboatutils:settings`

Эти пакеты управляют **базовой физикой лодки** (совместимы с оригинальным OpenBoatUtils):

| ID | Пакет | Описание | Данные |
|----|-------|----------|--------|
| 0 | RESET | Сброс настроек | *(нет данных)* |
| 1 | SET_STEP_HEIGHT | Высота ступеньки | `float` stepSize |
| 2 | SET_DEFAULT_SLIPPERINESS | Трение по умолчанию | `float` slipperiness |
| 3 | SET_BLOCKS_SLIPPERINESS | Трение для блоков | `Map<Block, Float>` |
| 4 | SET_BOAT_FALL_DAMAGE | Урон от падения | `boolean` enabled |
| 5 | SET_BOAT_WATER_ELEVATION | Подъём на воде | `boolean` enabled |
| 6 | SET_AIR_CONTROL | Управление в воздухе | `boolean` enabled |
| 7 | SET_BOAT_JUMP_FORCE | Сила прыжка | `float` jumpForce |
| 8 | SET_MODE | Режим (составной) | *(несколько полей)* |
| 9 | SET_GRAVITY | Гравитация | `double` gravityForce |
| 10 | SET_YAW_ACCEL | Ускорение поворота | `float` yawAcceleration |
| 11 | SET_FORWARD_ACCEL | Ускорение вперёд | `float` forwardsAcceleration |
| 12 | SET_BACKWARD_ACCEL | Ускорение назад | `float` backwardsAcceleration |
| 13 | SET_TURN_ACCEL | Ускорение при повороте | `float` turningForwardsAcceleration |
| 14 | ALLOW_ACCEL_STACKING | Суммирование ускорений | `boolean` allowAccelStacking |
| 15 | RESEND_VERSION | Повтор VERSION пакета | *(нет данных)* |
| 16 | SET_UNDERWATER_CONTROL | Управление под водой | `boolean` enabled |
| 17 | SET_SURFACE_WATER_CONTROL | Управление на поверхности | `boolean` enabled |
| 18 | SET_EXCLUSIVE_MODE | Эксклюзивный режим | *(нет данных)* |
| 19 | SET_COYOTE_TIME | Время coyote | `int` coyoteTime |
| 20 | SET_WATER_JUMPING | Прыжок с воды | `boolean` enabled |
| 21 | SET_SWIM_FORCE | Сила плавания | `float` swimForce |
| 22 | REMOVE_BLOCKS_SLIPPERINESS | Удалить трение блоков | `Set<Block>` |
| 23 | CLEAR_SLIPPERINESS | Очистить трение | *(нет данных)* |
| 24 | MODE_SERIES | Серия режимов | *(нет данных)* |
| 25 | EXCLUSIVE_MODE_SERIES | Эксклюзивная серия | *(нет данных)* |
| 26 | SET_PER_BLOCK | Настройки на блок | `Map<Block, Settings>` |
| 27 | SET_COLLISION_MODE | Режим коллизий | `CollisionMode` |
| 28 | SET_STEP_WHILE_FALLING | Шаг при падении | `boolean` enabled |
| 29 | SET_INTERPOLATION_COMPAT | Совместимость интерполяции | `boolean` enabled |
| 30 | SET_COLLISION_RESOLUTION | Разрешение коллизий | `byte` collisionResolution |
| 31 | ADD_COLLISION_ENTITYTYPE_FILTER | Фильтр сущностей | `List<EntityType>` |
| 32 | CLEAR_COLLISION_ENTITYTYPE_FILTER | Очистить фильтр | *(нет данных)* |

#### Реалистичные пакеты (IDs 33-69) — канал `ibrealistic:settings`

Эти пакеты управляют **реалистичной физикой** (уникальны для IBRealistic):

| ID | Пакет | Описание | Данные |
|----|-------|----------|--------|
| 33 | SET_REALISTIC_PHYSICS | Включить реалистичную физику | `boolean` enabled |
| 34 | SET_VEHICLE_TYPE | Тип машины (пресет) | `short` vehicleTypeId (0=WRC_CAR, 1=GROUP_B, ...) |
| 35 | SET_VEHICLE_MASS | Масса машины | `float` mass (кг) |
| 36 | SET_VEHICLE_WHEELBASE | Колёсная база | `float` wheelbase (м) |
| 37 | SET_VEHICLE_CG_HEIGHT | Высота центра масс | `float` cgHeight (м) |
| 38 | SET_VEHICLE_TRACK_WIDTH | Ширина колеи | `float` trackWidth (м) |
| 39 | SET_VEHICLE_MAX_STEERING | Макс. угол руля | `float` maxSteeringAngle (рад) |
| 40 | SET_VEHICLE_STEERING_SPEED | Скорость поворота руля | `float` steeringSpeed (рад/с) |
| 41 | SET_VEHICLE_BRAKING_FORCE | Сила торможения | `float` brakingForce (Н) |
| 42 | SET_VEHICLE_ENGINE_FORCE | Сила двигателя | `float` engineForce (Н) |
| 43 | SET_VEHICLE_DRAG | Аэродинамическое сопротивление | `float` dragCoefficient |
| 44 | SET_VEHICLE_BRAKE_BIAS | Распределение тормозов | `float` brakeBias (перед/зад) |
| 45 | SET_VEHICLE_SUBSTEPS | Подшаги физики | `int` substeps (1-10) |
| 46 | SET_VEHICLE_FRONT_WEIGHT_BIAS | Развесовка | `float` frontWeightBias (доля спереди) |
| 47 | SET_BLOCK_SURFACE_TYPE | Тип поверхности блока | `String` blockId + `String` surfaceType |
| 48 | SET_DEFAULT_SURFACE_TYPE | Поверхность по умолчанию | `String` surfaceName |
| 49 | SET_VEHICLE_DRIVETRAIN | Тип привода | `short` drivetrainId (0=RWD, 1=FWD, 2=AWD) |
| 50 | SET_VEHICLE_SPEED_STEERING | Зависимость руля от скорости | `float` speedSteeringFactor |
| 51 | SET_VEHICLE_ENGINE_BRAKING | Торможение двигателем | `float` engineBraking (Н) |
| 52 | SET_VEHICLE_ROLL_STIFFNESS | Жёсткость подвески на крен | `float` rollStiffnessRatioFront |
| 53 | SET_VEHICLE_CONFIG | Полная конфигурация | `ByteArrayInputStream` (бинарная) |
| 54 | SET_AWD_FRONT_SPLIT | Распределение тяги AWD | `float` awdFrontSplit (0=зад, 1=перед) |
| 55 | SET_FRONT_DIFFERENTIAL | Передний дифференциал | `byte` differentialType (OPEN/LOCKED/LSD) |
| 56 | SET_REAR_DIFFERENTIAL | Задний дифференциал | `byte` differentialType |
| 57 | SET_LSD_LOCKING_COEFF | Коэф. блокировки LSD | `float` lsdLockingCoeff (0-1) |
| 58 | SET_DOWNFORCE_COEFFICIENT | Прижимная сила | `float` downforceCoefficient |
| 59 | SET_DOWNFORCE_FRONT_BIAS | Распределение прижима | `float` downforceFrontBias |
| 60 | SET_WEATHER | Погодные условия | `byte` weatherCondition |
| 61 | SET_STEERING_RETURN_RATE | Скорость возврата руля | `float` steeringReturnRate (рад/с) |
| 61 | REALISTIC_SERVER_INFO | Информация о сервере | `int` version + `int` features + `String` name |
| 62 | SET_TIRE_PRESET | Пресет шин | `byte` tirePreset |
| 63 | SET_SUSPENSION_PRESET | Пресет подвески | `byte` suspensionPreset |
| 64 | SET_ENGINE_PRESET | Пресет двигателя | `byte` enginePreset |
| 65 | SET_BODY_PRESET | Пресет кузова | `byte` bodyPreset |
| 66 | SET_STEERING_PRESET | Пресет рулевого управления | `byte` steeringPreset |
| 67 | SET_BRAKE_PRESET | Пресет тормозов | `byte` brakePreset |
| 68 | SET_WEIGHT_DISTRIBUTION_PRESET | Пресет развесовки | `byte` weightDistributionPreset |
| 69 | SET_RACE_COUNTDOWN | Обратный отсчёт гонки | `long` goTimeMs + `int` seconds |

### Версионирование протокола

**Текущая версия протокола:** `VERSION = 18`

- Мод отправляет VERSION пакет при подключении к серверу
- Сервер проверяет совместимость версий
- При несовместимости — игрок получает уведомление и сброс настроек

### Взаимодействие с базовой физикой

Когда **реалистичная физика активна** (`fourWheelPhysics.isEnabled() == true`):

1. **BoatMixin блокирует ванильную физику:**
   - `cancelVanillaPaddles` — отменяет обработку W/A/S/D в `updatePaddles()`
   - `cancelVanillaVelocityDecay` — отменяет ванильное затухание скорости, устанавливает `velocityDecay = 1.0`

2. **FourWheelPhysicsEngine управляет движением:**
   - Рассчитывает силы шин, массообмен, ускорение
   - Напрямую устанавливает `velocity` и `yaw` лодки
   - Применяет landing speed preservation при приземлении

3. **Базовые настройки продолжают работать:**
   - `stepHeight` — для преодоления ступенек (от OBU-пакетов)
   - `collision` — режим коллизий (VANILLA/NOCOL)
   - `airControl` — определяет, работает ли физика в воздухе

---

## Типы машин

| Тип | Масса | База | ЦТ | Колея | Развесовка | Привод | Рулевой угол | Мощность | Торможение |
|-----|-------|------|-----|-------|------------|--------|-------------|----------|-----------|
| **WRC_CAR** | 1190 кг | 2.53 м | 0.45 м | 1.55 м | 55/45 | AWD | 0.50 рад | 5500 Н | 8000 Н |
| **GROUP_B** | 1100 кг | 2.40 м | 0.50 м | 1.50 м | 45/55 | RWD | 0.48 рад | 6000 Н | 7500 Н |
| **CLASSIC_RALLY** | 1000 кг | 2.45 м | 0.55 м | 1.45 м | 50/50 | RWD | 0.45 рад | 4000 Н | 6000 Н |
| **LIGHTWEIGHT** | 800 кг | 2.30 м | 0.42 м | 1.40 м | 60/40 | FWD | 0.55 рад | 3000 Н | 5500 Н |
| **TRUCK** | 2000 кг | 3.20 м | 0.90 м | 1.80 м | 50/50 | AWD | 0.35 рад | 8000 Н | 10000 Н |

### Характеристики каждого типа

**WRC_CAR** — Современный раллийный автомобиль (Subaru Impreza, Toyota Yaris WRC)
- Полный привод обеспечивает стабильность и тягу
- Компактная база, низкий ЦТ — отличная управляемость
- Высокая тормозная эффективность

**GROUP_B** — Легендарные машины Group B (Lancia 037, Audi Quattro)
- Задний привод — склонность к заносу, требует мастерства
- Мощный двигатель, лёгкий вес
- Низкий аэродинамический коэффициент

**CLASSIC_RALLY** — Классические раллийные автомобили (Ford Escort Mk1, Lancia Fulvia)
- Задний привод с нейтральной развесовкой 50/50
- Меньшая мощность, более мягкий руль
- Более высокое сопротивление качению

**LIGHTWEIGHT** — Лёгкие машины (Citroën DS3 R3, Peugeot 208 Rally4)
- Передний привод — безопаснее, но склонность к сносу
- Очень лёгкая — быстрый отклик
- Широкий рулевой угол — маневренность

**TRUCK** — Раллийные грузовики (КамАЗ, Татра)
- Полный привод, очень тяжёлый
- Высокий ЦТ — раскачивание при поворотах
- Максимальная тормозная и тяговая сила

---

## Типы привода

| Тип | Распределение тяги | Поведение |
|-----|-------------------|-----------|
| **RWD** (задний) | 0% перед / 100% зад | Склонность к заносу при газе, классический дрифт |
| **FWD** (передний) | 100% перед / 0% зад | Склонность к сносу, безопаснее на льду |
| **AWD** (полный) | 50% перед / 50% зад | Лучшая тяга, стабильность, современный стандарт |

### Влияние на физику
- **RWD**: при нажатии газа задние колёса получают всю тягу → легко перегрузить задние шины → занос. Ручник (пробел) + руль = классический дрифт.
- **FWD**: передние колёса тянут и рулят одновременно → при агрессивном газе снос передней оси. Ручник блокирует задние колёса → полезен для вращения вокруг передней оси.
- **AWD**: тяга распределяется равномерно → труднее вызвать занос, но возможно с ручником. Максимальное ускорение из поворота.

---

## Поверхности

### Пресеты

| Поверхность | μ_peak | μ_slide | Корн. жёсткость | Релаксация | Сопр. качению | Описание |
|-------------|--------|---------|-----------------|-----------|--------------|----------|
| **ASPHALT_DRY** | 0.85 | 0.70 | 65000 | 0.06 | 0.012 | Сухой асфальт — максимальное сцепление |
| **ASPHALT_WET** | 0.55 | 0.40 | 50000 | 0.08 | 0.015 | Мокрый асфальт — аквапланирование |
| **CONCRETE** | 0.80 | 0.65 | 60000 | 0.07 | 0.013 | Бетон — отличная дорога |
| **BRICK** | 0.75 | 0.60 | 55000 | 0.08 | 0.016 | Кирпич — хорошее сцепление |
| **METAL** | 0.70 | 0.55 | 55000 | 0.06 | 0.010 | Металл — среднее-высокое сцепление |
| **TERRACOTTA** | 0.65 | 0.55 | 45000 | 0.09 | 0.018 | Терракота — хорошее сцепление |
| **GRAVEL** | 0.55 | 0.50 | 30000 | 0.15 | 0.030 | Гравий — непредсказуемое сцепление |
| **WOOD** | 0.50 | 0.42 | 35000 | 0.10 | 0.020 | Дерево — среднее сцепление |
| **NETHER** | 0.50 | 0.40 | 35000 | 0.12 | 0.025 | Незер — инопланетная поверхность |
| **DIRT** | 0.45 | 0.40 | 25000 | 0.18 | 0.035 | Грунт — мягкая поверхность |
| **SAND** | 0.40 | 0.35 | 18000 | 0.20 | 0.060 | Песок — высокое сопротивление |
| **VEGETATION** | 0.40 | 0.35 | 22000 | 0.18 | 0.040 | Растительность — мягко и медленно |
| **GLASS** | 0.35 | 0.25 | 40000 | 0.05 | 0.008 | Стекло — скользкое |
| **SNOW** | 0.30 | 0.22 | 22000 | 0.12 | 0.025 | Снег — скользко с малым сопротивлением |
| **MUD** | 0.30 | 0.25 | 16000 | 0.25 | 0.050 | Грязь — минимальное сцепление |
| **WOOL** | 0.70 | 0.60 | 28000 | 0.20 | 0.045 | Шерсть — много сцепления, но медленно |
| **ICE** | 0.10 | 0.07 | 10000 | 0.10 | 0.008 | Лёд — очень скользко |
| **BLUE_ICE** | 0.06 | 0.04 | 7000 | 0.10 | 0.008 | Синий лёд — ещё более скользко |

### Привязка к блокам Minecraft

| Minecraft блоки | Поверхность |
|-----------------|-------------|
| `stone`, `deepslate`, `blackstone`, `smooth_stone`, `obsidian`, `quartz`, руды, песчаник | ASPHALT_DRY |
| `cobblestone`, `mossy_cobblestone`, `andesite`, `granite`, `tuff`, `prismarine` | ASPHALT_WET |
| Все цвета `concrete` (16 шт.) | CONCRETE |
| Все цвета `terracotta` + `glazed_terracotta` (32 шт.) | TERRACOTTA |
| Все виды досок, брёвна, обтёсанные брёвна, бамбук (60+ блоков) | WOOD |
| Все цвета `wool` + `carpet` + `moss` (34 шт.) | WOOL |
| `bricks`, `nether_bricks`, `deepslate_bricks`, `mud_bricks` | BRICK |
| `iron_block`, `gold_block`, все виды `copper`, `netherite_block`, и т.д. | METAL |
| Все виды `glass` + `stained_glass` (34 шт.) | GLASS |
| `netherrack`, `basalt`, `magma_block`, `glowstone` | NETHER |
| `hay_block`, `sponge`, `honeycomb_block`, `slime_block`, `honey_block` | VEGETATION |
| `gravel` | GRAVEL |
| `dirt`, `grass_block`, `dirt_path`, `podzol`, `mycelium`, `clay` | DIRT |
| `mud`, `soul_sand`, `soul_soil`, `mangrove_roots` | MUD |
| `snow_block`, `powder_snow` | SNOW |
| `ice`, `packed_ice`, `frosted_ice` | ICE |
| `blue_ice` | BLUE_ICE (μ=0.06) |
| `sand`, `red_sand`, все цвета `concrete_powder` | SAND |
| Все кровати (16 шт.) | WOOL |
| Все ящики шалкера (17 шт.) | WOOD |

> **Все немаппированные блоки** используют поверхность по умолчанию (ASPHALT_DRY).

### Поверхность по умолчанию

Блоки, не указанные в маппинге, используют **поверхность по умолчанию**. По умолчанию это `ASPHALT_DRY`.

Для ледовых треков (где все блоки — лёд) установите:
```
/defaultsurface ICE
```

---

## Вертикальная физика

### Кочки и прыжки

Система реалистично обрабатывает проезд по кочкам и прыжки:

1. **Детекция полёта** — когда лодка отрывается от поверхности, считается что она в воздухе
2. **В воздухе** — шины не работают, только аэродинамическое сопротивление. Руль почти не действует
3. **Приземление** — при посадке рассчитывается сила удара на основе вертикальной скорости
4. **Потеря сцепления** — жёсткая посадка временно снижает сцепление (до 60%)
5. **Восстановление** — сцепление плавно возвращается к норме (~0.75 секунды)

### Параметры вертикальной физики

| Параметр | Значение | Описание |
|----------|----------|----------|
| Порог удара | -1.5 м/с | Вертикальная скорость, при которой начинается потеря сцепления |
| Макс. потеря сцепления | 60% | Максимальное снижение μ при самом жёстком приземлении |
| Скорость восстановления | 0.05/тик | Как быстро сцепление возвращается к норме |
| Мин. время в воздухе | 3 тика | Минимальное время полёта для учёта удара (мелкие кочки игнорируются) |

### Визуальный наклон в полёте

- **При взлёте (вертикальная скорость > 0)** — нос лодки поднимается вверх
- **При падении (вертикальная скорость < 0)** — нос лодки опускается вниз
- Наклон пропорционален вертикальной скорости, ограничен для предотвращения нереалистичных углов

---

## Визуальная физика

### Наклон при торможении/разгоне (Pitch)
- **Торможение** → нос лодки наклоняется вниз (вес переносится на переднюю ось)
- **Разгон** → нос лодки наклоняется вверх (вес переносится на заднюю ось)
- **Взлёт** → нос поднимается вверх
- **Падение** → нос опускается вниз
- Максимальный угол: ±30°

### Крен при поворотах (Roll)
- **Поворот направо** → лодка кренится влево (перенос массы на левую сторону)
- **Поворот налево** → лодка кренится вправо
- Крен пропорционален боковому ускорению
- Визуальный крен применяется через отдельный Mixin в рендерере лодки
- Крен не влияет на физику — это только визуальный эффект

### Визуальные колёса
- При активной реалистичной физике на лодке отображаются 4 колеса
- **Передние колёса** поворачиваются в соответствии с углом руля
- **Все колёса** вращаются пропорционально продольной скорости
- Колёса расположены по углам лодки (FL, FR, RL, RR)
- Визуализация работает на всех поддерживаемых версиях MC

### Возврат руля (Self-Aligning Torque)
- При отпускании клавиш руления руль **пассивно возвращается в центр**
- Скорость возврата зависит от текущей скорости машины (быстрее едешь → быстрее возврат)
- Параметр: `steeringReturnRate` (по умолчанию 1.5 рад/с)
- При `steeringReturnRate = 0` — возврат руля отключён
- При высоком значении (10-20) — руль возвращается почти мгновенно

### Стабильность рулевого управления
- При **смене направления руля** (например, с лево на право) накопленные боковые силы сбрасываются — это предотвращает кратковременный контр-поворот
- **Боковая скорость** ограничена 120% от продольной — предотвращает неуправляемый занос при поворотах >90°
- Боковая скорость затухает даже при активном рулении (мягкое затухание 0.99/субшаг)

### Защита от высоких скоростей
- Максимальная скорость: 100 м/с (~360 км/ч)
- Максимальная скорость вращения: 15 рад/с
- Автоматическая защита от NaN/Infinity — при численной нестабильности состояние сбрасывается

### Ручник (Spacebar)
- Нажатие пробела блокирует задние колёса на 80% тормозной силы
- Работает независимо от обычного торможения
- Позволяет инициировать дрифт (особенно эффективно на RWD/AWD)

---

## Команды мода (Singleplayer)

### Основные
| Команда | Описание |
|---------|----------|
| `/realisticphysics true/false` | Включить/выключить реалистичную физику |
| `/vehicletype WRC_CAR\|GROUP_B\|CLASSIC_RALLY\|LIGHTWEIGHT\|TRUCK` | Выбрать пресет машины |
| `/vehicledrivetrain RWD\|FWD\|AWD` | Установить тип привода |
| `/defaultsurface ICE\|ASPHALT_DRY\|...` | Установить поверхность по умолчанию |

### Параметры машины
| Команда | Диапазон | Описание |
|---------|---------|----------|
| `/vehiclemass <кг>` | 100–5000 | Масса машины |
| `/vehiclewheelbase <м>` | 1–5 | Колёсная база |
| `/vehiclecgheight <м>` | 0.1–2 | Высота центра тяжести |
| `/vehiclemaxsteering <рад>` | 0.1–1.2 | Максимальный угол руля |
| `/vehiclesteeringspeed <рад/с>` | 0.5–10 | Скорость вращения руля |
| `/vehicleengineforce <Н>` | 1000–20000 | Тяговая сила двигателя |
| `/vehiclebrakingforce <Н>` | 1000–20000 | Тормозная сила |
| `/vehiclebrakebias <0–1>` | 0–1 | Распределение тормозов (1.0 = всё на перед) |
| `/vehiclesubsteps <шаги>` | 1–10 | Количество подшагов за тик (4 = 80Hz) |
| `/vehicleweightbias <0.3–0.7>` | 0.3–0.7 | Распределение веса (0.55 = 55% перед) |
| `/steeringreturnrate <рад/с>` | 0–20 | Скорость пассивного возврата руля в центр (0 = отключено) |

### Поверхности
| Команда | Описание |
|---------|----------|
| `/setsurfacetype <блок> <поверхность>` | Привязать блок к поверхности |
| `/defaultsurface <поверхность>` | Установить поверхность для немаппированных блоков |

---

## Команды плагина (TimingSystem)

### Редактирование режима (bume = Boat Utils Mode Edit)

```
/ts bume set realisticPhysics true
/ts bume set vehicleType 0               # 0=WRC, 1=GROUP_B, 2=CLASSIC, 3=LIGHTWEIGHT, 4=TRUCK
/ts bume set vehicleDrivetrain RWD       # RWD, FWD, или AWD
/ts bume set defaultSurfaceType ICE      # Поверхность по умолчанию
/ts bume set vehicleMass 1190
/ts bume set vehicleWheelbase 2.53
/ts bume set vehicleCgHeight 0.45
/ts bume set vehicleTrackWidth 1.55
/ts bume set vehicleMaxSteering 0.60
/ts bume set vehicleSteeringSpeed 2.5
/ts bume set vehicleBrakingForce 8000
/ts bume set vehicleEngineForce 5500
/ts bume set vehicleDrag 0.35
/ts bume set vehicleBrakeBias 0.65
/ts bume set vehicleSubsteps 4
/ts bume set vehicleFrontWeightBias 0.55
/ts bume set steeringReturnRate 1.5    # Скорость возврата руля (0 = отключено)
/ts bume addsurfacetype ICE minecraft:blue_ice
/ts bume clearsurfacetypes
```

---

## Режимы (Modes)

| ID | Режим | Описание |
|----|-------|----------|
| 25 | `REALISTIC` | Базовый реалистичный режим (WRC, slipperiness=0.98) |
| 26 | `REALISTIC_WRC` | WRC машина (AWD, 1190кг) |
| 27 | `REALISTIC_GROUP_B` | Group B (RWD, 1100кг) |
| 28 | `REALISTIC_CLASSIC` | Classic Rally (RWD, 1000кг) |
| 29 | `REALISTIC_LIGHTWEIGHT` | Lightweight (FWD, 800кг) |
| 30 | `REALISTIC_TRUCK` | Rally Truck (AWD, 2000кг) |
| 31 | `REALISTIC_ALLTERRAIN` | Вездеход (WRC, все блоки, без урона, airStepping) |

Все режимы автоматически включают: `airControl=true`, `fallDamage=false`, `stepSize=1.25`.
Режим `REALISTIC_ALLTERRAIN` дополнительно включает `canStepWhileFalling=true` для плавной езды по неровностям.

---

## Сетевые пакеты

**См. подробное описание в разделе [Архитектура клиент-сервер](#архитектура-клиент-сервер)**

Ниже приведён краткий список реалистичных пакетов (IDs 33-69) для быстрого reference:

| ID | Пакет | Данные |
|----|-------|--------|
| 33 | `SET_REALISTIC_PHYSICS` | `boolean` enabled |
| 34 | `SET_VEHICLE_TYPE` | `short` vehicleTypeId (0-4) |
| 35 | `SET_VEHICLE_MASS` | `float` mass |
| 36 | `SET_VEHICLE_WHEELBASE` | `float` wheelbase |
| 37 | `SET_VEHICLE_CG_HEIGHT` | `float` cgHeight |
| 38 | `SET_VEHICLE_TRACK_WIDTH` | `float` trackWidth |
| 39 | `SET_VEHICLE_MAX_STEERING` | `float` maxSteeringAngle |
| 40 | `SET_VEHICLE_STEERING_SPEED` | `float` steeringSpeed |
| 41 | `SET_VEHICLE_BRAKING_FORCE` | `float` brakingForce |
| 42 | `SET_VEHICLE_ENGINE_FORCE` | `float` engineForce |
| 43 | `SET_VEHICLE_DRAG` | `float` dragCoefficient |
| 44 | `SET_VEHICLE_BRAKE_BIAS` | `float` brakeBias |
| 45 | `SET_VEHICLE_SUBSTEPS` | `int` substeps |
| 46 | `SET_VEHICLE_FRONT_WEIGHT_BIAS` | `float` frontWeightBias |
| 47 | `SET_BLOCK_SURFACE_TYPE` | `String` blockId + `String` surfaceType |
| 48 | `SET_VEHICLE_DRIVETRAIN` | `short` drivetrainId (0=RWD, 1=FWD, 2=AWD) |
| 49 | `SET_DEFAULT_SURFACE_TYPE` | `String` surfaceName |
| 60 | `SET_STEERING_RETURN_RATE` | `float` steeringReturnRate (рад/с, 0 = отключено) |

Требуется версия мода: **18** (VERSION=18)

---

## Физическая модель

### Четырёхколёсная модель (Four-Wheel Model)

Полная четырёхколёсная модель с независимыми силами для каждого колеса (FL, FR, RL, RR):

```
    FL ────────●──────── FR
               │
               │ Lf (Front Axle Distance)
               │
          ─────●───── CG (Center of Gravity)
               │
               │ Lr (Rear Axle Distance)
               │
    RL ────────●──────── RR
```

- **Lf** = расстояние от ЦТ до передней оси = wheelbase × frontWeightBias
- **Lr** = расстояние от ЦТ до задней оси = wheelbase × (1 - frontWeightBias)

### Углы скольжения (Slip Angles)

Для каждого колеса вычисляется индивидуальный угол скольжения с учётом его положения:

```
// Боковая скорость каждого колеса (с учётом yawRate и ширины колеи)
vyFL = vy + yawRate × Lf + yawRate × halfTrack
vyFR = vy + yawRate × Lf - yawRate × halfTrack
vyRL = vy - yawRate × Lr + yawRate × halfTrack
vyRR = vy - yawRate × Lr - yawRate × halfTrack

// Углы скольжения
αFL = atan2(vyFL, |vx|) - effectiveSteering
αFR = atan2(vyFR, |vx|) - effectiveSteering
αRL = atan2(vyRL, |vx|)
αRR = atan2(vyRR, |vx|)
```

Эффективный угол руля уменьшается на высокой скорости:
```
effectiveSteering = steeringAngle / (1 + speedSteeringFactor × vx²)
```

### Модель шин Fiala/Brush

При малых углах скольжения (α < α_slide):
```
Fy = -Cα·tan(α) + Cα²/(3μFz)·|tan(α)|·tan(α) - Cα³/(27μ²Fz²)·tan³(α)
```

При больших углах (полное скольжение):
```
Fy = -μ_slide · Fz · sign(α)
```

С прогрессивным падением μ после пикового угла.

### Массообмен (Weight Transfer)

**Продольный** (торможение/разгон):
```
ΔFz = m · ax · h / L
Fz_front = static_front - ΔFz   (торможение → ax < 0 → ΔFz < 0 → front растёт)
Fz_rear  = static_rear  + ΔFz
```

**Боковой** (повороты):
```
ΔFz_lat = m · ay · h / track_width
```

### Распределение привода

```
driveForce_front = throttle × engineForce × frontDriveRatio
driveForce_rear  = throttle × engineForce × (1 - frontDriveRatio)
```

Где `frontDriveRatio`: RWD=0.0, FWD=1.0, AWD=0.5

Внутри каждой оси тяга распределяется между левым и правым колёсами через **дифференциал**:
- **Open** — тяга пропорциональна нагрузке на колесо (легко теряет тягу на разгруженном колесе)
- **Locked** — равная тяга на оба колеса (максимальная тяга, но руль тяжелее)
- **LSD** — смесь Open и Locked через коэффициент блокировки

### Ручник (Handbrake)

При нажатии пробела:
```
brakeForce_rear = brakingForce × 0.8
```

Это блокирует задние колёса, вызывая потерю бокового сцепления задней оси → занос.

### Субшаги (Substeps)

Каждый тик Minecraft (50мс) разбивается на `substeps` подшагов:
- 4 субшага → 80 Hz эффективная частота
- 8 субшагов → 160 Hz
- 10 субшагов → 200 Hz

Больше субшагов = стабильнее симуляция, но больше нагрузка на процессор.

---

## Примеры настройки

### Ледовое ралли (все треки на льду)
```
/defaultsurface ICE
/realisticphysics true
/vehicletype WRC_CAR
/vehicledrivetrain AWD
```

### Бездорожье
```
/defaultsurface DIRT
/setsurfacetype minecraft:gravel GRAVEL
/setsurfacetype minecraft:mud MUD
/setsurfacetype minecraft:sand SAND
/realisticphysics true
/vehicletype TRUCK
/vehicledrivetrain AWD
```

### Ретро-ралли (классические машины, задний привод)
```
/defaultsurface ASPHALT_DRY
/setsurfacetype minecraft:gravel GRAVEL
/setsurfacetype minecraft:dirt DIRT
/realisticphysics true
/vehicletype CLASSIC_RALLY
/vehicledrivetrain RWD
```

### Дрифт на льду
```
/defaultsurface ICE
/realisticphysics true
/vehicletype GROUP_B
/vehicledrivetrain RWD
/vehicleengineforce 7000
```
**Управление:** газ (W) + ручник (пробел) + руль (A/D) для дрифта.

### Настройка через TimingSystem (для серверов)
```
/ts bume new ice_rally
/ts bume select ice_rally
/ts bume set realisticPhysics true
/ts bume set defaultSurfaceType ICE
/ts bume set vehicleDrivetrain AWD
/ts bume set vehicleMass 1190
/ts bume save
```
