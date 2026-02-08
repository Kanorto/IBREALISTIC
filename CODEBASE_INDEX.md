# Кодовая база проекта IBREALISTIC

Последнее обновление: 2026-02-08

Полный индекс кодовой базы проекта расположен в каталоге [`codebase/`](codebase/).

Каждый аспект кодовой базы вынесен в отдельный файл для удобной навигации и категоризации.

## Структура индекса

| # | Файл | Описание |
|---|------|----------|
| — | [codebase/README.md](codebase/README.md) | Обзор и навигация |
| 01 | [01_FILE_INDEX.md](codebase/01_FILE_INDEX.md) | **Индекс файлов** — полный список всех файлов проекта с назначением |
| 02 | [02_MOD_PHYSICS.md](codebase/02_MOD_PHYSICS.md) | **Физический движок** — RealisticPhysicsEngine, TireModel, VehicleConfig, SurfaceProperties |
| 03 | [03_MOD_CORE.md](codebase/03_MOD_CORE.md) | **Ядро мода** — OpenBoatUtils, ClientboundPackets, Modes, SingleplayerCommands |
| 04 | [04_MOD_MIXINS.md](codebase/04_MOD_MIXINS.md) | **Миксины** — BoatMixin, AbstractBoatMixin, EntityMixin и др. |
| 05 | [05_PLUGIN_BOATUTILS.md](codebase/05_PLUGIN_BOATUTILS.md) | **BoatUtils интеграция** — CustomBoatUtilsMode, BoatUtilsManager, пакеты |
| 06 | [06_PLUGIN_CORE.md](codebase/06_PLUGIN_CORE.md) | **Ядро плагина** — TimingSystem, конфигурация, утилиты |
| 07 | [07_PLUGIN_TRACK.md](codebase/07_PLUGIN_TRACK.md) | **Трассы** — Track, регионы, локации, медали, голограммы |
| 08 | [08_PLUGIN_RACING.md](codebase/08_PLUGIN_RACING.md) | **Гоночная система** — Heat, Round, Event, Driver, TimeTrial |
| 09 | [09_PLUGIN_COMMANDS.md](codebase/09_PLUGIN_COMMANDS.md) | **Команды** — все 17 command-классов с субкомандами |
| 10 | [10_PLUGIN_DATABASE.md](codebase/10_PLUGIN_DATABASE.md) | **База данных** — интерфейсы, реализации, миграции |
| 11 | [11_PLUGIN_API.md](codebase/11_PLUGIN_API.md) | **API** — TimingSystemAPI, события, результаты |
| 12 | [12_PLUGIN_GUI_THEME.md](codebase/12_PLUGIN_GUI_THEME.md) | **GUI и темы** — меню, цвета, звуки, скорборды |
| 13 | [13_PLUGIN_MISC.md](codebase/13_PLUGIN_MISC.md) | **Прочее** — DRS, ghosting, permissions, teams, listeners |
| 14 | [14_CONNECTIONS.md](codebase/14_CONNECTIONS.md) | **Связи** — потоки данных, протокол пакетов, зависимости файлов |
| 15 | [15_EXTENSION_POINTS.md](codebase/15_EXTENSION_POINTS.md) | **Точки расширения** — как добавить новую фичу |

## Проекты

- **OpenBoatUtilsRealistic** — мод для клиента (Fabric), физика движения, ~20 Java-файлов
- **TimingSystem** — серверный плагин (Paper), управление гонками, ~180 Java-файлов

## Ключевые параметры

- Версия протокола: **19** (`OpenBoatUtils.VERSION`)
- Minecraft: 1.20.4, 1.21, 1.21.3
- Java: 21
- Канал пакетов: `openboatutils:settings`
- Типов пакетов: **50** (от RESET до SET_DEFAULT_SURFACE_TYPE)
