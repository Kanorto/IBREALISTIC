# Изменения: Проверка стадии 10 + Реализация стадии 11

## Дата
2026-02-13

## Краткое описание
Исправлены критические проблемы в стадии 10 (система гонок) и реализована стадия 11 полностью (настройки треков: погода, время суток, сложность, динамическая погода).

## Изменённые файлы

### Плагин (TimingSystem) — Исправления стадии 10

- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — добавлен вызов `SoloRaceManager.onShutdown()` в onDisable() + импорт
- `src/main/java/me/makkuusen/timing/system/TSListener.java` — добавлена detection финиша для соло-рейсов (handleSoloRaceRegions), отмена рейса при выходе игрока

### Плагин (TimingSystem) — Стадия 11

- `src/main/java/me/makkuusen/timing/system/track/TrackWeather.java` — **НОВЫЙ** enum: CLEAR, RAIN, HEAVY_RAIN, SNOW, FOG (с ID, совместимыми с модом)
- `src/main/java/me/makkuusen/timing/system/track/TrackTimeOfDay.java` — **НОВЫЙ** enum: DAWN, NOON, SUNSET, NIGHT, MIDNIGHT (с тиками)
- `src/main/java/me/makkuusen/timing/system/track/DynamicWeatherManager.java` — **НОВЫЙ**: планировщик динамической погоды
- `src/main/java/me/makkuusen/timing/system/track/Track.java` — добавлены поля: weatherCondition, trackTime, difficulty, dynamicWeather + сеттеры + getDifficultyStars() + getDifficultyCoinMultiplier() + getDifficultyXpMultiplier()
- `src/main/java/me/makkuusen/timing/system/track/editor/TrackEditor.java` — добавлены методы: setWeather(), setTrackTime(), setDifficulty()
- `src/main/java/me/makkuusen/timing/system/commands/CommandTrackEdit.java` — добавлены подкоманды: weather, time, difficulty
- `src/main/java/me/makkuusen/timing/system/commands/CommandTrack.java` — расширен sendTrackInfo() — отображение погоды, времени, сложности
- `src/main/java/me/makkuusen/timing/system/permissions/PermissionTrackEdit.java` — добавлены: WEATHER, TIME, DIFFICULTY
- `src/main/java/me/makkuusen/timing/system/theme/messages/Error.java` — добавлены: INVALID_VALUE, INVALID_WEATHER, INVALID_DIFFICULTY
- `src/main/java/me/makkuusen/timing/system/theme/messages/Info.java` — добавлены: TRACK_WEATHER, TRACK_TIME_OF_DAY, TRACK_DIFFICULTY, TRACK_BEST_TIME, TRACK_WORLD_RECORD, TRACK_REWARD
- `src/main/java/me/makkuusen/timing/system/database/updates/Version19.java` — **НОВЫЙ**: миграция для полей weatherCondition, trackTime, difficulty в ts_tracks
- `src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — версия БД 18→19, вызов Version19
- `src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — версия БД 18→19, вызов Version19
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — добавлен public метод sendWeatherConditionPacket()
- `src/main/java/me/makkuusen/timing/system/race/SoloRaceManager.java` — добавлены методы applyTrackEnvironment() и resetTrackEnvironment() + интеграция difficulty multiplier в награды

### Переводы
- `src/main/resources/lang/en_us.yml` — добавлены ключи: invalid_value, invalid_weather, invalid_difficulty, track_weather, track_time_of_day, track_difficulty, track_best_time, track_world_record, track_reward
- `src/main/resources/lang/de_de.yml` — добавлены ключи ошибок
- `src/main/resources/lang/triton.yml` — добавлены обёртки Triton для всех новых ключей
- `triton/timingsystem.json` — добавлены переводы на 8 языков для всех новых ключей
- `src/main/resources/plugin.yml` — добавлены permissions: trackedit.weather, trackedit.time, trackedit.difficulty

### Документация
- `PLAN.md` — отмечены выполненные пункты фазы 11 (11.1, 11.2, 11.4, 11.5)

## Детальное описание изменений

### 1. Исправление стадии 10: shutdown hook
**Файл:** `TimingSystem.java`
**Что сделано:** Добавлен вызов `SoloRaceManager.onShutdown()` в метод `onDisable()` для корректной очистки активных соло-рейсов при перезагрузке сервера.

### 2. Исправление стадии 10: finish line detection
**Файл:** `TSListener.java`
**Что сделано:** Добавлен метод `handleSoloRaceRegions()`, который вызывается в `onRegionEnterV2()`. Когда игрок в соло-рейсе пересекает END регион (или START для трасс без END), вызывается `SoloRaceManager.finishSoloRace()`. Без этого исправления рейсы никогда не завершались!

