# Прочие компоненты плагина

---

## DRS (me.makkuusen.timing.system.drs)

### DrsManager.java
**Назначение:** Система DRS (Drag Reduction System) — временное ускорение.

### Ключевые методы
| Метод | Описание |
|-------|----------|
| `activateDrs(Player)` | Активация DRS |
| `deactivateDrs(Player)` | Деактивация |
| `playerPassedDrsDetect(Player, int)` | Игрок прошёл зону детекции |
| `playerPassedDrsActivate(Player, int)` | Игрок прошёл зону активации |
| `sendForwardAccelerationPacket(Player, float)` | Отправка пакета ускорения |

---

## Ghosting (me.makkuusen.timing.system.loneliness)

### LonelinessController.java (Listener)
**Назначение:** Контроллер невидимости игроков в заездах.

| Метод | Описание |
|-------|----------|
| `updatePlayersVisibility(Player)` | Обновление видимости |
| `showPlayerAndCustomBoat(Player, Player)` | Показать игрока |
| `hidePlayerAndCustomBoat(Player, Player)` | Скрыть игрока |
| `showAllOthers(Player)` | Показать всех |
| `hideAllOthers(Player)` | Скрыть всех |
| `showHeatPlayersOnly(Player, Heat)` | Показать только гонщиков |
| `ghost(UUID)` | Включить невидимость |
| `unghost(UUID)` | Выключить |

### DeltaGhostingController.java
**Назначение:** Ghosting на основе дельты времени между гонщиками.

| Метод | Описание |
|-------|----------|
| `checkDeltas(Driver)` | Проверка дельт |
| `deltaGhost(Driver, Driver)` | Ghost по дельте |
| `unDeltaGhost(Driver, Driver)` | Unghost по дельте |
| `clearDeltaGhosts(Heat)` | Очистка для заезда |

---

## Разрешения (me.makkuusen.timing.system.permissions)

### Permissions.java (interface)
Базовый интерфейс для всех enum разрешений.

### Enum классы разрешений
| Класс | Описание | Количество узлов |
|-------|----------|-----------------|
| `PermissionTimingSystem` | Общие (reset, boat, settings, DRS, ghost) | 15+ |
| `PermissionTrack` | Трассы (menu, tp, info, delete, session) | 15+ |
| `PermissionTrackEdit` | Редактирование (create, move, region, option) | 20+ |
| `PermissionHeat` | Заезды | 10+ |
| `PermissionRound` | Раунды | 5+ |
| `PermissionRace` | Быстрые гонки | 5+ |
| `PermissionEvent` | События | 10+ |
| `PermissionTeam` | Команды | 5+ |
| `PermissionTimeTrial` | Тайм-трайлы | 5+ |
| `PermissionBoatUtilsMode` | Кастомные режимы (create, edit, save) | 3 |

---

## Команды (me.makkuusen.timing.system.team)

### Team.java
**Назначение:** Гоночная команда. Implements `Comparable`.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `int` | ID |
| `name` | `String` | Имя |
| `players` | `List<TPlayer>` | Участники |
| `creator` | `UUID` | Создатель |

### TeamManager.java
CRUD операции для команд (static методы).

---

## Игрок (me.makkuusen.timing.system.tplayer)

### TPlayer.java
**Назначение:** Данные игрока в системе TimingSystem.

| Поле | Тип | Описание |
|------|-----|----------|
| `uuid` | `UUID` | UUID |
| `name` | `String` | Имя |
| `settings` | `Settings` | Настройки |
| `theme` | `Theme` | Тема |
| `filter` | `TrackFilter` | Фильтр трасс |
| `boatUtilsVersion` | `Integer` | Версия BoatUtils клиента |

### Settings.java
Настройки игрока: лодка, звук, verbose, тайм-трайл, цвета и т.д.

---

## Лодки (me.makkuusen.timing.system.boat)

### BoatSpawner.java (interface)
| Метод | Описание |
|-------|----------|
| `spawnBoat(Location, String, Boolean)` | Спавн лодки |

### DefaultBoatSpawner.java
Реализация: поддержка 8 типов дерева (OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY, BAMBOO).

### BoatSpawnManager.java
Static менеджер для установки и получения текущего спавнера.

---

## Слушатели (me.makkuusen.timing.system.listeners)

| Класс | Описание |
|-------|----------|
| `GSitListener` | Интеграция с GSit (сидение) |
| `DriverSwapListener` | Обработка смены гонщика |
| `ReadyCheckListener` | Обработка проверки готовности |
| `DrsListener` | Обработка DRS |

---

## Прочее

### UUIDFetcher.java
Получение UUID по нику через Mojang API.

### TimingSystemPlaceholder.java
Интеграция с PlaceholderAPI для отображения данных в чате/скорбордах.

### LogEntry.java / LogEntryBuilder.java
Запись и создание логов событий.
