# Гайд по серверу IBREALISTIC

Полное руководство по настройке и управлению сервером с TimingSystem и OpenBoatUtilsRealistic.

---

## Содержание

1. [Установка и требования](#1-установка-и-требования)
2. [Создание трассы](#2-создание-трассы)
3. [Настройка регионов (чекпоинты, старт, финиш)](#3-настройка-регионов)
4. [Настройка локаций (решётки, лидерборды)](#4-настройка-локаций)
5. [Режимы BoatUtils](#5-режимы-boatutils)
6. [Реалистичный режим физики](#6-реалистичный-режим-физики)
7. [Создание кастомного режима](#7-создание-кастомного-режима)
8. [Тайм-трайл](#8-тайм-трайл)
9. [Проведение гонок (Heat)](#9-проведение-гонок-heat)
10. [Быстрые гонки (Race)](#10-быстрые-гонки-race)
11. [События (Event)](#11-события-event)
12. [Команды (Team)](#12-команды-team)
13. [DRS (Drag Reduction System)](#13-drs)
14. [Коллизии и Ghosting](#14-коллизии-и-ghosting)
15. [Настройки поверхностей для реалистичной физики](#15-настройки-поверхностей)
16. [Настройки игрока](#16-настройки-игрока)
17. [Администрирование](#17-администрирование)
18. [Справочник команд](#18-справочник-команд)

---

## 1. Установка и требования

### Серверная часть
- **Paper** сервер (1.20.4, 1.21 или 1.21.3)
- **Java 21**
- Плагин **TimingSystem** (`TimingSystem.jar` в `plugins/`)
- (Опционально) **DecentHolograms** или **HolographicDisplays** — для голограмм лидербордов

### Клиентская часть
- Мод **OpenBoatUtilsRealistic** (Fabric) установлен у игроков
- Версия мода должна соответствовать версии протокола сервера (текущая: **19**)

### Первый запуск
1. Поместите `TimingSystem.jar` в папку `plugins/`
2. Запустите сервер — создастся `config.yml`
3. Настройте `config.yml` (база данных, язык, DRS и т.д.)
4. Перезапустите сервер

---

## 2. Создание трассы

### Шаг 1: Выберите место и создайте трассу

```
/trackedit create BOAT <название>
```

Типы трасс:
| Тип | Описание |
|-----|----------|
| `BOAT` | Лодочная трасса (основной тип) |
| `ELYTRA` | Трасса для элитр |
| `PARKOUR` | Паркур |

**Пример:**
```
/trackedit create BOAT Rally_Finland
```

### Шаг 2: Установите точку спавна

Встаньте в нужное место и выполните:
```
/trackedit spawn
```

Это точка, куда будут телепортироваться игроки при входе на трассу.

### Шаг 3: Выберите трассу для редактирования

Если вы хотите работать с уже созданной трассой:
```
/trackedit select <название>
```

### Шаг 4: Настройте имя и иконку

```
/trackedit name <отображаемое_имя>
/trackedit item
```
Команда `item` установит предмет в руке как иконку трассы в GUI.

---

## 3. Настройка регионов

Регионы — это зоны на трассе, которые отслеживают прохождение игроков. Для создания региона нужно предварительно выделить область (WorldEdit или встроенные инструменты).

### Типы регионов

| Тип | Описание | Обязательный |
|-----|----------|-------------|
| `START` | Стартовая зона | **Да** |
| `END` | Финишная зона | **Да** |
| `CHECKPOINT` | Чекпоинт (промежуточная точка) | Рекомендуется |
| `PIT` | Питстоп | Нет |
| `RESET` | Зона сброса (возврат на старт) | Нет |
| `INPIT` | Зона внутри пита | Нет |
| `LAGSTART` | Начало lag-зоны | Нет |
| `LAGEND` | Конец lag-зоны | Нет |
| `DRSDETECT` | Зона детекции DRS | Нет |
| `DRSACTIVATE` | Зона активации DRS | Нет |

### Создание региона

1. Выделите область (два угла)
2. Выполните:

```
/trackedit region <тип> [индекс]
```

**Примеры:**
```
/trackedit region START
/trackedit region END
/trackedit region CHECKPOINT 1
/trackedit region CHECKPOINT 2
/trackedit region CHECKPOINT 3
/trackedit region PIT
/trackedit region RESET
```

> **Важно:** Чекпоинты нумеруются с 1. Игрок должен проехать их по порядку!

### Установка точки возврата для региона

Каждый регион имеет точку, куда телепортируется игрок при сбросе:
```
/trackedit regionspawn <регион>
```

### Перезапись чекпоинта

Если нужно обновить существующий чекпоинт (например, расширить зону):
```
/trackedit overload checkpoint <индекс>
```

### Минимальная настройка трассы

Для работающей трассы вам нужно как минимум:
1. **START** — стартовая зона
2. **END** — финишная зона
3. Хотя бы 1 **CHECKPOINT** (рекомендуется)

---

## 4. Настройка локаций

Локации — это точки на карте для специальных целей.

### Типы локаций

| Тип | Описание |
|-----|----------|
| `GRID` | Позиция на стартовой решётке |
| `QUALYGRID` | Позиция на решётке квалификации |
| `LEADERBOARD` | Позиция таблицы лидеров (голограмма) |
| `FINISH_TP_ALL` | Точка телепорта после финиша (для всех) |
| `FINISH_TP` | Точка телепорта после финиша (индивидуальная) |

### Создание локации

Встаньте в нужное место и выполните:
```
/trackedit location <тип> [индекс]
```

**Примеры:**
```
/trackedit location GRID 1
/trackedit location GRID 2
/trackedit location GRID 3
/trackedit location LEADERBOARD
/trackedit location FINISH_TP_ALL
```

### Настройка стартовой решётки

Для гонок с несколькими участниками нужно создать позиции на стартовой решётке:

```
/trackedit location GRID 1    (первая позиция — поул)
/trackedit location GRID 2    (вторая позиция)
/trackedit location GRID 3    (третья позиция)
...
```

Или используйте автоматический генератор:
```
/trackedit gridgenerator
```

---

## 5. Режимы BoatUtils

Каждая трасса имеет режим BoatUtils, который определяет физику лодки.

### Установка стандартного режима

```
/trackedit boatutils <режим>
```

### Доступные стандартные режимы

| Режим | Описание |
|-------|----------|
| `VANILLA` | Ванильная физика |
| `RALLY` | Стандартный Rally |
| `RALLY_BLUE` | Rally с blue ice |
| `BA` | Boat Attack |
| `BA_BLUE` | Boat Attack с blue ice |
| `PARKOUR` | Parkour |
| `PARKOUR_BLUE` | Parkour с blue ice |
| `BA_NOFD` | BA без урона от падения |
| `BA_JANKLESS` | BA без jank |
| `JUMP_BLOCKS` | Прыжковые блоки |
| `BOOSTER_BLOCKS` | Ускоряющие блоки |
| `DEFAULT_ICE` | Дефолтная поверхность — лёд |
| `DEFAULT_BLUE_ICE` | Дефолтная поверхность — blue ice |
| `NOCOL_BOATS_AND_PLAYERS` | Без коллизий с лодками/игроками |
| `NOCOL_ALL_ENTITIES` | Без коллизий вообще |
| **`REALISTIC`** | **Реалистичная физика (WRC)** |
| **`REALISTIC_WRC`** | **WRC Car** |
| **`REALISTIC_GROUP_B`** | **Group B** |
| **`REALISTIC_CLASSIC`** | **Classic Rally** |
| **`REALISTIC_LIGHTWEIGHT`** | **Lightweight** |
| **`REALISTIC_TRUCK`** | **Truck** |

**Пример — установка реалистичного режима:**
```
/trackedit boatutils REALISTIC_WRC
```

### Установка кастомного режима

```
/trackedit customboatutils <имя_режима>
```

---

## 6. Реалистичный режим физики

Реалистичные режимы используют Bicycle Model с моделью шин Fiala/Brush.

### Предустановленные типы машин

| Тип | Масса | База | Привод | Макс. руль | Описание |
|-----|-------|------|--------|------------|----------|
| `REALISTIC` / `REALISTIC_WRC` | 1190 кг | 2.53 м | AWD | 0.60 рад | WRC автомобиль |
| `REALISTIC_GROUP_B` | 1100 кг | 2.40 м | RWD | 0.55 рад | Group B (задний привод) |
| `REALISTIC_CLASSIC` | 1000 кг | 2.45 м | RWD | 0.50 рад | Классический ралли |
| `REALISTIC_LIGHTWEIGHT` | 800 кг | 2.30 м | FWD | 0.65 рад | Лёгкий автомобиль |
| `REALISTIC_TRUCK` | 2000 кг | 3.20 м | AWD | 0.40 рад | Грузовик |

### Быстрая настройка реалистичной трассы

```
# 1. Создайте трассу
/trackedit create BOAT Rally_Stage_1

# 2. Установите точку спавна
/trackedit spawn

# 3. Создайте регионы (START, чекпоинты, END)
/trackedit region START
/trackedit region CHECKPOINT 1
/trackedit region CHECKPOINT 2
/trackedit region CHECKPOINT 3
/trackedit region END

# 4. Установите реалистичный режим
/trackedit boatutils REALISTIC_WRC

# 5. Откройте для тайм-трайла
/trackedit timetrial
/trackedit open
```

### Поверхности в реалистичном режиме

Физика зависит от блоков, по которым едет лодка. Каждый блок маппится на тип поверхности:

| Поверхность | μ (трение) | Описание | Блоки по умолчанию |
|-------------|-----------|----------|-------------------|
| `ASPHALT_DRY` | 0.85 | Сухой асфальт | stone, deepslate, obsidian, quartz |
| `ASPHALT_WET` | 0.55 | Мокрый асфальт | cobblestone, andesite, diorite, granite |
| `GRAVEL` | 0.55 | Гравий | gravel |
| `DIRT` | 0.45 | Грунт | dirt, grass_block, podzol |
| `MUD` | 0.30 | Грязь | mud, soul_sand, soul_soil |
| `SNOW` | 0.30 | Снег | snow_block, powder_snow |
| `ICE` | 0.10 | Лёд | ice, packed_ice, frosted_ice |
| `SAND` | 0.40 | Песок | sand, red_sand |

> **Совет:** Используйте разные блоки на трассе для создания разнообразных поверхностей!

---

## 7. Создание кастомного режима

Кастомные режимы позволяют тонко настроить все параметры физики.

### Шаг 1: Создайте режим

```
/bumode create <имя>
```

### Шаг 2: Откройте для редактирования

```
/bumode edit <имя>
```

### Шаг 3: Настройте параметры

```
/bumode set <свойство> <значение>
```

#### Основные настройки физики

| Свойство | Тип | По умолчанию | Описание |
|----------|-----|-------------|----------|
| `stepHeight` | float | 0 | Высота шага (через бордюры) |
| `defaultSlipperiness` | float | 0.6 | Скользкость по умолчанию |
| `boatJumpForce` | float | 0 | Сила прыжка |
| `gravity` | double | -0.04 | Гравитация |
| `yawAcceleration` | float | 1.0 | Ускорение поворота |
| `forwardAcceleration` | float | 0.04 | Ускорение вперёд |
| `backwardAcceleration` | float | 0.005 | Ускорение назад |
| `turningForwardAcceleration` | float | 0.005 | Ускорение при повороте |
| `boatFallDamage` | boolean | true | Урон от падения |
| `boatAirControl` | boolean | false | Управление в воздухе |
| `airStepping` | boolean | false | Шаг при падении |
| `underwaterControl` | boolean | false | Управление под водой |
| `surfaceWaterControl` | boolean | false | Управление на воде |
| `waterJumping` | boolean | false | Прыжок с воды |
| `swimForce` | float | 0 | Сила плавания |
| `coyoteTime` | int | 0 | Coyote time (тики) |
| `allowAccelerationStacking` | boolean | false | Суммирование ускорений |

#### Настройки реалистичной физики

> **⚠️ ВАЖНО:** При создании кастомного реалистичного режима через `/bumode`, помимо `realisticPhysics true`,
> обязательно установите следующие параметры для корректной работы:
> - `boatAirControl true` — управление в воздухе (иначе лодка останавливается при переходе между блоками)
> - `defaultSlipperiness 0.98` — скользкость по умолчанию (как в режиме RALLY)
> - `stepHeight 1.25` — высота ступеньки
> - `boatFallDamage false` — отключение урона от падения
>
> Встроенные REALISTIC режимы (REALISTIC_WRC и др.) устанавливают эти параметры автоматически.

| Свойство | Тип | По умолчанию | Описание |
|----------|-----|-------------|----------|
| `realisticPhysics` | boolean | false | **Включить реалистичную физику** |
| `vehicleType` | short | -1 | Пресет (0=WRC, 1=GROUP_B, 2=CLASSIC, 3=LIGHTWEIGHT, 4=TRUCK) |
| `vehicleMass` | float | 1190 | Масса (кг) |
| `vehicleWheelbase` | float | 2.53 | Колёсная база (м) |
| `vehicleCgHeight` | float | 0.45 | Высота центра масс (м) |
| `vehicleTrackWidth` | float | 1.55 | Ширина колеи (м) |
| `vehicleMaxSteering` | float | 0.6 | Макс. угол руля (рад) |
| `vehicleSteeringSpeed` | float | 10.0 | Скорость руления (рад/с) |
| `vehicleBrakingForce` | float | 8000 | Сила торможения (Н) |
| `vehicleEngineForce` | float | 5500 | Сила двигателя (Н) |
| `vehicleDrag` | float | 0.35 | Аэродинамическое сопротивление |
| `vehicleBrakeBias` | float | 0.65 | Распределение тормозов (передняя ось) |
| `vehicleSubsteps` | int | 4 | Подшаги физики за тик |
| `vehicleFrontWeightBias` | float | 0.55 | Развесовка (доля на перед) |
| `vehicleDrivetrain` | string | AWD | Тип привода: `RWD`, `FWD`, `AWD` |
| `defaultSurfaceType` | string | ASPHALT_DRY | Дефолтная поверхность |

### Шаг 4: Настройте скользкость блоков (опционально)

```
/bumode addblockslip <скользкость> <блоки>
```

**Пример:**
```
/bumode addblockslip 0.98 minecraft:blue_ice
/bumode addblockslip 0.6 minecraft:stone,minecraft:deepslate
```

Очистка скользкости:
```
/bumode clearblockslip <блоки>
/bumode clearallblockslip
```

### Шаг 5: Настройте типы поверхностей (для реалистичной физики)

```
/bumode addsurfacetype <тип> <блоки>
```

**Пример:**
```
/bumode addsurfacetype GRAVEL minecraft:gravel,minecraft:coarse_dirt
/bumode addsurfacetype ASPHALT_DRY minecraft:black_concrete,minecraft:gray_concrete
/bumode addsurfacetype ICE minecraft:ice,minecraft:packed_ice
```

Очистка:
```
/bumode clearsurfacetypes
```

### Шаг 6: Настройте per-block параметры (опционально)

```
/bumode addperblock <настройка> <значение> <блоки>
```

### Шаг 7: Сохраните режим

```
/bumode save
```

### Шаг 8: Примените к трассе

```
/trackedit customboatutils <имя_режима>
```

### Просмотр информации о режиме

```
/bumode info
```

### Пример: Создание кастомного реалистичного режима

```
# Создание и открытие
/bumode create MyRallyMode
/bumode edit MyRallyMode

# Включение реалистичной физики
/bumode set realisticPhysics true

# ВАЖНО: эти параметры обязательны для корректной работы реалистичной физики
/bumode set boatAirControl true
/bumode set defaultSlipperiness 0.98
/bumode set stepHeight 1.25
/bumode set boatFallDamage false

# Базовые параметры (WRC-like)
/bumode set vehicleMass 1200
/bumode set vehicleWheelbase 2.55
/bumode set vehicleEngineForce 6000
/bumode set vehicleBrakingForce 9000
/bumode set vehicleDrivetrain AWD

# Настройка поверхностей
/bumode addsurfacetype ASPHALT_DRY minecraft:black_concrete,minecraft:gray_concrete
/bumode addsurfacetype GRAVEL minecraft:gravel,minecraft:coarse_dirt
/bumode addsurfacetype DIRT minecraft:dirt,minecraft:grass_block
/bumode addsurfacetype MUD minecraft:mud,minecraft:soul_sand
/bumode set defaultSurfaceType ASPHALT_DRY

# Сохранение
/bumode save

# Применение к трассе
/trackedit select Rally_Stage_1
/trackedit customboatutils MyRallyMode
```

---

## 8. Тайм-трайл

Тайм-трайл — одиночная попытка проехать трассу на время.

### Настройка трассы для тайм-трайла

```
/trackedit timetrial     (включить/выключить)
/trackedit open          (открыть трассу)
```

### Команды для игроков

| Команда | Описание |
|---------|----------|
| `/tt <трасса>` | Телепортация на трассу |
| `/ttr` | Случайная трасса |
| `/ttcancel` | Отмена текущей попытки |
| `/reset` | Сброс (вернуться к последнему чекпоинту) |

### Как работает тайм-трайл

1. Игрок выполняет `/tt <трасса>`
2. Телепортируется на точку спавна
3. Получает лодку с режимом BoatUtils трассы
4. Проезжает через START → CHECKPOINT 1 → ... → CHECKPOINT N → END
5. Время фиксируется, сравнивается с лучшим

### Медали

По результатам тайм-трайлов назначаются медали:

| Медаль | Описание |
|--------|----------|
| 🟫 Netherite | Лучший результат |
| 🟩 Emerald | Топ результат |
| 💎 Diamond | Отличный результат |
| 🥇 Gold | Хороший результат |
| 🥈 Silver | Средний результат |
| 🟤 Copper | Базовый результат |

### Управление результатами

```
/track leaderboard <трасса>     (показать лидерборд)
/track delete <трасса>          (удалить результаты)
```

---

## 9. Проведение гонок (Heat)

Heat — это заезд с несколькими гонщиками.

### Шаг 1: Выберите событие или создайте быструю гонку

Для полноценной гонки нужно **Событие** (Event). Для быстрой гонки см. раздел [10. Быстрые гонки](#10-быстрые-гонки-race).

### Шаг 2: Создайте заезд

```
/heat create <трасса>
```

### Шаг 3: Добавьте гонщиков

```
/heat add <игрок>
```

Или добавьте сразу всех:
```
/heat add alldrivers
```

### Шаг 4: Настройте параметры

```
/heat set laps <количество>         (количество кругов)
/heat set pits <количество>         (обязательные питстопы)
/heat set timelimit <секунды>       (лимит времени)
/heat set maxdrivers <количество>   (макс. гонщиков)
/heat set startdelay <секунды>      (задержка старта)
/heat set collision <режим>         (режим коллизий)
/heat set drs <true/false>          (включить DRS)
/heat set ghostingDelta <значение>  (дельта для ghosting)
```

### Шаг 5: Загрузите заезд

```
/heat load
```

Это телепортирует гонщиков на стартовую решётку.

### Шаг 6: Запустите

```
/heat start
```

Или с проверкой готовности:
```
/heat readycheck
```

### Шаг 7: Управление во время заезда

```
/heat finish        (принудительное завершение)
/heat reset         (сброс заезда)
```

### Результаты

```
/heat results
```

### Сортировка гонщиков

```
/heat sort tt       (по результатам тайм-трайла)
/heat sort random   (случайно)
```

---

## 10. Быстрые гонки (Race)

Упрощённый формат гонки без создания событий.

```
/race create <трасса> [круги] [питы]    (создание)
/race join                               (присоединение)
/race leave                              (выход)
/race start                              (старт)
/race end                                (завершение)
```

**Пример:**
```
/race create Rally_Finland 3 0
# Игроки присоединяются: /race join
/race start
```

---

## 11. События (Event)

Событие — полноценный турнир с раундами и несколькими заездами.

### Создание события

```
/event create <название> [трасса]
```

### Управление участниками

```
/event sign <событие>         (записаться)
/event unsign <событие>       (отписаться)
/event reserve <событие>      (зарезервировать место)
/event spectate <событие>     (наблюдение)
```

### Раунды

```
/round create               (создать раунд)
/round fill                 (заполнить гонщиками)
/round results              (результаты)
```

### Управление событием

```
/event start <событие>
/event finish <событие>
/event delete <событие>
```

---

## 12. Команды (Team)

```
/team create <название>         (создать)
/team delete <название>         (удалить)
/team add <команда> <игрок>     (добавить игрока)
/team remove <команда> <игрок>  (удалить игрока)
/team list                      (список команд)
/team info <команда>            (информация)
```

---

## 13. DRS (Drag Reduction System)

DRS даёт временное ускорение при проезде через специальные зоны.

### Настройка DRS на трассе

1. **Создайте зону детекции:**
```
/trackedit region DRSDETECT 1
```

2. **Создайте зону активации:**
```
/trackedit region DRSACTIVATE 1
```

3. **Включите DRS в заезде:**
```
/heat set drs true
```

### Как работает

1. Игрок проезжает через `DRSDETECT` → система запоминает
2. При проезде через `DRSACTIVATE` → DRS активируется
3. Игрок получает повышенное ускорение
4. DRS деактивируется при следующем повороте/торможении

### Настройки DRS

```
/ts drs set <параметр> <значение>
/heat set drsdowntime <секунды>     (время восстановления)
```

---

## 14. Коллизии и Ghosting

### Режимы коллизий в заезде

```
/heat set collision <режим>
```

| Режим | Описание |
|-------|----------|
| `0` | Ванильные коллизии |
| `1` | Без коллизий с лодками и игроками |
| `2` | Без коллизий со всеми сущностями |
| `3` | Фильтр по типу сущности |
| `4` | Без лодок/игроков + фильтр |

### Ghosting

Ghosting делает игроков невидимыми для других участников:

```
/ghost          (включить невидимость)
/unghost        (выключить невидимость)
```

### Delta Ghosting

Автоматический ghosting на основе разницы во времени между гонщиками:

```
/heat set ghostingDelta <значение>
```

---

## 15. Настройки поверхностей

### Для реалистичной физики

В кастомном режиме можно назначить поверхности конкретным блокам:

```
/bumode addsurfacetype <поверхность> <блоки_через_запятую>
```

### Доступные поверхности

| Поверхность | μ_peak | μ_slide | Сцепление | Характеристика |
|-------------|--------|---------|-----------|----------------|
| `ASPHALT_DRY` | 0.85 | 0.70 | Высокое | Лучшее сцепление, предсказуемо |
| `ASPHALT_WET` | 0.55 | 0.40 | Среднее | Мокрый асфальт, скользко |
| `GRAVEL` | 0.55 | 0.50 | Среднее | Гравий, рыхлый |
| `DIRT` | 0.45 | 0.40 | Ниже среднего | Грунт, мягкий |
| `MUD` | 0.30 | 0.25 | Низкое | Грязь, очень скользко |
| `SNOW` | 0.30 | 0.22 | Низкое | Снег |
| `ICE` | 0.10 | 0.07 | Очень низкое | Лёд, минимальное сцепление |
| `SAND` | 0.40 | 0.35 | Ниже среднего | Песок, вязкий |

### Установка дефолтной поверхности

```
/bumode set defaultSurfaceType ASPHALT_DRY
```

Эта поверхность будет использоваться для всех блоков, которые не назначены явно.

### Пример: Микс-трасса (асфальт + гравий + грязь)

```
/bumode edit MixedSurface
/bumode set realisticPhysics true
/bumode set boatAirControl true
/bumode set defaultSlipperiness 0.98
/bumode set stepHeight 1.25
/bumode set boatFallDamage false
/bumode set defaultSurfaceType ASPHALT_DRY
/bumode addsurfacetype GRAVEL minecraft:gravel,minecraft:coarse_dirt
/bumode addsurfacetype DIRT minecraft:dirt,minecraft:grass_block,minecraft:dirt_path
/bumode addsurfacetype MUD minecraft:mud,minecraft:muddy_mangrove_roots
/bumode addsurfacetype ASPHALT_WET minecraft:cobblestone,minecraft:mossy_cobblestone
/bumode save
```

---

## 16. Настройки игрока

Игроки могут настроить свой клиент:

```
/settings sound <on/off>              (звуки)
/settings verbose <on/off>            (подробный вывод)
/settings timetrial <on/off>          (уведомления тайм-трайла)
/settings compactscoreboard <on/off>  (компактный скорборд)
/settings color <параметр>            (настройки цвета)
/settings shortname <имя>             (короткое имя)
```

### Спавн лодки

```
/boat [тип_дерева]
```

Типы: OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY, BAMBOO

---

## 17. Администрирование

### Теги трасс

```
/ts tag create <имя>
/ts tag delete <имя>
/ts tag set <тег> <трасса>
```

### Управление трассой

```
/trackedit open              (открыть трассу)
/trackedit close             (закрыть трассу)
/trackedit timetrial         (вкл/выкл тайм-трайл)
/trackedit weight <число>    (вес для рандома)
/trackedit owner <игрок>     (передать владельца)
/trackedit delete            (удалить трассу)
/trackedit move              (переместить трассу)
```

### Информация и просмотр

```
/track info <трасса>
/trackedit view              (визуализация регионов)
/track tp <трасса>           (телепортация)
```

### Перезагрузка

```
/track reload
```

---

## 18. Справочник команд

### Все алиасы

| Команда | Алиасы | Назначение |
|---------|--------|------------|
| `/trackedit` | `/te` | Редактирование трасс |
| `/track` | — | Информация о трассах |
| `/heat` | — | Управление заездами |
| `/round` | — | Управление раундами |
| `/event` | — | Управление событиями |
| `/race` | — | Быстрые гонки |
| `/team` | — | Управление командами |
| `/tt` | — | Тайм-трайл |
| `/ttr` | — | Случайная трасса |
| `/ttcancel` | — | Отмена тайм-трайла |
| `/boat` | — | Спавн лодки |
| `/boatutilsmodeedit` | `/bume`, `/bumode` | Кастомные режимы |
| `/ghost` | — | Невидимость |
| `/unghost` | — | Отмена невидимости |
| `/settings` | — | Настройки игрока |
| `/reset` | — | Сброс позиции |
| `/ts` | — | Настройки системы |

---

## Полный пример: Создание реалистичной раллийной трассы

```bash
# === СОЗДАНИЕ ТРАССЫ ===
/trackedit create BOAT Rally_Monte_Carlo

# === ТОЧКА СПАВНА ===
# Встаньте на старт и выполните:
/trackedit spawn

# === РЕГИОНЫ ===
# Выделите зону старта и:
/trackedit region START

# Выделите первый чекпоинт:
/trackedit region CHECKPOINT 1

# Второй чекпоинт:
/trackedit region CHECKPOINT 2

# Третий чекпоинт:
/trackedit region CHECKPOINT 3

# Финишная зона:
/trackedit region END

# === СТАРТОВАЯ РЕШЁТКА ===
# Встаньте на позицию 1 (поул):
/trackedit location GRID 1

# Позиция 2:
/trackedit location GRID 2

# Позиция 3:
/trackedit location GRID 3

# === КАСТОМНЫЙ РЕАЛИСТИЧНЫЙ РЕЖИМ ===
/bumode create MonteCarlo
/bumode edit MonteCarlo

# Включение реалистичной физики
/bumode set realisticPhysics true
/bumode set boatAirControl true
/bumode set defaultSlipperiness 0.98
/bumode set stepHeight 1.25
/bumode set vehicleMass 1200
/bumode set vehicleEngineForce 5800
/bumode set vehicleBrakingForce 8500
/bumode set vehicleDrivetrain AWD
/bumode set vehicleMaxSteering 0.58
/bumode set boatFallDamage false

# Поверхности трассы
/bumode set defaultSurfaceType ASPHALT_DRY
/bumode addsurfacetype ASPHALT_WET minecraft:cobblestone,minecraft:stone_bricks
/bumode addsurfacetype GRAVEL minecraft:gravel
/bumode addsurfacetype ICE minecraft:packed_ice,minecraft:ice
/bumode addsurfacetype SNOW minecraft:snow_block

# Сохранение
/bumode save

# === ПРИМЕНЕНИЕ РЕЖИМА ===
/trackedit select Rally_Monte_Carlo
/trackedit customboatutils MonteCarlo

# === ОТКРЫТИЕ ===
/trackedit timetrial
/trackedit open

# === ГОТОВО! Игроки могут ехать: ===
# /tt Rally_Monte_Carlo
```
