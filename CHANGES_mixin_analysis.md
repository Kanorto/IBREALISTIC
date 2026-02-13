# Анализ отличий миксинов: IBRealistic vs OpenBoatUtils-main

## Дата
2026-02-13

## Краткое описание
Полный анализ отличий миксинов IBRealistic от оригинального OpenBoatUtils (OBU) для определения, какие части можно удалить из IBRealistic, чтобы сделать его аддоном к OBU без конфликтов.

---

## Причина краш-лога

**Ошибка:**
```
Critical injection failure: Constant modifier method forwardsAccel(F)F 
in openboatutils.mixins.json:AbstractBoatMixin from mod openboatutils 
failed injection check, (0/1) succeeded. Scanned 0 target(s).
```

**Причина:** На MC 1.21.4 оригинальный OBU использует `@ModifyConstant` на метод `updatePaddles` в `AbstractBoatMixin.java` (в файле `versions/1.21.3/`). Mojang изменили метод `updatePaddles` в 1.21.4, и литерал `0.04f` больше не найден. Это краш самого OBU — **не IBRealistic**.

Однако **IBRealistic тоже** использует `@ModifyConstant` в тех же точках, что привело бы к двойному конфликту если бы оба мода были установлены.

**Вывод:** MC 1.21.4 не поддерживается ни OBU, ни IBRealistic (поддерживаются 1.20.4, 1.21, 1.21.3). Но задача — сделать IBRealistic совместимым с OBU при одновременной установке.

---

## Сравнение файлов миксинов

### 1. Конфигурация миксинов

| Файл | OBU | IBRealistic | Конфликт? |
|------|-----|-------------|-----------|
| `openboatutils.mixins.json` / `ibrealistic.mixins.json` | BoatMixin, ClientWorldMixin, EntityMixin, ServerPlayNetworkHandlerMixin | BoatMixin, **BoatEntityRendererMixin**, ClientWorldMixin, EntityMixin, ServerPlayNetworkHandlerMixin | ДА — одинаковые миксины на одни классы |
| Версионный `AbstractBoatMixin` (>=1.21.3) | Отдельный файл `versions/1.21.3/.../AbstractBoatMixin.java` | Нет — всё в основном `BoatMixin.java` через Stonecutter | Конфликт через Stonecutter |

**Ключевое отличие:** OBU для MC 1.21.3+ использует ОТДЕЛЬНЫЙ класс `AbstractBoatMixin.java`, а IBRealistic использует ЕДИНЫЙ `BoatMixin.java` с Stonecutter conditionals для переключения между `BoatEntity` (<=1.21) и `AbstractBoatEntity` (>=1.21.3).

### 2. BoatMixin.java — Детальное сравнение

#### 2.1. Целевой класс (@Mixin target)

| | OBU | IBRealistic |
|---|-----|-------------|
| MC <=1.21 | `@Mixin(BoatEntity.class)` | `@Mixin(BoatEntity.class)` |
| MC >=1.21.3 | `@Mixin(AbstractBoatEntity.class)` (отдельный файл) | `@Mixin(AbstractBoatEntity.class)` (Stonecutter в том же файле) |

**Конфликт:** Оба мода инжектятся в один и тот же класс. При одновременной установке будут два миксина на одном классе.

#### 2.2. Типы инъекций

| Точка инъекции | OBU | IBRealistic | Конфликт? |
|---|---|---|---|
| `checkLocation()` в `getPaddleSoundEvent` | `@Redirect` | `@WrapOperation` | **ДА** — но `@WrapOperation` от MixinExtras обёртывает `@Redirect`, они могут конфликтовать |
| `checkLocation()` в `tick()` | `@Redirect` | `@WrapOperation` | **ДА** |
| `Block.getSlipperiness()` в `getNearbySlipperiness` | `@Redirect` | `@WrapOperation` | **ДА** |
| `yawVelocity` PUTFIELD в `updatePaddles` | `@Redirect` | `@WrapOperation` | **ДА** |
| `pressingForward` GETFIELD в `updatePaddles` | `@Redirect` | `@WrapOperation` | **ДА** |
| `pressingBack` GETFIELD в `updatePaddles` | `@Redirect` | `@WrapOperation` | **ДА** |
| `velocityDecay` PUTFIELD ordinal 1 в `updateVelocity` | `@Redirect` | `@WrapOperation` | **ДА** |
| `velocityDecay` PUTFIELD ordinal 2 в `updateVelocity` | `@Redirect` | `@WrapOperation` | **ДА** |
| `velocityDecay` PUTFIELD ordinal 3 в `updateVelocity` | `@Redirect` | `@WrapOperation` | **ДА** |
| `move()` INVOKE в `tick()` | `@Redirect` | `@WrapOperation` | **ДА** |
| `@ModifyConstant` forwardsAccel (0.04f) | ✅ | ✅ | **ДА — КРИТИЧЕСКИЙ!** Два `@ModifyConstant` на одну константу |
| `@ModifyConstant` turnAccel (0.005f ordinal 0) | ✅ | ✅ | **ДА — КРИТИЧЕСКИЙ!** |
| `@ModifyConstant` backwardsAccel (0.005f ordinal 1) | ✅ | ✅ | **ДА — КРИТИЧЕСКИЙ!** |
| `collidesWith` HEAD | `@Inject` | `@Inject` | ⚠️ — Два `@Inject` на HEAD могут работать, но логика дублируется |
| `fall` HEAD | `@Inject` | `@Inject` | ⚠️ — Дублируется |
| `getGravity` HEAD (>=1.21) | `@Inject` | `@Inject` | ⚠️ — Дублируется |
| `updateVelocity` STORE ordinal 1 (<=1.20.4) | `@ModifyVariable` | `@ModifyVariable` | **ДА — КРИТИЧЕСКИЙ!** |
| `getPassengerAttachmentPos` | ❌ | ✅ (IBRealistic only) | Нет конфликта |

