# Прогресс рефакторинга: IBRealistic → Аддон к OpenBoatUtils

## Дата начала: 2026-02-14
## Статус: В ПРОЦЕССЕ (ФАЗА 3)

---

## ✅ Завершено

### ФАЗА 1: Анализ дублирующегося кода
- ✅ Идентифицированы все дублирующиеся файлы и поля
- ✅ Составлен список файлов для удаления
- ✅ Составлен список миксинов для модификации

### ФАЗА 2: Архитектурные решения
- ✅ Решено оставить пакет `dev.kanorto.ibrealistic` (без переименования)
- ✅ Создан детальный план из 7 фаз
- ✅ Определены точки интеграции с OBU API

### ФАЗА 4: Добавление зависимости (частично)
- ✅ Обновлён `fabric.mod.json`: добавлен `"openboatutils": ">=0.4.10"`
- ✅ Обновлено описание мода
- ⏸️ build.gradle: требуется добавить OBU в зависимости компиляции

---

## 🔄 В процессе

### ФАЗА 3: Удаление дублирующегося кода

Это самая большая и критическая фаза. Требует аккуратного рефакторинга множества файлов.

#### Шаг 3.1: IBRealistic.java (главный файл)
**Задача:** Удалить OBU-базовые поля (строки 125-245)

**OBU-поля для удаления:**
```java
// Строки 125-145:
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

// Строки 153-195:
public static HashMap<String, Float> vanillaSlipperinessMap;
public static HashMap<String, Float> slipperinessMap;
public static ArrayList<String> collision_filter;
public static boolean interpolationCompat = false;
public static byte collisionResolution = 1;
public enum PerBlockSettingType { ... }
public static HashMap<Integer, HashMap<String, Float>> perBlockSettings;
// + методы getVanillaSlipperinessMap(), settingHasPerBlock(), getPerBlockForBlock()
// + метод getNearbySetting() (огромный, ~50 строк)
```

**IBRealistic-поля для сохранения:**
```java
// Строки 147-151 (уникальные):
public static FourWheelPhysicsEngine fourWheelPhysics = new FourWheelPhysicsEngine();
public static volatile boolean realisticDebugHud = false;

// Визуальные поля (должны быть где-то в коде):
public static float visualRollAngle = 0f;
public static float visualSteeringAngle = 0f;
public static boolean visualHandbrake = false;

// Countdown поля (где-то в коде):
public static long countdownGoTimeMs = 0;
public static int countdownSeconds = 0;
public static boolean countdownActive = false;

// Server info поля (где-то в коде):
public static int serverRealisticVersion = 0;
public static int serverFeatures = 0;
public static String serverName = "";
```

**Места использования OBU-полей:**
- `IBRealistic.airControl` → `OpenBoatUtils.airControl` (1 место: BoatMixin.java:95)
- `IBRealistic.coyoteTimer` → `OpenBoatUtils.coyoteTimer` (1 место: BoatMixin.java:134)
- `IBRealistic.enabled` → `OpenBoatUtils.enabled` (1 место: IBRealistic.java:570)
- `IBRealistic.collision_filter` → `OpenBoatUtils.collision_filter` (3 места: IBRealistic.java)
- `IBRealistic.collisionResolution` → `OpenBoatUtils.collisionResolution` (1 место: IBRealistic.java:571)

**Статус:** ⏸️ НЕ НАЧАТО

---

#### Шаг 3.2: BoatMixin.java (критический файл)
**Задача:** Удалить все дублирующие хуки, оставить только уникальные

**Текущее состояние:** 5 хуков
1. `liftPassenger` — @Inject getPassengerAttachmentPos RETURN (УНИКАЛЬНЫЙ)
2. `realisticPhysicsTick` — @Inject tick HEAD (УНИКАЛЬНЫЙ)
3. `cancelVanillaPaddles` — @Inject updatePaddles HEAD cancel (УНИКАЛЬНЫЙ)
4. `cancelVanillaVelocityDecay` — @Inject updateVelocity HEAD cancel (УНИКАЛЬНЫЙ)
5. `moveHook` — @WrapOperation move (УНИКАЛЬНЫЙ - landing speed preservation)

**ПРИМЕЧАНИЕ:** Хуки 3 и 4 (cancelVanillaPaddles, cancelVanillaVelocityDecay) нужны, чтобы **блокировать OBU/vanilla физику** когда реалистичная физика активна. Это правильный подход!