### 3. Исправление стадии 10: cancel on disconnect
**Файл:** `TSListener.java`
**Что сделано:** В `onPlayerQuit()` добавлен вызов `SoloRaceManager.cancelRace()` для автоматической отмены рейса при выходе игрока.

### 4. Стадия 11: Погода для треков (11.1)
**Файлы:** `TrackWeather.java`, `Track.java`, `TrackEditor.java`, `CommandTrackEdit.java`
**Что сделано:**
- Создан enum TrackWeather с 5 значениями (CLEAR, RAIN, HEAVY_RAIN, SNOW, FOG)
- Добавлено поле weatherCondition в Track с сеттером
- Добавлена команда `/te weather <CLEAR|RAIN|HEAVY_RAIN|SNOW|FOG>`
- При старте соло-рейса погода трека отправляется через пакет SET_WEATHER_CONDITION + устанавливается визуальная погода для игрока
- При завершении/отмене рейса погода сбрасывается

### 5. Стадия 11: Время суток для треков (11.2)
**Файлы:** `TrackTimeOfDay.java`, `Track.java`, `CommandTrackEdit.java`
**Что сделано:**
- Создан enum TrackTimeOfDay с 5 пресетами (DAWN, NOON, SUNSET, NIGHT, MIDNIGHT)
- Добавлено поле trackTime (null = серверное время) в Track
- Добавлена команда `/te time <preset|ticks|CLEAR>` — поддержка пресетов и произвольных тиков (0-24000)
- Интеграция с SoloRaceManager: setPlayerTime при старте, resetPlayerTime при завершении

### 6. Стадия 11: Сложность трека (11.4)
**Файлы:** `Track.java`, `TrackEditor.java`, `CommandTrackEdit.java`, `SoloRaceManager.java`
**Что сделано:**
- Добавлено поле difficulty (1-5) в Track
- getDifficultyStars() — визуальное отображение (★★★☆☆)
- getDifficultyCoinMultiplier() / getDifficultyXpMultiplier() — множители наград
- Множители интегрированы в awardRaceRewards()
- Команда `/te difficulty <1-5>`

### 7. Стадия 11: Информация о треке (11.5)
**Файл:** `CommandTrack.java`
**Что сделано:** В sendTrackInfo() добавлено отображение погоды, времени суток и сложности.

### 8. Database Version 19
**Файлы:** `Version19.java`, `SQLiteDatabase.java`, `MySQLDatabase.java`
**Что сделано:** ALTER TABLE ts_tracks ADD COLUMN для weatherCondition (int), trackTime (int nullable), difficulty (int default 1), dynamicWeather (boolean).

### 9. Стадия 11: Динамическая погода (11.3)
**Файлы:** `DynamicWeatherManager.java`, `Track.java`, `TrackEditor.java`, `CommandTrackEdit.java`
**Что сделано:**
- DynamicWeatherManager с BukkitTask для циклической смены погоды
- Цикл: CLEAR → RAIN → HEAVY_RAIN → RAIN → CLEAR
- Поле dynamicWeather в Track
- Команда `/te dynamicweather enable|disable`
- Config: dynamic_weather.change_interval_minutes: 5
- Корректная обработка нескольких игроков на одном треке

### 10. Дополнительные исправления
- Сброс погоды мода (SET_WEATHER_CONDITION = CLEAR) при завершении рейса
- Исправление feature flags: MULTIPLAYER_RACE теперь читается из config.yml realistic.features
- Добавлен multiplayerRace flag в config.yml
- Добавлен DYNAMICWEATHER permission в PermissionTrackEdit и plugin.yml

## Изменения DB version
- Старая версия: 18
- Новая версия: 19
- Причина: добавлены 4 новых поля в таблицу ts_tracks

## Версия realistic_version
- Не изменена (эти изменения затрагивают только плагин, без изменений протокола)

## Тестирование
- [x] Плагин собирается успешно (Maven)
- [x] Мод собирается успешно (Gradle, все MC версии)
- [ ] Протестировано на MC 1.20.4
- [ ] Протестировано на MC 1.21
- [ ] Протестировано на MC 1.21.3

## Примечание: CODEBASE_INDEX.md
При появлении файла CODEBASE_INDEX.md необходимо обновить:
- Добавить TrackWeather.java, TrackTimeOfDay.java, DynamicWeatherManager.java, Version19.java
- Обновить описание Track.java (новые поля и методы)
- Обновить описание TrackEditor.java (новые методы)
- Обновить описание CommandTrackEdit.java (новые подкоманды)
- Обновить описание SoloRaceManager.java (track environment методы)
- Обновить описание TSListener.java (handleSoloRaceRegions)
- Обновить описание BoatUtilsManager.java (feature flags исправление)
