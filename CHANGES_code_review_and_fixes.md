# Изменения: Полная проверка кода, 4-колёсная система, погода, аэродинамика

## Дата
2026-02-09

## Краткое описание
Проведён полный аудит кодовой базы. Исправлены 3 критические проблемы. Реализована 4-колёсная физическая модель (FourWheelPhysicsEngine) с per-wheel forces, дифференциалами (Open/Locked/LSD), погодной зависимостью (WeatherCondition), аэродинамическим прижимом (downforce). VERSION обновлён с 19 до 20.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic) — Исправления
- `physics/RealisticPhysicsEngine.java` — исправлена мутация shared SurfaceProperties, добавлены instance-level FrictionCircleResult, исправлена формула момента инерции
- `physics/TireModel.java` — добавлены overloaded методы computeLateralForce и computeLongitudinalForce с параметрами mu вместо SurfaceProperties, applyFrictionCircle теперь принимает result-объект

### Мод (OpenBoatUtilsRealistic) — Новый функционал
- `physics/FourWheelPhysicsEngine.java` — **НОВЫЙ** 4-колёсный движок (FL/FR/RL/RR)
- `physics/WheelPosition.java` — **НОВЫЙ** enum позиций колёс
- `physics/DifferentialType.java` — **НОВЫЙ** enum дифференциалов (Open/Locked/LSD)
- `physics/WeatherCondition.java` — **НОВЫЙ** enum погодных условий (CLEAR/RAIN/HEAVY_RAIN/SNOW/FOG)
- `physics/VehicleConfig.java` — добавлены awdFrontSplit, frontDifferential, rearDifferential, lsdLockingCoeff, downforceCoefficient, downforceFrontBias
- `OpenBoatUtils.java` — fourWheelPhysics, новые setter-методы, VERSION 19→20
- `mixin/BoatMixin.java` — использует fourWheelPhysics вместо realisticPhysics
- `ClientboundPackets.java` — 7 новых пакетов (ID 53-59)

### Плагин (TimingSystem)
- `boatutils/CustomBoatUtilsMode.java` — новые @Expose поля, packet sending, resetToVanilla, applySettingsFrom для 4WD, погоды, downforce

### Документация
- `DOCS_REALISTIC_PHYSICS.md` — обновлена таблица maxSteeringAngle для всех типов машин (приведена в соответствие с кодом)
- `codebase/02_MOD_PHYSICS.md` — исправлены неверные default-значения

## Детальное описание изменений

### 1. Thread-safety: Мутация SurfaceProperties (КРИТИЧЕСКАЯ)
**Файл:** `physics/RealisticPhysicsEngine.java`
**Строки:** 352-370 (до исправления)
**Что сделано:**
- Убрана прямая мутация `currentSurface.muPeak` и `currentSurface.muSlide` в цикле субшагов
- Вместо этого передаются per-axle mu значения (`muFront`, `muRear`) напрямую в TireModel через новые overloaded методы
- Для computeLongitudinalForce добавлен аналогичный overload с параметром `float muPeak`

**Причина:**
SurfaceProperties.ASPHALT_DRY и другие пресеты — это `public static` объекты, общие для всех экземпляров. При параллельной обработке нескольких лодок, мутация их полей muPeak/muSlide вызывает race condition, приводящий к некорректным расчётам физики.

### 2. Thread-safety: Static FrictionCircleResult (КРИТИЧЕСКАЯ)
**Файл:** `physics/TireModel.java`
**Строки:** 70-96 (до исправления)
**Что сделано:**
- Убран `private static final FrictionCircleResult frictionResult` (один объект на все вызовы)
- `applyFrictionCircle()` теперь принимает `FrictionCircleResult result` как параметр
- В `RealisticPhysicsEngine` добавлены два instance-level объекта: `frictionResultFront` и `frictionResultRear`

**Причина:**
Статический мутабельный объект разделялся между всеми вызовами из всех лодок, что при параллельном доступе приводило к перезаписи результатов.

### 3. Формула момента инерции (ФИЗИЧЕСКАЯ ОШИБКА)
**Файл:** `physics/RealisticPhysicsEngine.java`
**Строка:** 437
**Было:** `I = m·L²/12` (формула тонкого стержня)
**Стало:** `I = m·(L² + W²)/12` (формула прямоугольного тела)

**Причина:**
Автомобиль — не стержень, а двумерное тело. Для WRC_CAR разница: 635 vs 873 кг·м². Старая формула недооценивала инерцию на ~40%, делая машину чрезмерно "дёрганой" в поворотах.

### 4. Несоответствие документации
**Файл:** `DOCS_REALISTIC_PHYSICS.md`
**Что исправлено:** Таблица maxSteeringAngle теперь соответствует VehicleType.java:
| Тип | Документация (было) | Код (правильно) |
|-----|---------------------|-----------------|
| WRC_CAR | 0.60 | 0.50 |
| GROUP_B | 0.55 | 0.48 |
| CLASSIC_RALLY | 0.50 | 0.45 |
| LIGHTWEIGHT | 0.65 | 0.55 |
| TRUCK | 0.40 | 0.35 |

