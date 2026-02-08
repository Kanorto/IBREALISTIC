# Изменения: Исправление поведения в воздухе, ручника и перекрута руля

## Дата
2026-02-08

## Краткое описание
Исправлено некорректное поведение лодки в воздухе (потеря скорости), неработающий ручник на пробел при реалистичной физике, и чрезмерный перекрут руля на высокой скорости.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/physics/RealisticPhysicsEngine.java` — добавлена airborne физика, self-aligning torque, уменьшена сила ручника
- `src/main/java/dev/o7moon/openboatutils/physics/VehicleConfig.java` — обновлены значения по умолчанию (speedSteeringFactor, maxSteeringAngle, steeringSpeed)
- `src/main/java/dev/o7moon/openboatutils/physics/VehicleType.java` — обновлены параметры всех типов машин
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java` — исправлен ручник при реалистичной физике, передача состояния airborne

## Детальное описание изменений

### 1. Исправление поведения лодки в воздухе
**Файл:** `RealisticPhysicsEngine.java`
**Что сделано:**
- Добавлено поле `airborne` для отслеживания состояния полёта
- Добавлен блок airborne-физики: когда лодка в воздухе, движок пропускает расчёт шинных сил, сопротивления качению и торможения
- В воздухе применяется только аэродинамический drag (формула: `F = -0.5 * Cd * A * ρ * v²`)
- Yaw rate медленно затухает (коэффициент 0.998 вместо 0.995 на земле)
- Скорость сохраняется практически полностью

**Причина:**
Ранее при полёте лодки в воздухе движок применял полную физику шин, что приводило к резкой потере скорости.

### 2. Исправление ручника на пробел
**Файл:** `BoatMixin.java`
**Что сделано:**
- При активной реалистичной физике, пробел используется ТОЛЬКО как ручник (прыжок отключается)
- Ручник работает только на земле (не в воздухе)
- Добавлена проверка `!realisticActive` перед обработкой прыжка

**Файл:** `RealisticPhysicsEngine.java`
**Что сделано:**
- Уменьшена сила ручника с 80% до 50% от brakingForce для более контролируемого дрифта

**Причина:**
Ручник конфликтовал с системой прыжков, а его сила была чрезмерной.

### 3. Исправление перекрута руля
**Файл:** `VehicleConfig.java`
**Что сделано:**
- `speedSteeringFactor`: 0.0001 → 0.004 (увеличение в 40 раз)
- `maxSteeringAngle`: 0.60 → 0.50 рад
- `steeringSpeed`: 2.5 → 5.0 рад/с (значение по умолчанию соответствует WRC_CAR)

**Файл:** `VehicleType.java`
**Что сделано:**
- WRC_CAR: maxSteeringAngle 0.60→0.50, steeringSpeed 10.0→5.0
- GROUP_B: maxSteeringAngle 0.55→0.48, steeringSpeed 9.0→4.5
- CLASSIC_RALLY: maxSteeringAngle 0.50→0.45, steeringSpeed 7.0→4.0
- LIGHTWEIGHT: maxSteeringAngle 0.65→0.55, steeringSpeed 12.0→6.0
- TRUCK: maxSteeringAngle 0.40→0.35, steeringSpeed 5.0→3.0

**Файл:** `RealisticPhysicsEngine.java`
**Что сделано:**
- Добавлен self-aligning torque: при отпускании руля, колёса автоматически возвращаются в центр
- Скорость возврата зависит от скорости движения (формула: `alignRate = 3.0 * min(1.0, speed/5.0)`)

**Причина:**
speedSteeringFactor=0.0001 давал лишь 4% снижение руля на 20 м/с. С новым значением 0.004:
- 10 м/с: 29% снижение
- 20 м/с: 62% снижение
- 30 м/с: 78% снижение

## Константы воздушной физики
- `AIR_DRAG_COEFFICIENT = 0.35` — коэффициент аэродинамического сопротивления
- `AIR_DENSITY = 1.225` — плотность воздуха (кг/м³)
- `FRONTAL_AREA = 2.0` — лобовая площадь (м²)
- `AIR_YAW_RATE_DAMPING = 0.998` — затухание вращения в воздухе

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4 (Gradle)
- [x] Мод собирается успешно на MC 1.21 (Gradle)
- [x] Мод собирается успешно на MC 1.21.3 (Gradle)
- [x] Плагин собирается успешно (Maven)
- [ ] Ручное тестирование в игре

## Полный список настраиваемых параметров
Все параметры можно настроить через:
- Команды в одиночной игре (SingleplayerCommands)
- Пакеты от сервера (ClientboundPackets)
- Серверный плагин TimingSystem (CustomBoatUtilsMode + CommandBoatUtilsModeEdit)

| Параметр | Команда | Диапазон | По умолчанию |
|---|---|---|---|
| speedSteeringFactor | `/vehiclespeedsteeringfactor` | 0—0.1 | 0.004 |
| engineBraking | `/vehicleenginebraking` | 0—5000 | 800 |
| rollStiffnessRatio | `/vehiclerollstiffness` | 0—1 | 0.55 |
| trackWidth | `/vehicletrackwidth` | 1—3 | 1.55 |
| drag | `/vehicledrag` | 0—2 | 0.35 |

## Примечание
- CODEBASE_INDEX.md нужно будет обновить после его создания
- VERSION протокола не изменялся (новые пакеты используют ID 50-52, но протокол остаётся v19)
