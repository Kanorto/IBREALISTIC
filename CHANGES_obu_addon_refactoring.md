# Изменения: Рефакторинг IBRealistic в аддон OpenBoatUtils

## Дата
2026-02-14

## Краткое описание
IBRealistic переведён из самостоятельного мода в аддон к OpenBoatUtils (OBU). 
Базовые пакеты (0-32) теперь обрабатываются оригинальным OBU на канале `openboatutils:settings`,
а IBRealistic обрабатывает только реалистичные пакеты (33-69) на канале `ibrealistic:settings`.
Это обеспечивает совместимость с обычными OBU-серверами.

## Изменённые файлы

### Мод (IBRealistic)
- `build.gradle` — добавлена зависимость `modCompileOnly` на OBU JAR
- `src/main/resources/fabric.mod.json` — добавлена зависимость `"openboatutils": ">=0.4.10"`
- `src/main/java/dev/kanorto/ibrealistic/IBRealistic.java` — удалены все дублированные OBU поля и методы
- `src/main/java/dev/kanorto/ibrealistic/ClientboundPackets.java` — удалена обработка пакетов 1-32
- `src/main/java/dev/kanorto/ibrealistic/Modes.java` — все OBU-вызовы делегированы в OpenBoatUtils
- `src/main/java/dev/kanorto/ibrealistic/SingleplayerCommands.java` — двухканальная маршрутизация
- `src/main/java/dev/kanorto/ibrealistic/mixin/BoatMixin.java` — ссылки на OpenBoatUtils
- `src/main/java/dev/kanorto/ibrealistic/CollisionMode.java` — **УДАЛЁН** (используется OBU версия)
- `src/main/java/dev/kanorto/ibrealistic/ISettingContext.java` — **УДАЛЁН** (неиспользуемый)

### CI/CD
- `.github/workflows/build-release.yml` — добавлен шаг сборки OBU перед IBRealistic
- `.github/workflows/prerelease.yml` — добавлен шаг сборки OBU перед IBRealistic

## Детальное описание изменений

### 1. Добавление OBU как зависимости
**Файл:** `build.gradle`
**Что сделано:**
- Добавлена `modCompileOnly` зависимость на OBU JAR из `libs/` директории
- JAR файлы выбираются по версии MC с помощью `mc_suffix` из gradle.properties
- Формат: `libs/OpenBoatUtils-{obu_version}_{mc_suffix}.jar`

**Файл:** `fabric.mod.json`
**Что сделано:**
- Добавлено `"openboatutils": ">=0.4.10"` в секцию `depends`
- OBU теперь является обязательным модом для работы IBRealistic

### 2. Рефакторинг IBRealistic.java
**Файл:** `src/main/java/dev/kanorto/ibrealistic/IBRealistic.java`
**Что сделано:**
- Удалены ~25 дублированных OBU полей (enabled, stepSize, fallDamage, gravity и т.д.)
- Удалены ~30 дублированных OBU методов (setStepSize, setFallDamage, и т.д.)
- Удалены все OBU-утилитарные методы (getBlockSlipperiness, getSlipperinessMap и т.д.)
- Удалён enum PerBlockSettingType (используется OpenBoatUtils.PerBlockSettingType)
- Добавлен import `dev.o7moon.openboatutils.OpenBoatUtils`
- `resetAll()` теперь вызывает `OpenBoatUtils.resetAll()` + `resetRealisticState()`
- `resetSettings()` теперь вызывает `OpenBoatUtils.resetSettings()` + `resetRealisticState()`
- Все реалистичные setter-методы теперь устанавливают `OpenBoatUtils.enabled = true`

**Причина:**
Поля и методы были точной копией из OBU. Теперь OBU сам управляет своим состоянием.

### 3. Рефакторинг ClientboundPackets.java
**Файл:** `src/main/java/dev/kanorto/ibrealistic/ClientboundPackets.java`
**Что сделано:**
- Удалена обработка пакетов 1-32 (OBU обрабатывает на openboatutils:settings)
- Оставлен case 0 (RESET) — сбрасывает только IBRealistic-состояние
- Оставлены cases 33-69 (IBRealistic-специфичные пакеты)
- Enum значения 0-32 сохранены для совместимости ordinal=packetID

