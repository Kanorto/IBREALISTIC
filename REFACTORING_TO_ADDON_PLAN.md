# План рефакторинга: IBRealistic → Аддон к OpenBoatUtils

## Дата: 2026-02-14

## Цель
Преобразовать IBRealistic из самостоятельного мода в полноценный **аддон к OpenBoatUtils**, устраняющий дублирование кода и обеспечивающий корректную совместную работу.

---

## ФАЗА 1: Анализ дублирующегося кода ✅

### Дублирующиеся файлы (полностью или частично)
- ✅ `ClientboundPackets.java` — дублирует обработку пакетов 0-32 (OBU-базовые)
- ✅ `CollisionMode.java` — полная копия из OBU
- ✅ `ISettingContext.java` — полная копия из OBU
- ✅ `Modes.java` — расширенная версия OBU (добавлены REALISTIC_* режимы)
- ✅ `ServerboundPackets.java` — дублирует VERSION пакет
- ✅ `SingleplayerCommands.java` — дублирует базовые команды + добавляет реалистичные

### Дублирующиеся миксины
- ✅ `BoatMixin.java` — дублирует 15+ хуков из OBU, добавляет уникальные (liftPassenger, realisticPhysicsTick, moveHook)
- ✅ `ClientWorldMixin.java` — дублирует сброс состояния

### Дублирующиеся поля в IBRealistic.java (строки 125-145)
```java
// Эти поля ПОЛНОСТЬЮ дублируют OpenBoatUtils.java:
public static boolean enabled = false;
public static boolean fallDamage = true;
public static boolean waterElevation = false;
public static boolean airControl = false;
public static float defaultSlipperiness = 0.6f;
public static float jumpForce = 0f;
public static float stepSize = 0f;
public static double gravityForce = -0.03999999910593033;
public static float yawAcceleration = 1.0f;
public static float forwardsAcceleration = 0.04f;
public static float backwardsAcceleration = 0.005f;
public static float turningForwardsAcceleration = 0.005f;
public static boolean allowAccelStacking = false;
public static boolean underwaterControl = false;
public static boolean surfaceWaterControl = false;
public static int coyoteTime = 0;
public static int coyoteTimer = 0;
public static boolean waterJumping = false;
public static float swimForce = 0.0f;
public static CollisionMode collision = CollisionMode.VANILLA;
public static boolean canStepWhileFalling = false;
```

### Уникальные компоненты IBRealistic (сохранить)
- ✅ `IBRealistic.java` (главный класс) — уникальная инициализация
- ✅ `RealisticFeature.java` — feature flags для реалистичной физики
- ✅ `SurfaceDebugHelper.java` — debug утилита для поверхностей
- ✅ `physics/` — **ВСЯ ПАПКА** (FourWheelPhysicsEngine, VehicleType, SurfaceProperties, etc.)
- ✅ `mixin/BoatEntityRendererMixin.java` — визуальные колёса и руль
- ✅ `client/` — рендереры (WheelRenderer, SteeringWheelRenderer, RaceCountdownRenderer)

---

## ФАЗА 2: Архитектурные решения ✅

### Решение 1: Структура пакетов
**Вариант A (рекомендуется):** Переименовать в `dev.o7moon.ibrealistic`
- ✅ Нет конфликта с OBU (`dev.o7moon.openboatutils`)
- ✅ Чёткое разделение: OBU = базовая физика, IBRealistic = реалистичная физика
- ✅ Возможность использовать OBU API напрямую (import `dev.o7moon.openboatutils.OpenBoatUtils`)

**Вариант B:** Остаться на `dev.kanorto.ibrealistic`
- ✅ Уже переименовано, не требует изменений
- ✅ Уникальный package owner (kanorto)
- ⚠️ Требует явного import OBU классов

**ВЫБОР: Вариант B (`dev.kanorto.ibrealistic`)** — уже реализовано, меньше изменений.

### Решение 2: Зависимость от OBU
```json
// fabric.mod.json
"depends": {
  "openboatutils": ">=0.4.10",
  ...
}
```

### Решение 3: Структура IBRealistic.java
Разделить на две части:
1. **Уникальные IBRealistic-поля** (сохранить):
   - `fourWheelPhysics`
   - `visualRollAngle`, `visualSteeringAngle`, `visualHandbrake`
   - `realisticDebugHud`
   - `countdownGoTimeMs`, `countdownSeconds`, `countdownActive`
   - `serverRealisticVersion`, `serverFeatures`, `serverName`
   
2. **OBU-базовые поля** (удалить, читать из OBU):
   - `enabled`, `stepSize`, `gravityForce`, `yawAcceleration`, etc.
   - Доступ через: `dev.o7moon.openboatutils.OpenBoatUtils.enabled`

---

## ФАЗА 3: Удаление дублирующегося кода

### Шаг 3.1: Удалить OBU-поля из IBRealistic.java
- [ ] Удалить строки 125-145 (все OBU-базовые поля)
- [ ] Обновить все ссылки на эти поля → `OpenBoatUtils.XXX`
- [ ] Добавить import: `import dev.o7moon.openboatutils.OpenBoatUtils;`

### Шаг 3.2: Рефакторинг ClientboundPackets.java
- [ ] Удалить обработку пакетов 0-32 (OBU обрабатывает на своём канале)
- [ ] Оставить только пакеты 33-69 на канале `ibrealistic:settings`
- [ ] Удалить регистрацию на канале `openboatutils:settings` (OBU регистрирует)

