# Изменения: Фаза 12 — Раллийные процедуры (12.1-12.3)

## Дата
2026-02-13

## Краткое описание
Реализованы раллийные процедуры: синхронизированная стартовая процедура с клиентским рендером (цветной блок + частицы), система обнаружения фальстарта с штрафами, и временные контроли (Time Controls) для раллийных трасс.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/ClientboundPackets.java` — добавлен SET_RACE_COUNTDOWN (ID 69) с обработчиком (long goTimeMs, int countdownSeconds)
- `src/main/java/dev/o7moon/openboatutils/OpenBoatUtils.java` — добавлены поля countdown state (countdownGoTimeMs, countdownSeconds, countdownActive), методы setRaceCountdown(), getCountdownRemaining(), isCountdownGo(), reset в resetSettings()
- `src/main/java/dev/o7moon/openboatutils/client/OpenBoatUtilsClient.java` — регистрация ClientTickEvents для tick() и WorldRenderEvents.LAST для render(), мультиверсионная поддержка tickDelta/tickCounter
- `src/main/java/dev/o7moon/openboatutils/client/RaceCountdownRenderer.java` — **НОВЫЙ**: клиентский рендер обратного отсчёта с 3D блоком (RED_CONCRETE→YELLOW_CONCRETE→LIME_CONCRETE) и частицами (FLAME→END_ROD→HAPPY_VILLAGER)

### Плагин (TimingSystem)

#### Java (логика)
- `src/main/java/me/makkuusen/timing/system/race/SoloRaceManager.java` — полная переработка: цветной обратный отсчёт (красный→жёлтый→зелёный), заморозка игрока при отсчёте, детекция фальстарта, обработка Time Control регионов, учёт штрафов при финише
- `src/main/java/me/makkuusen/timing/system/race/RaceSession.java` — добавлены поля: falseStartPenaltySeconds, timeControlPenaltySeconds, passedTimeControls, countdownLocation; методы: getTotalPenaltyMs(), getAdjustedTimeMs(), recordFalseStart(), hasPassedTimeControl(), recordTimeControlPass()
- `src/main/java/me/makkuusen/timing/system/race/FalseStartResult.java` — **НОВЫЙ**: enum результатов фальстарта (PENALTY, RESTART, DISQUALIFIED)
- `src/main/java/me/makkuusen/timing/system/TSListener.java` — интеграция false start detection при движении игрока, обработка TIME_CONTROL регионов при пересечении
- `src/main/java/me/makkuusen/timing/system/track/regions/TrackRegion.java` — добавлен RegionType.TIMECONTROL

#### Enum сообщений
- `src/main/java/me/makkuusen/timing/system/theme/messages/Error.java` — +5: FALSE_START_PENALTY, FALSE_START_RESTART, FALSE_START_DISQUALIFIED, TIME_CONTROL_LATE, TIME_CONTROL_EARLY
- `src/main/java/me/makkuusen/timing/system/theme/messages/Warning.java` — +1: FALSE_START_WARNING
- `src/main/java/me/makkuusen/timing/system/theme/messages/Success.java` — +1: RACE_RESTARTED
- `src/main/java/me/makkuusen/timing/system/theme/messages/Info.java` — +3: RACE_PENALTY_SUMMARY, TIME_CONTROL_PASSED, TIME_CONTROL_PENALTY
- `src/main/java/me/makkuusen/timing/system/theme/messages/Broadcast.java` — +2: RACE_COUNTDOWN_RED, RACE_COUNTDOWN_YELLOW

#### Конфигурация
- `src/main/resources/config.yml` — добавлены секции `race.false_start` и `race.time_control`

#### Переводы
- `src/main/resources/lang/en_us.yml` — английский (основной)
- `src/main/resources/lang/de_de.yml` — немецкий
- `src/main/resources/lang/id_id.yml` — индонезийский
- `src/main/resources/lang/zh_cn.yml` — китайский
- `src/main/resources/lang/pl_pl.yml` — польский
- `src/main/resources/lang/nl_nl.yml` — голландский
- `src/main/resources/lang/es_es.yml` — испанский
- `src/main/resources/lang/pt_br.yml` — португальский
- `src/main/resources/lang/fr_fr.yml` — французский
- `src/main/resources/lang/triton.yml` — обёртки для Triton
- `triton/timingsystem.json` — Triton JSON коллекция

## Детальное описание изменений

### 1. Стартовая процедура (12.1)
**Файлы:** `SoloRaceManager.java`, `CustomBoatUtilsMode.java`, `ClientboundPackets.java`, `OpenBoatUtils.java`, `RaceCountdownRenderer.java`, `OpenBoatUtilsClient.java`
**Что сделано:**

**Серверная часть (плагин):**
- При запуске гонки рассчитывается `goTimeMs = System.currentTimeMillis() + countdownSeconds * 1000`
- Отправляется S2C пакет `SET_RACE_COUNTDOWN` (ID 69) с goTimeMs и countdownSeconds
- Fallback: цветные chat messages (красный 🔴, жёлтый 🟡, зелёный ✦ GO! ✦)
- Заморозка игрока: setWalkSpeed(0) на время отсчёта
- При отмене — отправляется пакет с goTimeMs=0 для отмены клиентского отсчёта

**Клиентская часть (мод):**
- Получает пакет с goTimeMs и countdownSeconds
- Самостоятельно рассчитывает оставшиеся секунды по `System.currentTimeMillis()`
- Рендерит цветной 3D блок (0.5 масштаб) перед лицом игрока на расстоянии 2 блока:
  - 5-4: RED_CONCRETE с частицами FLAME
  - 3-2-1: YELLOW_CONCRETE с частицами END_ROD
  - GO: LIME_CONCRETE с частицами HAPPY_VILLAGER
- Играет звуки: HAT на каждую секунду, PLING на GO
- **GO у всех клиентов в одну миллисекунду** — независимо от пинга
- Мультиверсионная поддержка: tickDelta (1.20.4) / tickCounter (1.21+)

### 2. Фальстарт (12.2)
**Файл:** `SoloRaceManager.java`, `RaceSession.java`, `FalseStartResult.java`
**Что сделано:**
- Детекция горизонтального перемещения во время COUNTDOWN (порог 0.5 блоков)
- Проверка вызывается как из countdown цикла, так и из TSListener.handleCountdownMovement()
- Ступенчатые штрафы:
  - 1-й фальстарт: +10 секунд штрафа, телепорт обратно
  - 2-й фальстарт: +60 секунд штрафа, перезапуск обратного отсчёта
  - 3-й фальстарт: дисквалификация (cancelRace)
- Все штрафы накапливаются в falseStartPenaltySeconds и учитываются при финише
- Конфигурация: `race.false_start.enabled` (по умолчанию true)

### 3. Time Controls (12.3)
**Файл:** `TrackRegion.java`, `SoloRaceManager.java`, `RaceSession.java`, `TSListener.java`
**Что сделано:**
- Новый тип региона: `TrackRegion.RegionType.TIMECONTROL`
- Расчёт целевого времени прибытия по индексу региона: (index+1) × window_seconds
- Штрафы:
  - Опоздание: 10 секунд за каждую минуту задержки
  - Ранее прибытие: 60 секунд за каждую минуту
  - Вовремя: сообщение об успешном прохождении
- Отслеживание пройденных контролей через Set<Integer> в RaceSession
- При финише отображается сводка штрафов (raw time + penalties = total)
- Конфигурация: `race.time_control.enabled`, `window_seconds`, `late_penalty_seconds`, `early_penalty_seconds`

### 4. Финиш с учётом штрафов
**Файл:** `SoloRaceManager.java`
**Что сделано:**
- При финише рассчитывается adjustedTimeMs = elapsedMs + totalPenaltyMs
- В базу данных записывается скорректированное время
- Если есть штрафы — игроку показывается сводка (raw time | penalties | total)
- Награды рассчитываются от скорректированного времени

### 5. Корректная отмена гонки
**Файл:** `SoloRaceManager.java`
**Что сделано:**
- При отмене гонки (cancelRace) обязательно вызывается unfreezePlayer
- При отмене очищается savedWalkSpeeds
- При onShutdown() очищается savedWalkSpeeds

## Новые пакеты
- SET_RACE_COUNTDOWN (ID: 69) — S2C пакет для синхронизации обратного отсчёта
  - Формат: `[short: packetId=69] [long: goTimeMs] [int: countdownSeconds]`
  - goTimeMs=0 отменяет текущий отсчёт

## Конфигурация (config.yml)

Новые секции:
```yaml
race:
  false_start:
    enabled: true
  time_control:
    enabled: true
    window_seconds: 60
    late_penalty_seconds: 10
    early_penalty_seconds: 60
```

## Тестирование
- [x] Плагин собирается успешно (Maven compile)
- [x] Мод собирается успешно (Gradle chiseledBuild, все 3 MC версии: 1.20.4, 1.21, 1.21.3)
- [ ] Протестировано в игре (требует ручного тестирования)

## Заметки
- CODEBASE_INDEX.md нужно обновить при его создании:
  - Добавить FalseStartResult.java
  - Обновить описание SoloRaceManager.java (новые методы)
  - Обновить описание RaceSession.java (новые поля)
  - Добавить TIMECONTROL в описание TrackRegion.RegionType
