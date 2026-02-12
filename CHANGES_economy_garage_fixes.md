# Изменения: Проверка и исправление фаз 8-9 (Экономика и Кастомизация)

## Дата
2026-02-12

## Краткое описание
Полная проверка реализации фаз 8 (Экономика) и 9 (Кастомизация транспорта). Обнаружены и исправлены критические баги в системе разрешений, регистрации команд, защите от дублирования покупок, и добавлены Triton переводы.

## Изменённые файлы

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/permissions/PermissionGarage.java` — новый enum для разрешений гаража

#### Исправленные файлы
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — добавлена регистрация PermissionGarage.init(cr)
- `src/main/java/me/makkuusen/timing/system/commands/CommandGarage.java` — исправлены разрешения, добавлена проверка дублей, добавлен `type` в CommandCompletion
- `src/main/java/me/makkuusen/timing/system/commands/CommandShop.java` — исправлены разрешения, добавлен `type` в CommandCompletion
- `src/main/java/me/makkuusen/timing/system/economy/GarageManager.java` — добавлен метод getCurrentPreset()
- `src/main/java/me/makkuusen/timing/system/economy/RallyCoinManager.java` — исправлена гонка данных в spendCoins()
- `src/main/java/me/makkuusen/timing/system/theme/messages/Error.java` — добавлен GARAGE_ALREADY_INSTALLED
- `src/main/resources/plugin.yml` — добавлены команды garage/shop, разрешение timingsystem.garage.use
- `src/main/resources/lang/en_us.yml` — добавлен garage_already_installed, исправлены categories/component сообщения
- `src/main/resources/lang/triton.yml` — добавлены все гаражные сообщения для Triton
- `triton/timingsystem.json` — добавлены 20 записей переводов гаража (8 языков)

## Детальное описание изменений

### 1. Критический баг: Отсутствие PermissionGarage
**Файлы:** `PermissionGarage.java`, `TimingSystem.java`, `CommandGarage.java`, `CommandShop.java`
**Что было:** Команды /garage и /shop использовали `%permissiongarage` в `@CommandPermission`, но этот placeholder никогда не регистрировался. ACF не мог разрешить его, команды были недоступны.
**Что сделано:**
- Создан enum `PermissionGarage` с единственным значением `USE` (формирует разрешение `timingsystem.garage.use`)
- Все `%permissiongarage` заменены на `%permissiongarage_use` (соответствует паттерну: `className_enumValue`)
- PermissionGarage.init(cr) зарегистрирован в TimingSystem.java

### 2. Критический баг: Команды garage/shop не объявлены в plugin.yml
**Файл:** `plugin.yml`
**Что было:** Команды garage и shop не были объявлены, Bukkit не регистрировал их.
**Что сделано:**
- Добавлены объявления команд: `garage` (aliases: cars), `shop` (aliases: carshop)
- Добавлено разрешение `timingsystem.garage.use` (default: true)
- Разрешение добавлено в пак `timingsystem.packs.default`

### 3. Баг: Двойная оплата при повторной покупке
**Файлы:** `CommandGarage.java`, `GarageManager.java`, `Error.java`, `en_us.yml`
**Что было:** Если у игрока уже был установлен пресет SOFT, он мог заплатить за SOFT ещё раз.
**Что сделано:**
- Добавлен метод `GarageManager.getCurrentPreset(car, component)` для проверки текущего пресета
- В CommandGarage.onUpgrade() добавлена проверка `currentPreset == presetId` перед покупкой
- Добавлен новый enum `Error.GARAGE_ALREADY_INSTALLED` с сообщением

### 4. Баг: Гонка данных в spendCoins()
**Файл:** `RallyCoinManager.java`
**Что было:** TOCTOU (time-of-check-time-of-use): баланс проверялся отдельным SELECT, затем UPDATE мог списать монеты в минус при конкурентных запросах.
**Что сделано:** Добавлено `AND balance >= ?` в SQL UPDATE, проверяется `rows == 0` для атомарной проверки.

### 5. Отсутствие type в списке компонентов
**Файлы:** `CommandGarage.java`, `CommandShop.java`, `en_us.yml`
**Что было:** В CommandCompletion и текстах не было `type` (vehicletype), хотя GarageManager его поддерживал.
**Что сделано:** Добавлен `type` в все CommandCompletion аннотации и текстовые сообщения.

### 6. Отсутствие Triton переводов
**Файлы:** `triton.yml`, `timingsystem.json`
**Что было:** Гаражные сообщения не были обёрнуты в Triton теги, переводов не было.
**Что сделано:** Добавлены все 20 записей (error, success, info) в triton.yml и timingsystem.json (8 языков).

## Проверка существующей реализации (что работает корректно)

### Фаза 8 (Экономика) ✅
- RallyCoinManager: getBalance, addCoins, spendCoins (исправлен), transfer, setBalance, history — ОК
- LevelManager: XP формула, level-up, XP bar, getTopPlayers — ОК
- DailyChallengeManager: 5 типов заданий, прогресс, награды, regenerate — ОК
- EconomyListener: TimeTrialFinishEvent + DriverFinishHeatEvent — ОК
- CommandCoins: balance, history, pay, admin give/take/set — ОК
- CommandLevel: level, top, admin setlevel/addxp — ОК
- CommandDaily: daily, admin regenerate — ОК
- DB миграции: Version14 (coins), Version15 (levels), Version16 (daily), Version17 (garage) — ОК
- Конфигурация: economy.*, levels.* в config.yml — ОК

### Фаза 9 (Кастомизация) ✅
- 7 пресетов (Tire, Suspension, Engine, Body, Steering, Brake, WeightDistribution) — ОК
- VehicleConfig effective getters с multipliers — ОК
- ClientboundPackets 62-68 для пресетов — ОК
- CustomBoatUtilsMode: поля, applyToPlayer(), sendPresetPacketsOnly() — ОК
- PlayerCar DTO — ОК
- GarageManager CRUD — ОК
- SpawnManager: hotbar item с именем машины — ОК
- ApiUtilities.applyGarageCarPresets() — ОК
- TrackOption.ALLOW_GARAGE_CARS — ОК

## Тестирование
- [x] Мод собирается успешно (Gradle, все MC версии: 1.20.4, 1.21, 1.21.3)
- [x] Плагин собирается успешно (Maven)
- [x] JSON файлы валидны
- [x] YAML файлы валидны

## Примечания
- CODEBASE_INDEX.md: нужно будет обновить при его создании (добавить PermissionGarage.java, обновить CommandGarage.java, GarageManager.java)
- Другие языковые файлы (de_de, pl_pl, etc.) не имеют garage переводов — fallback к en_us.yml работает корректно через LanguageManager.setDefaults()
