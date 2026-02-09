# Физический движок мода (physics/)

Пакет: `dev.o7moon.openboatutils.physics`

> **Важно:** Классы физики НЕ должны импортировать Minecraft API (за исключением `RealisticPhysicsEngine`, который работает с `BoatEntity` для детекции поверхности).

---

## RealisticPhysicsEngine.java

**Назначение:** Основной Stateful движок Bicycle Model. Один экземпляр на клиент.

### Константы
| Имя | Тип | Значение | Описание |
|-----|-----|----------|----------|
| `GRAVITY` | `float` | `9.81f` | Ускорение свободного падения (м/с²) |
| `TICK_TIME` | `float` | `0.05f` | Длительность тика (20 TPS) |
| `MIN_MU_PEAK` | `float` | `0.01f` | Минимальное значение μ для защиты от деления на ноль |
| `YAW_RATE_DAMPING` | `float` | `0.995f` | Демпфирование угловой скорости |

### Поля состояния
| Имя | Тип | Описание |
|-----|-----|----------|
| `vx` | `float` | Продольная скорость (м/с) в системе координат машины |
| `vy` | `float` | Поперечная скорость (м/с) в системе координат машины |
| `yawAngle` | `float` | Угол курса (рад) |
| `yawRate` | `float` | Угловая скорость (рад/с) |
| `steeringAngle` | `float` | Текущий угол поворота руля с задержкой (рад) |
| `axPrev` | `float` | Продольное ускорение предыдущего шага (для массообмена) |
| `ayPrev` | `float` | Поперечное ускорение предыдущего шага (для массообмена) |
| `fyFrontActual` | `float` | Текущая боковая сила передней оси (с релаксацией) |
| `fyRearActual` | `float` | Текущая боковая сила задней оси (с релаксацией) |
| `fzFront` | `float` | Вертикальная нагрузка на переднюю ось (Н) |
| `fzRear` | `float` | Вертикальная нагрузка на заднюю ось (Н) |
| `lastBoatId` | `int` | ID последней лодки (для сброса состояния) |
| `config` | `VehicleConfig` | Конфигурация текущей машины |
| `enabled` | `boolean` | Включена ли реалистичная физика |
| `currentSurface` | `SurfaceProperties` | Текущая поверхность под лодкой |
| `frictionResultFront` | `FrictionCircleResult` | Переиспользуемый объект для передней оси (thread-safe) |
| `frictionResultRear` | `FrictionCircleResult` | Переиспользуемый объект для задней оси (thread-safe) |

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `RealisticPhysicsEngine()` | — | Конструктор, создаёт default config |
| `setConfig(VehicleConfig)` | `void` | Установка конфигурации и сброс состояния |
| `setEnabled(boolean)` | `void` | Включение/выключение |
| `isEnabled()` | `boolean` | Проверка статуса |
| `resetState()` | `void` | Сброс всех полей состояния к начальным |
| `getConfig()` | `VehicleConfig` | Получение текущей конфигурации |
| `update(BoatEntity, float, float, float, boolean)` | `PhysicsResult` | **Основной метод.** Расчёт физики за один тик. Параметры: лодка, руль, газ, тормоз, ручник |
| `detectSurface(BoatEntity)` | `SurfaceProperties` | Детекция поверхности под лодкой по блокам |
| `getVx()` | `float` | Getter продольной скорости |
| `getVy()` | `float` | Getter поперечной скорости |
| `getYawRate()` | `float` | Getter угловой скорости |
| `getSteeringAngle()` | `float` | Getter угла руля |
| `getFzFront()` | `float` | Getter нагрузки спереди |
| `getFzRear()` | `float` | Getter нагрузки сзади |
| `getCurrentSurface()` | `SurfaceProperties` | Getter текущей поверхности |

### Алгоритм `update()` (10 шагов в substeps):
1. **Steering** — ограничение скорости руления + снижение от скорости
2. **Weight Transfer** — продольный и поперечный перенос массы
3. **Load Sensitivity** — вычисление эффективного μ для каждой оси
4. **Slip Angles** — углы проскальзывания передней и задней оси
5. **Lateral Forces** — боковые силы по модели Fiala + релаксация
6. **Longitudinal Forces** — тяга, торможение, engine braking
7. **Friction Circle** — ограничение суммарного вектора сил окружностью трения
8. **Aerodynamic Drag** — аэродинамическое сопротивление + сопротивление качению
9. **Sum Forces** — суммирование сил и вычисление ускорений
10. **Integrate** — интегрирование скоростей и угловой скорости

### Внутренний класс: PhysicsResult
| Поле | Тип | Описание |
|------|-----|----------|
| `velocityX` | `float` | Скорость X (blocks/tick) |
| `velocityY` | `float` | Скорость Y (blocks/tick, вертикальная) |
| `velocityZ` | `float` | Скорость Z (blocks/tick) |
| `yawDelta` | `float` | Изменение курса (градусы) |
| `fzFront` | `float` | Нагрузка на переднюю ось (Н) |
| `fzRear` | `float` | Нагрузка на заднюю ось (Н) |
| `pitchAngle` | `float` | Угол тангажа (рад) |
| `rollAngle` | `float` | Угол крена (рад) |
| `steeringAngle` | `float` | Угол руля (рад) |

