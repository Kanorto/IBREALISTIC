# Изменения: Улучшение рендеринга колёс

## Дата
2026-02-12

## Краткое описание
Увеличен размер колёс, расширено расстояние между ними по бокам, добавлена визуальная блокировка задних колёс при ручнике, увеличен подъём камеры для вида от первого лица.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/client/WheelRenderer.java` — увеличен радиус колёс (0.4→0.55), увеличен боковой отступ (0.7→0.9), добавлена блокировка задних колёс при ручнике
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatEntityRendererMixin.java` — увеличен VISUAL_LIFT (0.25→0.45) для более высокой камеры от первого лица, передача состояния ручника в рендерер
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java` — передача состояния ручника в OpenBoatUtils.visualHandbrake
- `src/main/java/dev/o7moon/openboatutils/OpenBoatUtils.java` — добавлено volatile поле visualHandbrake для синхронизации между тик-потоком и рендер-потоком

### Версионирование
- `gradle.properties` — realistic_version 1.0.5 → 1.0.6
- `versions/1.21/gradle.properties` — realistic_version 1.0.5 → 1.0.6
- `versions/1.21.3/gradle.properties` — realistic_version 1.0.5 → 1.0.6
- `TimingSystem/pom.xml` — realistic_version 1.0.5 → 1.0.6

## Детальное описание изменений

### 1. Увеличение размера колёс
**Файл:** `WheelRenderer.java`
**Что сделано:**
- `WHEEL_RADIUS` увеличен с `0.4f` до `0.55f` — колёса теперь визуально больше
- `LATERAL_OFFSET` увеличен с `0.7f` до `0.9f` — колёса расположены шире по бокам лодки

**Причина:** Колёса были слишком маленькими (размером в 1 пиксель) и располагались слишком близко к центру.

### 2. Увеличение высоты камеры
**Файл:** `BoatEntityRendererMixin.java`
**Что сделано:**
- `VISUAL_LIFT` увеличен с `0.25f` до `0.45f` — лодка поднимается выше визуально

**Причина:** Для улучшения вида от первого лица камера должна быть выше.

### 3. Блокировка задних колёс при ручнике
**Файлы:** `WheelRenderer.java`, `BoatEntityRendererMixin.java`, `BoatMixin.java`, `OpenBoatUtils.java`
**Что сделано:**
- Добавлено volatile поле `visualHandbrake` в `OpenBoatUtils.java` для потокобезопасной передачи состояния ручника
- `BoatMixin.java` записывает состояние ручника в `OpenBoatUtils.visualHandbrake`
- `BoatEntityRendererMixin.java` передаёт состояние ручника в `WheelRenderer.renderWheels()`
- `WheelRenderer.renderWheels()` принимает параметр `handbrake` — при включённом ручнике задние колёса не вращаются (spinDeg=0)
- Состояние сбрасывается при отключении мода

**Причина:** При активации ручника задние колёса должны визуально блокироваться.

## Изменения realistic_version
- Старая версия: 1.0.5
- Новая версия: 1.0.6
- Причина: визуальные исправления (PATCH)

## Тестирование
- [x] Мод собирается успешно (Gradle) — все 3 версии MC (1.20.4, 1.21, 1.21.3)
- [ ] Плагин собирается успешно (Maven) — версия обновлена, код плагина не менялся

## Примечание
- Файл CODEBASE_INDEX.md нужно будет обновить при его создании: добавить описание поля `visualHandbrake` и параметра `handbrake` в `WheelRenderer.renderWheels()`
