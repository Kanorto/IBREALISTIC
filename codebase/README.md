# Кодовая база проекта IBREALISTIC

Последнее обновление: 2026-02-08

Этот каталог содержит полный индекс кодовой базы проекта, разделённый на категории для удобной навигации.

## Структура файлов

| Файл | Описание |
|------|----------|
| [01_FILE_INDEX.md](01_FILE_INDEX.md) | Полный индекс всех файлов проекта с описанием назначения каждого файла |
| [02_MOD_PHYSICS.md](02_MOD_PHYSICS.md) | Физический движок мода: классы, методы, поля, константы |
| [03_MOD_CORE.md](03_MOD_CORE.md) | Ядро мода: OpenBoatUtils, пакеты, режимы, команды |
| [04_MOD_MIXINS.md](04_MOD_MIXINS.md) | Миксины мода: BoatMixin, AbstractBoatMixin, EntityMixin и др. |
| [05_PLUGIN_BOATUTILS.md](05_PLUGIN_BOATUTILS.md) | Интеграция плагина с BoatUtils: режимы, пакеты, команды |
| [06_PLUGIN_CORE.md](06_PLUGIN_CORE.md) | Ядро плагина: TimingSystem, конфигурация, утилиты |
| [07_PLUGIN_TRACK.md](07_PLUGIN_TRACK.md) | Система трасс: Track, регионы, локации, медали, голограммы |
| [08_PLUGIN_RACING.md](08_PLUGIN_RACING.md) | Гоночная система: Heat, Round, Event, Driver, TimeTrial |
| [09_PLUGIN_COMMANDS.md](09_PLUGIN_COMMANDS.md) | Все команды плагина с описанием субкоманд |
| [10_PLUGIN_DATABASE.md](10_PLUGIN_DATABASE.md) | Слой базы данных: интерфейсы, реализации, миграции |
| [11_PLUGIN_API.md](11_PLUGIN_API.md) | Публичный API плагина: события, результаты, интеграция |
| [12_PLUGIN_GUI_THEME.md](12_PLUGIN_GUI_THEME.md) | GUI-меню, темы сообщений, цвета, звуки |
| [13_PLUGIN_MISC.md](13_PLUGIN_MISC.md) | Прочее: DRS, ghosting, permissions, listeners, team |
| [14_CONNECTIONS.md](14_CONNECTIONS.md) | Связи между компонентами: потоки данных, протокол пакетов |
| [15_EXTENSION_POINTS.md](15_EXTENSION_POINTS.md) | Точки расширения: как добавить новую фичу |
| [SERVER_GUIDE.md](SERVER_GUIDE.md) | **Гайд по серверу** — создание трасс, чекпоинты, реалистичный режим, гонки |

## Проекты

- **OpenBoatUtilsRealistic** — мод для клиента (Fabric), физика движения
- **TimingSystem** — серверный плагин (Paper), управление гонками

## Версии

- Протокол: **19** (`OpenBoatUtils.VERSION`)
- Minecraft: 1.20.4, 1.21, 1.21.3 (через Stonecutter)
- Java: 21
