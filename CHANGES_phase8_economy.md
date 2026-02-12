# Изменения: Фаза 8 — Экономика и Прогрессия (полная)

## Дата
2026-02-12

## Краткое описание
Полная реализация экономической системы: Rally Coins (внутренняя валюта), система уровней/XP с отображением в XP-баре, ежедневные задания. Vault удалён.

## Новые файлы

- `economy/RallyCoinManager.java` — Менеджер внутренней валюты Rally Coins
- `economy/LevelManager.java` — Система уровней и XP с прогрессивной формулой
- `economy/DailyChallengeManager.java` — Ежедневные задания (7 типов, 3 в день)
- `economy/EconomyListener.java` — Обработчик TimeTrialFinishEvent (монеты + XP + челленджи)
- `commands/CommandCoins.java` — /coins с подкомандами
- `commands/CommandLevel.java` — /level (view, top, admin setlevel/addxp)
- `commands/CommandDaily.java` — /daily (view, admin regenerate)
- `database/updates/Version14.java` — ts_player_coins + ts_coin_transactions
- `database/updates/Version15.java` — ts_player_levels
- `database/updates/Version16.java` — ts_daily_challenges + ts_player_daily_progress

## Изменённые файлы

- `TimingSystem.java` — Регистрация команд, LevelManager.initialize()
- `pom.xml` — Удалён VaultAPI
- `plugin.yml` — Удалён Vault; добавлены команды level, daily; permissions
- `config.yml` — Добавлена секция levels
- `MySQLDatabase.java` — DB version → 16
- `SQLiteDatabase.java` — DB version → 16

## Удалённые файлы

- `economy/EconomyManager.java` — Vault (удалена по запросу)

## Тестирование
- [x] Плагин собирается успешно (Maven package)
- [x] Vault полностью удалён