#### 2.3. Уникальные для IBRealistic хуки

| Хук | Описание |
|---|---|
| Реалистичная физика в `oncePerTick()` | Вызов `FourWheelPhysicsEngine`, handbrake, wheel rendering |
| Passenger visual lift | `getPassengerAttachmentPos` — поднятие позиции пассажира |
| Debug HUD | Отображение физических параметров |
| Landing speed preservation | В `moveHook` — сохранение скорости при приземлении |
| Surface names map | `SURFACE_NAMES` IdentityHashMap для отладки |

#### 2.4. Поведенческие отличия

| Поведение | OBU | IBRealistic |
|---|---|---|
| Прыжок (spacebar) при реалистичной физике | Всегда прыгает | Spacebar = handbrake, прыжок отключён |
| `@Redirect` vs `@WrapOperation` | `@Redirect` (не даёт другим модам вмешаться) | `@WrapOperation` (позволяет цепочку модов) |
| Когда `!enabled`, original.call() | N/A (прямое обращение к полю) | Вызывает `original.call()` для пропуска через OBU |
| `velocityDecayHook` при `!enabled` | `velocityDecay = orig` (прямое присвоение) | `original.call(boat, orig)` (делегирует OBU) |
| Landing speed preservation | ❌ | ✅ — восстанавливает скорость при приземлении |
| Move hook при `!enabled` | `instance.move()` | `original.call()` (делегирует OBU) |

### 3. ClientWorldMixin.java

**ИДЕНТИЧНЫ** — оба вызывают `OpenBoatUtils.resetSettings()` в `@Inject` на `<init>`.

**Конфликт:** ДА — два `@Inject` на одну точку, каждый вызовет `resetSettings()` своего класса `OpenBoatUtils`. Так как оба мода имеют свой `OpenBoatUtils` класс в пакете `dev.o7moon.openboatutils`, это приведёт к конфликту пакетов/классов.

### 4. EntityMixin.java

**ИДЕНТИЧНЫ** — оба обрабатывают `getStepHeight` и `adjustMovementForCollisions`.

**Конфликт:** ДА — дублирование инъекций.

### 5. ServerPlayNetworkHandlerMixin.java

**Почти идентичны.** Единственное отличие:

| | OBU | IBRealistic |
|---|---|---|
| `preventMovedWronglyLog` | `@Redirect` | `@WrapOperation` |

**Конфликт:** ДА — `@Redirect` от OBU и `@WrapOperation` от IBRealistic на одну точку.

### 6. BoatEntityRendererMixin.java

**Уникален для IBRealistic** — OBU не имеет этого миксина.

**Конфликт:** Нет — это чисто IBRealistic функциональность (визуальные колёса, руль, крен).

---

## Критическая проблема: Одинаковый пакет Java

**ОБА мода** используют пакет `dev.o7moon.openboatutils` для основных классов и `dev.o7moon.openboatutils.mixin` для миксинов.

Это означает:
- Оба мода определяют класс `dev.o7moon.openboatutils.OpenBoatUtils`
- Оба мода определяют класс `dev.o7moon.openboatutils.mixin.BoatMixin`
- И т.д.

**Java не может загрузить два класса с одинаковым полным именем из разных JAR-файлов!** Один перезапишет другой.

---

## Рекомендации по превращению IBRealistic в аддон

### Вариант A: Минимальные изменения (рекомендуемый)

1. **Сменить пакет** IBRealistic с `dev.o7moon.openboatutils` на `dev.o7moon.ibrealistic` (или подобный)
   - Это устранит конфликт одинаковых классов
   - IBRealistic будет импортировать и использовать классы OBU

2. **Удалить из IBRealistic дублирующие миксины:**
   - ❌ Убрать `ClientWorldMixin.java` → OBU уже сбрасывает настройки
   - ❌ Убрать `EntityMixin.java` → OBU уже обрабатывает stepHeight
   - ❌ Убрать `ServerPlayNetworkHandlerMixin.java` → OBU уже отключает anti-cheat
   
3. **Перенести BoatMixin** → оставить только УНИКАЛЬНЫЕ хуки IBRealistic:
   - ✅ Реалистичная физика в `oncePerTick()` (дополнение к OBU)
   - ✅ Passenger visual lift
   - ✅ Landing speed preservation (дополнение к moveHook)
   - ❌ Убрать все `@WrapOperation` / `@ModifyConstant` которые дублируют OBU

