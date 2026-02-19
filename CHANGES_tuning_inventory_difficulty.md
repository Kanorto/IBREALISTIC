# Изменения: Расширенный тюнинг, инвентарь деталей, выбор сложности

## Дата
2026-02-19

## Краткое описание
Добавлена система физического инвентаря деталей (с количеством), 5 новых компонентов тюнинга, GUI выбора сложности перед гонкой с ограничениями на высоких уровнях, и автоматическое ранжирование пресетов.

## Изменённые файлы

### Плагин (TimingSystem)

- `src/main/java/me/makkuusen/timing/system/database/updates/Version27.java` — **НОВЫЙ**: миграция БД v27 (quantity для покупок + новые колонки компонентов)
- `src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — обновлена версия БД до 27 + вызов Version27
- `src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — обновлена версия БД до 27 + вызов Version27
- `src/main/java/me/makkuusen/timing/system/economy/PlayerCar.java` — добавлены 5 новых полей (exhaust, differential, gearbox, turbo, intercooler)
- `src/main/java/me/makkuusen/timing/system/economy/GarageManager.java` — расширены компоненты (13 вместо 8), система количественного инвентаря, автоматическое ранжирование, методы canEquip/getAvailableQuantity
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — 5 новых Packet ID (77-81), поля и отправка пресетов
- `src/main/java/me/makkuusen/timing/system/gui/ShopGui.java` — расширены категории (13 шт), показ количества для каждого пресета
- `src/main/java/me/makkuusen/timing/system/gui/GarageGui.java` — расширены компоненты (13 шт), показ инвентаря
- `src/main/java/me/makkuusen/timing/system/gui/DifficultyGui.java` — **НОВЫЙ**: GUI выбора сложности (5 уровней)
- `src/main/java/me/makkuusen/timing/system/gui/TimeTrialGui.java` — интеграция DifficultyGui при включенной опции
- `src/main/java/me/makkuusen/timing/system/race/RaceSession.java` — поле selectedDifficulty + getEffectiveDifficulty()
- `src/main/java/me/makkuusen/timing/system/race/SoloRaceManager.java` — использование сложности для наград
- `src/main/java/me/makkuusen/timing/system/tplayer/TPlayer.java` — поле selectedDifficulty
- `src/main/java/me/makkuusen/timing/system/theme/messages/Gui.java` — 22 новых enum-значения
- `src/main/resources/config.yml` — 5 новых компонентов + difficulty_selection_enabled
- `src/main/resources/lang/en_us.yml` — 22 новых ключа переводов
- `src/main/resources/lang/de_de.yml` — 22 новых ключа переводов
- `src/main/resources/lang/id_id.yml` — 22 новых ключа переводов
- `src/main/resources/lang/zh_cn.yml` — 22 новых ключа переводов
- `src/main/resources/lang/pl_pl.yml` — 22 новых ключа переводов
- `src/main/resources/lang/nl_nl.yml` — 22 новых ключа переводов
- `src/main/resources/lang/es_es.yml` — 22 новых ключа переводов
- `src/main/resources/lang/pt_br.yml` — 22 новых ключа переводов
- `src/main/resources/lang/fr_fr.yml` — 22 новых ключа переводов
- `src/main/resources/lang/triton.yml` — 22 новых ключа переводов (Triton формат)

### Мод (IBRealistic)
- `src/main/java/dev/kanorto/ibrealistic/ClientboundPackets.java` — обработка пакетов 77-81
- `src/main/java/dev/kanorto/ibrealistic/IBRealistic.java` — сеттеры для новых компонентов

## Детальное описание изменений

### 1. Система физического инвентаря деталей
**Файлы:** GarageManager.java, Version27.java
**Что сделано:**
- Каждая покупка теперь имеет количество (quantity)
- Можно купить несколько одинаковых деталей
- Система отслеживает: куплено / установлено на машинах / доступно
- Защита от дюпа: нельзя установить больше деталей, чем есть в инвентаре
- Новые методы: getPurchasedQuantity(), getInstalledCount(), getAvailableQuantity(), canEquip()

### 2. Новые компоненты тюнинга
**Файлы:** config.yml, GarageManager.java, PlayerCar.java, CustomBoatUtilsMode.java
**Компоненты:**
- **Exhaust** (Выхлоп): STANDARD, SPORT, PERFORMANCE, RACING
- **Differential** (Дифференциал): OPEN, LIMITED_SLIP, LOCKED, TORSEN
- **Gearbox** (Коробка передач): STANDARD_5SPD, CLOSE_RATIO, SEQUENTIAL, DOG_BOX
- **Turbo** (Турбонаддув): NONE, SMALL, MEDIUM, LARGE, ANTI_LAG
- **Intercooler** (Интеркулер): STANDARD, SPORT, RACING, WATER_SPRAY

### 3. GUI выбора сложности
**Файлы:** DifficultyGui.java, TimeTrialGui.java, RaceSession.java, SoloRaceManager.java
**Уровни сложности:**
- ★☆☆☆☆ Easy — без ограничений, награда ×1.0
- ★★☆☆☆ Normal — без ограничений, награда ×1.3
- ★★★☆☆ Hard — макс. уровень деталей 10, награда ×1.6
- ★★★★☆ Expert — макс. уровень деталей 5, награда ×2.0
- ★★★★★ Extreme — только стандартные детали, награда ×2.5
- Включается через `race.difficulty_selection_enabled: true` в конфиге

### 4. Автоматическое ранжирование
**Файл:** GarageManager.java
**Что сделано:**
- Метод autoRankLevel(price) вычисляет уровень из цены: level = sqrt(price) * 0.5
- ensureConfigDefaults() автоматически добавляет все новые компоненты в конфиг при старте

## Новые пакеты
- PACKET_ID_SET_EXHAUST_PRESET (ID: 77)
- PACKET_ID_SET_DIFFERENTIAL_PRESET (ID: 78)
- PACKET_ID_SET_GEARBOX_PRESET (ID: 79)
- PACKET_ID_SET_TURBO_PRESET (ID: 80)
- PACKET_ID_SET_INTERCOOLER_PRESET (ID: 81)

## Изменения в базе данных
- Старая версия: 26
- Новая версия: 27
- Добавлен столбец `quantity` в `ts_player_purchases`
- Добавлены столбцы: exhaust_preset, differential_preset, gearbox_preset, turbo_preset, intercooler_preset в `ts_player_garage`

## Тестирование
- [x] Мод собирается успешно (Gradle, все 3 версии MC)
- [x] Плагин собирается успешно (Maven)
- [ ] Функциональное тестирование на сервере

## Примечание
CODEBASE_INDEX.md отсутствует — при его создании необходимо будет добавить описание всех новых файлов и методов.
