# План реалистичного ралли — статус реализации

## ФАЗА 1: ФИЗИКА ДВИЖЕНИЯ (Мод — OpenBoatUtilsRealistic) ✅

### 1.1 Bicycle Model (Велосипедная модель) ✅
- [x] Состояния: vx, vy, yawAngle, yawRate, steeringAngle
- [x] Ускорения предыдущего тика для массообмена: axPrev, ayPrev
- [x] Нагрузка на оси: fzFront, fzRear
- [x] Релаксация боковых сил: fyFrontActual, fyRearActual
- **Файл:** `physics/RealisticPhysicsEngine.java`

### 1.2 Типы машин (VehicleType) ✅
- [x] WRC_CAR: 1190кг, база 2.53м, ЦТ 0.45м, развесовка 55/45, **AWD**
- [x] GROUP_B: 1100кг, база 2.40м, ЦТ 0.50м, развесовка 45/55, **RWD**
- [x] CLASSIC_RALLY: 1000кг, база 2.45м, ЦТ 0.55м, развесовка 50/50, **RWD**
- [x] LIGHTWEIGHT: 800кг, база 2.30м, ЦТ 0.42м, развесовка 60/40, **FWD**
- [x] TRUCK: 2000кг, база 3.20м, ЦТ 0.90м, развесовка 50/50, **AWD**
- **Файл:** `physics/VehicleType.java`

### 1.3 Конфигурация машины (VehicleConfig) ✅
- [x] mass, wheelbase, cgHeight, trackWidth
- [x] frontWeightBias, maxSteeringAngle, steeringSpeed
- [x] brakingForce, engineForce, dragCoefficient
- [x] rollingResistance, brakeBias, engineBraking
- [x] substeps, speedSteeringFactor, rollStiffnessRatioFront
- [x] **drivetrain** (FWD / RWD / AWD)
- [x] Расчёт статических нагрузок осей
- **Файл:** `physics/VehicleConfig.java`

### 1.3a Тип привода (DrivetrainType) ✅
- [x] RWD: задний привод — вся тяга на задние колёса (0% перед, 100% зад)
- [x] FWD: передний привод — вся тяга на передние колёса (100% перед, 0% зад)
- [x] AWD: полный привод — равное распределение (50/50)
- [x] getFrontDriveRatio() определяет распределение тяги
- **Файл:** `physics/DrivetrainType.java`

### 1.4 Массообмен (Weight Transfer) ✅
- [x] Продольный: ΔFz = (m × ax × h) / L
- [x] При торможении: перед догружается, зад разгружается
- [x] При разгоне: зад догружается, перед разгружается
- [x] Поперечный: ΔFz = (m × ay × h) / T
- [x] Распределение по осям через rollStiffnessRatio
- [x] Защита от отрицательных нагрузок (clamp >= 0)
- **Эффекты:** trail braking, lift-off oversteer, Scandinavian flick

### 1.5 Tire Model (Модель шин — Fiala/Brush) ✅
- [x] Slip angle: α = atan2(vy + r×L, |vx|) - δ
- [x] Нелинейная боковая сила (кубический полином)
- [x] Зона насыщения с прогрессивным спадом
- [x] Friction Circle: sqrt(Fx² + Fy²) ≤ μ × Fz
- [x] Load Sensitivity: μ_eff = μ × (1 - sens × (Fz/Fz_nom - 1))
- [x] Защита от деления на ноль (cAlpha, mu)
- **Файл:** `physics/TireModel.java`

### 1.6 Relaxation Length ✅
- [x] Фильтр первого порядка: Fy += (Fy_target - Fy) × (|v| × dt / σ)
- [x] Разные значения σ для разных поверхностей
- [x] На гравии σ=0.40 (медленная реакция), на асфальте σ=0.15 (резкая)

### 1.7 Руль с перекрутом (Steering) ✅
- [x] Ограничение скорости поворота: steeringSpeed × dt
- [x] Speed-dependent ratio: δ_eff = δ / (1 + factor × v²)
- [x] Задержка реакции руля (не мгновенно!)

### 1.8 Поверхности (Surface System) ✅
- [x] 17 пресетов: ASPHALT_DRY, ASPHALT_WET, CONCRETE, BRICK, METAL, TERRACOTTA, GRAVEL, WOOD, NETHER, DIRT, SAND, VEGETATION, GLASS, SNOW, MUD, WOOL, ICE
- [x] Параметры: μ_peak, μ_slide, corneringStiffness, relaxationLength, rollingResistance, peakSlipAngle, slipAngleFalloff, loadSensitivity
- [x] Привязка 300+ блоков Minecraft к соответствующим поверхностям
- [x] Blue ice с ещё меньшим μ
- [x] Интерполяция между поверхностями при переходе

### 1.9 Вертикальная физика (Vertical Physics) ✅
- [x] Отслеживание вертикальной скорости для определения взлёта/падения
- [x] Детекция приземления и расчёт силы удара
- [x] Потеря сцепления при жёсткой посадке (до 60%)
- [x] Плавное восстановление сцепления (~0.75 сек)
- [x] Фильтрация мелких кочек (< 3 тиков в воздухе)
- [x] Визуальный pitch в полёте (нос вверх/вниз)
- [x] Использование предыдущей вертикальной скорости для корректного расчёта удара
- [x] Обнаружение поверхности под лодкой (detectSurface)
- [x] **Настраиваемая поверхность по умолчанию** (defaultSurface) — для ледовых треков ставится ICE, тогда все немаппированные блоки считаются льдом
- [x] getSurfaceByName() — получение пресета по имени (для пакетов и команд)
- **Файл:** `physics/SurfaceProperties.java`

