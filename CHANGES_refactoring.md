# Изменения: Рефакторинг IBRealistic — удаление дубликатов и двухканальная система

## Дата
2026-02-13

## Краткое описание
Удалены 15 дублирующих OBU хуков из BoatMixin мода IBRealistic. Добавлена двухканальная система в TimingSystem: базовые OBU-пакеты (0-32) отправляются через `openboatutils:settings`, реалистичные пакеты IBRealistic (33-69) — через `ibrealistic:settings`.

## Изменённые файлы

### Мод (IBRealistic)

- `src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java` — **ПОЛНАЯ переработка**: удалены 15 дублирующих хуков, оставлены 3 уникальных (liftPassenger, realisticPhysicsTick, moveHook). 640→214 строк, 23→3 хука.
- `src/main/java/dev/o7moon/openboatutils/SurfaceDebugHelper.java` — **НОВЫЙ**: утилитный класс, вынесенный из миксина (SURFACE_NAMES + getSurfaceName)
- `src/main/java/dev/o7moon/openboatutils/mixin/ClientWorldMixin.java` — Переделан: сбрасывает только IBRealistic-поля (fourWheelPhysics, visual*, countdown*)
- `src/main/java/dev/o7moon/openboatutils/mixin/EntityMixin.java` — **УДАЛЁН**: OBU обрабатывает stepHeight
- `src/main/java/dev/o7moon/openboatutils/mixin/ServerPlayNetworkHandlerMixin.java` — **УДАЛЁН**: OBU обрабатывает anti-cheat
- `src/main/java/dev/o7moon/openboatutils/GetStepHeight.java` — **УДАЛЁН**: не используется
- `src/main/resources/ibrealistic.mixins.json` — Убраны EntityMixin и ServerPlayNetworkHandlerMixin
- `src/main/resources/ibrealistic.mixins.json5` — Аналогично

### Плагин (TimingSystem)

- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — Добавлена регистрация канала `openboatutils:settings`
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — Добавлены CHANNEL_OBU/CHANNEL_IBREALISTIC, getChannelForPacket(), sendPacketToChannel(), обновлены все 12 send-методов
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsManager.java` — Пакеты 0/8/29 маршрутизируются через CHANNEL_OBU
- `src/main/java/me/makkuusen/timing/system/boatutils/NocolManager.java` — Пакеты 27/31 через CHANNEL_OBU
- `src/main/java/me/makkuusen/timing/system/drs/DrsManager.java` — Пакеты 0/11 через CHANNEL_OBU
- `src/main/java/me/makkuusen/timing/system/PluginMessageReceiver.java` — Принимает сообщения на обоих каналах

### Документация
- `REFACTORING_PLAN.md` — Детальный план рефакторинга с описанием каждой переменной

## Детальное описание изменений

### 1. Удаление дублирующих хуков из BoatMixin

**Файл:** `IBRealistic/src/main/java/dev/o7moon/openboatutils/mixin/BoatMixin.java`

**Удалённые хуки (15 штук):**
| # | Хук | Аннотация | Причина удаления |
|---|------|-----------|------------------|
| 1 | paddleHook | @WrapOperation | OBU @Redirect делает то же |
| 2 | tickHook | @WrapOperation | OBU @Redirect делает то же |
| 3 | hookCheckLocation | helper | Заменён на realisticPhysicsTick |
| 4 | oncePerTick | helper | OBU-часть удалена, реалист. часть в realisticPhysicsTick |
| 5 | getFriction | @WrapOperation | OBU @Redirect делает то же |
| 6 | redirectYawVelocityIncrement | @WrapOperation | OBU @Redirect делает то же |
| 7 | forwardsAccel | @ModifyConstant | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ с OBU |
| 8 | turnAccel | @ModifyConstant | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ с OBU |
| 9 | backwardsAccel | @ModifyConstant | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ с OBU |
| 10 | pressingForwardHook | @WrapOperation | OBU @Redirect делает то же |
| 11 | pressingBackHook | @WrapOperation | OBU @Redirect делает то же |
| 12-14 | velocityDecayHook1/2/3 | @WrapOperation | OBU @Redirect делает то же |
| 15 | canCollideHook | @Inject | OBU @Inject делает то же |
| 16 | fallHook | @Inject | OBU @Inject делает то же |
| 17 | updateVelocityHook | @ModifyVariable | ‼️ КРИТИЧЕСКИЙ КОНФЛИКТ с OBU |
| 18 | onGetGravity | @Inject | OBU @Inject делает то же |
| 19 | interpolationStepsHook | @ModifyVariable | OBU @ModifyVariable делает то же |

**Оставленные хуки (3 штуки):**
1. `liftPassenger` — подъём пассажира при реалистичной физике (уникальный для IBRealistic)
2. `realisticPhysicsTick` — **НОВЫЙ** @Inject(tick, HEAD): вызов FourWheelPhysicsEngine, обновление visual state, debug HUD
3. `moveHook` — **ПЕРЕДЕЛАН**: вызывает original.call() для делегации OBU, затем применяет landing speed preservation

### 2. Вынос SURFACE_NAMES из миксина

**Файл:** `IBRealistic/src/main/java/dev/o7moon/openboatutils/SurfaceDebugHelper.java`

Статическая Map<SurfaceProperties, String> SURFACE_NAMES перенесена из миксин-класса в обычный utility-класс. Миксин-классы не должны содержать static initializer blocks (static {}).

### 3. Двухканальная система в TimingSystem

**Файл:** `TimingSystem/.../CustomBoatUtilsMode.java`

Добавлены:
- `CHANNEL_OBU = "openboatutils:settings"` — для базовых OBU-пакетов (IDs 0-32)
- `CHANNEL_IBREALISTIC = "ibrealistic:settings"` — для реалистичных пакетов (IDs 33-69)
- `getChannelForPacket(short packetId)` — автоматическая маршрутизация по ID
- `sendPacketToChannel(Player, short, String)` — отправка в конкретный канал
- `resetPlayer()` — отправляет RESET в ОБА канала

## Переменные, которые настраиваются по-другому

См. REFACTORING_PLAN.md разделы 1.1, 1.2, 1.3, 6.1-6.5 для полного описания каждой переменной.

## Тестирование
- [x] Мод собирается на MC 1.20.4
- [x] Мод собирается на MC 1.21
- [x] Мод собирается на MC 1.21.3
- [x] Плагин собирается (Maven)

## Примечания
- CODEBASE_INDEX.md необходимо обновить с новой архитектурой
- Пакет Java остаётся `dev.o7moon.openboatutils` (смена пакета — отдельная большая задача)
- ClientboundPackets.java сохраняет обработку всех пакетов 0-69 для singleplayer совместимости
- OpenBoatUtils.java сохраняет все поля для singleplayer совместимости
