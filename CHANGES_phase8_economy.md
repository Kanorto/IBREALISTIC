# Изменения: Фаза 8 — Экономика (основы)

## Дата
2026-02-12

## Краткое описание
Реализована система внутренней валюты (Rally Coins) с Vault интеграцией, командами, базой данных и автоматическими начислениями за прохождение треков.

## Изменённые файлы

### Плагин (TimingSystem) — Новые файлы

- `src/main/java/me/makkuusen/timing/system/economy/EconomyManager.java` — **НОВЫЙ**: Обёртка над Vault API с graceful fallback
- `src/main/java/me/makkuusen/timing/system/economy/RallyCoinManager.java` — **НОВЫЙ**: Менеджер внутренней валюты (Rally Coins), CRUD операции с БД
- `src/main/java/me/makkuusen/timing/system/economy/EconomyListener.java` — **НОВЫЙ**: Автоматические начисления при TimeTrialFinishEvent
- `src/main/java/me/makkuusen/timing/system/commands/CommandCoins.java` — **НОВЫЙ**: Команда /coins с подкомандами (balance, history, pay, admin)
- `src/main/java/me/makkuusen/timing/system/database/updates/Version14.java` — **НОВЫЙ**: Миграция БД — таблицы ts_player_coins и ts_coin_transactions

### Плагин (TimingSystem) — Изменённые файлы

- `pom.xml` — Добавлена зависимость VaultAPI 1.7.1 (scope: provided)
- `src/main/resources/plugin.yml` — Vault в softdepend; команда coins; permissions
- `src/main/resources/config.yml` — Секция economy с настройками наград
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — Регистрация CommandCoins, EconomyListener, EconomyManager.initialize()
- `src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — DB version 14
- `src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — DB version 14

## Детальное описание

### 1. Vault интеграция (EconomyManager)
- Автоматическое обнаружение Vault при старте плагина
- Если Vault установлен — используется для внешней валюты
- Если нет — работает только внутренняя валюта (Rally Coins)
- API: `isVaultAvailable()`, `getBalance()`, `withdraw()`, `deposit()`, `format()`

### 2. Rally Coins (RallyCoinManager)
- Хранение в БД (SQLite/MySQL/MariaDB)
- Операции: `getBalance()`, `addCoins()`, `spendCoins()`, `setBalance()`, `transfer()`
- История транзакций с причиной
- Включение/отключение через config.yml

### 3. Команды (/coins)
- `/coins` — баланс (total earned, total spent)
- `/coins history` — последние 10 транзакций
- `/coins pay <player> <amount>` — перевод (с проверкой баланса)
- `/coins admin give/take/set` — администрирование

### 4. Автоматические начисления (EconomyListener)
- TimeTrialFinishEvent → базовая награда (20 монет)
- Первое прохождение → ×3 множитель
- Побитие рекорда → +25 бонус
- Все значения настраиваются в config.yml

### 5. База данных (Version14)
- `ts_player_coins`: uuid (PK), balance, total_earned, total_spent
- `ts_coin_transactions`: id (PK), uuid, amount, reason, timestamp

## Тестирование
- [x] Плагин собирается успешно (Maven)
- [x] VaultAPI — нет уязвимостей (проверено через gh-advisory-database)

## Примечание
- CODEBASE_INDEX.md нужно будет обновить при его создании
- Система уровней и XP (8.3), ежедневные задания (8.4) — будущие фазы
