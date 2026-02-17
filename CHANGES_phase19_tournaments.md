# Изменения: Фаза 19 — Автоматические турниры 🏆

## Дата
2026-02-17

## Краткое описание
Полная реализация турнирной системы: 4 типа турниров (Sprint, Rally, Endurance, Bracket с double elimination),
рейтинговая система Glicko-2, сезонная система, GUI, команды, 4 новых ежедневных задания.

## Изменённые файлы

### CI/CD
- `.github/workflows/build-release.yml` — удалена генерация valid_hashes.txt и hash_summary.md
- `.github/workflows/prerelease.yml` — удалена генерация valid_hashes.txt и hash_summary.md

### Плагин (TimingSystem) — Новые файлы

#### Турнирная система (`tournament/`)
- `tournament/TournamentType.java` — enum: SPRINT, RALLY, ENDURANCE, BRACKET (4 типа турниров)
- `tournament/TournamentState.java` — enum: SCHEDULED, QUALIFYING, ACTIVE, CALCULATING, FINISHED, ARCHIVED, CANCELLED
- `tournament/CarRestriction.java` — enum: ALL, SYSTEM_ONLY, CUSTOM_ONLY
- `tournament/RatingRank.java` — enum: BRONZE, SILVER, GOLD, DIAMOND, CHAMPION (с иконками)
- `tournament/TournamentReward.java` — DTO наград: coins, xp, titleReward
- `tournament/Tournament.java` — основной DTO турнира с JSON сериализацией для trackIds и rewards
- `tournament/TournamentResult.java` — DTO результата игрока с JSON сериализацией для trackTimes
- `tournament/BracketMatch.java` — DTO матча в сетке double elimination
- `tournament/PlayerRating.java` — DTO рейтинга Glicko-2 (rating, deviation, volatility, peak)
- `tournament/Season.java` — DTO сезона
- `tournament/SeasonPoints.java` — DTO очков за сезон (F1-style: 25-18-15-12-10-8-6-4-2-1)
- `tournament/RatingManager.java` — менеджер рейтингов Glicko-2 (CRUD, пересчёт, top, soft reset)
- `tournament/SeasonManager.java` — менеджер сезонов (создание, завершение, очки, автоматическая ротация)
- `tournament/TournamentManager.java` — центральный менеджер (жизненный цикл, scheduler, bracket, результаты)

#### Команды
- `commands/CommandTournament.java` — 12 подкоманд:
  - `/tournament` — открывает GUI
  - `/tournament info` — текстовая информация
  - `/tournament results [type]` — результаты
  - `/tournament top` — топ сезона
  - `/tournament history` — история
  - `/tournament rating` — ELO рейтинг
  - `/tournament season` — информация о сезоне
  - `/tournament bracket` — информация о bracket
  - `/tournament admin create <type> <name> <days> [trackIds]`
  - `/tournament admin cancel <type>`
  - `/tournament admin season start`
  - `/tournament admin bracket generate`
  - `/tournament admin finish <type>`

#### GUI
- `gui/TournamentGui.java` — 6-рядный GUI с турнирами, рейтингом, сезоном

#### БД миграция
- `database/updates/Version24.java` — 6 новых таблиц:
  - `ts_tournaments` — определения турниров
  - `ts_tournament_results` — результаты игроков
  - `ts_bracket_matches` — матчи bracket турнира
  - `ts_player_rating` — Glicko-2 рейтинги
  - `ts_seasons` — определения сезонов
  - `ts_season_points` — сезонные очки

### Плагин (TimingSystem) — Изменённые файлы
- `database/SQLiteDatabase.java` — version 23→24, добавлен Version24 в update chain
- `database/MySQLDatabase.java` — version 23→24, добавлен Version24 в update chain
- `TimingSystem.java` — регистрация CommandTournament, инициализация TournamentManager
- `economy/DailyChallengeManager.java` — 4 новых типа ежедневных заданий:
  - PARTICIPATE_TOURNAMENT (60 coins, 40 XP)
  - TOURNAMENT_TOP_THREE (100 coins, 60 XP)
  - COMPLETE_DIFFERENT_DIFFICULTY (70 coins, 45 XP)
  - WIN_STREAK (90 coins, 55 XP)

### Переводы
- `lang/en_us.yml` — 40+ ключей секции `tournament:`
- `lang/de_de.yml` — немецкие переводы
- `lang/es_es.yml` — испанские переводы
- `lang/fr_fr.yml` — французские переводы
- `lang/id_id.yml` — индонезийские переводы (англ.)
- `lang/nl_nl.yml` — голландские переводы (англ.)
- `lang/pl_pl.yml` — польские переводы (англ.)
- `lang/pt_br.yml` — португальские переводы (англ.)
- `lang/zh_cn.yml` — китайские переводы (англ.)
- `lang/triton.yml` — обёртки Triton для всех турнирных ключей
- `triton/timingsystem.json` — 15 новых записей Triton (8 языков)