4. **Оставить BoatEntityRendererMixin** — уникален для IBRealistic

### Вариант B: Использование MixinSquared (сложнее)

1. Добавить MixinSquared как зависимость
2. Использовать `MixinCanceller` для отмены OBU миксинов когда IBRealistic активен
3. IBRealistic полностью заменяет OBU при активации

**Недостаток:** Сложнее, требует дополнительную зависимость, и OBU не будет работать когда IBRealistic активен (а нужно чтобы оба работали параллельно).

### Вариант C: IBRealistic зависит от OBU (идеальный аддон)

1. **IBRealistic объявляет OBU как зависимость** в `fabric.mod.json`
2. **IBRealistic использует свой пакет** (`dev.o7moon.ibrealistic`)
3. **IBRealistic читает состояние OBU** через его публичные поля (`OpenBoatUtils.enabled`, etc.)
4. **IBRealistic добавляет только свои миксины:**
   - `BoatMixin` с хуками ТОЛЬКО для реалистичной физики
   - `BoatEntityRendererMixin` для визуалки
5. **IBRealistic слушает свой канал** `ibrealistic:settings`
6. **Нет дублирования** — все базовые функции выполняет OBU

---

## Конкретные миксины для удаления/изменения

### Удалить полностью:
| Файл | Причина |
|---|---|
| `ClientWorldMixin.java` | Полностью дублирует OBU. IBRealistic может сбрасывать свои настройки через слушатель события или хук OBU |
| `EntityMixin.java` | Полностью дублирует OBU |
| `ServerPlayNetworkHandlerMixin.java` | Полностью дублирует OBU |

### Изменить (убрать дублирующие хуки):
| Хук | Действие |
|---|---|
| `paddleHook` / `tickHook` (@WrapOperation на checkLocation) | Убрать — OBU делает то же самое |
| `getFriction` (@WrapOperation на getSlipperiness) | Убрать — OBU делает то же самое |
| `redirectYawVelocityIncrement` | Убрать — OBU делает то же самое |
| `forwardsAccel` / `turnAccel` / `backwardsAccel` (@ModifyConstant) | Убрать — OBU делает то же самое |
| `pressingForwardHook` / `pressingBackHook` | Убрать — OBU делает то же самое |
| `velocityDecayHook1/2/3` | Убрать — OBU делает то же самое |
| `canCollideHook` | Убрать — OBU делает то же самое |
| `fallHook` | Убрать — OBU делает то же самое |
| `updateVelocityHook` (gravity, <=1.20.4) | Убрать — OBU делает то же самое |
| `interpolationStepsHook` (>=1.21.3) | Убрать — OBU делает то же самое |
| `onGetGravity` (>=1.21) | Убрать — OBU делает то же самое |

### Оставить (уникальное для IBRealistic):
| Хук | Причина |
|---|---|
| `oncePerTick` (реалистичная физика) | Уникальная функциональность IBRealistic |
| `liftPassenger` (getPassengerAttachmentPos) | Уникальная визуальная функция |
| `moveHook` (landing speed preservation) | Уникальная физика приземления |
| **BoatEntityRendererMixin** целиком | Визуальные колёса и руль |

### Проблема с `hookCheckLocation`:
Метод `hookCheckLocation` в IBRealistic содержит как OBU-логику (waterElevation, airControl, swimForce, jumping) так и IBRealistic-логику (реалистичная физика в `oncePerTick`). При удалении OBU-дублей нужно:
- Оставить вызов `oncePerTick()` для реалистичной физики
- Удалить всю логику waterElevation, airControl, swimming — это делает OBU
- Хук `hookCheckLocation` можно заменить на простой `@Inject` в `tick()` для вызова реалистичной физики

---

## Дополнительная работа (необходима)

1. **Сменить пакет** с `dev.o7moon.openboatutils` на собственный (например `dev.o7moon.ibrealistic`)
2. **Добавить OBU как зависимость** в `fabric.mod.json` и `build.gradle`
3. **Импортировать OBU** для доступа к `OpenBoatUtils.enabled`, `OpenBoatUtils.airControl`, etc.
4. **Перенести уникальные поля** (fourWheelPhysics, visualRollAngle, etc.) в свой класс
5. **Обновить ibrealistic.mixins.json** — убрать дубли
6. **Протестировать совместную работу** на всех MC версиях

---

## Заключение

IBRealistic в текущем виде **НЕВОЗМОЖНО** установить рядом с OBU из-за:
1. Одинакового пакета Java (конфликт классов)
2. Дублирования миксинов на одних и тех же точках инъекции
3. Одинаковых `@ModifyConstant` (особенно критично — вызывает краш)

Для превращения IBRealistic в аддон потребуется **значительный рефакторинг**:
- Смена пакета
- Удаление ~80% миксинов (они дублируют OBU)
- Зависимость от OBU
- Доступ к OBU API через его публичные поля

Это большая, но чистая работа, которая сделает IBRealistic настоящим аддоном.
