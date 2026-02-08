# Слой базы данных

Пакет: `me.makkuusen.timing.system.database`

---

## Архитектура

Используется паттерн **Interface + Multiple Implementations**:
- `TSDatabase` — основной интерфейс (игроки)
- `TrackDatabase` — трассы, регионы, локации, финиши
- `EventDatabase` — события, раунды, заезды, гонщики, круги
- `TeamDatabase` — команды
- `LogDatabase` — логи

Реализации:
- `SQLiteDatabase` — SQLite (по умолчанию)
- `MySQLDatabase` — MySQL
- `MariaDBDatabase` — MariaDB

---

## TSDatabase.java (interface)

### Методы управления
| Метод | Описание |
|-------|----------|
| `initialize()` | Инициализация подключения |
| `update()` | Обновление схемы |
| `createTables()` | Создание таблиц |

### Методы игроков
| Метод | Описание |
|-------|----------|
| `selectPlayers()` | Все игроки |
| `createPlayer(UUID, String)` | Создание |
| `playerUpdateValue(UUID, String, String/Boolean)` | Обновление |

### Статические методы
| Метод | Описание |
|-------|----------|
| `synchronize()` | Синхронизация кеша |
| `getPlayer(UUID/String/CommandSender)` | Получение игрока |
| `reload()` | Перезагрузка |

---

## TrackDatabase.java (interface)

### Методы SELECT
| Метод | Описание |
|-------|----------|
| `selectTracks()` | Все трассы |
| `selectFinishes(int)` | Финиши трассы |
| `selectAttempts(int)` | Попытки трассы |
| `selectTags()` | Все теги |
| `selectRegions(int)` | Регионы трассы |
| `selectLocations(int)` | Локации трассы |
| `selectOptions(int)` | Опции трассы |

### Статические методы
| Метод | Описание |
|-------|----------|
| `initDatabaseSynchronize()` | Инициализация |
| `trackNew(...)` | Создание трассы |
| `removeTrack(Track)` | Удаление |
| `getTrack(String)` | По имени |
| `getTrackById(int)` | По ID |
| `getOpenTracks()` | Открытые трассы |
| `getAvailableTracks(Player)` | Доступные игроку |

---

## EventDatabase.java (interface)

### Статические поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `events` | `Set<Event>` | Все события |
| `heats` | `Set<Heat>` | Все заезды |
| `playerInRunningHeat` | `HashMap<UUID, Driver>` | Игроки в активных заездах |
| `playerSelectedEvent` | `HashMap<UUID, Event>` | Выбранное событие |

---

## Миграции (database/updates/)

| Класс | Описание |
|-------|----------|
| `Version2` — `Version13` | Последовательные обновления схемы БД |

Каждая версия добавляет новые таблицы, колонки или индексы.