---

## VehicleConfig.java

**Назначение:** Mutable конфигурация машины. Содержит все параметры автомобиля.

### Поля
| Имя | Тип | Default | Описание |
|-----|-----|---------|----------|
| `mass` | `float` | `1190f` | Масса (кг) |
| `wheelbase` | `float` | `2.53f` | Колёсная база (м) |
| `cgHeight` | `float` | `0.45f` | Высота центра масс (м) |
| `trackWidth` | `float` | `1.55f` | Ширина колеи (м) |
| `frontWeightBias` | `float` | `0.55f` | Развесовка (доля массы на передней оси) |
| `maxSteeringAngle` | `float` | `0.50f` | Максимальный угол руля (рад) |
| `steeringSpeed` | `float` | `5.0f` | Скорость поворота руля (рад/с) |
| `brakingForce` | `float` | `8000f` | Сила торможения (Н) |
| `engineForce` | `float` | `5500f` | Сила двигателя (Н) |
| `dragCoefficient` | `float` | `0.35f` | Коэффициент аэродинамического сопротивления |
| `rollingResistance` | `float` | `0.015f` | Коэффициент сопротивления качению |
| `brakeBias` | `float` | `0.65f` | Распределение тормозного усилия (доля на переднюю ось) |
| `engineBraking` | `float` | `800f` | Сила торможения двигателем (Н) |
| `substeps` | `int` | `4` | Количество подшагов за тик |
| `speedSteeringFactor` | `float` | `0.004f` | Фактор снижения руления от скорости |
| `rollStiffnessRatioFront` | `float` | `0.55f` | Доля жёсткости стабилизатора на передней оси |
| `drivetrain` | `DrivetrainType` | `AWD` | Тип привода |

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `getFrontAxleDistance()` | `float` | Расстояние от ЦМ до передней оси |
| `getRearAxleDistance()` | `float` | Расстояние от ЦМ до задней оси |
| `getStaticFrontLoad()` | `float` | Статическая нагрузка на переднюю ось (Н) |
| `getStaticRearLoad()` | `float` | Статическая нагрузка на заднюю ось (Н) |
| `createDefault()` | `VehicleConfig` | (static) Создание дефолтной конфигурации (WRC_CAR) |

---

## VehicleType.java

**Назначение:** Enum пресетов машин с предопределёнными параметрами.

### Значения
| Тип | Масса | База | CG | Колея | Развес. | Макс.руль | Привод |
|-----|-------|------|----|-------|---------|-----------|--------|
| `WRC_CAR` | 1190 | 2.53 | 0.45 | 1.55 | 0.55 | 0.50 | AWD |
| `GROUP_B` | 1100 | 2.40 | 0.50 | 1.50 | 0.45 | 0.48 | RWD |
| `CLASSIC_RALLY` | 1000 | 2.45 | 0.55 | 1.45 | 0.50 | 0.45 | RWD |
| `LIGHTWEIGHT` | 800 | 2.30 | 0.42 | 1.40 | 0.60 | 0.55 | FWD |
| `TRUCK` | 2000 | 3.20 | 0.90 | 1.80 | 0.50 | 0.35 | AWD |

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `toConfig()` | `VehicleConfig` | Преобразование пресета в mutable конфигурацию |

---

## DrivetrainType.java

**Назначение:** Enum типов привода с логикой распределения тяги.

### Значения
| Тип | ID | Описание | Доля тяги на переднюю ось |
|-----|----|----------|---------------------------|
| `RWD` | 0 | Задний привод | 0.0 |
| `FWD` | 1 | Передний привод | 1.0 |
| `AWD` | 2 | Полный привод | 0.5 |

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `fromId(int)` | `DrivetrainType` | (static) Получение типа по ID |
| `getFrontDriveRatio()` | `float` | Доля тяги на переднюю ось |

---

## TireModel.java

**Назначение:** Stateless модель шин Fiala/Brush. Все методы — чистые static функции.

### Константы
| Имя | Тип | Значение | Описание |
|-----|-----|----------|----------|
| `DEG_TO_RAD` | `float` | `π/180` | Градусы в радианы |
| `MIN_SPEED` | `float` | `1.0f` | Минимальная скорость для расчёта slip angle |

