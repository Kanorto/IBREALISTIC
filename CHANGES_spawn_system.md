# Изменения: Система спавна игроков и хотбар-предметы

## Дата
2026-02-11

## Краткое описание
Добавлена система спавна игроков при заходе на сервер с телепортацией в настраиваемый мир и выдачей хотбар-предметов, которые выполняют команды по клику.

## Изменённые файлы

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/spawn/SpawnManager.java` — управление спавн-локацией и создание хотбар-предметов
- `src/main/java/me/makkuusen/timing/system/spawn/SpawnListener.java` — обработка событий: клик предметов, дроп, респавн, вход

#### Изменённые файлы
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — регистрация SpawnListener и инициализация SpawnManager
- `src/main/resources/config.yml` — добавлена секция spawn с настройками
- `src/main/resources/lang/en_us.yml` — добавлены ключи перевода для предметов
- `src/main/resources/lang/triton.yml` — добавлены Triton-обёртки для ключей предметов

### Документация
- `PLAN.md` — добавлена ФАЗА 5a (система спавна), ФАЗА 6 (кастомизация автомобилей)

## Детальное описание изменений

### 1. SpawnManager.java
**Файл:** `src/main/java/me/makkuusen/timing/system/spawn/SpawnManager.java`
**Что сделано:**
- Класс для управления спавн-локацией (мир, координаты из config.yml)
- Создание 3 хотбар-предметов с PersistentDataContainer-тегом для идентификации:
  - Слот 0: NETHER_STAR (Треки — /tt)
  - Слот 1: RECOVERY_COMPASS (Сброс — /reset)
  - Слот 8: CHERRY_BOAT (Лодка — /b)
- Поддержка Triton через LanguageManager для переводимых имён предметов
- Методы: initialize(), loadConfig(), teleportToSpawn(), giveHotbarItems(), isSpawnItem(), getSpawnItemType()

### 2. SpawnListener.java
**Файл:** `src/main/java/me/makkuusen/timing/system/spawn/SpawnListener.java`
**Что сделано:**
- PlayerJoinEvent: телепортация на спавн + выдача предметов (с задержкой 5 тиков)
- PlayerRespawnEvent: установка respawn location на спавн + выдача предметов
- PlayerInteractEvent: обработка правого клика на хотбар-предметы с выполнением команд
- PlayerDropItemEvent: блокировка выбрасывания хотбар-предметов
- InventoryClickEvent: блокировка перемещения предметов из хотбара (разрешено перемещение внутри хотбара)
- PlayerSwapHandItemsEvent: блокировка перемещения в offhand
- Система кулдаунов:
  - /tt: 1 секунда
  - /reset: 2 секунды
  - /b: 2 секунды

### 3. TimingSystem.java
**Файл:** `src/main/java/me/makkuusen/timing/system/TimingSystem.java`
**Строки:** ~107-113
**Что сделано:**
- Добавлены импорты SpawnManager и SpawnListener
- Вызов SpawnManager.initialize() при загрузке плагина
- Регистрация SpawnListener как обработчика событий

### 4. config.yml
**Файл:** `src/main/resources/config.yml`
**Что сделано:**
- Добавлена секция `spawn` с настройками:
  - enabled: false (по умолчанию выключено)
  - world: "world"
  - x, y, z, yaw, pitch: координаты спавна

### 5. Языковые файлы
**Файлы:** `en_us.yml`, `triton.yml`
**Что сделано:**
- Добавлена секция `spawn` с ключами:
  - item_tracks_name, item_tracks_lore
  - item_reset_name, item_reset_lore
  - item_boat_name, item_boat_lore
  - cooldown

### 6. PLAN.md
**Что сделано:**
- Добавлена ФАЗА 5a: Система спавна игроков (завершена)
- Добавлена ФАЗА 6: Кастомизация автомобилей (будущее) с подразделами:
  - 6.1 Система пресетов компонентов (шины, двигатели, кузова)
  - 6.2 Система сборки автомобиля из пресетов
  - 6.3 Внутриигровая экономика (валюта, магазин)
  - 6.4 Система поломок автомобилей (износ, перегрев, повреждения)
  - 6.5 Спавн кастомного автомобиля

## Тестирование
- [x] Плагин собирается успешно (Maven, Java 21)

## Примечания
- Система спавна по умолчанию **выключена** (spawn.enabled: false) и требует ручного включения
- Предметы идентифицируются через PersistentDataContainer (NamespacedKey "timingsystem:spawn_item")
- Кулдауны хранятся в памяти (сбрасываются при перезагрузке сервера)
- CODEBASE_INDEX.md: нужно обновить при появлении файла (добавить spawn/ пакет)
