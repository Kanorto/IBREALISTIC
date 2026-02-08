# Публичный API плагина

Пакет: `me.makkuusen.timing.system.api`

---

## TimingSystemAPI.java

**Назначение:** Основной публичный API для интеграции с другими плагинами.

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `getDriverFromRunningHeat(UUID)` | `Optional<Driver>` | Гонщик в активном заезде |
| `getTrack(String)` | `Optional<Track>` | Трасса по имени |
| `getTrackById(int)` | `Optional<Track>` | Трасса по ID |
| `getTracks()` | `List<Track>` | Все трассы |
| `getAvailableTracks(Player)` | `List<Track>` | Доступные игроку |
| `getOpenTracks()` | `List<Track>` | Открытые трассы |
| `getRandomTrack()` | `Optional<Track>` | Случайная трасса |
| `getBestTime(UUID, int)` | `Optional<TimeTrialFinish>` | Лучшее время |
| `getTPlayer(UUID)` | `TPlayer` | Данные игрока |
| `teleportPlayerAndSpawnBoat(Player, Track, Location)` | `void` | ТП и спавн лодки |
| `resetPlayer(Player)` | `void` | Сброс игрока |
| `getHeats()` | `List<Heat>` | Все заезды |
| `getRunningHeats()` | `List<Heat>` | Активные заезды |
| `getEvents()` | `List<Event>` | Все события |
| `getActiveEvents()` | `List<Event>` | Активные события |
| `setBoatSpawner(BoatSpawner)` | `void` | Установка спавнера лодок |

---

## QuickRaceAPI.java

**Назначение:** API для быстрых гонок.

### Методы
| Метод | Описание |
|-------|----------|
| `create(UUID, Track, int, int)` | Создание быстрой гонки |
| `start()` | Старт |
| `end()` | Конец |
| `addPlayer(Player)` | Добавление игрока |
| `removePlayer(Player)` | Удаление |
| `quickRaceActive()` | Активна ли гонка |

---

## EventResultsAPI.java

**Назначение:** API результатов событий.

### Методы
| Метод | Возврат | Описание |
|-------|---------|----------|
| `getEventResult(String)` | `EventResult` | Результат по имени |
| `getEventResults()` | `List<EventResult>` | Все результаты |

---

## DriverDetails.java

**Назначение:** DTO с деталями гонщика для API.

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `name` | `String` | Имя игрока |
| `uuid` | `UUID` | UUID |
| `position` | `int` | Позиция |
| `heat` | `Heat` | Заезд |
| `laps` | `List<Lap>` | Круги |

---

## DTO результатов (api/event/)

| Класс | Поля | Описание |
|-------|------|----------|
| `EventResult` | name, date, trackName, state, rounds | Результат события |
| `RoundResult` | name, type, index, heatResults | Результат раунда |
| `HeatResult` | name, totalLaps, dateStarted, driverResults | Результат заезда |
| `DriverResult` | position, name, uuid, finishTimeInMs, laps | Результат гонщика |
| `LapResult` | timeInMs, pitstop, fastest | Результат круга |

---

## События (api/events/)

### Общие события
| Событие | Описание | Cancellable |
|---------|----------|-------------|
| `HeatFinishEvent` | Заезд завершён | Нет |
| `BoatSpawnEvent` | Спавн лодки | **Да** |
| `TimeTrialStartEvent` | Начало тайм-трайла | Нет |
| `TimeTrialFinishEvent` | Завершение тайм-трайла | Нет |
| `TimeTrialAttemptEvent` | Попытка тайм-трайла | Нет |
| `GuiOpenEvent` | Открытие GUI | **Да** |
| `BoatUtilsAppliedEvent` | Режим BoatUtils применён | Нет |

### События гонщика (api/events/driver/)
| Событие | Описание |
|---------|----------|
| `DriverStartEvent` | Старт гонщика |
| `DriverFinishLapEvent` | Завершение круга |
| `DriverFinishHeatEvent` | Финиш заезда |
| `DriverPassCheckpointEvent` | Прохождение чекпоинта |
| `DriverPassPitEvent` | Прохождение пита |
| `DriverDisqualifyEvent` | Дисквалификация |
| `DriverNewLapEvent` | Новый круг |
| `DriverSwapEvent` | Смена гонщика |
| `DriverScoreboardTitleUpdateEvent` | Обновление заголовка скорборда |
| `SpectatorScoreboardTitleUpdateEvent` | Обновление скорборда зрителей |
| `DriverPlacedOnGrid` | Размещение на решётке |
