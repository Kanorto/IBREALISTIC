# Изменения: Фаза 12 — Раллийные процедуры (12.1-12.3)

## Дата
2026-02-13

## Краткое описание
Реализованы раллийные процедуры: улучшенная стартовая процедура с цветным обратным отсчётом, система обнаружения фальстарта с штрафами, и временные контроли (Time Controls) для раллийных трасс.

## Изменённые файлы

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
**Файл:** `SoloRaceManager.java`
**Что сделано:**
- Обратный отсчёт через chat messages (НЕ через Titles, по требованию пользователя)
- Цветная система: красный (🔴) для 5-4, жёлтый (🟡) для 3-2-1, зелёный (✦ GO! ✦) при старте
- Звуки: NOTE_BLOCK_HAT на каждую секунду отсчёта, NOTE_BLOCK_PLING на GO
- Заморозка игрока: setWalkSpeed(0) на время отсчёта, восстановление при GO
- Сохранение/восстановление оригинальной скорости ходьбы игрока

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
- [ ] Протестировано в игре (требует ручного тестирования)

## Заметки
- CODEBASE_INDEX.md нужно обновить при его создании:
  - Добавить FalseStartResult.java
  - Обновить описание SoloRaceManager.java (новые методы)
  - Обновить описание RaceSession.java (новые поля)
  - Добавить TIMECONTROL в описание TrackRegion.RegionType
