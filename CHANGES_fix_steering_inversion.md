# Изменения: Переход на архитектуру PR #22 — velocity decay hooks вместо полной отмены vanilla physics

## Дата
2026-02-18

## Краткое описание
Изменена архитектура взаимодействия с vanilla physics на модель PR #22:
- Steering: `left→+1, right→-1` (как в v1.0.1 / PR #22)
- updateVelocity() НЕ отменяется — vanilla gravity работает нормально
- Только velocityDecay перехватывается через @Redirect ordinal=4 (IN_AIR) и ordinal=5 (ON_LAND)
- updatePaddles() НЕ отменяется — vanilla yaw и OBU acceleration работают

## Изменённые файлы

### Мод (IBRealistic)
- `IBRealistic/src/main/java/dev/kanorto/ibrealistic/mixin/BoatMixin.java`:
  - Steering: `left→+1, right→-1` (восстановлена конвенция PR #22)
  - Удалён `cancelVanillaPaddles()` — vanilla yaw и OBU acceleration теперь работают
  - Заменён `cancelVanillaVelocityDecay()` на два @Redirect хука:
    - `velocityDecayOnLand` (ordinal=5) → velocityDecay=1.0 при реалистичной физике
    - `velocityDecayInAir` (ordinal=4) → velocityDecay=1.0 при реалистичной физике
  - Добавлены импорты: `Redirect`, `Opcodes`

### Физика (FourWheelPhysicsEngine)
- Гравитация в airborne ветке — оставлена vanilla (entityVel.y) без ручного применения gravityForce,
  потому что updateVelocity() больше не отменяется и vanilla gravity работает

## Анализ PRs #19, #21, #22

### PR #19: Полная отмена updateVelocity()
- Добавил `cancelUpdateVelocityForRealisticPhysics` (cancel на HEAD)
- Изменил steering: `left→-1, right→+1`
- Проблема: отмена updateVelocity() убирала gravity → лодка зависала

### PR #21: Проверка версии (не касается физики)
- Добавил флаг `requiresRealisticMod` в BoatUtilsMode

### PR #22: Правильный подход — ordinal hooks
- Удалил `cancelUpdateVelocityForRealisticPhysics` → gravity работает
- Добавил velocityDecayOnLand (ordinal=5) и velocityDecayInAir (ordinal=4)
- Вернул steering к `left→+1, right→-1`
- Этот подход РАБОТАЛ правильно и подтверждён пользователем

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4 (Gradle)
- [x] Мод собирается успешно на MC 1.21 (Gradle)
- [x] Мод собирается успешно на MC 1.21.3 (Gradle)

## Примечание
- Файл `CODEBASE_INDEX.md` нужно обновить:
  - Удалить описание `cancelVanillaPaddles` и `cancelVanillaVelocityDecay`
  - Добавить описание `velocityDecayOnLand` (ordinal=5) и `velocityDecayInAir` (ordinal=4)
  - Обновить steering convention: `left→+1, right→-1`