**Причина:**
Пакеты 0-32 — это стандартные OBU пакеты, которые OBU уже обрабатывает самостоятельно.

### 4. Рефакторинг Modes.java
**Файл:** `src/main/java/dev/kanorto/ibrealistic/Modes.java`
**Что сделано:**
- Все `IBRealistic.setXxx()` для OBU-настроек заменены на `OpenBoatUtils.setXxx()`
- `IBRealistic.setBlockSlipperiness()` заменён на `OpenBoatUtils.setBlocksSlipperiness(List.of())`
- `IBRealistic.breakSlimePlease()` → `OpenBoatUtils.breakSlimePlease()`
- `IBRealistic.setCollisionMode()` → `OpenBoatUtils.setCollisionMode()`
- `IBRealistic.setVehicleType()` — остался как есть (IBRealistic-специфичный)

### 5. Рефакторинг SingleplayerCommands.java
**Файл:** `src/main/java/dev/kanorto/ibrealistic/SingleplayerCommands.java`
**Что сделано:**
- OBU-команды (пакеты 0-32): отправляются через `OpenBoatUtils.sendPacketS2C()`
- IBRealistic-команды (пакеты 33+): отправляются через `IBRealistic.sendPacketS2C()`
- Команда "reset": отправляет RESET на оба канала (OBU + IBRealistic)
- `IBRealistic.PerBlockSettingType` → `OpenBoatUtils.PerBlockSettingType`
- Добавлен import `dev.o7moon.openboatutils.CollisionMode`

### 6. Обновление BoatMixin.java
**Файл:** `src/main/java/dev/kanorto/ibrealistic/mixin/BoatMixin.java`
**Что сделано:**
- `IBRealistic.airControl` → `OpenBoatUtils.airControl`
- `IBRealistic.coyoteTimer` → `OpenBoatUtils.coyoteTimer`
- Добавлен import `dev.o7moon.openboatutils.OpenBoatUtils`

### 7. Обновление CI/CD
**Файлы:** `.github/workflows/build-release.yml`, `.github/workflows/prerelease.yml`
**Что сделано:**
- Добавлен шаг "Build OpenBoatUtils dependency" перед сборкой IBRealistic
- Добавлен шаг "Copy OBU JARs to IBRealistic libs" для копирования JAR файлов

## Архитектура после рефакторинга

### Поток данных (мультиплеер)
```
TimingSystem (плагин)
  ├── Пакеты 0-32 → openboatutils:settings → OBU мод → OBU настройки
  └── Пакеты 33-69 → ibrealistic:settings → IBRealistic мод → Реалистичная физика
```

### Поток данных (одиночная игра)
```
SingleplayerCommands
  ├── OBU-команды → OpenBoatUtils.sendPacketS2C → openboatutils:settings → OBU обработчик
  └── IBRealistic-команды → IBRealistic.sendPacketS2C → ibrealistic:settings → IBRealistic обработчик
  └── RESET → оба канала
```

### Совместимость
- На обычном OBU-сервере: OBU работает нормально, IBRealistic молчит
- На IBRealistic-сервере: оба мода работают, каждый на своём канале

## Удалённые файлы
- `CollisionMode.java` — заменён на `dev.o7moon.openboatutils.CollisionMode`
- `ISettingContext.java` — неиспользуемый интерфейс, ссылавшийся на удалённые типы

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4
- [x] Мод собирается успешно на MC 1.21
- [x] Мод собирается успешно на MC 1.21.3
- [ ] Протестировано в игре на MC 1.20.4
- [ ] Протестировано в игре на MC 1.21
- [ ] Протестировано в игре на MC 1.21.3

## Примечания
- OBU JAR файлы в `libs/` не коммитятся (исключены через `.gitignore` правило `*.jar`)
- CI автоматически собирает OBU перед IBRealistic
- Для локальной разработки нужно сначала собрать OBU: `cd OpenBoatUtils-main && ./gradlew chiseledBuild`, затем скопировать JAR файлы в `IBRealistic/libs/`
- CODEBASE_INDEX.md нужно будет обновить после создания
