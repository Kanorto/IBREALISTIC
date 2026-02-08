# Система трасс

Пакет: `me.makkuusen.timing.system.track`

---

## Track.java

**Назначение:** Основной класс трассы. Хранит все данные о трассе.
**Аннотации:** `@Getter`

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `id` | `int` | Уникальный ID |
| `displayName` | `String` | Отображаемое имя |
| `commandName` | `String` | Имя для команд |
| `owner` | `TPlayer` | Владелец |
| `contributors` | `List<TPlayer>` | Контрибьюторы |
| `spawnLocation` | `Location` | Точка спавна |
| `type` | `TrackType` | Тип (BOAT/ELYTRA/PARKOUR) |
| `boatUtilsMode` | `BoatUtilsMode` | Режим BoatUtils |
| `customBoatUtilsModeId` | `Integer` | ID кастомного режима |
| `open` | `boolean` | Открыта для игроков |
| `timeTrial` | `boolean` | Доступен тайм-трайл |
| `weight` | `int` | Вес (для рандома) |
| `trackOptions` | `TrackOptions` | Опции |
| `trackLocations` | `TrackLocations` | Локации |
| `trackRegions` | `TrackRegions` | Регионы |
| `trackTags` | `TrackTags` | Теги |
| `timeTrials` | `TimeTrials` | Данные тайм-трайлов |
| `trackMedals` | `TrackMedals` | Медали |

### Enum: TrackType
| Значение | Описание |
|----------|----------|
| `BOAT` | Лодочная трасса |
| `ELYTRA` | Трасса для элитр |
| `PARKOUR` | Паркур |

### Ключевые методы
| Метод | Описание |
|-------|----------|
| `setBoatUtilsMode(BoatUtilsMode)` | Установка режима |
| `setCustomBoatUtilsModeId(Integer)` | Установка кастомного режима |
| `getNumberOfCheckpoints()` | Количество чекпоинтов |
| `isBoatUtils()` | Использует ли BoatUtils |

---

## TrackRegion.java (abstract)

**Назначение:** Абстрактный класс региона трассы.

### Enum: RegionType (10 типов)
| Значение | Описание |
|----------|----------|
| `START` | Стартовая зона |
| `END` | Финишная зона |
| `PIT` | Питстоп |
| `CHECKPOINT` | Чекпоинт |
| `RESET` | Зона сброса |
| `INPIT` | Внутри пита |
| `LAGSTART` | Начало lag-зоны |
| `LAGEND` | Конец lag-зоны |
| `DRSDETECT` | DRS детекция |
| `DRSACTIVATE` | DRS активация |

### Enum: RegionShape
| Значение | Описание |
|----------|----------|
| `POLY` | Полигональный |
| `CUBOID` | Кубоидный |

---

## TrackRegions.java, TrackCuboidRegion.java, TrackPolyRegion.java

Управление коллекцией регионов, кубоидная и полигональная реализации.

---

## TrackLocation.java

### Enum: Type (5 типов)
| Значение | Описание |
|----------|----------|
| `LEADERBOARD` | Лидерборд |
| `GRID` | Стартовая решётка |
| `QUALYGRID` | Решётка квалификации |
| `FINISH_TP_ALL` | Телепорт финиша (все) |
| `FINISH_TP` | Телепорт финиша (индивидуальный) |

---

## TimeTrials.java

**Назначение:** Управление попытками и финишами тайм-трайла для трассы.

### Ключевые методы
| Метод | Описание |
|-------|----------|
| `addFinish(TimeTrialFinish)` | Добавление финиша |
| `getBestFinish(TPlayer)` | Лучший результат игрока |
| `getTopList(int)` | Топ-список |
| `getPlayerTopListPosition(TPlayer)` | Позиция игрока в топе |
| `deleteAllFinishes()` | Удаление всех финишей |

---

## TrackEditor.java

**Назначение:** Редактор трасс. Все методы static.

Функции: создание, удаление, перемещение трасс, управление регионами/локациями/тегами/опциями.

---

## TrackMedals.java

**Назначение:** Система медалей (Netherite, Emerald, Diamond, Gold, Silver, Copper).

---

## HologramManager.java (interface)

**Назначение:** Интерфейс для голограмм (реализации: HologramDH, HologramHD).
