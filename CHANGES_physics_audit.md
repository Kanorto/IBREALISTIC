# Изменения: Полный аудит системы физики

## Дата
2026-02-09

## Краткое описание
Полный аудит и исправление всех найденных недоработок в системе реалистичной физики: оптимизация GC, потокобезопасность, удаление мёртвого кода, иммутабельность, исправление знаков в физических формулах.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `physics/SurfaceProperties.java` — сделаны поля final/immutable, добавлен SurfaceAccumulator, потокобезопасная инициализация, пресет BLUE_ICE
- `physics/FourWheelPhysicsEngine.java` — использует SurfaceAccumulator, исправлен yaw moment sign
- `physics/RealisticPhysicsEngine.java` — использует SurfaceAccumulator, убран неиспользуемый import
- `OpenBoatUtils.java` — удалён мёртвый код RealisticPhysicsEngine из всех setter'ов

## Детальное описание изменений

### 1. Иммутабельные поля SurfaceProperties (FIX 5)
**Файл:** `physics/SurfaceProperties.java`
**Что сделано:**
- Все 8 полей `muPeak`, `muSlide`, `corneringStiffness`, `relaxationLength`, `rollingResistance`, `peakSlipAngleDeg`, `slipAngleFalloff`, `loadSensitivity` стали `final`
- Все 17 статических пресетов (ASPHALT_DRY, ICE и т.д.) стали `public static final`
- Блок blue_ice, который раньше мутировал copy() ICE, заменён на отдельный пресет `BLUE_ICE`
- Добавлен `BLUE_ICE` в `getSurfaceByName()`

**Причина:**
Ранее любой код мог изменить поля пресетов, что влияло бы на всех игроков на сервере. Final поля предотвращают случайные мутации.

### 2. Потокобезопасная инициализация blockSurfaceMap (FIX 4)
**Файл:** `physics/SurfaceProperties.java`
**Что сделано:**
- `blockSurfaceMap` стал `volatile`
- `getBlockSurfaceMap()` использует double-checked locking с `synchronized(SurfaceProperties.class)`
- `resetBlockSurfaceMap()` стал `synchronized`
- Тип поля изменён с `HashMap` на `Map` (интерфейс)

**Причина:**
В мультиплеере несколько потоков могли одновременно вызвать lazy init, что приводило к race condition и двойной инициализации HashMap.

### 3. SurfaceAccumulator — оптимизация GC (FIX 1 & 3)
**Файл:** `physics/SurfaceProperties.java` (новый вложенный класс)
**Что сделано:**
- Добавлен `SurfaceAccumulator` — переиспользуемый объект для усреднения поверхностей
- Методы: `reset()`, `accumulate(SurfaceProperties)`, `getResult()`
- Создаётся один раз на движок, переиспользуется каждый тик
- Оба движка (FourWheelPhysicsEngine и RealisticPhysicsEngine) обновлены для использования

**Причина:**
Ранее `detectSurface()` создавал `new SurfaceProperties(...)` каждый тик (20 раз в секунду на каждого игрока). При нескольких игроках это значительная нагрузка на GC.

### 4. Исправление знака yaw moment от продольных сил (FIX 6)
**Файл:** `physics/FourWheelPhysicsEngine.java`
**Строки:** 477-480
**Что сделано:**
- Исправлен знак: `(-fxWheel[0] + fxWheel[1])` → `(fxWheel[0] - fxWheel[1])`
- То же для задней оси: `(-fxWheel[2] + fxWheel[3])` → `(fxWheel[2] - fxWheel[3])`

**Причина:**
Левое колесо с положительной drive force создаёт положительный yaw (поворот налево), правое — отрицательный. Ранее знаки были инвертированы.

### 5. Удаление мёртвого кода RealisticPhysicsEngine (FIX 9)
**Файл:** `OpenBoatUtils.java`
**Что сделано:**
- Удалено поле `public static RealisticPhysicsEngine realisticPhysics`
- Удалены все дублирующие вызовы `realisticPhysics.getConfig().*` и `realisticPhysics.setConfig()`/`setEnabled()` из ~20 setter-методов
- Удалён import `RealisticPhysicsEngine` из OpenBoatUtils.java

**Причина:**
В BoatMixin используется ТОЛЬКО `fourWheelPhysics` для расчёта физики. `realisticPhysics` (старый bicycle model) создавался и обновлялся параллельно, но никогда не использовался. Это удваивало количество работы при каждой настройке параметров.

## CODEBASE_INDEX.md
Необходимо обновить:
- Удалить упоминание RealisticPhysicsEngine из активного использования в OpenBoatUtils
- Добавить SurfaceAccumulator в индекс SurfaceProperties
- Обновить описание FourWheelPhysicsEngine (surfaceAccumulator)

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4 (Gradle)
- [x] Мод собирается успешно на MC 1.21 (Gradle)
- [x] Мод собирается успешно на MC 1.21.3 (Gradle)
- [ ] Плагин не изменялся — сборка не требуется

## Проблемы и решения
1. **Blue ice mutation** — пресет ICE мутировался через `copy()` для blue_ice. Решение: создан отдельный пресет `BLUE_ICE` с нужными параметрами.
2. **Double-checked locking** — первая попытка содержала баг (map creation вне synchronized). Исправлено на корректный паттерн.
