# Изменения: Система спавна игроков и хотбар-предметы

## Дата
2026-02-11

## Краткое описание
Добавлена система спавна игроков при заходе на сервер с телепортацией в настраиваемый мир и выдачей хотбар-предметов, которые выполняют команды по клику. Добавлены кулдауны на команды /reset и /b.

## Изменённые файлы

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/spawn/SpawnManager.java` — управление спавн-локацией и создание хотбар-предметов
- `src/main/java/me/makkuusen/timing/system/spawn/SpawnListener.java` — обработка событий: клик предметов, дроп, респавн, вход

#### Изменённые файлы
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — регистрация SpawnListener и инициализация SpawnManager
- `src/main/java/me/makkuusen/timing/system/commands/CommandReset.java` — добавлен кулдаун 2 сек на команду /reset
- `src/main/java/me/makkuusen/timing/system/commands/CommandBoat.java` — добавлен кулдаун 2 сек на команду /b
- `src/main/java/me/makkuusen/timing/system/TSListener.java` — очистка кулдаунов при выходе игрока
- `src/main/resources/config.yml` — добавлена секция spawn с настройками
- `src/main/resources/lang/en_us.yml` — добавлены ключи перевода для предметов
- `src/main/resources/lang/triton.yml` — добавлены Triton-обёртки для ключей предметов

### Документация
- `PLAN.md` — добавлена ФАЗА 5a (система спавна), ФАЗА 6 (кастомизация автомобилей)

## Детальное описание изменений

### 1. SpawnManager.java (утилитный класс, final)
**Файл:** `src/main/java/me/makkuusen/timing/system/spawn/SpawnManager.java`
**Что сделано:**
- Управление спавн-локацией (мир, координаты из config.yml)
- Единый метод `createSpawnItem()` для создания предметов (DRY)
- 3 хотбар-предмета с PersistentDataContainer-тегом:
  - Слот 0: NETHER_STAR (Треки — /tt)
  - Слот 1: RECOVERY_COMPASS (Сброс — /reset)
  - Слот 8: CHERRY_BOAT (Лодка — /b)
- Поддержка Triton через LanguageManager
- `isSpawnItem()` делегирует к `getSpawnItemType()` (без дублирования)
- Приватный конструктор (утилитный класс)

### 2. SpawnListener.java
**Файл:** `src/main/java/me/makkuusen/timing/system/spawn/SpawnListener.java`
**Что сделано:**
- PlayerJoinEvent: телепортация на спавн + выдача предметов (5 тиков задержки)
- PlayerRespawnEvent: respawn location на спавн + выдача предметов
- PlayerQuitEvent: очистка cooldown-мап (предотвращение утечки памяти)
- PlayerInteractEvent: правый клик → executeWithCooldown (единый метод)
- PlayerDropItemEvent: блокировка выброса
- InventoryClickEvent: разрешено перемещение в хотбаре, запрещено за пределы
- PlayerSwapHandItemsEvent: блокировка swap в offhand
- Именованные константы для всех кулдаунов и задержек

### 3. CommandReset.java (изменён)
**Что добавлено:**
- Кулдаун 2 секунды (COOLDOWN_MS = 2000)
- HashMap<UUID, Long> для отслеживания
- Метод clearCooldown(UUID) для очистки при выходе игрока
- Проверка кулдауна в начале onReset()

### 4. CommandBoat.java (изменён)
**Что добавлено:**
- Кулдаун 2 секунды (COOLDOWN_MS = 2000)
- HashMap<UUID, Long> для отслеживания
- Метод clearCooldown(UUID) для очистки при выходе игрока
- Проверка кулдауна после проверки isOnGround()

### 5. TSListener.java (изменён)
**Что добавлено:**
- Импорты CommandBoat и CommandReset
- В onPlayerLeave: вызов clearCooldown для обеих команд
- Использование локальной переменной uuid для оптимизации

### 6. config.yml
**Добавлена секция:**
```yaml
spawn:
  enabled: false
  world: "world"
  x: -33
  y: 100
  z: 1798
  yaw: 0
  pitch: 0
```

### 7. Языковые файлы (en_us.yml, triton.yml)
**Добавлена секция spawn:**
- item_tracks_name / item_tracks_lore
- item_reset_name / item_reset_lore
- item_boat_name / item_boat_lore
- cooldown

## Качество кода
- ✅ DRY: единые методы для создания предметов и обработки кулдаунов
- ✅ Нет утечек памяти: все Map<UUID> очищаются при PlayerQuitEvent
- ✅ Именованные константы: HOTBAR_MAX_SLOT, JOIN_DELAY_TICKS, COOLDOWN_MS и др.
- ✅ Final class + private constructor для утилитных классов
- ✅ Early return паттерн
- ✅ JavaDoc для публичных методов
- ✅ Нет неиспользуемых импортов

## Тестирование
- [x] Плагин собирается успешно (Maven, Java 21)
- [x] CodeQL: 0 уязвимостей

## Примечания
- Система спавна по умолчанию **выключена** (spawn.enabled: false)
- Предметы идентифицируются через PersistentDataContainer (NamespacedKey "timingsystem:spawn_item")
- Двойная защита кулдаунами: на предметах (SpawnListener) + на командах (CommandReset/CommandBoat)
- CODEBASE_INDEX.md: нужно обновить при появлении файла
