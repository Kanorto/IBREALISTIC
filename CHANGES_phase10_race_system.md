# Изменения: Фаза 10 — Система гонок (Соло + Мультиплеер)

## Дата
2026-02-12

## Краткое описание
Реализация Фазы 10: единая система гонок с поддержкой как соло-режима (изоляция без коллизий и видимости других игроков), так и мультиплеерного режима (совместные гонки через существующую систему QuickRace/Heat).

## Изменённые файлы

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/race/RaceType.java` — enum SOLO/MULTIPLAYER
- `src/main/java/me/makkuusen/timing/system/race/RaceState.java` — enum WAITING/COUNTDOWN/RACING/FINISHED/CANCELLED
- `src/main/java/me/makkuusen/timing/system/race/RaceSession.java` — сессия игрока в гонке (UUID, трек, тип, время)
- `src/main/java/me/makkuusen/timing/system/race/SoloRaceManager.java` — менеджер гонок (изоляция, обратный отсчёт, результаты, награды)
- `src/main/java/me/makkuusen/timing/system/database/updates/Version18.java` — миграция БД для таблицы ts_race_results

#### Изменённые файлы
- `src/main/java/me/makkuusen/timing/system/commands/CommandRace.java` — добавлены команды solo, cancel, results, top
- `src/main/java/me/makkuusen/timing/system/permissions/PermissionRace.java` — добавлены SOLO, RESULTS
- `src/main/java/me/makkuusen/timing/system/theme/messages/Error.java` — RACE_NOT_ENABLED, RACE_ALREADY_ACTIVE, RACE_SERVER_FULL, RACE_TIMEOUT, RACE_NOT_SOLO
- `src/main/java/me/makkuusen/timing/system/theme/messages/Success.java` — RACE_STARTED, RACE_SOLO_FINISH, RACE_CANCELLED
- `src/main/java/me/makkuusen/timing/system/theme/messages/Broadcast.java` — RACE_COUNTDOWN, RACE_GO
- `src/main/java/me/makkuusen/timing/system/theme/messages/Info.java` — RACE_RESULTS_TITLE, RACE_RESULTS_ENTRY, RACE_NO_RESULTS, RACE_SOLO_INFO
- `src/main/java/me/makkuusen/timing/system/boatutils/RealisticFeature.java` — добавлен MULTIPLAYER_RACE(5)
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsManager.java` — feature flag для MULTIPLAYER_RACE
- `src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — версия БД 17→18
- `src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — версия БД 17→18

#### Конфигурация и языки
- `src/main/resources/config.yml` — добавлена секция race (enabled, max_concurrent, countdown_seconds, timeout_minutes, rewards)
- `src/main/resources/plugin.yml` — добавлены permissions: timingsystem.race.solo, timingsystem.race.results
- `src/main/resources/lang/en_us.yml` — все новые ключи гонок (error, success, broadcast, info)
- `src/main/resources/lang/de_de.yml` — немецкие переводы новых ключей
- `src/main/resources/lang/triton.yml` — Triton обёртки для новых ключей
- `triton/timingsystem.json` — 15 новых записей с 8 языками каждая

### Мод (OBURealistic)
- `src/main/java/dev/o7moon/openboatutils/RealisticFeature.java` — добавлен MULTIPLAYER_RACE(5)

### Документация
- `PLAN.md` — обновлена Фаза 10 с отметками выполнения
- `CHANGES_phase10_race_system.md` — этот файл

## Детальное описание изменений

### 1. Система гонок (SoloRaceManager)
**Файл:** `race/SoloRaceManager.java`

**Что сделано:**
- Единый менеджер для соло и мультиплеерных гонок
- Соло-режим: скрытие других игроков через `Player.hideEntity()`, телепортация на старт, обратный отсчёт с Title и звуками
- Мультиплеер: интеграция с существующей системой QuickRace (Event/Round/Heat)
- Сохранение результатов в БД (асинхронно)
- Награды: Rally Coins + XP через RallyCoinManager и LevelManager
- Конфигурируемые параметры через config.yml
- Авто-отмена по тайм-ауту

### 2. Команды (/race)
**Файл:** `commands/CommandRace.java`

**Что сделано:**
- `/race solo <трек> [system|custom]` — запуск соло-гонки (новое)
- `/race cancel` — отмена соло-гонки (новое)
- `/race results <трек> [system|custom]` — просмотр топ-10 результатов (новое)
- `/race top <трек> [system|custom]` — алиас для results (новое)
- `/race leave` — обновлён для поддержки отмены соло-гонок
- Все существующие команды (create, join, start, end) сохранены для мультиплеера

### 3. База данных (Version18)
**Таблица:** `ts_race_results`
- uuid, track_id, time_ms, race_type (SOLO/MULTIPLAYER), car_type (SYSTEM/CUSTOM), created_at
- Индексы по track_id, uuid, track_id+car_type, track_id+race_type

### 4. Feature Flags
- Добавлен MULTIPLAYER_RACE (bit 5) в обоих RealisticFeature (мод и плагин)
- Feature flag устанавливается когда race.enabled = true в config.yml
- soloRace feature flag теперь включён по умолчанию в config.yml

## Изменения VERSION
- Версия БД: 17 → 18 (добавлена таблица ts_race_results)
- Версия протокола: без изменений (не добавлены новые S2C/C2S пакеты)

## Тестирование
- [x] Плагин собирается успешно (Maven)
- [x] Мод собирается успешно (Gradle, все MC версии: 1.20.4, 1.21, 1.21.3)

## Примечания
- CODEBASE_INDEX.md необходимо обновить после его создания (новые файлы в race/ пакете)
- Мультиплеерный режим использует существующую инфраструктуру QuickRace (Event/Round/Heat) без дублирования
- Соло-режим полностью независим от Event/Heat системы
