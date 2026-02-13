# Изменения: Откат BoatMixin к рабочей версии 1.0.1

## Дата
2026-02-11

## Краткое описание
Возвращена логика BoatMixin к версии 1.0.1 (последней рабочей версии), удалены хуки подавления vanilla physics (velocityDecay, yaw, acceleration), добавленные в версиях 1.0.2-1.0.5.3.

## Корневая причина проблемы

После версии 1.0.1, в PR #15 (v1.0.2) и последующих изменениях были добавлены хуки для "изоляции" физического движка от vanilla physics:

1. **velocityDecayOnLand (ordinal 5)** — устанавливал velocityDecay=1.0 при реалистичной физике
2. **velocityDecayInAir (ordinal 4)** — устанавливал velocityDecay=1.0 при реалистичной физике в воздухе
3. **Suppression vanilla yaw** — обнулял yawVelocity при реалистичной физике
4. **Suppression vanilla accel** — возвращал 0f для forward/turn/backward acceleration при реалистичной физике

**Проблема:** Физический движок FourWheelPhysicsEngine был изначально настроен (в v1.0.1) для работы СОВМЕСТНО с vanilla velocity decay (~0.6 на земле) и vanilla yaw. Подавление этих vanilla-механизмов нарушало баланс физики:
- Без velocity decay (×1.0 вместо ×0.6) — скорость обрабатывалась некорректно
- Без vanilla yaw — управление инвертировалось (физический движок полагался на взаимодействие с vanilla yaw)

## Изменённые файлы

### Мод (OBURealistic)
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java`:
  - Удалён метод `velocityDecayOnLand` (@Redirect ordinal=5)
  - Удалён метод `velocityDecayInAir` (@Redirect ordinal=4)
  - В `redirectYawVelocityIncrement`: удалена ветка `if (fourWheelPhysics.isEnabled()) { yawVelocity = 0f; return; }` — vanilla yaw теперь работает нормально
  - В `forwardsAccel`, `turnAccel`, `backwardsAccel`: удалены проверки `if (fourWheelPhysics.isEnabled()) return 0f;` — vanilla acceleration через OBU hooks работает нормально

## Результат
BoatMixin теперь функционально идентичен версии 1.0.1 (за исключением WheelRenderer.tickWheelSpin — визуальный вызов, не влияющий на физику).

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4
- [x] Мод собирается успешно на MC 1.21
- [x] Мод собирается успешно на MC 1.21.3
- [ ] Протестировано на сервере

## Дополнительные исправления

### Рендер колёс не отображался
**Причина:** `BoatEntityRendererMixin` отсутствовал в `openboatutils.mixins.json5` (файл, используемый при сборке через j52j). Мixin никогда не загружался.
**Исправление:** Добавлен `BoatEntityRendererMixin` в `openboatutils.mixins.json5`.

### Колёса были слишком маленькими
**Исправление:** Увеличен WHEEL_RADIUS с 0.15f до 0.25f, WHEEL_WIDTH с 0.1f до 0.15f, LATERAL_OFFSET с 0.5f до 0.55f.

### Запись в план
Добавлен раздел 6.6 "Серверная валидация скорости (Античит)" в PLAN.md для будущей реализации проверки скорости на стороне плагина.

## Примечание
- Файл `CODEBASE_INDEX.md` нужно будет обновить:
  - Удалить описание `velocityDecayOnLand` и `velocityDecayInAir`
  - Обновить описание `redirectYawVelocityIncrement` (убрать упоминание suppression)
  - Обновить описание `forwardsAccel`/`turnAccel`/`backwardsAccel` (убрать упоминание suppression)
