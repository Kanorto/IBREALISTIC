# Изменения: Фаза 20 — Командные гонки и питстопы

## Дата
2026-02-17

## Краткое описание
Добавлена система полноценных командных гонок с ролями (пилот/механик), 
механикой питстопов (смена шин, заправка, ремонт кузова) через хотбар-предметы,
командным лидербордом и интеграцией с существующей системой повреждений.

## Изменённые файлы

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/team/TeamRole.java` — enum ролей PILOT/MECHANIC
- `src/main/java/me/makkuusen/timing/system/team/TeamMember.java` — DTO участника с ролью
- `src/main/java/me/makkuusen/timing/system/team/PitStopSession.java` — состояние питстопа (шины, топливо, ремонт)
- `src/main/java/me/makkuusen/timing/system/team/PitStopManager.java` — управление питстопами (хотбар, BossBar, таймер)
- `src/main/java/me/makkuusen/timing/system/team/PitStopListener.java` — обработчик кликов механика
- `src/main/java/me/makkuusen/timing/system/team/TeamRaceSession.java` — сессия командной гонки
- `src/main/java/me/makkuusen/timing/system/team/TeamRaceManager.java` — управление командными гонками
- `src/main/java/me/makkuusen/timing/system/database/updates/Version25.java` — миграция БД

#### Изменённые файлы
- `src/main/java/me/makkuusen/timing/system/team/Team.java` — добавлены maxMembers, role tracking, getCreatorUuid()
- `src/main/java/me/makkuusen/timing/system/team/TeamManager.java` — invite/accept/decline, role management, player team lookup
- `src/main/java/me/makkuusen/timing/system/commands/CommandTeam.java` — полная переработка: invite/accept/decline/kick/role/leave/race/results
- `src/main/java/me/makkuusen/timing/system/commands/CommandRace.java` — leave integration для team races
- `src/main/java/me/makkuusen/timing/system/TSListener.java` — handleTeamRaceRegions() для питстопов и кругов
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — PitStopManager init, PitStopListener registration, shutdown
- `src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — version 24→25, Version25 migration
- `src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — version 24→25, Version25 migration
- `src/main/java/me/makkuusen/timing/system/theme/messages/Error.java` — +9 новых enum
- `src/main/java/me/makkuusen/timing/system/theme/messages/Success.java` — +13 новых enum
- `src/main/java/me/makkuusen/timing/system/theme/messages/Info.java` — +18 новых enum
- `src/main/java/me/makkuusen/timing/system/theme/messages/Warning.java` — +5 новых enum
- `src/main/java/me/makkuusen/timing/system/permissions/PermissionTeam.java` — +4 пермишена (INVITE, ROLE, RACE, ADMIN)

#### Переводы
- `src/main/resources/lang/en_us.yml` — +44 новых ключа
- `src/main/resources/lang/de_de.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/id_id.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/zh_cn.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/pl_pl.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/nl_nl.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/es_es.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/pt_br.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/fr_fr.yml` — +44 новых ключа (English fallback)
- `src/main/resources/lang/triton.yml` — +44 Triton wrapper ключа
- `triton/timingsystem.json` — +44 Triton JSON записи (en_GB, ru_RU)

### Мод (IBRealistic)
- `gradle.properties` — realistic_version 1.3.0→1.4.0
- `versions/1.21/gradle.properties` — realistic_version 1.3.0→1.4.0
- `versions/1.21.3/gradle.properties` — realistic_version 1.3.0→1.4.0

## Детальное описание изменений

### 1. Система ролей (TeamRole, TeamMember)
**Файлы:** `TeamRole.java`, `TeamMember.java`, `Team.java`
**Что сделано:**
- Добавлен enum TeamRole: PILOT (пилот — управляет машиной), MECHANIC (механик — обслуживает на питстопе)
- TeamMember хранит UUID, роль и время вступления
- Team.java расширен Map<UUID, TeamMember> для отслеживания ролей
- Добавлены методы: hasPilot(), getPilotUuid(), getMechanicUuids(), isFull(), addMember(), setMemberRole()
- maxMembers по умолчанию = 4 (1 пилот + 3 механика)

### 2. Механика питстопов (PitStopManager, PitStopSession)
**Файлы:** `PitStopManager.java`, `PitStopSession.java`, `PitStopListener.java`
**Что сделано:**
- Хотбар механика с 4 предметами:
  - Slot 0 — 🔧 Tires (IRON_HOE): кликать для смены шин (4 клика)
  - Slot 1 — ⛽ Refuel (BLAZE_ROD): удерживать ПКМ для заправки (5 секунд)
  - Slot 2 — 🔩 Repair (ANVIL): кликать для ремонта кузова (6 кликов)
  - Slot 3 — ✅ Done (LIME_DYE): выпустить пилота
- BossBar прогресса питстопа (красный→жёлтый→зелёный)
- Таймер с предупреждением за 5 секунд до таймаута
- Минимальное время питстопа (8 секунд по умолчанию)
- Штраф за превышение лимита времени
- Интеграция с DamageWearManager (сброс износа шин, охлаждение двигателя, ремонт кузова)
- PersistentDataContainer для идентификации предметов

### 3. Командная гонка (TeamRaceManager, TeamRaceSession)
**Файлы:** `TeamRaceManager.java`, `TeamRaceSession.java`
**Что сделано:**
- Полный жизненный цикл гонки: WAITING→COUNTDOWN→RACING→IN_PIT_STOP→FINISHED/CANCELLED
- Обратный отсчёт с красной/жёлтой подсветкой + GO пакет на клиент
- Отслеживание кругов с обязательными питстопами
- Защита от финиша без выполнения обязательных питстопов
- Сохранение результатов в ts_team_race_results
- Командный лидерборд (топ результатов по треку)
- Награды: монеты и XP с множителями сложности трека
- Уведомления механиков о начале/конце гонки
- Таймаут гонки (15 минут по умолчанию)

### 4. Система приглашений (TeamManager)
**Что сделано:**
- invitePlayer() — отправка приглашения с таймаутом 60 секунд
- acceptInvite() — принятие приглашения с автоматическим назначением роли MECHANIC
- declineInvite() — отклонение приглашения
- Автоматическая очистка просроченных приглашений
- getPlayerTeam() — получение команды игрока

### 5. Команды (CommandTeam)
**Что сделано:**
- `/team create <name>` — создание команды (создатель становится пилотом)
- `/team delete|disband` — удаление команды (только владелец/админ)
- `/team invite <player>` — приглашение в команду
- `/team accept` — принять приглашение
- `/team decline` — отклонить приглашение
- `/team kick <player>` — кик из команды
- `/team role <player> <pilot|mechanic>` — назначение роли
- `/team leave` — покинуть команду
- `/team info [team]` — информация о команде с ролями
- `/team list` — список всех команд с пилотами
- `/team race <track> [laps] [pits]` — запуск командной гонки
- `/team results <track>` — командный лидерборд

### 6. Миграция БД (Version25)
**Что сделано:**
- ALTER TABLE `ts_team_players` ADD `role` TEXT DEFAULT 'MECHANIC'
- ALTER TABLE `ts_teams` ADD `maxMembers` INTEGER DEFAULT 4
- CREATE TABLE `ts_team_race_results` (id, team_id, track_id, pilot_uuid, race/pit/penalty/total time_ms, created_at)

### 7. Конфигурация (config.yml)
Новые настройки (значения по умолчанию):
```yaml
team_race:
  enabled: true
  min_mechanics: 1
  max_mechanics: 3
  rewards:
    coins: 50
    xp: 60
  pitstop:
    tire_clicks: 4
    refuel_time_seconds: 5
    repair_clicks: 6
    min_pitstop_seconds: 8
    timeout_seconds: 30
    penalty_per_extra_second: 2
```

## Изменения VERSION
- Версия БД: 24→25
- Новая таблица: ts_team_race_results
- Новые колонки: ts_team_players.role, ts_teams.maxMembers

## Изменения realistic_version
- Старая версия: 1.3.0
- Новая версия: 1.4.0
- Причина: новая функциональность (командные гонки + питстопы)

## Пермишены
- `timingsystem.team.invite` — приглашение в команду
- `timingsystem.team.role` — назначение ролей
- `timingsystem.team.race` — запуск командной гонки
- `timingsystem.team.admin` — администрирование команд

## Тестирование
- [ ] Протестировано на MC 1.20.4
- [ ] Протестировано на MC 1.21
- [ ] Протестировано на MC 1.21.3
- [x] Плагин собирается успешно (Maven BUILD SUCCESS)

## Заметки
- CODEBASE_INDEX.md нужно обновить при его создании
- Переводы для всех языков добавлены с English fallback
- Русские переводы добавлены в triton/timingsystem.json
