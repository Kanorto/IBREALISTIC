# Изменения: Фаза 7 — Кастомная система версионирования серверов

## Дата
2026-02-12

## Краткое описание
Реализована система версионирования для связи между сервером (TimingSystem) и клиентом (OBURealistic). Сервер сообщает клиенту свою реалистичную версию, поддерживаемые фичи и имя. Клиент автоматически отвечает своей версией и фичами.

## Изменённые файлы

### Мод (OBURealistic)

- `src/main/java/dev/o7moon/openboatutils/RealisticFeature.java` — **НОВЫЙ**: Enum feature flags (FOUR_WHEEL, WEATHER, ECONOMY, SOLO_RACE, CUSTOM_CARS) с bitfield кодированием
- `src/main/java/dev/o7moon/openboatutils/ClientboundPackets.java` — Добавлен пакет `REALISTIC_SERVER_INFO` (ID 61), обработка: парсинг версии, фич, имени сервера
- `src/main/java/dev/o7moon/openboatutils/ServerboundPackets.java` — Добавлен пакет `REALISTIC_CLIENT_INFO` (ID 1), обработка на серверной стороне мода (для LAN)
- `src/main/java/dev/o7moon/openboatutils/OpenBoatUtils.java` — Добавлены поля `serverRealisticVersion`, `serverFeatures`, `serverName`; метод `getClientRealisticVersion()` для динамического чтения версии из fabric.mod.json; метод `sendRealisticClientInfoPacket()`; сброс серверной информации в `resetAll()`

### Плагин (TimingSystem)

- `src/main/java/me/makkuusen/timing/system/boatutils/RealisticFeature.java` — **НОВЫЙ**: Зеркало мод-версии enum feature flags
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java` — Добавлен `PACKET_ID_REALISTIC_SERVER_INFO = 61`; метод `sendRealisticServerInfo()` для отправки пакета
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsManager.java` — Обработка C2S пакета `REALISTIC_CLIENT_INFO` (ID 1); отправка `REALISTIC_SERVER_INFO` при подключении realistic клиента; методы `getServerRealisticVersion()`, `getServerFeatureFlags()`, `getServerName()`; VarInt string reading для совместимости с PacketByteBuf
- `src/main/java/me/makkuusen/timing/system/tplayer/TPlayer.java` — Добавлены поля `clientRealisticVersion` и `clientFeatures` с Lombok аннотациями
- `src/main/java/me/makkuusen/timing/system/theme/messages/Info.java` — Добавлены `REALISTIC_VERSION_COMPATIBLE`, `REALISTIC_NOT_SUPPORTED`
- `src/main/resources/config.yml` — Добавлена секция `realistic:` с настройками `serverName` и `features`

### Переводы

- `src/main/resources/lang/en_us.yml` — Добавлены `realistic_version_compatible`, `realistic_not_supported`
- `src/main/resources/lang/de_de.yml` — Немецкие переводы
- `src/main/resources/lang/id_id.yml` — Индонезийские переводы
- `src/main/resources/lang/zh_cn.yml` — Китайские переводы
- `src/main/resources/lang/triton.yml` — Triton обёртки для новых ключей
- `triton/timingsystem.json` — Все языки (en_GB, ru_RU, de_DE, pl_PL, nl_NL, es_ES, pt_BR, fr_FR)

### Документация

- `PLAN.md` — Фаза 7 отмечена как ✅ выполненная

## Детальное описание изменений

### 1. Пакет REALISTIC_SERVER_INFO (S2C, ID 61)
**Формат:** `[short:61][string:version][int:features][string:serverName]`

Сервер отправляет этот пакет клиенту после получения VERSION пакета с флагом `isRealistic=true`.
Содержит:
- `version` — реалистичная версия сервера (извлекается автоматически из plugin metadata)
- `features` — bitfield поддерживаемых фич (из config.yml)
- `serverName` — имя сервера (из config.yml)

### 2. Пакет REALISTIC_CLIENT_INFO (C2S, ID 1)
**Формат:** `[short:1][string:version][int:features]`

Клиент автоматически отправляет этот пакет серверу после получения REALISTIC_SERVER_INFO.
Содержит:
- `version` — реалистичная версия мода (извлекается из fabric.mod.json через FabricLoader)
- `features` — bitfield поддерживаемых клиентом фич

### 3. Feature Flags (RealisticFeature enum)
Битовая маска для кодирования поддерживаемых возможностей:
- Bit 0: FOUR_WHEEL (четырёхколёсная физика)
- Bit 1: WEATHER (погода)
- Bit 2: ECONOMY (экономика) — зарезервировано для будущего
- Bit 3: SOLO_RACE (соло-рейсы) — зарезервировано для будущего
- Bit 4: CUSTOM_CARS (кастомизация) — зарезервировано для будущего

### 4. Конфигурация сервера (config.yml)
```yaml
realistic:
  serverName: "Realistic Rally Server"
  features:
    fourWheel: true
    weather: true
    economy: false
    soloRace: false
    customCars: false
```

### 5. Динамическое определение версий
- **Мод:** Версия извлекается из fabric.mod.json через `FabricLoader.getInstance().getModContainer("openboatutils")`
  Формат: `{obu_version}-{realistic_version}_{mc_suffix}` → извлекается `realistic_version`
- **Плагин:** Версия извлекается из plugin metadata через `getPluginMeta().getVersion()`
  Формат: `{ts_base_version}-{realistic_version}` → извлекается `realistic_version`

## Новые пакеты
- `REALISTIC_SERVER_INFO` (ID: 61) — S2C: версия сервера, фичи, имя
- `REALISTIC_CLIENT_INFO` (ID: 1) — C2S: версия клиента, фичи

## Поток данных
```
1. Клиент подключается к серверу
2. Клиент отправляет VERSION (ID 0) с isRealistic=true
3. Сервер обрабатывает VERSION, запоминает isRealistic в TPlayer
4. Сервер отправляет INTERPOLATION_COMPAT (ID 29)
5. Сервер отправляет REALISTIC_SERVER_INFO (ID 61) с версией, фичами, именем
6. Клиент получает REALISTIC_SERVER_INFO, сохраняет в OpenBoatUtils
7. Клиент автоматически отправляет REALISTIC_CLIENT_INFO (ID 1)
8. Сервер получает REALISTIC_CLIENT_INFO, сохраняет в TPlayer
```

## Тестирование
- [x] Мод собирается успешно на MC 1.20.4
- [x] Мод собирается успешно на MC 1.21
- [x] Мод собирается успешно на MC 1.21.3
- [x] Плагин собирается успешно (Maven)

## Примечание
- Версии НЕ хардкодятся — определяются автоматически из build artifacts
- gradle.properties и pom.xml НЕ модифицированы
- VERSION (18) НЕ изменён
- CODEBASE_INDEX.md нужно будет обновить при его создании
