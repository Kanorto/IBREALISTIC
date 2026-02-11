# Изменения: Настройка системы сцепления, реалистичности физики и исправления PR review

## Дата
2026-02-11

## Краткое описание
Исправлена система сцепления для реалистичных режимов: машина теперь плавно тормозит при повороте после полёта и сохраняет траекторию (инерцию) вместо резкой остановки. Усилен ручник, добавлены реалистичные настройки дифференциалов и торможения двигателем для каждого типа машины. Исправлены проблемы из PR review.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/physics/FourWheelPhysicsEngine.java` — настройка параметров сцепления, воздушной физики и ручника
- `src/main/java/dev/o7moon/openboatutils/physics/RealisticPhysicsEngine.java` — аналогичные параметры для bicycle model
- `src/main/java/dev/o7moon/openboatutils/physics/VehicleType.java` — добавлены настройки дифференциалов, торможения двигателем и аэродинамики для каждого типа машины

## Детальное описание изменений

### 1. Убрано агрессивное затухание боковой скорости в воздухе
**Файл:** `FourWheelPhysicsEngine.java`
**Что сделано:**
- Убрана строка `vy *= LATERAL_VELOCITY_DAMPING` в секции airborne-физики
- Теперь при полёте боковая скорость затухает только от аэродинамического сопротивления (реалистично)
- Ранее боковая скорость теряла 5% каждый тик — за 1 сек полёта оставалось только 36% бокового импульса

### 2. Замедлено восстановление сцепления после посадки
**Файлы:** `FourWheelPhysicsEngine.java`, `RealisticPhysicsEngine.java`
**Что сделано:**
- `LANDING_GRIP_RECOVERY_RATE`: `0.08f` → `0.05f`
- Восстановление за ~12 тиков (0.6 сек) вместо ~8 тиков (0.4 сек)

### 3. Уменьшено затухание боковой скорости на земле
**Файлы:** `FourWheelPhysicsEngine.java`, `RealisticPhysicsEngine.java`
**Что сделано:**
- `LATERAL_VELOCITY_DAMPING`: `0.95f` → `0.97f` (без руления)
- `LATERAL_VELOCITY_DAMPING_ACTIVE`: `0.98f` → `0.99f` (при рулении, только FourWheel)

### 4. Увеличен лимит боковой скорости
**Файл:** `FourWheelPhysicsEngine.java`
**Что сделано:**
- `MAX_LATERAL_SPEED_RATIO`: `0.8f` → `1.2f`

### 5. Усилен ручник
**Файлы:** `FourWheelPhysicsEngine.java`, `RealisticPhysicsEngine.java`
**Что сделано:**
- `HANDBRAKE_FORCE_MULTIPLIER`: `0.5f` → `0.8f`
- Ручник теперь сильнее блокирует задние колёса, что вызывает более выраженный занос
- При 0.8: сила ручника = 80% от полной тормозной силы на задние колёса (было 50%)

### 6. Добавлен аэродинамический drag на vy в RealisticPhysicsEngine
**Файл:** `RealisticPhysicsEngine.java`
**Что сделано:**
- Добавлен расчёт аэродинамического сопротивления для боковой скорости в режиме полёта

### 7. Реалистичные настройки дифференциалов и двигателя для каждого типа машины
**Файл:** `VehicleType.java`
**Что сделано:**
- **WRC_CAR**: LSD front/rear (коэффициент 0.4), AWD split 45/55 (чуть заднеприводнее), engine braking 900N, downforce 0.6
- **GROUP_B**: Open front + LSD rear (коэффициент 0.6), высокое engine braking 1200N, downforce 0.4
- **CLASSIC_RALLY**: Open front/rear (старые автомобили), низкое engine braking 600N, минимальный downforce 0.2
- **LIGHTWEIGHT**: LSD front (FWD нужен LSD для тяги), open rear, engine braking 500N, downforce 0.3
- **TRUCK**: Locked front/rear (максимальная тяга на бездорожье), высокое engine braking 1500N

## Тестирование
- [x] Мод собирается успешно (Gradle) для всех версий MC (1.20.4, 1.21, 1.21.3)

## Примечание
- Файл `CODEBASE_INDEX.md` нужно обновить при его создании — изменения затрагивают FourWheelPhysicsEngine, RealisticPhysicsEngine и VehicleType
