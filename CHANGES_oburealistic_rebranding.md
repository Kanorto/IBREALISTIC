# Изменения: Ребрендинг OBURealistic

## Дата
2026-02-13

## Краткое описание
Полный ребрендинг проекта: переименование канала пакетов, mod ID, названия мода, JAR-файлов, и всех текстовых упоминаний с `openboatutils` / `IBREALISTIC` на `oburealistic` / `OBURealistic`.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/OpenBoatUtils.java` — канал `oburealistic:settings`, логгер `OBURealistic`, mod container lookup `oburealistic`
- `src/main/java/dev/o7moon/openboatutils/ClientboundPackets.java` — лог-сообщение `oburealistic packet`
- `src/main/java/dev/o7moon/openboatutils/ServerboundPackets.java` — лог-сообщение `oburealistic packet`
- `src/main/resources/fabric.mod.json` — mod ID `oburealistic`, name `OBURealistic`, icon path, mixins reference
- `src/main/resources/oburealistic.mixins.json` — переименован из `openboatutils.mixins.json`
- `src/main/resources/oburealistic.mixins.json5` — переименован из `openboatutils.mixins.json5`
- `src/main/resources/assets/oburealistic/` — переименована директория из `assets/openboatutils/`
- `gradle.properties` — `archives_base_name=OBURealistic`, комментарий версионирования
- `versions/1.21/gradle.properties` — аналогично
- `versions/1.21.3/gradle.properties` — аналогично

### Плагин (TimingSystem)
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — канал `oburealistic:settings`
- `src/main/java/me/makkuusen/timing/system/PluginMessageReceiver.java` — канал `oburealistic:settings`
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsManager.java` — канал, URL, тексты предупреждений
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — канал, URL
- `src/main/java/me/makkuusen/timing/system/boatutils/NocolManager.java` — канал
- `src/main/java/me/makkuusen/timing/system/drs/DrsManager.java` — канал
- `src/main/java/me/makkuusen/timing/system/track/TrackWeather.java` — комментарий
- `pom.xml` — комментарий версионирования

### Переводы
- `src/main/resources/lang/en_us.yml` — OpenBoatUtils → OBURealistic в сообщениях
- `src/main/resources/lang/de_de.yml` — аналогично
- `src/main/resources/lang/zh_cn.yml` — аналогично
- `src/main/resources/lang/id_id.yml` — аналогично
- `triton/timingsystem.json` — все 8 языков (en_GB, ru_RU, de_DE, pl_PL, nl_NL, es_ES, pt_BR, fr_FR)

### CI/CD
- `.github/workflows/build-release.yml` — артефакты `oburealistic-mod`, название релиза `OBURealistic`
- `.github/workflows/prerelease.yml` — название пререлиза `OBURealistic`

### Документация
- `README.md` — IBREALISTIC → OBURealistic
- `OpenBoatUtilsRealistic/README.md` — OpenBoatUtils → OBURealistic
- Все CHANGES_*.md, PLAN.md, PLAYER_GUIDE.md, CHANGELOG.md, CODEBASE_INDEX.md
- Codebase docs (codebase/*.md)
- `.github/copilot-instructions.md`

## Детальное описание изменений

### 1. Канал пакетов
**Что сделано:** Канал Minecraft Plugin Messaging изменён с `openboatutils:settings` на `oburealistic:settings`.
**Затронуто:** Мод (Identifier.of) и плагин (registerIncomingPluginChannel, registerOutgoingPluginChannel, все sendPluginMessage).
**Причина:** Ребрендинг проекта на собственное пространство имён.

### 2. Mod ID и метаданные
**Что сделано:** Mod ID в fabric.mod.json изменён с `openboatutils` на `oburealistic`.
**Затронуто:** ID мода, путь к иконке, ссылка на mixins, имя мода, имя JAR.
**Причина:** Мод должен иметь уникальный ID, отличный от оригинального OpenBoatUtils.

### 3. JAR-файлы
**Что сделано:** `archives_base_name` изменён с `OpenBoatUtils` на `OBURealistic`.
**Результат:** JAR файлы теперь называются `OBURealistic-0.4.10-1.0.6_*.jar`.

### 4. URL скачивания
**Что сделано:** Все URL скачивания обновлены с modrinth.com/mod/openboatutils на github.com/Kanorto/OBURealistic/releases/latest.

### 5. Тексты предупреждений
**Что сделано:** Все пользовательские сообщения обновлены: "OpenBoatUtils" → "OBURealistic".

## Что НЕ изменено
- Java пакеты (`dev.o7moon.openboatutils.*`) — слишком инвазивное изменение, затронуло бы десятки файлов
- Имена Java классов (`OpenBoatUtils.java`, `OpenBoatUtilsClient.java`) — зависят от пакета
- Упоминания оригинального upstream проекта OpenBoatUtils (исторические ссылки)

## Тестирование
- [x] Мод собирается успешно (Gradle) — все 3 версии MC (1.20.4, 1.21, 1.21.3)
- [x] Плагин собирается успешно (Maven)
- [x] JAR файлы именуются корректно (OBURealistic-*.jar)

## Примечание о CODEBASE_INDEX.md
CODEBASE_INDEX.md обновлён в части текстовых упоминаний бренда. При появлении нового содержимого следует также отразить изменение mod ID и канала.