### 1.9 Sub-stepping ✅
- [x] 4-10 подшагов на тик (настраиваемо)
- [x] Эффективная частота 80-200Hz
- [x] Численная стабильность при высокой скорости

### 1.10 Торможение ✅
- [x] Распределение перед/зад через brakeBias
- [x] Ручной тормоз (задняя ось, 80% силы)
- [x] Блокировка колёс через friction circle

### 1.11 Аэродинамика ✅
- [x] Drag: F = 0.5 × Cd × A × ρ × v²
- [x] Rolling resistance: F = μ_roll × m × g
- [x] Engine braking при отпускании газа

### 1.12 Интеграция в мод ✅
- [x] BoatMixin: подключение физики при ON_LAND
- [x] Определение ввода (руль, газ, тормоз, ручник)
- [x] Применение результата (velocity + yaw)
- [x] 5 новых режимов в Modes.java
- [x] 16 новых типов пакетов в ClientboundPackets.java
- [x] 14 новых команд в SingleplayerCommands.java
- [x] VERSION обновлён: 18 → 19

## ФАЗА 2: СЕРВЕРНАЯ СТОРОНА (Плагин — TimingSystem) ✅

### 2.1 BoatUtilsMode ✅
- [x] REALISTIC (id=25, version=19)
- [x] REALISTIC_WRC (id=26)
- [x] REALISTIC_GROUP_B (id=27)
- [x] REALISTIC_CLASSIC (id=28)
- [x] REALISTIC_LIGHTWEIGHT (id=29)
- [x] REALISTIC_TRUCK (id=30)
- [x] REALISTIC_ALLTERRAIN (id=31) — вездеход для езды по всем блокам

### 2.2 CustomBoatUtilsMode ✅
- [x] 15 новых полей с @Expose для JSON
- [x] Пакеты отправки: 15 новых packet ID (33-47)
- [x] resetToVanilla: сброс всех новых полей
- [x] applyToPlayer: отправка всех пакетов
- [x] applySettingsFrom: копирование настроек
- [x] getNonDefaultSettings: категория "Realistic Physics"
- [x] getVersionRequirementFromSettingName: version=19
- [x] Именованные константы для всех дефолтных значений
- [x] Хелпер-методы: sendShortAndShortPacket, sendShortAndTwoStringsPacket
- [x] blockSurfaceTypes Map для привязки поверхностей к блокам

### 2.3 CommandBoatUtilsModeEdit ✅
- [x] 13 новых свойств в команде `set`
- [x] Подкоманда `addsurfacetype` (привязка поверхности к блокам)
- [x] Подкоманда `clearsurfacetypes`
- [x] Отображение realistic physics в `info`

## ФАЗА 3: ВИЗУАЛЬНАЯ ФИЗИКА НА КЛИЕНТЕ ✅

### 3.1 Визуальный наклон лодки ✅
- [x] **Pitch** (нос вверх/вниз): наклон вперёд при торможении, назад при разгоне
  - Рассчитывается из продольного ускорения: `pitchAngle = -(ax / g) * 0.15`
  - Масштабирование: ×15 → 1g ≈ 8.6° наклона
  - Ограничение: ±25° для предотвращения артефактов
- [x] **Roll** (крен): наклон в сторону при повороте
  - Рассчитывается из бокового ускорения: `rollAngle = (ay / g) * 0.12`
  - Передаётся в PhysicsResult для расширения визуализации
- [x] **Steering angle**: текущий угол руля в PhysicsResult
- [x] Реализовано через `instance.setPitch()` в BoatMixin и AbstractBoatMixin
- [x] Работает на всех 3 версиях MC

## ФАЗА 4: CI/CD ✅

### 3.1 GitHub Actions ✅
- [x] build-release.yml: автосборка при создании release
- [x] Сборка мода (Gradle + Java 21)
- [x] Сборка плагина (Maven + Java 21)
- [x] Загрузка JAR-файлов в release

## ФАЗА 4: РАЛЛИЙНЫЕ ПРОЦЕДУРЫ (БУДУЩЕЕ) ⏳
- [ ] Time Controls + штрафы (10 сек/мин опоздание, 1 мин/мин раннее)
- [ ] Стартовая процедура (обратный отсчёт, световая система)
- [ ] Фальстарт (ступенчатые штрафы: 10с → 1мин → 3мин)
- [ ] Рестарт после схода (10 мин за пропущенный СУ)
- [ ] Сервис-парк / parc fermé логика
- [ ] Recce режим + псевдо-pace notes
- [ ] Дорожные секции с графиком
- [ ] Система повреждений машины

## ФАЗА 5: ПОДДЕРЖКА РУЛЕЙ И ПЕДАЛЕЙ (БУДУЩЕЕ) ⏳
- [ ] Интеграция с GLFW/LWJGL для чтения аналоговых осей рулей и педалей
- [ ] Маппинг аналоговых осей руля на steeringInput (плавный поворот 0.0–1.0 вместо бинарного A/D)
- [ ] Маппинг педалей газа и тормоза на throttleInput/brakeInput (плавное нажатие 0.0–1.0)
- [ ] Настройка кнопок: ручной тормоз, переключение камеры и др.
- [ ] GUI для калибровки и назначения осей
- [ ] Force Feedback (обратная связь на руль) — сопротивление руля в зависимости от скорости и сцепления
