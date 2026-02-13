# Изменения: Исправление движения лодки в реалистичных режимах (velocityDecay ordinal fix)

## Дата
2026-02-11

## Краткое описание
Исправлены три критические проблемы, появившиеся после версии 1.0.1, вызывавшие крайне низкую скорость, зависание по Y и неправильное рулевое управление в реалистичных режимах.

## Корневые причины проблемы

### Проблема 1: Неправильный ordinal для velocityDecay ON_LAND (PR #15, версия 1.0.2)
**Файл:** `BoatMixin.java`, метод `velocityDecayOnLand`

В vanilla Minecraft метод `updateVelocity()` содержит 6 записей (PUTFIELD) в поле `velocityDecay`:
- ordinal 0: `this.velocityDecay = 0.05F;` — дефолтное значение (ВСЕГДА выполняется)
- ordinal 1: `this.velocityDecay = 0.9F;` — IN_WATER
- ordinal 2: `this.velocityDecay = 0.9F;` — UNDER_FLOWING_WATER
- ordinal 3: `this.velocityDecay = 0.45F;` — UNDER_WATER
- ordinal 4: `this.velocityDecay = 0.9F;` — IN_AIR
- ordinal 5: `this.velocityDecay = this.nearbySlipperiness;` — ON_LAND (~0.6)

Хук `velocityDecayOnLand` из PR #15 перехватывал **ordinal=0** (дефолтное 0.05F) и устанавливал 1.0.
Но реальное значение для ON_LAND (ordinal=5) записывалось ПОСЛЕ и **перезаписывало 1.0** обратно на ~0.6.

**Результат:** Velocity множилась на ~0.6 каждый тик → крайне низкая скорость.

### Проблема 2: cancelUpdateVelocityForRealisticPhysics отключал гравитацию (PR #19, версия 1.0.4+)
**Файл:** `BoatMixin.java`, метод `cancelUpdateVelocityForRealisticPhysics`

В vanilla boat, гравитация для лодки применяется **ТОЛЬКО** внутри метода `updateVelocity()`:
```java
double d = -this.getFinalGravity();  // = -0.04
// ...
this.setVelocity(vec3d.x * velocityDecay, vec3d.y + d, vec3d.z * velocityDecay);
```

В отличие от LivingEntity, Entity.applyGravity() НЕ вызывается для лодок (baseTick() не вызывает applyGravity()).

PR #19 добавил `@Inject(method="updateVelocity", HEAD, cancel)` который отменял ВЕСЬ метод.
Это убирало velocityDecay (хорошо), но также убирало гравитацию (плохо).

**Результат:** Лодка зависала по Y или поднималась вверх.

### Проблема 3: Инверсия рулевого управления (PR #19)
**Файл:** `BoatMixin.java`, метод `oncePerTick`

PR #19 инвертировал рулевое управление:
- До: `leftKey → steeringInput += 1f`, `rightKey → steeringInput -= 1f` (v1.0.1, правильно)
- После: `leftKey → steeringInput -= 1f`, `rightKey → steeringInput += 1f` (v1.0.2+, неправильно)

Физический движок FourWheelPhysicsEngine использует конвенцию, где positive steeringInput соответствует повороту налево в системе координат Minecraft (через atan2 для slip angles).

**Результат:** Повороты "неохотные", "сильная отдача".

## Изменённые файлы

### Мод (OBURealistic)
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java`:
  - Удалён метод `cancelUpdateVelocityForRealisticPhysics` (оба варианта: <=1.21 и >=1.21.3)
  - Исправлен `velocityDecayOnLand`: ordinal 0 → ordinal 5 (реальный ON_LAND)
  - Добавлен новый хук `velocityDecayInAir` (ordinal 4): velocityDecay=1.0 при реалистичной физике
  - Возвращены знаки steering: left=+1, right=-1 (конвенция v1.0.1)

## Детальное описание изменений

### 1. Удалён cancelUpdateVelocityForRealisticPhysics
**Строки:** удалены ~22 строки (оба варианта Stonecutter)
**Что сделано:** Полностью удалён @Inject на HEAD updateVelocity с cancel
**Причина:** 
- Отмена updateVelocity() убирала гравитацию для лодки
- Правильный подход: перехватывать только velocityDecay, а не весь метод
- Гравитация (`vec3d.y + d`) должна продолжать работать

### 2. Исправлен velocityDecayOnLand ordinal
**Что было:** `ordinal = 0` → перехватывал дефолтное присваивание `0.05F`
**Что стало:** `ordinal = 5` → перехватывает реальное ON_LAND присваивание `nearbySlipperiness`
**Причина:** Значение ordinal=0 перезаписывалось ordinal=5 (ON_LAND), делая хук бесполезным

### 3. Добавлен velocityDecayInAir (ordinal 4)
**Что сделано:** Новый @Redirect для IN_AIR velocityDecay (ordinal=4)
**Причина:** При реалистичной физике в воздухе, физический движок сам обрабатывает аэродинамическое сопротивление. Vanilla decay 0.9 не нужен.

### 4. Возврат рулевого управления к v1.0.1
**Что было:** left → -1, right → +1 (PR #19)
**Что стало:** left → +1, right → -1 (как в v1.0.1)
**Причина:** Физический движок ожидает эту конвенцию

## Порядок вызовов в vanilla tick() (MC 1.21.3+)
```
tick() {
    1. this.location = this.checkLocation();  // ← tickHook → hookCheckLocation → oncePerTick
                                               //    → physics engine sets velocity
    2. super.tick();                           // ← Entity.baseTick() — НЕ вызывает applyGravity()
    3. this.updateVelocity();                  // ← гравитация + velocityDecay × velocity
                                               //    velocityDecay = 1.0 для ON_LAND/IN_AIR (наш hook)
    4. this.updatePaddles();                   // ← vanilla accel = 0 (suppressed), yaw = 0 (suppressed)
    5. this.move(SELF, this.getVelocity());    // ← движение с velocity
}
```

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4
- [x] Мод собирается успешно на MC 1.21
- [x] Мод собирается успешно на MC 1.21.3 (поддерживает 1.21.4)
- [ ] Протестировано на сервере на MC 1.21.4

## Примечание
- Файл `CODEBASE_INDEX.md` нужно будет обновить:
  - Удалить описание `cancelUpdateVelocityForRealisticPhysics`
  - Добавить описание `velocityDecayInAir` (ordinal=4)
  - Обновить описание `velocityDecayOnLand` (ordinal=5)
  - Обновить описание steering convention

## Связь с предыдущими изменениями
- **PR #15 (v1.0.2):** Первоначальная попытка изоляции vanilla physics — содержала ошибку с ordinal
- **PR #19 (v1.0.5):** Попытка исправить slowness через cancel updateVelocity — создала проблему с гравитацией
- **Данное исправление:** Правильный подход — хуки на правильные ordinals, без отмены метода целиком