---

## Реализованная 4-колёсная система (Four-Wheel Model)

### Что реализовано:
**FourWheelPhysicsEngine.java** — полный 4-колёсный движок, заменяющий Bicycle Model:

1. **4 независимых колеса (FL, FR, RL, RR):**
   - Отдельные вертикальные нагрузки (fzWheel[4])
   - Отдельные slip angles с учётом yawRate × halfTrack
   - Отдельные lateral/longitudinal forces
   - Отдельные friction circle constraints

2. **Дифференциалы (DifferentialType.java):**
   - **Open** — тяга пропорционально нагрузке колеса
   - **Locked** — 50/50 на оба колеса оси
   - **LSD** — blend между Open и Locked по lsdLockingCoeff

3. **Настраиваемый AWD split:**
   - `awdFrontSplit` от 0.0 (full rear) до 1.0 (full front)
   - По умолчанию 0.5 (50/50)

4. **Погодная зависимость (WeatherCondition.java):**
   - CLEAR: gripMultiplier=1.0, relaxationMultiplier=1.0
   - RAIN: gripMultiplier=0.70, relaxationMultiplier=1.3
   - HEAVY_RAIN: gripMultiplier=0.50, relaxationMultiplier=1.6
   - SNOW: gripMultiplier=0.40, relaxationMultiplier=1.5
   - FOG: gripMultiplier=0.95, relaxationMultiplier=1.1

5. **Аэродинамический прижим (downforce):**
   - `Fz_aero = 0.5 × downforceCoefficient × ρ × v²`
   - Распределение: `downforceFrontBias` (default 40% перед, 60% зад)
   - Добавляется к vertical load каждого колеса

6. **Совместимость:**
   - Все существующие настройки (VehicleConfig, пакеты) продолжают работать
   - Старый RealisticPhysicsEngine сохранён для обратной совместимости
   - Все setter-методы обновляют оба движка

### Новые пакеты (VERSION 20):
| ID | Имя | Тип данных |
|----|-----|-----------|
| 53 | SET_AWD_FRONT_SPLIT | float |
| 54 | SET_FRONT_DIFFERENTIAL | short (0=Open, 1=Locked, 2=LSD) |
| 55 | SET_REAR_DIFFERENTIAL | short |
| 56 | SET_LSD_LOCKING_COEFF | float |
| 57 | SET_DOWNFORCE_COEFFICIENT | float |
| 58 | SET_DOWNFORCE_FRONT_BIAS | float |
| 59 | SET_WEATHER_CONDITION | short (0=Clear, 1=Rain, 2=Heavy, 3=Snow, 4=Fog) |

---

## Новый функционал, который можно реализовать сейчас

### Приоритет 1 (Высокий — улучшает геймплей):

1. **Спидометр / HUD** — отображение скорости (км/ч), оборотов руля, текущей поверхности через ActionBar или overlay
2. **Система повреждений** — износ шин при дрифте (увеличение muSlide со временем), снижение engineForce при столкновениях
3. **Настраиваемый AWD split** — не только 50/50, а 30/70, 40/60 и т.д. (расширить DrivetrainType)
4. **Система тяги (Traction Control)** — ограничение wheelSpin при разгоне на скользких поверхностях
5. **Турбо/бустер** — временное увеличение engineForce с cooldown (через пакеты)

### Приоритет 2 (Средний — расширяет возможности):

6. **Погодный модификатор поверхностей** — серверная команда для глобального снижения μ (дождь = ×0.7)
7. **Система аэродинамики** — downforce (Fz += Cd_down × v²), увеличение сцепления на высокой скорости
8. **Настройка подвески** (в Bicycle Model) — жёсткость отката при посадке, скорость массообмена
9. **Pace Notes / Дорожная книга** — автоматическая генерация подсказок на основе геометрии трассы
10. **Телеметрия** — запись данных физики (vx, vy, slip angles, forces) для анализа заезда

### Приоритет 3 (Низкий — косметика и QoL):

11. **Звуковой движок** — визг шин при дрифте, звук двигателя по скорости
12. **Частицы** — дым от шин при скольжении, пыль на гравии
13. **Настройка передаточных чисел** — gear ratio для разных скоростей
14. **Реплей система** — запись и воспроизведение заезда (ghost car)
15. **Настройка чувствительности клавиш** — плавность нажатия W/S/A/D

---

## Тестирование
- [x] Мод собирается на MC 1.20.4 ✅
- [x] Мод собирается на MC 1.21 ✅
- [x] Мод собирается на MC 1.21.3 ✅
- [x] Плагин собирается (Maven) ✅

## Заметка
- CODEBASE_INDEX.md существует — нужно будет обновить при следующем изменении кода