### Шаг 3.3: Рефакторинг BoatMixin.java
- [ ] Удалить ВСЕ @WrapOperation/@Redirect, дублирующие OBU
- [ ] Удалить @ModifyConstant (критические конфликты)
- [ ] Удалить @Shadow полей OBU (pressingForward, pressingBack, velocityDecay)
- [ ] Оставить ТОЛЬКО уникальные хуки:
  - `liftPassenger` (@Inject getPassengerAttachmentPos RETURN)
  - `realisticPhysicsTick` (@Inject tick HEAD) — вызов FourWheelPhysicsEngine
  - `moveHook` (@WrapOperation move) — landing speed preservation
  
### Шаг 3.4: Рефакторинг ClientWorldMixin.java
- [ ] Оставить только `resetRealisticState()` — сброс IBRealistic-полей
- [ ] НЕ вызывать `OpenBoatUtils.resetSettings()` — OBU делает сам

### Шаг 3.5: Удалить дублирующиеся файлы
- [ ] **CollisionMode.java** — удалить, использовать `dev.o7moon.openboatutils.CollisionMode`
- [ ] **ISettingContext.java** — удалить, использовать OBU версию

### Шаг 3.6: Рефакторинг SingleplayerCommands.java
- [ ] Удалить базовые OBU-команды
- [ ] Оставить только реалистичные команды
- [ ] Использовать `OpenBoatUtils.XXX` для базовых полей

### Шаг 3.7: Рефакторинг Modes.java
- [ ] Удалить базовые OBU режимы (или импортировать из OBU)
- [ ] Оставить только REALISTIC_* режимы

---

## ФАЗА 4: Добавление зависимости от OBU

### Шаг 4.1: fabric.mod.json
```json
{
  "depends": {
    "fabricloader": ">=${loader_version}",
    "fabric": "*",
    "minecraft": "${minecraft_version_allowed_range}",
    "openboatutils": ">=0.4.10"
  }
}
```

### Шаг 4.2: build.gradle
```gradle
repositories {
    // Добавить репозиторий OBU (если нужно)
    maven { url = "https://..." }
}

dependencies {
    // Добавить OBU как зависимость для компиляции
    modImplementation "dev.o7moon:openboatutils:0.4.10"
}
```

---

## ФАЗА 5: Интеграция с OBU API

### Принципы интеграции
1. **Чтение OBU-полей:**
   ```java
   // Было:
   if (IBRealistic.enabled && IBRealistic.airControl) { ... }
   
   // Стало:
   import dev.o7moon.openboatutils.OpenBoatUtils;
   if (OpenBoatUtils.enabled && OpenBoatUtils.airControl) { ... }
   ```

2. **Использование OBU хуков:**
   - IBRealistic НЕ дублирует миксины OBU
   - OBU обрабатывает: stepHeight, collisions, gravity, paddle physics
   - IBRealistic добавляет: реалистичную 4-колёсную физику ПОВЕРХ OBU

3. **Блокировка ванильной физики:**
   ```java
   // В BoatMixin при enabled реалистичной физики:
   @Inject(method = "updatePaddles", at = @At("HEAD"), cancellable = true)
   private void cancelVanillaPaddles(CallbackInfo ci) {
       if (fourWheelPhysics.isEnabled()) {
           ci.cancel(); // Блокируем OBU/vanilla W/A/S/D
       }
   }
   ```

---

## ФАЗА 6: Тестирование

### Шаг 6.1: Сборка с OBU
- [ ] Добавить OBU JAR в `mods/` для тестирования
- [ ] Собрать IBRealistic: `./gradlew chiseledBuild`
- [ ] Проверить отсутствие ошибок компиляции

### Шаг 6.2: Проверка зависимости
- [ ] Запустить без OBU → должна быть ошибка "Missing required mod: openboatutils"
- [ ] Запустить с OBU → оба мода загружаются

### Шаг 6.3: Функциональное тестирование
- [ ] Базовая физика работает (OBU)
- [ ] Реалистичная физика работает (IBRealistic)
- [ ] Переключение между режимами работает
- [ ] Нет конфликтов миксинов

---

## ФАЗА 7: Обновление документации

### Файлы для обновления
- [ ] `README.md` — добавить требование установки OBU
- [ ] `DOCS_REALISTIC_PHYSICS.md` — обновить архитектуру
- [ ] `PLAN.md` — отметить рефакторинг как завершённый
- [ ] Создать `CHANGES_addon_refactoring.md`

### Содержание README.md
```markdown
## Requirements

- Minecraft 1.20.4, 1.21, or 1.21.3
- Fabric Loader
- Fabric API
- **OpenBoatUtils >= 0.4.10** (required dependency)

## Installation

1. Download and install [OpenBoatUtils](https://github.com/o7-Fire/OpenBoatUtils)
2. Download IBRealistic
3. Place both JARs in your `mods/` folder
```

---

## Статус выполнения

- [x] ФАЗА 1: Анализ дублирующегося кода
- [x] ФАЗА 2: Архитектурные решения
- [ ] ФАЗА 3: Удаление дублирующегося кода (в процессе)
- [ ] ФАЗА 4: Добавление зависимости от OBU
- [ ] ФАЗА 5: Интеграция с OBU API
- [ ] ФАЗА 6: Тестирование
- [ ] ФАЗА 7: Обновление документации

---

## Ожидаемые результаты

**До рефакторинга:**
- IBRealistic: ~15,000 строк кода (включая дубликаты OBU)
- BoatMixin: 23 хука (15 дублируют OBU)
- Работает автономно, НО конфликтует с OBU

**После рефакторинга:**
- IBRealistic: ~8,000 строк кода (только уникальная функциональность)
- BoatMixin: 3 хука (все уникальные)
- Работает ТОЛЬКО с OBU, нет конфликтов
- Чистая архитектура: OBU = база, IBRealistic = расширение