**Проверить:**
- Есть ли @Shadow полей OBU? Если да — удалить.
- Есть ли другие дублирующие хуки? Если да — удалить.

**Статус:** ⚠️ ТРЕБУЕТСЯ ПРОВЕРКА

---

#### Шаг 3.3: ClientboundPackets.java
**Задача:** Удалить обработку пакетов 0-32 (OBU обрабатывает на своём канале)

**Текущее состояние:**
- Регистрация на канале `ibrealistic:settings`
- Обрабатывает пакеты 0-69

**Целевое состояние:**
- Регистрация только на канале `ibrealistic:settings`
- Обрабатывает только пакеты 33-69 (реалистичные)
- Пакеты 0-32 удалены (OBU обрабатывает на `openboatutils:settings`)

**Статус:** ⏸️ НЕ НАЧАТО

---

#### Шаг 3.4: ClientWorldMixin.java
**Задача:** Упростить до вызова только `resetRealisticState()`

**Текущее состояние:** Вызывает `IBRealistic.resetAll()` или `resetSettings()`

**Целевое состояние:**
- Вызывает только `IBRealistic.resetRealisticState()`
- НЕ вызывает `OpenBoatUtils.resetSettings()` — OBU делает сам

**Статус:** ⏸️ НЕ НАЧАТО

---

#### Шаг 3.5: Удалить дублирующиеся файлы
- [ ] **CollisionMode.java** — удалить полностью, использовать `dev.o7moon.openboatutils.CollisionMode`
- [ ] **ISettingContext.java** — удалить полностью, использовать OBU версию

**Статус:** ⏸️ НЕ НАЧАТО

---

#### Шаг 3.6: SingleplayerCommands.java
**Задача:** Удалить базовые OBU-команды, оставить только реалистичные

**Проверить:** Какие команды дублируют OBU?
- `/ibrealistic gravity` → OBU `/boatutils gravity`
- `/ibrealistic stepheight` → OBU `/boatutils stepheight`
- etc.

**Оставить только:**
- `/ibrealistic vehicletype`
- `/ibrealistic surface`
- `/ibrealistic weather`
- `/ibrealistic debug`
- И другие реалистичные команды

**Статус:** ⏸️ НЕ НАЧАТО

---

#### Шаг 3.7: Modes.java
**Задача:** Упростить, оставить только REALISTIC_* режимы

**Проверить:** Какие режимы дублируют OBU?

**Статус:** ⏸️ НЕ НАЧАТО

---

## ⏳ Ожидает выполнения

### ФАЗА 5: Интеграция с OBU API
- [ ] Добавить import `dev.o7moon.openboatutils.OpenBoatUtils`
- [ ] Заменить все `IBRealistic.XXX` → `OpenBoatUtils.XXX` для OBU-полей
- [ ] Убедиться что читаем OBU-поля корректно

### ФАЗА 6: Тестирование
- [ ] Собрать OBU
- [ ] Собрать IBRealistic с OBU в classpath
- [ ] Тестирование без OBU (должна быть ошибка зависимости)
- [ ] Тестирование с OBU (оба мода работают)
- [ ] Функциональное тестирование

### ФАЗА 7: Документация
- [ ] Обновить README.md
- [ ] Обновить DOCS_REALISTIC_PHYSICS.md
- [ ] Создать CHANGES_addon_refactoring.md

---

## 📊 Статистика

**Прогресс фаз:** 2.5 / 7 (36%)

**Коммиты:**
1. `f9f8db9` — Завершён анализ плана рефакторинга: обновлены документы
2. `6c65b78` — Добавлена зависимость от OpenBoatUtils >= 0.4.10

**Следующий шаг:** Начать Шаг 3.1 — удаление OBU-полей из IBRealistic.java

---

## ⚠️ Важные заметки

1. **Это большой рефакторинг** — потребуется много времени и тестирования
2. **Критические изменения** — неправильная реализация сломает мод
3. **Требуется OBU JAR** для тестирования компиляции
4. **Версия OBU** — используем 0.4.10 (та что в OpenBoatUtils-main/)
5. **Тестировать на всех версиях MC:** 1.20.4, 1.21, 1.21.3

---

## 🎯 Конечная цель

**IBRealistic станет чистым аддоном к OBU:**
- Требует обязательную установку OBU
- Не дублирует код OBU
- Добавляет только уникальную функциональность (реалистичную 4-колёсную физику)
- Корректно работает вместе с OBU без конфликтов
- Сокращение кода с ~15,000 до ~8,000 строк
