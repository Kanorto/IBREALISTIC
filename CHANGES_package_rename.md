# Изменения: Переименование пакета dev.o7moon.openboatutils → dev.kanorto.ibrealistic

## Дата
2026-02-13

## Краткое описание
Полное переименование Java пакета мода IBRealistic с `dev.o7moon.openboatutils` на `dev.kanorto.ibrealistic`. IBRealistic теперь полностью отдельный аддон с собственным идентификатором пакета.

## Что было сделано

### 1. Смена пакета Java (33 файла)
- **Старый пакет:** `dev.o7moon.openboatutils` (+ .physics, .mixin, .client, .compat)
- **Новый пакет:** `dev.kanorto.ibrealistic` (+ .physics, .mixin, .client)

### 2. Переименование главных классов
- `OpenBoatUtils.java` → `IBRealistic.java` (главный класс мода)
- `OpenBoatUtilsClient.java` → `IBRealisticClient.java` (клиентский entrypoint)

### 3. Обновление конфигурационных файлов
- `fabric.mod.json` — entrypoints изменены на `dev.kanorto.ibrealistic.*`
- `ibrealistic.mixins.json` — `"package": "dev.kanorto.ibrealistic.mixin"`
- `ibrealistic.mixins.json5` — то же самое
- `gradle.properties` (все 3 версии MC) — `maven_group=dev.kanorto`

### 4. Полный список перемещённых файлов

| Старый путь | Новый путь |
|---|---|
| dev/o7moon/openboatutils/OpenBoatUtils.java | dev/kanorto/ibrealistic/IBRealistic.java |
| dev/o7moon/openboatutils/ClientboundPackets.java | dev/kanorto/ibrealistic/ClientboundPackets.java |
| dev/o7moon/openboatutils/ServerboundPackets.java | dev/kanorto/ibrealistic/ServerboundPackets.java |
| dev/o7moon/openboatutils/Modes.java | dev/kanorto/ibrealistic/Modes.java |
| dev/o7moon/openboatutils/CollisionMode.java | dev/kanorto/ibrealistic/CollisionMode.java |
| dev/o7moon/openboatutils/ISettingContext.java | dev/kanorto/ibrealistic/ISettingContext.java |
| dev/o7moon/openboatutils/RealisticFeature.java | dev/kanorto/ibrealistic/RealisticFeature.java |
| dev/o7moon/openboatutils/SingleplayerCommands.java | dev/kanorto/ibrealistic/SingleplayerCommands.java |
| dev/o7moon/openboatutils/SurfaceDebugHelper.java | dev/kanorto/ibrealistic/SurfaceDebugHelper.java |
| dev/o7moon/openboatutils/client/OpenBoatUtilsClient.java | dev/kanorto/ibrealistic/client/IBRealisticClient.java |
| dev/o7moon/openboatutils/client/WheelRenderer.java | dev/kanorto/ibrealistic/client/WheelRenderer.java |
| dev/o7moon/openboatutils/client/SteeringWheelRenderer.java | dev/kanorto/ibrealistic/client/SteeringWheelRenderer.java |
| dev/o7moon/openboatutils/client/RaceCountdownRenderer.java | dev/kanorto/ibrealistic/client/RaceCountdownRenderer.java |
| dev/o7moon/openboatutils/mixin/BoatMixin.java | dev/kanorto/ibrealistic/mixin/BoatMixin.java |
| dev/o7moon/openboatutils/mixin/BoatEntityRendererMixin.java | dev/kanorto/ibrealistic/mixin/BoatEntityRendererMixin.java |
| dev/o7moon/openboatutils/mixin/ClientWorldMixin.java | dev/kanorto/ibrealistic/mixin/ClientWorldMixin.java |
| dev/o7moon/openboatutils/physics/* (18 файлов) | dev/kanorto/ibrealistic/physics/* |

## Что НЕ изменилось
- **Mod ID:** `ibrealistic` (без изменений)
- **Канал пакетов:** `ibrealistic:settings` (без изменений)
- **Имя JAR:** `IBRealistic-*` (без изменений)
- **Логгер:** `IBRealistic` (без изменений)
- **TimingSystem:** не требует изменений (не импортирует пакет мода)

## Тестирование
- [x] Мод собирается на MC 1.20.4
- [x] Мод собирается на MC 1.21
- [x] Мод собирается на MC 1.21.3
- [x] Плагин TimingSystem собирается
- [x] CodeQL: 0 alerts
