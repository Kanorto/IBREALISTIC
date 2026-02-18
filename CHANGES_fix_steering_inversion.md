# Изменения: Исправление инвертированного управления в реалистичной физике

## Дата
2026-02-18

## Краткое описание
Исправлена инверсия рулевого управления: нажатие клавиши «вправо» поворачивало машину налево и наоборот.

## Изменённые файлы

### Мод (IBRealistic)
- `IBRealistic/src/main/java/dev/kanorto/ibrealistic/mixin/BoatMixin.java` — инвертированы знаки steeringInput для правильного направления поворота

## Детальное описание изменений

### 1. Исправлено инвертированное управление
**Файл:** `BoatMixin.java`
**Строки:** 119-121
**Что было (НЕПРАВИЛЬНО):**
```java
if (minecraft.options.leftKey.isPressed()) steeringInput += 1f;
if (minecraft.options.rightKey.isPressed()) steeringInput -= 1f;
```

**Что стало (ПРАВИЛЬНО):**
```java
if (minecraft.options.leftKey.isPressed()) steeringInput -= 1f;
if (minecraft.options.rightKey.isPressed()) steeringInput += 1f;
```

## Анализ PRs #19, #21, #22

### PR #19: Исправление инвертированного управления (left→-1, right→+1)
PR #19 менял знаки с `left→+1, right→-1` на `left→-1, right→+1`.
Также добавлял `cancelUpdateVelocityForRealisticPhysics` — полную отмену updateVelocity().
В этой архитектуре (полная отмена updateVelocity) конвенция `left→-1, right→+1` работала правильно.

### PR #21: Проверка версии (не касается steering)
PR #21 добавлял флаг `requiresRealisticMod` в BoatUtilsMode и не менял физику или steering.

### PR #22: Исправление velocityDecay + возврат steering (left→+1, right→-1)
PR #22 делал три вещи:
1. Удалял `cancelUpdateVelocityForRealisticPhysics` — updateVelocity() снова работал
2. Исправлял velocityDecay ordinal (0→5) + добавлял velocityDecayInAir (ordinal=4)
3. Возвращал steering к `left→+1, right→-1`

**Ключевое отличие PR #22 от текущей архитектуры:**
В PR #22 `updateVelocity()` НЕ отменялся — только velocityDecay перехватывался через @Redirect.
Это значит, что vanilla yaw (через `redirectYawVelocityIncrement` в updatePaddles) всё ещё работал.
Vanilla yaw добавлялся ПОВЕРХ физического yaw и инвертировал общее направление поворота.
Поэтому `left→+1` работало правильно — vanilla yaw компенсировал знак.

## Почему текущая архитектура IBRealistic требует ДРУГИЕ знаки

Текущая IBRealistic архитектура (с **PR #62** и далее):
- `cancelVanillaPaddles()` — **полностью отменяет** updatePaddles() → vanilla yaw не применяется
- `cancelVanillaVelocityDecay()` — **полностью отменяет** updateVelocity() → velocityDecay = 1.0

Это совпадает с архитектурой **PR #19** (полная отмена updateVelocity), а НЕ с архитектурой **PR #22** (точечные хуки).

Математическое доказательство для текущей архитектуры:
1. `steeringInput = +1` → `effectiveSteering > 0`
2. `alphaFront = atan2(vy, |vx|) - effectiveSteering` → при vy≈0 → `alpha ≈ -effectiveSteering < 0`
3. Fiala tire model: `fy ≈ -cAlpha * tan(alpha)` → при alpha<0 → `fy > 0` (positive)
4. `yawMoment = fy_front * Lf > 0` → positive
5. `yawRate > 0` → `yawDelta > 0` → MC yaw увеличивается → поворот НАПРАВО
6. Итог: `steeringInput = +1` → поворот НАПРАВО

Следовательно для поворота НАЛЕВО нужен `steeringInput = -1` → `leftKey → -1`.

## Когда баг был впервые введён в IBRealistic

Баг был введён при создании BoatMixin.java в IBRealistic репозитории.
Файл был создан со знаками `left→+1, right→-1` из конвенции PR #22/v1.0.1,
но архитектура IBRealistic использует полную отмену vanilla physics (как PR #19),
что требует ПРОТИВОПОЛОЖНЫХ знаков `left→-1, right→+1`.

Первый коммит с файлом BoatMixin.java в IBRealistic — это рефакторинг мода
из OpenBoatUtilsRealistic в IBRealistic (новый пакет dev.kanorto.ibrealistic).

### 2. Исправлена гравитация при прыжках (FourWheelPhysicsEngine.java)
**Файл:** `FourWheelPhysicsEngine.java`
**Строки:** 355-362 (airborne branch)
**Что было (НЕПРАВИЛЬНО):**
```java
float clampedVelY = (float) entityVel.y;
```

**Что стало (ПРАВИЛЬНО):**
```java
float clampedVelY = (float) entityVel.y + (float) OpenBoatUtils.gravityForce;
```

**Причина:**
Когда `cancelVanillaVelocityDecay()` отменяет `updateVelocity()`, vanilla gravity
для лодки тоже отменяется — потому что gravity применяется ТОЛЬКО внутри
`updateVelocity()` через `getFinalGravity()`. На земле это не заметно благодаря
GROUND_SNAP_VELOCITY (-0.04f), но в воздухе лодка падала со скоростью всего
~0.04 blocks/tick вместо естественного ускорения свободного падения.

Теперь при каждом тике в воздухе к вертикальной скорости добавляется
`OpenBoatUtils.gravityForce` (≈-0.04 blocks/tick²), что создаёт правильное
ускорение свободного падения.

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4 (Gradle)
- [x] Мод собирается успешно на MC 1.21 (Gradle)
- [x] Мод собирается успешно на MC 1.21.3 (Gradle)

## Примечание
- Файл `CODEBASE_INDEX.md` нужно обновить: отразить правильную конвенцию знаков steeringInput и airborne gravity fix
