# Изменения: Фаза 18 — Ghost Racing / Replay 👻

## Дата
2026-02-17

## Краткое описание
Система ghost racing (призрачных заездов) — игроки видят replay своего личного лучшего времени или конкурентов из лидерборда во время гонки. Данные ghost хранятся только на сервере, отправляются клиенту через фрагментированные пакеты.

## Изменённые файлы

### Мод (IBRealistic)

**Новые файлы:**
- `src/main/java/dev/kanorto/ibrealistic/ghost/GhostFrame.java` — данные одного тика ghost (24 байта: 6 floats)
- `src/main/java/dev/kanorto/ibrealistic/ghost/GhostDisplayMode.java` — enum режимов: OFF, LINE, BOAT, COMPETITION
- `src/main/java/dev/kanorto/ibrealistic/ghost/GhostDataManager.java` — приём, сборка, декомпрессия и воспроизведение ghost
- `src/main/java/dev/kanorto/ibrealistic/client/GhostRenderer.java` — рендеринг ghost (LINE trail, BOAT маркер, HUD overlay)

**Изменённые файлы:**
- `ClientboundPackets.java` — добавлены enum значения GHOST_DATA_START(85), GHOST_DATA_CHUNK(86), GHOST_DATA_END(87), GHOST_REQUEST(88) + обработчики
- `ServerboundPackets.java` — добавлена константа GHOST_REQUEST = 88
- `IBRealistic.java` — VERSION 22→23, ghostState reset, sendGhostRequest()
- `client/IBRealisticClient.java` — ghost lifecycle: авто-запрос при countdown, старт при GO, tick, render, reset

### Плагин (TimingSystem)

**Новые файлы:**
- `src/main/java/.../ghost/GhostFrame.java` — серверный ghost frame (24 байта + writeTo/readFrom)
- `src/main/java/.../ghost/GhostDisplayMode.java` — enum: OFF(0), LINE(1), BOAT(2), COMPETITION(3)
- `src/main/java/.../ghost/GhostManager.java` — сохранение/загрузка ghost файлов, кэш, competition selection
- `src/main/java/.../ghost/GhostSender.java` — фрагментированная отправка ghost клиенту (пакеты 85-87)
- `src/main/java/.../ghost/TelemetryToGhostConverter.java` — конвертация 82-байтных телеметрических фреймов в 24-байтные ghost
- `src/main/java/.../commands/CommandLine.java` — `/line` toggle, mode, count, info
- `src/main/java/.../database/updates/Version23.java` — миграция БД: ghostDisplayMode, ghostCount в ts_players

**Изменённые файлы:**
- `PluginMessageReceiver.java` — маршрутизация пакетов 80-88 к TelemetryReceiver/ghost handlers
- `TimingSystem.java` — регистрация команды `/line`
- `tplayer/Settings.java` — ghostDisplayMode, ghostCount поля + методы
- `gui/SettingsGui.java` — кнопка Ghost Line в GUI настроек
- `telemetry/TelemetryReceiver.java` — handleGhostRequest() обработчик
- `database/SQLiteDatabase.java` — databaseVersion 22→23
- `database/MySQLDatabase.java` — databaseVersion 22→23
- `theme/messages/Success.java` — GHOST_LINE_ON/OFF, GHOST_MODE_SET, GHOST_COUNT_SET
- `theme/messages/Gui.java` — TOGGLE_GHOST_LINE, GHOST_MODE_LINE/BOAT/COMPETITION

### Конфигурация
- `config.yml` — добавлена секция ghost (enabled, max_ghost_ticks, storage_directory, chunk_size_bytes)

### Переводы
- Все 10 языковых файлов (en_us, de_de, es_es, fr_fr, id_id, nl_nl, pl_pl, pt_br, zh_cn, triton.yml)
- `triton/timingsystem.json` — 8 новых записей с переводами для 8 языков

### Версии
- `IBRealistic/gradle.properties` (3 файла) — realistic_version 1.1.0 → 1.2.0
- `TimingSystem/pom.xml` — version 3.2-1.1.0 → 3.2-1.2.0

## Новые пакеты
- GHOST_DATA_START (ID: 85, server → client) — заголовок: ghostIndex, totalChunks, totalBytes, totalTicks, finishTimeMs, displayMode
- GHOST_DATA_CHUNK (ID: 86, server → client) — чанк: ghostIndex, chunkIndex, chunkData[]
- GHOST_DATA_END (ID: 87, server → client) — завершение: ghostIndex
- GHOST_REQUEST (ID: 88, client → server) — запрос: trackId, requestedMode

## Изменения VERSION
- Старая версия протокола: 22
- Новая версия протокола: 23
- Причина: добавлены 4 новых типа пакетов (85-88)

## Изменения realistic_version
- Старая версия: 1.1.0
- Новая версия: 1.2.0
- Причина: новая функциональность Ghost Racing

## Безопасность
- Клиент: лимиты на размер ghost данных (MAX_GHOST_BYTES=2MB, MAX_GHOST_CHUNKS=512, MAX_ACTIVE_TRANSFERS=8)
- Сервер: валидация trackId > 0, UUID.fromString для имён файлов (предотвращает path traversal)
- Сервер: асинхронная загрузка/отправка ghost через Bukkit scheduler

## Тестирование
- [x] Мод собирается успешно (Gradle, все MC версии: 1.20.4, 1.21, 1.21.3)
- [ ] Плагин собирается успешно (Maven)
- [ ] Интеграционное тестирование на сервере

## CODEBASE_INDEX.md
Необходимо обновить при создании:
- Добавить ghost/ пакеты (мод и плагин)
- Добавить GhostRenderer в client/ секцию
- Обновить поток данных Ghost Racing
- Добавить точку расширения "Добавление нового режима отображения ghost"
