# Изменения: Фаза 14 — Античит

## Дата
2026-02-16

## Краткое описание
Полноценная реализация серверной системы античита: валидация скорости, защита экономики, валидация конфигурации машин, детекция аномалий, логирование нарушений. Переводы на все поддерживаемые языки.

## Изменённые файлы

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/boatutils/AntiCheatManager.java` — серверный менеджер античита
- `src/main/java/me/makkuusen/timing/system/database/updates/Version21.java` — миграция БД (таблица `ts_anticheat_violations`)

#### Изменённые файлы
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — регистрация AntiCheatManager (start/stop)
- `src/main/java/me/makkuusen/timing/system/economy/RallyCoinManager.java` — добавлена защита экономики (rate limiting, daily transfer limit, max transaction, dupe detection)
- `src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — регистрация Version21, таблица в createTables
- `src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — регистрация Version21, таблица в createTables
- `src/main/java/me/makkuusen/timing/system/theme/messages/Warning.java` — добавлены ANTICHEAT_WARNING, ANTICHEAT_VIOLATION, ANTICHEAT_DISQUALIFIED, ANTICHEAT_CAR_RESET, ANTICHEAT_RATE_LIMIT, ANTICHEAT_TRANSFER_LIMIT
- `src/main/java/me/makkuusen/timing/system/theme/messages/Info.java` — добавлены ANTICHEAT_STATUS_TITLE, ANTICHEAT_STATUS_ENABLED, ANTICHEAT_STATUS_VIOLATIONS, ANTICHEAT_ADMIN_RESET, ANTICHEAT_ADMIN_NOTIFY, ANTICHEAT_ECONOMY_AUDIT_OK, ANTICHEAT_ECONOMY_AUDIT_FAIL

### Переводы
- `src/main/resources/lang/en_us.yml` — 13 новых ключей (6 warning + 7 info)
- `src/main/resources/lang/de_de.yml` — немецкий перевод
- `src/main/resources/lang/es_es.yml` — испанский перевод
- `src/main/resources/lang/fr_fr.yml` — французский перевод
- `src/main/resources/lang/pl_pl.yml` — польский перевод
- `src/main/resources/lang/nl_nl.yml` — голландский перевод
- `src/main/resources/lang/pt_br.yml` — португальский перевод
- `src/main/resources/lang/id_id.yml` — индонезийский перевод
- `src/main/resources/lang/zh_cn.yml` — китайский перевод
- `src/main/resources/lang/triton.yml` — обёртки для Triton
- `triton/timingsystem.json` — Triton JSON коллекция (13 новых записей)

### Документация
- `PLAN.md` — отмечена Фаза 14 как завершённая
- `CHANGES_phase14_anticheat.md` — данный файл

## Детальное описание изменений

### 1. AntiCheatManager.java
**Файл:** `boatutils/AntiCheatManager.java`
**Что сделано:**
- Серверная проверка скорости каждые N тиков (конфигурируемо)
- Макс. скорость по типу машины (WRC: 2.5, GROUP_B: 2.8, CLASSIC: 2.0, LIGHTWEIGHT: 2.2, TRUCK: 1.8 блоков/тик)
- Допуск +20% для сетевых задержек (speedTolerance=1.2)
- Детекция аномальной акселерации (MAX_ACCELERATION_PER_TICK=0.5)
- Детекция телепортации (расстояние > TELEPORT_DETECTION_DISTANCE=20 блоков)
- 3-ступенчатая система нарушений:
  1. Предупреждение — текстовое сообщение
  2. Телепорт — возврат на последнюю безопасную позицию (lastSafeLocation)
  3. Дисквалификация — выброс из транспорта + 30с кулдаун
- Уведомление админов (permission: timingsystem.admin)
- Асинхронное логирование нарушений в БД
- Валидация конфигурации машины (проверка уровней пресетов)
- Конфигурация через config.yml (anticheat.*)

### 2. Защита экономики (RallyCoinManager.java)
**Файл:** `economy/RallyCoinManager.java`
**Что сделано:**
- Rate limiting: максимум N транзакций в минуту (anticheat.economy.max_transactions_per_minute, default=30)
- Лимит переводов: максимум M монет в день (anticheat.economy.max_daily_transfer, default=10000)
- Максимальная сумма одной транзакции (anticheat.economy.max_single_transaction, default=50000)
- Детекция дюпов: auditCoinSupply() — проверяет SUM(balance) vs SUM(transactions)
- Все проверки с логированием подозрительных действий
- Admin операции (setBalance) обходят rate limiting

### 3. База данных (Version21.java)
**Что сделано:**
- Новая таблица `ts_anticheat_violations` (id, uuid, type, details, timestamp)
- Индексы по uuid и type для быстрого поиска
- Миграция для MySQL и SQLite

### 4. Переводы
**Что сделано:**
- 6 ключей warning: anticheat_warning, anticheat_violation, anticheat_disqualified, anticheat_car_reset, anticheat_rate_limit, anticheat_transfer_limit
- 7 ключей info: anticheat_status_title, anticheat_status_enabled, anticheat_status_violations, anticheat_admin_reset, anticheat_admin_notify, anticheat_economy_audit_ok, anticheat_economy_audit_fail
- Переведено на все 9 языков + triton обёртки
- Triton JSON коллекция обновлена

## Конфигурация (config.yml)

```yaml
# ─── ANTI-CHEAT ───
anticheat:
  enabled: true
  check_interval_ticks: 10
  speed_tolerance: 1.2
  max_violations: 3
  notify_admins: true
  log_violations: true
  economy:
    max_transactions_per_minute: 30
    max_daily_transfer: 10000
    max_single_transaction: 50000
```

## Дополнительные меры безопасности (сверх плана)

1. **Детекция аномальной акселерации** — обнаруживает невозможные ускорения, которые не могут быть достигнуты обычным геймплеем
2. **Детекция телепортации** — обнаруживает перемещения на большие расстояния за один тик (логирует, но не наказывает, т.к. может быть серверный телепорт)
3. **Отслеживание безопасной позиции** — lastSafeLocation обновляется только при чистых тиках, так телепорт назад работает точнее
4. **Кулдаун после дисквалификации** — 30 секунд, чтобы предотвратить спам нарушений
5. **Макс. сумма одной транзакции** — защита от неконтролируемых начислений
6. **Аудит общей суммы монет** — auditCoinSupply() сравнивает сумму балансов с суммой транзакций для обнаружения дюпов

## Тестирование
- [x] Плагин собирается успешно (Maven)
- [x] Мод собирается успешно (Gradle) — изменения мода не затронуты
- [x] Все MC версии (1.20.4, 1.21, 1.21.3) — сборка проходит

## Примечание
- CODEBASE_INDEX.md нужно будет обновить, когда он будет создан
- Версия протокола (VERSION) не изменена, т.к. не добавлены новые пакеты клиент-сервер
- realistic_version не изменена, т.к. изменения только серверные (плагин)
