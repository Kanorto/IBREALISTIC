# Гоночная система

Пакеты: `heat`, `round`, `event`, `participant`, `timetrial`

---

## Heat.java

**Назначение:** Основной класс заезда. Управляет гонщиками, кругами, состоянием.
**Аннотации:** `@Getter`, `@Setter`

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `id` | `int` | Уникальный ID |
| `event` | `Event` | Родительское событие |
| `round` | `Round` | Родительский раунд |
| `heatNumber` | `Integer` | Номер заезда |
| `heatState` | `HeatState` | Состояние |
| `drivers` | `HashMap<UUID, Driver>` | Гонщики |
| `startPositions` | `List<Driver>` | Стартовые позиции |
| `livePositions` | `List<Driver>` | Текущие позиции |
| `totalLaps` | `Integer` | Всего кругов |
| `totalPits` | `Integer` | Количество питстопов |
| `timeLimit` | `Integer` | Лимит времени |
| `maxDrivers` | `Integer` | Макс. гонщиков |
| `collisionMode` | `CollisionMode` | Режим коллизий |
| `ghostingDelta` | `Integer` | Дельта для ghosting |
| `drs` | `Boolean` | DRS включён |

### Enum: HeatState
| Значение | Описание |
|----------|----------|
| `SETUP` | Настройка |
| `LOADED` | Загружен |
| `STARTING` | Запускается |
| `RACING` | Гонка идёт |
| `FINISHED` | Завершён |

### Ключевые методы
| Метод | Описание |
|-------|----------|
| `loadHeat()` | Загрузка заезда |
| `startCountdown()` | Запуск обратного отсчёта |
| `startHeat()` | Старт гонки |
| `finishHeat()` | Завершение |
| `resetHeat()` | Сброс |
| `addDriver(Driver)` | Добавление гонщика |
| `removeDriver(Driver)` | Удаление гонщика |
| `disqualifyDriver(Driver)` | Дисквалификация |

---

## Driver.java

**Назначение:** Гонщик в заезде. Extends `Participant`.
**Аннотации:** `@Getter`, `@Setter`

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `heat` | `Heat` | Заезд |
| `position` | `Integer` | Текущая позиция |
| `startPosition` | `int` | Стартовая позиция |
| `pits` | `int` | Количество питстопов |
| `state` | `DriverState` | Состояние |
| `laps` | `List<Lap>` | Круги |
| `scoreboard` | `DriverScoreboard` | Скорборд |

### Enum: DriverState
| Значение | Описание |
|----------|----------|
| `SETUP` | Настройка |
| `LOADED` | Загружен |
| `STARTING` | Стартует |
| `RUNNING` | Гонка |
| `FINISHED` | Финишировал |
| `DISQUALIFIED` | Дисквалифицирован |
| `RESET` | Сброс |
| `LAPRESET` | Сброс круга |

### Ключевые методы
| Метод | Описание |
|-------|----------|
| `start()` | Старт гонки |
| `finish()` | Финиш |
| `disqualify()` | Дисквалификация |
| `passLap()` | Прохождение круга |
| `passPit()` | Прохождение пита |
| `getBestLap()` | Лучший круг |
| `getCurrentLap()` | Текущий круг |

---

## Lap.java

**Назначение:** Круг в заезде.
**Аннотации:** `@Getter`, `@Setter`

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `player` | `TPlayer` | Игрок |
| `heatId` | `int` | ID заезда |
| `track` | `Track` | Трасса |
| `lapStart` | `Instant` | Начало круга |
| `lapEnd` | `Instant` | Конец круга |
| `checkpoints` | `ArrayList<Instant>` | Чекпоинты |

---

## QualifyHeat.java, FinalHeat.java

**Назначение:** Логика квалификационных и финальных заездов. Все методы static.

---

## Round.java (abstract)

**Назначение:** Абстрактный раунд. Содержит список заездов.

| Тип | Описание |
|-----|----------|
| `QUALIFICATION` | Квалификация |
| `FINAL` | Финал |

---

## Event.java

**Назначение:** Гоночное событие. Содержит расписание раундов.

### Enum: EventState
| Значение | Описание |
|----------|----------|
| `SETUP` | Настройка |
| `RUNNING` | Запущено |
| `FINISHED` | Завершено |

---

## TimeTrial.java

**Назначение:** Тайм-трайл (одиночная попытка на трассе).

### Ключевые методы
| Метод | Описание |
|-------|----------|
| `playerStartingTimeTrial()` | Начало тайм-трайла |
| `playerEndedMap()` | Завершение (с сохранением) |
| `playerRestartMap()` | Перезапуск |
| `passNextCheckpoint()` | Прохождение чекпоинта |
| `validateFinish(Player)` | Валидация финиша |
| `saveAndAnnounceFinish(Player, long)` | Сохранение и объявление |

---

## TimeTrialFinish.java

**Назначение:** Результат финиша тайм-трайла.

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `id` | `int` | ID |
| `trackId` | `int` | ID трассы |
| `uuid` | `UUID` | UUID игрока |
| `time` | `long` | Время (мс) |
| `checkpointTimes` | `Map<Integer, Long>` | Времена чекпоинтов |

---

## TimeTrialController.java

**Назначение:** Контроллер тайм-трайлов. Static maps для активных сессий.

### Статические поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `timeTrials` | `HashMap<UUID, TimeTrial>` | Активные тайм-трайлы |
| `timeTrialSessions` | `HashMap<UUID, TimeTrialSession>` | Сессии |
| `lastTimeTrialTrack` | `HashMap<UUID, Track>` | Последняя трасса |
