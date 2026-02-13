# Изменения: Исправление реалистичной физики

## Дата
2026-02-08

## Краткое описание
Исправлена остановка лодки в воздухе при переходе между блоками в реалистичных режимах. Добавлены обязательные настройки (airControl, slipperiness) для всех REALISTIC пресетов. Значительно улучшен отклик рулевого управления и визуальная обратная связь. Обновлена документация SERVER_GUIDE.md.

## Изменённые файлы

### Мод (OBURealistic)
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java` — реалистичная физика работает в IN_AIR; улучшена визуальная обратная связь (pitch/roll)
- `src/main/java/dev/o7moon/openboatutils/Modes.java` — все REALISTIC режимы устанавливают `setAllBlocksSlipperiness(0.98f)`
- `src/main/java/dev/o7moon/openboatutils/physics/VehicleType.java` — увеличена скорость руления для всех типов машин (steeringSpeed: 2.5→10.0 для WRC и пропорционально для остальных)
- `src/main/java/dev/o7moon/openboatutils/physics/VehicleConfig.java` — уменьшен speedSteeringFactor (0.0003→0.0001) для менее агрессивного ослабления руля на скорости
- `src/main/java/dev/o7moon/openboatutils/physics/RealisticPhysicsEngine.java` — увеличен YAW_RATE_DAMPING (0.98→0.995); увеличены масштабы pitch/roll для визуала
- `src/main/java/dev/o7moon/openboatutils/physics/SurfaceProperties.java` — увеличена corneringStiffness и уменьшена relaxationLength для всех поверхностей

### Плагин (TimingSystem)
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — обновлён DEFAULT_VEHICLE_STEERING_SPEED (2.5→10.0)

### Документация
- `codebase/SERVER_GUIDE.md` — добавлено предупреждение об обязательных параметрах для кастомных реалистичных режимов

## Детальное описание изменений

### 1. Исправление остановки в воздухе (BoatMixin.java)
**Что сделано:**
- Реалистичная физика работает не только при `loc == ON_LAND`, но и при `loc == IN_AIR` (если `airControl == true`)
- Предотвращает потерю скорости при переходе лодки с блока на нижний

### 2. Моментальный отклик руления (VehicleType.java, VehicleConfig.java, RealisticPhysicsEngine.java)
**Что сделано:**
- `steeringSpeed`: WRC 2.5→10.0, GROUP_B 2.2→9.0, CLASSIC 1.8→7.0, LIGHTWEIGHT 3.0→12.0, TRUCK 1.2→5.0
- `speedSteeringFactor`: 0.0003→0.0001 (руль меньше ослабляется на скорости)
- `YAW_RATE_DAMPING`: 0.98→0.995 (меньше подавление поворота за тик)

**Причина:**
При steeringSpeed=2.5 полный поворот руля занимал ~240мс. При 10.0 — ~60мс (почти мгновенно). YAW_RATE_DAMPING=0.98 терял 8% скорости вращения каждый тик (при 4 подшагах), при 0.995 теряется только 2%.

### 3. Улучшенный отклик шин (SurfaceProperties.java)
**Что сделано:**
- Увеличена corneringStiffness для всех поверхностей (ASPHALT_DRY: 45000→65000, и т.д.)
- Уменьшена relaxationLength для всех поверхностей (ASPHALT_DRY: 0.15→0.06, и т.д.)

**Причина:**
Более высокая corneringStiffness = более резкий отклик шин на угол поворота. Меньшая relaxationLength = более быстрое нарастание боковых сил (без задержки).

### 4. Улучшенная визуальная обратная связь (BoatMixin.java, RealisticPhysicsEngine.java)
**Что сделано:**
- pitchAngle масштаб: 0.15→0.25 (более заметный наклон при торможении/разгоне)
- rollAngle масштаб: 0.12→0.20 (более заметный крен в поворотах)
- Визуальный множитель pitch: 15→25, лимит: 25°→30°

## Тестирование
- [x] Мод собирается успешно (Gradle) для всех версий MC (1.20.4, 1.21, 1.21.3)

## Примечание
Файл CODEBASE_INDEX.md необходимо обновить, чтобы отразить перечисленные выше изменения.