### Методы
| Метод | Параметры | Возврат | Описание |
|-------|-----------|---------|----------|
| `computeSlipAngle` | `vy, vx, yawRate, axleDist, steer` | `float` | Угол проскальзывания (рад) |
| `computeLateralForce` | `slipAngle, fz, surface` | `float` | Боковая сила шины по модели Fiala (Н), читает mu из surface |
| `computeLateralForce` | `slipAngle, fz, muPeak, muSlide, corneringStiffness, peakSlipAngleDeg, slipAngleFalloff` | `float` | Боковая сила с явными параметрами mu (thread-safe) |
| `computeLongitudinalForce` | `driveForce, brakeForce, fz, surface, vx` | `float` | Продольная сила (Н), читает mu из surface |
| `computeLongitudinalForce` | `driveForce, brakeForce, fz, muPeak, vx` | `float` | Продольная сила с явным muPeak (thread-safe) |
| `applyFrictionCircle` | `fx, fy, fz, muPeak, result` | `FrictionCircleResult` | Ограничение сил окружностью трения (result object передаётся извне) |
| `computeEffectiveMu` | `fz, fzNominal, surface` | `float` | Эффективный μ с учётом чувствительности к нагрузке |
| `applyRelaxation` | `currentForce, targetForce, speed, dt, relaxLength` | `float` | Релаксация силы (сглаживание) |

### Внутренний класс: FrictionCircleResult
| Поле | Тип | Описание |
|------|-----|----------|
| `fx` | `float` | Продольная сила после ограничения |
| `fy` | `float` | Боковая сила после ограничения |

---

## SurfaceProperties.java

**Назначение:** Свойства поверхностей с пресетами и маппингом блоков Minecraft.

### Поля экземпляра
| Имя | Тип | Описание |
|-----|-----|----------|
| `muPeak` | `float` | Пиковый коэффициент трения |
| `muSlide` | `float` | Коэффициент трения скольжения |
| `corneringStiffness` | `float` | Угловая жёсткость шин (Н/рад) |
| `relaxationLength` | `float` | Длина релаксации (м) |
| `rollingResistance` | `float` | Коэффициент сопротивления качению |
| `peakSlipAngleDeg` | `float` | Пиковый угол проскальзывания (°) |
| `slipAngleFalloff` | `float` | Скорость падения μ за пиком |
| `loadSensitivity` | `float` | Чувствительность μ к нагрузке |

### Пресеты (static)
| Имя | μ_peak | μ_slide | Cornering | Relax | Rolling | Peak° | Falloff | LoadSens |
|-----|--------|---------|-----------|-------|---------|-------|---------|----------|
| `ASPHALT_DRY` | 0.85 | 0.70 | 65000 | 0.06 | 0.012 | 8.0 | 0.7 | 0.10 |
| `ASPHALT_WET` | 0.55 | 0.40 | 50000 | 0.08 | 0.015 | 10.0 | 0.6 | 0.12 |
| `GRAVEL` | 0.55 | 0.50 | 30000 | 0.15 | 0.030 | 14.0 | 0.3 | 0.15 |
| `DIRT` | 0.45 | 0.40 | 25000 | 0.18 | 0.035 | 16.0 | 0.25 | 0.18 |
| `MUD` | 0.30 | 0.25 | 16000 | 0.25 | 0.050 | 18.0 | 0.2 | 0.20 |
| `SNOW` | 0.30 | 0.22 | 22000 | 0.12 | 0.025 | 12.0 | 0.35 | 0.16 |
| `ICE` | 0.10 | 0.07 | 10000 | 0.10 | 0.008 | 6.0 | 0.5 | 0.08 |
| `SAND` | 0.40 | 0.35 | 18000 | 0.20 | 0.060 | 15.0 | 0.2 | 0.22 |

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `copy()` | `SurfaceProperties` | Копия экземпляра |
| `setDefaultSurface(SurfaceProperties)` | `void` | (static) Установка дефолтной поверхности |
| `getDefaultSurface()` | `SurfaceProperties` | (static) Получение дефолтной поверхности |
| `setDefaultSurfaceByName(String)` | `void` | (static) Установка дефолтной по имени |
| `getSurfaceByName(String)` | `SurfaceProperties` | (static) Получение пресета по имени |
| `getBlockSurfaceMap()` | `HashMap<String, SurfaceProperties>` | (static) Маппинг блоков → поверхностей |
| `getSurfaceForBlock(String)` | `SurfaceProperties` | (static) Поверхность для блока |
| `resetBlockSurfaceMap()` | `void` | (static) Сброс маппинга |
| `setBlockSurface(String, SurfaceProperties)` | `void` | (static) Установка поверхности для блока |
| `interpolate(SurfaceProperties, SurfaceProperties, float)` | `SurfaceProperties` | (static) Линейная интерполяция |

### Маппинг блоков по умолчанию
- **ASPHALT_DRY:** stone, deepslate, blackstone, polished variants, obsidian, quartz
- **ASPHALT_WET:** cobblestone, mossy variants, andesite, diorite, granite, tuff, prismarine
- **GRAVEL:** gravel
- **DIRT:** dirt variants, grass_block, podzol, mycelium, farmland, dirt_path
- **MUD:** mud, soul_sand, soul_soil, muddy_mangrove_roots
- **SNOW:** snow_block, powder_snow, snow
- **ICE:** ice, packed_ice, frosted_ice + blue_ice (ещё меньше сцепления)
- **SAND:** sand, red_sand, suspicious_sand
