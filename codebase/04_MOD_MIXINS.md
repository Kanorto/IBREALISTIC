# Миксины мода OBURealistic

Пакет: `dev.o7moon.openboatutils.mixin`

Миксины — это патчи, которые Fabric внедряет в классы Minecraft для модификации поведения лодок.

---

## BoatMixin.java (MC 1.20.4, 1.21)

**Target:** `net.minecraft.entity.vehicle.BoatEntity`
**Implements:** `GetStepHeight`

### Shadow-поля (из BoatEntity)
| Имя | Тип | Описание |
|-----|-----|----------|
| `location` | `BoatEntity.Location` | Текущее местоположение лодки |
| `velocityDecay` | `float` | Затухание скорости |
| `nearbySlipperiness` | `float` | Скользкость ближайших блоков |
| `waterLevel` | `double` | Уровень воды |
| `yawVelocity` | `float` | Скорость поворота |
| `pressingForward` | `boolean` | Нажата клавиша вперёд |
| `pressingBack` | `boolean` | Нажата клавиша назад |

### Shadow-методы
| Метод | Описание |
|-------|----------|
| `checkBoatInWater()` | Проверка лодки в воде |
| `getUnderWaterLocation()` | Получение подводного местоположения |
| `checkLocation()` | Определение текущего location |

### Методы миксина
| Метод | Аннотация | Описание |
|-------|-----------|----------|
| `set_step_height(float)` | `@Unique` | Установка высоты шага |
| `oncePerTick(BoatEntity, Location, MinecraftClient)` | — | Вызывается один раз за тик — ядро всей физики |
| `paddleHook(BoatEntity)` | `@Redirect` | Хук в метод tick() — определяет когда вызывать oncePerTick |
| `tickHook(BoatEntity)` | `@Redirect` | Второй хук в tick() |
| `hookCheckLocation(BoatEntity, boolean)` | — | Логика определения Location с поддержкой airControl, waterControl |
| `getFriction(Block)` | `@Redirect` | Подмена скользкости блока |
| `canCollideHook(Entity, CIR<Boolean>)` | `@Inject` | Модификация коллизий |
| `fallHook(CallbackInfo)` | `@Inject` | Отключение урона от падения |
| `updateVelocityHook(double)` | `@ModifyVariable` | Модификация гравитации |
| `redirectYawVelocityIncrement(BoatEntity, float)` | `@Redirect` | Модификация ускорения поворота |
| `forwardsAccel(float)` | `@ModifyConstant` | Модификация ускорения вперёд |
| `turnAccel(float)` | `@ModifyConstant` | Модификация ускорения при повороте |
| `backwardsAccel(float)` | `@ModifyConstant` | Модификация ускорения назад |
| `pressingForwardHook(BoatEntity)` | `@Redirect` | Управление вперёд (underwaterControl) |
| `pressingBackHook(BoatEntity)` | `@Redirect` | Управление назад (underwaterControl) |
| `velocityDecayHook1/2/3(BoatEntity, float)` | `@Redirect` | Подмена затухания скорости |
| `moveHook(BoatEntity, MovementType, Vec3d)` | `@Redirect` | Хук движения (collisionResolution) |

### Ключевая логика `oncePerTick()`
1. Обработка coyoteTime (таймер грунта)
2. Прыжок (jumpForce, waterJumping)
3. Плавание (swimForce)
4. **Реалистичная физика** — вызов `realisticPhysics.update()` и применение результата

---

## AbstractBoatMixin.java (MC 1.21.3)

**Target:** `net.minecraft.entity.vehicle.AbstractBoatEntity`
**Implements:** `GetStepHeight`
**Расположение:** `versions/1.21.3/src/main/java/...`

Аналог BoatMixin для MC 1.21.3, где лодка переименована в AbstractBoatEntity.

### Дополнительные методы (отличия от BoatMixin)
| Метод | Аннотация | Описание |
|-------|-----------|----------|
| `getStepHeight()` | public | Getter высоты шага |
| `interpolationStepsHook(int)` | `@ModifyVariable` | Хук интерполяции (interpolationCompat) |
| `onGetGravity(CIR<Double>)` | `@Inject` | Хук гравитации (другой API в 1.21.3) |

---

## ServerPlayNetworkHandlerMixin.java

**Target:** `net.minecraft.server.network.ServerPlayNetworkHandler`

**Назначение:** Отключение серверных проверок "moved wrongly" для лодок с модифицированной физикой.

### Методы
| Метод | Аннотация | Описание |
|-------|-----------|----------|
| `isMovementInvalid(CIR<Boolean>)` | `@Inject` | Возвращает false для проверки невалидного движения |
| `onVehicleMove_WronglyFlag(boolean)` | `@ModifyVariable` | Сброс флага "moved wrongly" в false |
| `preventMovedWronglyLog(Logger, String, Object[])` | `@Redirect` | Подавление лог-сообщений о невалидном движении |

---

## EntityMixin.java

**Target:** `net.minecraft.entity.Entity`

**Назначение:** Модификация высоты шага и проверки земли для лодок.

### Методы
| Метод | Аннотация | Описание |
|-------|-----------|----------|
| `getStepHeight(CIR<Float>)` | `@Inject` | Подмена высоты шага для лодок (использует GetStepHeight interface) |
| `hookStepHeightOnGroundCheck(boolean)` | `@ModifyVariable` | Разрешение шага при падении (canStepWhileFalling) |

---

## ClientWorldMixin.java

**Target:** `net.minecraft.client.world.ClientWorld`

**Назначение:** Хук загрузки клиентского мира.

### Методы
| Метод | Аннотация | Описание |
|-------|-----------|----------|
| `postWorldLoad(CallbackInfo)` | `@Inject` | Вызывается после загрузки мира — используется для инициализации |
