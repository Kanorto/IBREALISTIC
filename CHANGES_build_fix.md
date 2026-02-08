# Изменения: Исправление ошибок сборки мода

## Дата
2026-02-08

## Краткое описание
Объединение BoatMixin и AbstractBoatMixin в единый файл с Stonecutter predicates, исправление всех warnings и ошибок компиляции при сборке мода для всех версий Minecraft (1.20.4, 1.21, 1.21.3).

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java` — полностью переписан с использованием Stonecutter version predicates для поддержки всех версий MC в одном файле
- `src/main/java/dev/o7moon/openboatutils/mixin/ServerPlayNetworkHandlerMixin.java` — добавлен `remap = false` для @Redirect на Logger.warn()
- `src/main/resources/openboatutils.mixins.json5` — упрощён (убраны version predicates, так как BoatMixin теперь единый)

### Удалённые файлы
- `versions/1.21.3/src/main/java/dev/o7moon/openboatutils/mixin/AbstractBoatMixin.java` — больше не нужен, функционал перенесён в BoatMixin.java

## Детальное описание изменений

### 1. Объединение BoatMixin и AbstractBoatMixin
**Файл:** `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java`
**Что сделано:**
- Заменены все прямые ссылки на `BoatEntity` на Stonecutter version predicates:
  - `//? <=1.21 {` блоки для версий 1.20.4 и 1.21 (используют `BoatEntity`)
  - `//? >=1.21.3 {` блоки для версии 1.21.3 (используют `AbstractBoatEntity`)
- Объединены все version-specific части:
  - `@Mixin` target: `BoatEntity.class` vs `AbstractBoatEntity.class`
  - `@Shadow` annotations: типы Location, методы checkLocation, getUnderWaterLocation
  - `@Redirect` annotations: строки target с правильным путём класса
  - Метод `getPaddleSoundEvent` (<=1.21) vs `getPaddleSound` (>=1.21.3)
  - `interpolationStepsHook` — только для >=1.21.3
  - `updateVelocityHook` — только для <=1.20.4
  - `getGravity` injection — для >=1.21

**Причина:**
Ранее AbstractBoatMixin.java был отдельным файлом только для 1.21.3. При компиляции для 1.21.3 BoatMixin.java тоже компилировался и генерировал 26+ warnings от mixin annotation processor, так как `@Shadow`/`@Redirect`/`@Inject` не могли найти целевые методы в `BoatEntity` (они переехали в `AbstractBoatEntity`). Объединение в один файл с predicates решает проблему полностью.

### 2. Исправление ServerPlayNetworkHandlerMixin warning
**Файл:** `src/main/java/dev/o7moon/openboatutils/mixin/ServerPlayNetworkHandlerMixin.java`
**Строка:** 26
**Что сделано:**
- Добавлен `remap = false` к `@At` annotation в `@Redirect` на `Logger.warn()`

**Причина:**
Mixin annotation processor пытался найти маппинг для `org.slf4j.Logger.warn()` через yarn mappings, но SLF4J — это библиотечный класс, не Minecraft класс. `remap = false` указывает что ремаппинг для этого target не требуется.

### 3. Упрощение mixin JSON5
**Файл:** `src/main/resources/openboatutils.mixins.json5`
**Что сделано:**
- Убраны Stonecutter predicates `//? <=1.21 {` и `//? >=1.21.3 {`
- `BoatMixin` теперь всегда включён в client mixins для всех версий
- `AbstractBoatMixin` полностью удалён из конфигурации

**Причина:**
Так как BoatMixin теперь сам обрабатывает все версии через Stonecutter predicates, отдельный AbstractBoatMixin не нужен.

## Тестирование
- [x] Мод собирается успешно (Gradle chiseledBuild)
- [x] 0 errors при сборке
- [x] 0 warnings при сборке
- [x] JAR файл для MC 1.20.4 создан
- [x] JAR файл для MC 1.21 создан
- [x] JAR файл для MC 1.21.3 создан
- [x] Mixin JSON в каждом JAR содержит правильную конфигурацию
- [x] Stonecutter predicates корректно обработаны для всех 3 версий

## Примечание
- Файл `CODEBASE_INDEX.md` нужно будет обновить при его создании:
  - Удалить запись об `AbstractBoatMixin.java`
  - Обновить описание `BoatMixin.java` — теперь он единый для всех версий MC
