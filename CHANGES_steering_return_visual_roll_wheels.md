# Изменения: Возврат руля, визуальный крен и колёса

## Дата
2026-02-10

## Краткое описание
Добавлен настраиваемый возврат руля (self-aligning torque), визуальный крен лодки при поворотах (roll), и визуальные колёса на лодке при активной реалистичной физике.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/physics/VehicleConfig.java` — добавлено поле `steeringReturnRate`
- `src/main/java/dev/o7moon/openboatutils/physics/FourWheelPhysicsEngine.java` — заменена константа `SELF_ALIGN_BASE_RATE` на настраиваемый `config.steeringReturnRate`
- `src/main/java/dev/o7moon/openboatutils/physics/RealisticPhysicsEngine.java` — аналогичная замена для legacy движка
- `src/main/java/dev/o7moon/openboatutils/ClientboundPackets.java` — добавлен пакет `SET_STEERING_RETURN_RATE` (ID: 60)
- `src/main/java/dev/o7moon/openboatutils/OpenBoatUtils.java` — добавлен `setSteeringReturnRate()`, визуальные переменные `visualRollAngle` и `visualSteeringAngle`
- `src/main/java/dev/o7moon/openboatutils/SingleplayerCommands.java` — добавлена команда `/steeringreturnrate`
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java` — разделён pitch и roll (ранее были смешаны), сохранение визуального состояния
- `src/main/java/dev/o7moon/openboatutils/mixin/BoatEntityRendererMixin.java` — **НОВЫЙ ФАЙЛ** — миксин в рендерер лодки для применения roll и рендеринга колёс
- `src/main/java/dev/o7moon/openboatutils/client/WheelRenderer.java` — **НОВЫЙ ФАЙЛ** — рендеринг 4 колёс с поворотом и вращением
- `src/main/resources/openboatutils.mixins.json` — зарегистрирован `BoatEntityRendererMixin`

### Плагин (TimingSystem)
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — добавлено поле `steeringReturnRate` с @Expose, пакет, сброс
- `src/main/java/me/makkuusen/timing/system/commands/CommandBoatUtilsModeEdit.java` — добавлена команда `steeringReturnRate`

## Детальное описание изменений

### 1. Настраиваемый возврат руля (Self-Aligning Torque)
**Файлы:** VehicleConfig.java, FourWheelPhysicsEngine.java, RealisticPhysicsEngine.java
**Что сделано:**
- Добавлен параметр `steeringReturnRate` в VehicleConfig (по умолчанию 3.0 rad/s)
- Ранее в обоих физических движках использовалась хардкодная константа `SELF_ALIGN_BASE_RATE = 3.0f`
- Теперь используется `config.steeringReturnRate`, которая настраивается через пакеты
- При `steeringReturnRate = 0` возврат руля отключён
- При высоком значении (10-20) руль возвращается почти мгновенно

**Причина:**
Пользователь хотел реалистичное поведение руля: при отпускании клавиш поворота руль пассивно возвращается в прямое положение, как в реальном автомобиле.

### 2. Визуальный крен лодки (Roll)
**Файлы:** BoatMixin.java, BoatEntityRendererMixin.java (НОВЫЙ)
**Что сделано:**
- Ранее roll и pitch смешивались в одно значение setPitch(), что не давало видимого эффекта крена
- Теперь pitch и roll разделены:
  - Pitch применяется через setPitch() (нос вверх/вниз при торможении/разгоне)
  - Roll применяется в рендерере через MatrixStack.multiply(RotationAxis.POSITIVE_Z) 
- Roll виден как наклон лодки в сторону при поворотах
- Roll масштабируется: `rollAngle * 15.0°` для визуальной заметности

**Причина:**
Мод не показывал визуально, что лодка качается при поворотах. Minecraft Entity не имеет нативного roll, поэтому применяем его через рендерер.

### 3. Визуальные колёса
**Файлы:** WheelRenderer.java (НОВЫЙ), BoatEntityRendererMixin.java
**Что сделано:**
- Создан WheelRenderer — рендерит 4 колеса в позициях FL/FR/RL/RR
- Колёса — это небольшие тёмные кубоиды (ModelPart), расположенные по углам лодки
- Передние колёса поворачиваются по оси Y в соответствии с углом поворота руля
- Все колёса вращаются по оси X в зависимости от скорости движения
- Поддерживаются все 3 версии MC (1.20.4, 1.21, 1.21.3) через Stonecutter conditions
- Используется текстура `minecraft:textures/block/black_concrete.png` для тёмного цвета

**Причина:**
Пользователь хотел добавить визуальные колёса к лодке.

## Новые пакеты
- SET_STEERING_RETURN_RATE (ID: 60) — float, скорость возврата руля в rad/s

## Изменения VERSION
- Версия не изменена (остаётся 20), так как пакет добавлен в конце списка и обратно совместим

## Тестирование
- [x] Мод собирается успешно для MC 1.20.4 (Gradle)
- [x] Мод собирается успешно для MC 1.21 (Gradle)
- [x] Мод собирается успешно для MC 1.21.3 (Gradle)
- [ ] Функциональное тестирование в игре

## Примечания
- CODEBASE_INDEX.md: нужно обновить при создании (добавить WheelRenderer, BoatEntityRendererMixin, steeringReturnRate)
- Wheel rendering использует простые кубоиды — в будущем можно заменить на более детальную модель
