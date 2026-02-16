# Изменения: Фаза 17 — Телеметрия и валидация рейсов

## Дата
2026-02-16

## Краткое описание
Реализована полная система телеметрии: клиентская запись каждого тика физики с округлением до 3 знаков,
локальное сохранение в .ibrt файлы, отправка на сервер чанками, серверная валидация и хранение.

## Изменённые файлы

### Мод (IBRealistic) — НОВЫЕ ФАЙЛЫ

- `telemetry/TelemetryFrame.java` — структура данных одного тика (22 поля: позиция, скорость, руль, slip angles, G-forces)
- `telemetry/TelemetryHeader.java` — метаданные записи (UUID, трек, тип рейса, checksum)
- `telemetry/TelemetryRecorder.java` — запись и финализация, GZIP сжатие, CRC32
- `telemetry/TelemetryFileManager.java` — локальное сохранение .ibrt файлов (magic "IBRT", ротация 500 файлов / 30 дней)
- `telemetry/TelemetrySender.java` — фрагментированная отправка на сервер (пакеты 80-82, чанки по 16KB)

### Мод (IBRealistic) — ИЗМЕНЁННЫЕ ФАЙЛЫ

- `physics/FourWheelPhysicsEngine.java` — добавлены slipAngles[], getSlipAngle(), getYawAngle(), getAxPrev(), getAyPrev()
- `physics/SurfaceProperties.java` — добавлен getSurfaceId() для численной идентификации поверхностей
- `IBRealistic.java` — telemetry state (telemetryRecorder, startTelemetryRecording, stopTelemetryRecording, resetTelemetryState)
- `ClientboundPackets.java` — enum значения пакетов 77-84, обработка ACK (83) и RESULT (84)
- `ServerboundPackets.java` — константы TELEMETRY_START/CHUNK/END (80-82)
- `mixin/BoatMixin.java` — вызов recordTick() каждый тик физики
- `client/IBRealisticClient.java` — авто-старт при GO countdown, авто-стоп при выходе из транспорта
- `SingleplayerCommands.java` — команды /telemetry start|stop|status

### Плагин (TimingSystem) — НОВЫЕ ФАЙЛЫ

- `telemetry/TelemetryReceiver.java` — приём фрагментированных данных (пакеты 80-82), сборка, ACK (83), RESULT (84)
- `telemetry/TelemetryValidator.java` — трёхуровневая валидация (integrity, physical, statistical)
- `telemetry/TelemetryStorage.java` — хранение файлов + метаданных в БД
- `database/updates/Version22.java` — миграция: ts_telemetry_meta таблица + validation_status колонка

### Плагин (TimingSystem) — ИЗМЕНЁННЫЕ ФАЙЛЫ

- `database/SQLiteDatabase.java` — DB version 21 → 22
- `database/MySQLDatabase.java` — DB version 21 → 22

## Новые пакеты

| ID | Направление | Название | Описание |
|----|-------------|----------|----------|
| 80 | C→S | TELEMETRY_START | Начало передачи: totalChunks, totalBytes, totalTicks, trackId, finishTimeMs |
| 81 | C→S | TELEMETRY_CHUNK | Фрагмент данных: chunkIndex, length, data[] |
| 82 | C→S | TELEMETRY_END | Конец передачи: checksum (CRC32) |
| 83 | S→C | TELEMETRY_ACK | Подтверждение: status (0=OK, 1=RETRY, 2=REJECT) |
| 84 | S→C | TELEMETRY_RESULT | Результат валидации: status + reason string |

## Изменения VERSION
- VERSION протокола: **22** (без изменений — телеметрия не требует нового протокола, пакеты используют существующий канал)
- Новые пакеты 80-84 добавлены в существующий диапазон

## Изменения realistic_version
- Текущая: **1.1.0** (без изменений до полного завершения фазы)

## Тестирование
- [x] Мод собирается на MC 1.20.4
- [x] Мод собирается на MC 1.21
- [x] Мод собирается на MC 1.21.3
- [x] Плагин компилируется (Maven)

## Примечание
- Файл CODEBASE_INDEX.md необходимо будет обновить при его создании
