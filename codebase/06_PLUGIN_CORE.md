# Ядро плагина TimingSystem

Пакет: `me.makkuusen.timing.system`

---

## TimingSystem.java

**Назначение:** Главный класс плагина (extends `JavaPlugin`). Точка входа.

### Поля
| Имя | Тип | Описание |
|-----|-----|----------|
| `plugin` | `TimingSystem` | (static @Getter) Singleton |
| `database` | `TSDatabase` | (static @Getter) Интерфейс БД |
| `eventDatabase` | `EventDatabase` | (static @Getter) БД событий |
| `trackDatabase` | `TrackDatabase` | (static @Getter) БД трасс |
| `logDatabase` | `LogDatabase` | (static @Getter) БД логов |
| `teamDatabase` | `TeamDatabase` | (static @Getter) БД команд |
| `configuration` | `TimingSystemConfiguration` | (static) Конфигурация |
| `players` | `Map<UUID, TPlayer>` | (static) Кеш игроков |
| `languageManager` | `LanguageManager` | (static @Getter) Менеджер локализации |
| `scoreboardLibrary` | `ScoreboardLibrary` | (static) Библиотека скорбордов |
| `defaultTheme` | `Theme` | (static) Тема по умолчанию |

### Методы
| Метод | Описание |
|-------|----------|
| `onEnable()` | Инициализация: БД, команды, слушатели, задачи |
| `onDisable()` | Выключение: сохранение данных, закрытие БД |
| `newChain()` | (static) Создание TaskChain для async операций |

---

## TimingSystemConfiguration.java

**Назначение:** Загрузка и управление конфигурацией из config.yml.

Содержит getter/setter методы для всех параметров конфигурации (сервер, DRS, гонки, скорборды).

---

## ApiUtilities.java

**Назначение:** Статические утилиты общего назначения.

### Категории методов
- **Форматирование времени** — преобразование миллисекунд в читаемый формат
- **Работа с локациями** — копирование, сравнение, вычисления
- **Работа с регионами** — проверка попадания, создание
- **Работа с лодками** — спавн, определение типа
- **Строковые утилиты** — нормализация имён, парсинг

---

## Tasks.java

**Назначение:** Асинхронные повторяющиеся задачи.

- Обновление скорбордов
- Рендеринг частиц регионов
- Подсчёт общего времени
- Периодическая синхронизация

---

## TSListener.java

**Назначение:** Основной слушатель событий Minecraft.

Обрабатывает: движение игрока, взаимодействие, смерть, вход/выход, смена мира, чат.

---

## PluginMessageReceiver.java

**Назначение:** Получение plugin messages от клиента. Implements `PluginMessageListener`.

Передаёт сообщения в `BoatUtilsManager.pluginMessageListener()`.

---

## ContextResolvers.java

**Назначение:** Резолверы для ACF (Annotation Command Framework).

Регистрирует: контекст TPlayer, Track, Heat, Event, Round, BoatUtilsMode, и автодополнение для всех типов.

---

## LanguageManager.java

**Назначение:** Менеджер локализации. Загрузка yml файлов из lang/.

Поддерживаемые языки: en_us, de_de, zh_cn, id_id.

---

## ItemBuilder.java

**Назначение:** Fluent builder для создания ItemStack.

### Методы
- `type(Material)` — материал
- `name(String/Component)` — имя
- `lore(List)` — описание
- `amount(int)` — количество
- `enchant(Enchantment, int)` — зачарование
- `build()` — создание ItemStack

---

## LeaderboardManager.java

**Назначение:** Менеджер таблиц лидеров (голограммы).

### Методы
- `updateLeaderboards()` — обновление всех лидербордов
- `removeLeaderboards()` — удаление всех

---

## ReadyCheckManager.java

**Назначение:** Менеджер проверок готовности перед заездами.

---

## TrackTagManager.java

**Назначение:** CRUD операции для тегов трасс.

---

## PlayerRegionData.java

**Назначение:** Кеш данных регионов для каждого игрока (последний регион, последний чекпоинт).
