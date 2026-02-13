# Точки расширения

Как добавить новые фичи в проект OBURealistic.

---

## 1. Добавление нового типа машины

1. **Мод:** Добавить значение в `VehicleType.java` с параметрами
2. **Мод:** Добавить режим в `Modes.java` (вызывающий `setVehicleType`)
3. **Плагин:** Добавить значение в `BoatUtilsMode.java` с ID и требуемой версией
4. **Плагин:** Добавить обработку в `CustomBoatUtilsMode.drivetrainName()` (если новый drivetrain)
5. Обновить `VERSION` в обоих проектах (если новый packet ID)

---

## 2. Добавление новой настройки физики

### Шаг 1: Мод — хранение
Добавить поле в `VehicleConfig.java` или `OpenBoatUtils.java`.

### Шаг 2: Мод — пакет
1. Добавить значение в enum `ClientboundPackets.java`
2. В `handlePacket()` добавить `case` для нового пакета
3. Вызвать соответствующий сеттер в `OpenBoatUtils`

### Шаг 3: Мод — команда
Добавить команду в `SingleplayerCommands.registerCommands()`.

### Шаг 4: Мод — применение
Использовать новое поле в `BoatMixin.oncePerTick()` или `RealisticPhysicsEngine.update()`.

### Шаг 5: Плагин — хранение и отправка
1. Добавить поле с `@Expose` в `CustomBoatUtilsMode.java`
2. Добавить `PACKET_ID_*` константу
3. В `finallyApplyToPlayer()` отправить пакет если значение нестандартное
4. В `resetToVanilla()` установить дефолтное значение
5. В `getNonDefaultSettings()` добавить проверку

### Шаг 6: Плагин — команда
Добавить обработку в `CommandBoatUtilsModeEdit.onSet()`.

### Шаг 7: Обновить VERSION
В `OpenBoatUtils.VERSION` и `BoatUtilsMode` (getRequiredVersion для новых режимов).

---

## 3. Добавление новой поверхности

1. **Мод:** Добавить пресет в `SurfaceProperties.java` (static field)
2. **Мод:** Добавить `case` в `getSurfaceByName()`
3. **Мод:** Добавить маппинг блоков в `getBlockSurfaceMap()` (опционально)
4. **Плагин:** Обновить документацию валидных имён поверхностей

---

## 4. Добавление нового per-block параметра

1. **Мод:** Добавить значение в `OpenBoatUtils.PerBlockSettingType`
2. **Мод:** Обновить `defaultPerBlock()` для нового типа
3. **Мод:** Использовать `getNearbySetting()` в `BoatMixin`
4. **Плагин:** Обновить `getSettingTypeFromName()` в `CommandBoatUtilsModeEdit`
5. **Плагин:** Обновить `parseValueForSetting()` для нового типа

---

## 5. Добавление нового типа трассы

1. **Плагин:** Добавить значение в `Track.TrackType`
2. **Плагин:** Обновить `getTypeAsString()` и `isTrackType()`
3. **Плагин:** Обновить `TrackEditor.setTrackType()`
4. **Плагин:** Обновить фильтрацию в `TrackFilter`

---

## 6. Добавление нового события API

1. **Плагин:** Создать класс в `api/events/` extending `org.bukkit.event.Event`
2. Добавить `@Getter` поля и `HandlerList`
3. Вызвать `Bukkit.getPluginManager().callEvent(new YourEvent(...))` в нужном месте

---

## 7. Добавление новой команды

1. **Плагин:** Создать класс в `commands/` extending `BaseCommand`
2. Добавить `@CommandAlias`, `@Subcommand`, `@CommandPermission` аннотации
3. Зарегистрировать в `TimingSystem.onEnable()`
4. Добавить enum разрешений в `permissions/`

---

## 8. Поддержка новой версии Minecraft

1. **Мод:** Добавить папку в `versions/` с `gradle.properties`
2. Если API лодки изменился — создать новый миксин в `versions/X.Y.Z/src/...`
3. Использовать Stonecutter директивы `//? >=X.Y.Z {` для условной компиляции
4. Обновить `stonecutter.gradle` со списком версий