### Версионирование
- `IBRealistic/gradle.properties` — realistic_version 1.2.0→1.3.0
- `IBRealistic/versions/1.21/gradle.properties` — realistic_version 1.2.0→1.3.0
- `IBRealistic/versions/1.21.3/gradle.properties` — realistic_version 1.2.0→1.3.0
- `TimingSystem/pom.xml` — version 3.2-1.2.0→3.2-1.3.0

### Документация
- `PLAN.md` — Фаза 19 отмечена как завершённая

## Детальное описание изменений

### 1. Турнирная система
**Файлы:** `tournament/*.java`
**Что сделано:**
- 4 типа турниров: Sprint (1 трек), Rally (3-5 треков), Endurance (кол-во кругов), Bracket (плей-офф)
- Bracket: double elimination, max 64 игрока, автоматический сидинг по квалификации
- Жизненный цикл: SCHEDULED → QUALIFYING (для bracket) → ACTIVE → CALCULATING → FINISHED
- Ограничение: только один турнир каждого типа одновременно
- Автоматическое участие при завершении трека турнира
- Автоматическое начисление наград (coins + XP) по позициям

### 2. Рейтинговая система Glicko-2
**Файл:** `tournament/RatingManager.java`
**Что сделано:**
- Упрощённая реализация Glicko-2 с пересчётом rating, deviation, volatility
- Попарные сравнения участников турнира
- 5 рангов: Bronze (0-999), Silver (1000-1299), Gold (1300-1599), Diamond (1600-1899), Champion (1900+)
- Soft reset при смене сезона: `newRating = (rating - 1000) * 0.5 + 1000`

### 3. Сезонная система
**Файл:** `tournament/SeasonManager.java`
**Что сделано:**
- Сезоны по 90 дней (настраиваемо)
- F1-style очки: 25-18-15-12-10-8-6-4-2-1
- Автоматическая проверка истечения сезона
- Soft reset рейтингов при новом сезоне

### 4. Bracket (double elimination)
**Файлы:** `tournament/BracketMatch.java`, `tournament/TournamentManager.java`
**Что сделано:**
- Max 64 игрока (округляется до степени 2)
- Квалификация → сидинг → upper bracket → lower bracket → финал
- Каждый матч: два игрока проезжают один трек, лучшее время побеждает
- Проигравший в upper bracket идёт в lower bracket
- Проигравший в lower bracket выбывает

### 5. Новые ежедневные задания
**Файл:** `economy/DailyChallengeManager.java`
**Что сделано:**
- PARTICIPATE_TOURNAMENT — участие в турнирной гонке (60 🪙, 40 XP)
- TOURNAMENT_TOP_THREE — финиш в топ-3 турнирного заезда (100 🪙, 60 XP)
- COMPLETE_DIFFERENT_DIFFICULTY — треки разных сложностей (70 🪙, 45 XP)
- WIN_STREAK — 3 трека подряд без фейла (90 🪙, 55 XP)
- Добавлен расширенный метод onTrackComplete с параметрами турнира и сложности

### 6. Удаление хешей из CI/CD
**Файлы:** `.github/workflows/build-release.yml`, `.github/workflows/prerelease.yml`
**Что сделано:**
- Удалена генерация valid_hashes.txt (SHA-256 хеши JAR файлов)
- Удалена генерация hash_summary.md
- Удалены ссылки на эти файлы из артефактов релиза

## Изменения DB версии
- Старая версия: 23
- Новая версия: 24
- Причина: 6 новых таблиц для турнирной системы

## Изменения realistic_version
- Старая версия: 1.2.0
- Новая версия: 1.3.0
- Причина: новая функциональность (турниры, рейтинги, сезоны)

## Тестирование
- [x] Плагин собирается успешно (Maven, BUILD SUCCESS)
- [ ] Протестировано на MC 1.20.4
- [ ] Протестировано на MC 1.21
- [ ] Протестировано на MC 1.21.3
- [x] Мод не затронут (только версия обновлена)

## Проблемы и решения
1. **Конфликт импортов Optional**: `java.util.Optional` конфликтовал с `co.aikar.commands.annotation.Optional`.
   Решение: заменены wildcard-импорты на явные в CommandTournament.java.
2. **Отсутствующий импорт Comparator**: добавлен `java.util.Comparator`.

## Примечание: CODEBASE_INDEX.md
Необходимо обновить CODEBASE_INDEX.md с описанием всех 16 новых файлов турнирной системы.
