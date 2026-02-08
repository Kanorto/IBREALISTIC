# Индекс файлов проекта IBREALISTIC

## Мод: OpenBoatUtilsRealistic

### Корневые файлы
| Файл | Назначение |
|------|-----------|
| `build.gradle` | Конфигурация сборки Gradle |
| `settings.gradle` | Настройки проекта Gradle |
| `gradle.properties` | Свойства Gradle (версия мода, MC, Fabric) |
| `stonecutter.gradle` | Мультиверсионная сборка через Stonecutter |
| `LICENSE.txt` | Лицензия |
| `README.md` | Описание мода |
| `CONTRIBUTING.md` | Руководство для контрибьюторов |

### Пакет: `dev.o7moon.openboatutils`
| Файл | Назначение |
|------|-----------|
| `OpenBoatUtils.java` | Главный класс мода, точка входа (ModInitializer), глобальное состояние |
| `ClientboundPackets.java` | Обработка пакетов от сервера (50 типов) |
| `ServerboundPackets.java` | Пакеты от клиента к серверу (VERSION) |
| `Modes.java` | Enum предустановленных режимов (31 режим) |
| `CollisionMode.java` | Enum режимов столкновений (5 вариантов) |
| `GetStepHeight.java` | Интерфейс для получения высоты шага |
| `ISettingContext.java` | Интерфейс контекста настроек |
| `SingleplayerCommands.java` | Команды для одиночной игры (50+ команд) |

### Пакет: `dev.o7moon.openboatutils.client`
| Файл | Назначение |
|------|-----------|
| `OpenBoatUtilsClient.java` | Клиентская инициализация (ClientModInitializer) |

### Пакет: `dev.o7moon.openboatutils.physics`
| Файл | Назначение |
|------|-----------|
| `RealisticPhysicsEngine.java` | Основной движок Bicycle Model, состояние и расчёт физики |
| `VehicleConfig.java` | Конфигурация машины (масса, база, развесовка и т.д.) |
| `VehicleType.java` | Enum пресетов машин (WRC_CAR, GROUP_B, CLASSIC_RALLY, LIGHTWEIGHT, TRUCK) |
| `DrivetrainType.java` | Enum типов привода (RWD, FWD, AWD) |
| `TireModel.java` | Модель шин Fiala/Brush (slip angle, lateral/longitudinal forces) |
| `SurfaceProperties.java` | Свойства поверхностей (8 пресетов + маппинг блоков) |

### Пакет: `dev.o7moon.openboatutils.mixin`
| Файл | Назначение |
|------|-----------|
| `BoatMixin.java` | Основной миксин для BoatEntity (MC 1.20.4, 1.21) |
| `AbstractBoatMixin.java` | Миксин для AbstractBoatEntity (MC 1.21.3) |
| `ServerPlayNetworkHandlerMixin.java` | Отключение "moved wrongly" проверок |
| `EntityMixin.java` | Модификация высоты шага и проверки земли |
| `ClientWorldMixin.java` | Хук загрузки мира |

### Ресурсы
| Файл | Назначение |
|------|-----------|
| `fabric.mod.json` | Метаданные мода для Fabric |
| `openboatutils.mixins.json` | Конфигурация миксинов |
| `openboatutils.mixins.json5` | Расширенная конфигурация миксинов |

### Версионные файлы
| Файл | Назначение |
|------|-----------|
| `versions/1.21/gradle.properties` | Свойства для MC 1.21 |
| `versions/1.21.3/gradle.properties` | Свойства для MC 1.21.3 |
| `versions/1.21.3/src/.../AbstractBoatMixin.java` | Миксин для 1.21.3 |

---

## Плагин: TimingSystem

### Корневые файлы
| Файл | Назначение |
|------|-----------|
| `pom.xml` | Конфигурация Maven |
| `LICENSE.txt` | Лицензия |
| `README.md` | Описание плагина |

### Пакет: `me.makkuusen.timing.system`
| Файл | Назначение |
|------|-----------|
| `TimingSystem.java` | Главный класс плагина (JavaPlugin), точка входа |
| `TimingSystemConfiguration.java` | Загрузка и управление конфигурацией |
| `ApiUtilities.java` | Статические утилиты (форматирование, регионы, лодки) |
| `Tasks.java` | Асинхронные задачи (частицы, таймеры) |
| `TSListener.java` | Основной обработчик событий |
| `PluginMessageReceiver.java` | Получение plugin messages |
| `ContextResolvers.java` | Резолверы для ACF команд |
| `LanguageManager.java` | Менеджер локализации |
| `ItemBuilder.java` | Fluent builder для ItemStack |
| `LeaderboardManager.java` | Менеджер таблиц лидеров |
| `ReadyCheckManager.java` | Менеджер готовности |
| `TrackTagManager.java` | CRUD для тегов трасс |
| `PlayerRegionData.java` | Данные регионов игроков |

### Пакет: `me.makkuusen.timing.system.boatutils`
| Файл | Назначение |
|------|-----------|
| `BoatUtilsManager.java` | Менеджер режимов BoatUtils для игроков |
| `BoatUtilsMode.java` | Enum стандартных режимов (31 шт.) |
| `CustomBoatUtilsMode.java` | DTO кастомных режимов с отправкой пакетов |
| `NonDefaultSetting.java` | Record для отображения нестандартных настроек |
| `PerBlockSetting.java` | Настройки на уровне блоков |
| `NocolManager.java` | Менеджер коллизий (nocol) |

### Пакет: `me.makkuusen.timing.system.commands`
| Файл | Назначение |
|------|-----------|
| `CommandTimingSystem.java` | Команды системы (теги, цвета, скорборд) |
| `CommandTrack.java` | Команды трасс (инфо, TP, TT, лидерборд) |
| `CommandTrackEdit.java` | Редактирование трасс (регионы, локации, настройки) |
| `CommandHeat.java` | Управление заездами (setup, drivers, results) |
| `CommandRound.java` | Управление раундами |
| `CommandRace.java` | Быстрые гонки (создание, join/leave, старт) |
| `CommandEvent.java` | Управление событиями |
| `CommandTeam.java` | Управление командами |
| `CommandTimeTrial.java` | Команда телепортации на тайм-трайл |
| `CommandTimeTrialRandom.java` | Случайная трасса |
| `CommandTimeTrialCancel.java` | Отмена тайм-трайла |
| `CommandBoat.java` | Спавн лодок |
| `CommandBoatUtilsModeEdit.java` | Редактирование кастомных режимов |
| `CommandGhost.java` | Включение невидимости |
| `CommandUnghost.java` | Выключение невидимости |
| `CommandSettings.java` | Настройки игрока |
| `CommandReset.java` | Сброс позиции |

### Пакет: `me.makkuusen.timing.system.track`
| Файл | Назначение |
|------|-----------|
| `Track.java` | Основной класс трассы |
| `TimeTrials.java` | Управление попытками и финишами тайм-трайла |

### Пакет: `me.makkuusen.timing.system.track.editor`
| Файл | Назначение |
|------|-----------|
| `TrackEditor.java` | Редактор трасс (CRUD операции) |
| `TrackMove.java` | Перемещение трасс |
| `RegionEditor.java` | Редактор регионов |
| `LocationEditor.java` | Редактор локаций |

### Пакет: `me.makkuusen.timing.system.track.regions`
| Файл | Назначение |
|------|-----------|
| `TrackRegion.java` | Абстрактный класс региона (10 типов) |
| `TrackRegions.java` | Коллекция регионов трассы |
| `TrackCuboidRegion.java` | Кубоидный регион |
| `TrackPolyRegion.java` | Полигональный регион |

### Пакет: `me.makkuusen.timing.system.track.locations`
| Файл | Назначение |
|------|-----------|
| `TrackLocation.java` | Локация на трассе (5 типов) |
| `TrackLocations.java` | Коллекция локаций |
| `TrackLeaderboard.java` | Таблица лидеров трассы |

### Пакет: `me.makkuusen.timing.system.track.tags`
| Файл | Назначение |
|------|-----------|
| `TrackTag.java` | Тег трассы |
| `TrackTags.java` | Коллекция тегов |

### Пакет: `me.makkuusen.timing.system.track.options`
| Файл | Назначение |
|------|-----------|
| `TrackOption.java` | Опция трассы |
| `TrackOptions.java` | Коллекция опций |

### Пакет: `me.makkuusen.timing.system.track.medals`
| Файл | Назначение |
|------|-----------|
| `TrackMedals.java` | Система медалей трассы |
| `TrackMedalsData.java` | Данные медалей |
| `Medals.java` | Enum типов медалей |
| `DynamicPos.java` | Динамическая позиция |

### Пакет: `me.makkuusen.timing.system.track.holograms`
| Файл | Назначение |
|------|-----------|
| `HologramManager.java` | Интерфейс менеджера голограмм |
| `HologramDH.java` | Реализация для DecentHolograms |
| `HologramHD.java` | Реализация для HolographicDisplays |

### Пакет: `me.makkuusen.timing.system.heat`
| Файл | Назначение |
|------|-----------|
| `Heat.java` | Основной класс заезда |
| `HeatState.java` | Enum состояний заезда (SETUP, LOADED, STARTING, RACING, FINISHED) |
| `QualifyHeat.java` | Логика квалификационного заезда |
| `FinalHeat.java` | Логика финального заезда |
| `Lap.java` | Круг в заезде |
| `CollisionMode.java` | Режимы коллизий для заездов |
| `GridManager.java` | Менеджер стартовой решётки |
| `DriverScoreboard.java` | Скорборд для гонщика |
| `SpectatorScoreboard.java` | Скорборд для зрителей |
| `ScoreboardUtils.java` | Утилиты скорборда |
| `ReadyCheck.java` | Проверка готовности перед заездом |
| `DriverSwapHandler.java` | Обработка смены гонщика |
| `TeamHeatEntry.java` | Запись команды в заезде |

### Пакет: `me.makkuusen.timing.system.round`
| Файл | Назначение |
|------|-----------|
| `Round.java` | Абстрактный класс раунда |
| `RoundType.java` | Enum типов раунда (QUALIFICATION, FINAL) |
| `QualificationRound.java` | Квалификационный раунд |
| `FinalRound.java` | Финальный раунд |

### Пакет: `me.makkuusen.timing.system.event`
| Файл | Назначение |
|------|-----------|
| `Event.java` | Основной класс события |
| `EventSchedule.java` | Расписание раундов события |
| `EventCountdown.java` | Обратный отсчёт до события |
| `EventResults.java` | Результаты события |
| `EventAnnouncements.java` | Объявления события |

### Пакет: `me.makkuusen.timing.system.participant`
| Файл | Назначение |
|------|-----------|
| `Participant.java` | Абстрактный класс участника |
| `Driver.java` | Гонщик (управление кругами, позицией, состоянием) |
| `DriverState.java` | Enum состояний гонщика |
| `Spectator.java` | Зритель |
| `Subscriber.java` | Подписчик события |
| `Streaker.java` | Стрикер (свободный гонщик) |

### Пакет: `me.makkuusen.timing.system.timetrial`
| Файл | Назначение |
|------|-----------|
| `TimeTrial.java` | Основной класс тайм-трайла |
| `TimeTrialSession.java` | Сессия тайм-трайла игрока |
| `TimeTrialAttempt.java` | Попытка тайм-трайла |
| `TimeTrialFinish.java` | Финиш тайм-трайла |
| `TimeTrialController.java` | Контроллер тайм-трайлов (static maps) |
| `TimeTrialListener.java` | Слушатель событий тайм-трайла |
| `TimeTrialScoreboard.java` | Скорборд тайм-трайла |
| `TimeTrialDateComparator.java` | Сортировка по дате |
| `TimeTrialFinishComparator.java` | Сортировка по времени |

### Пакет: `me.makkuusen.timing.system.database`
| Файл | Назначение |
|------|-----------|
| `TSDatabase.java` | Основной интерфейс БД (игроки) |
| `TrackDatabase.java` | Интерфейс БД трасс |
| `EventDatabase.java` | Интерфейс БД событий |
| `TeamDatabase.java` | Интерфейс БД команд |
| `LogDatabase.java` | Интерфейс БД логов |
| `SQLiteDatabase.java` | Реализация SQLite |
| `MySQLDatabase.java` | Реализация MySQL |
| `MariaDBDatabase.java` | Реализация MariaDB |

### Пакет: `me.makkuusen.timing.system.database.updates`
| Файл | Назначение |
|------|-----------|
| `Version2.java` — `Version13.java` | 12 файлов миграций схемы БД (v2 — v13) |

### Пакет: `me.makkuusen.timing.system.gui`
| Файл | Назначение |
|------|-----------|
| `BaseGui.java` | Базовый класс GUI |
| `GuiCommon.java` | Общие элементы GUI |
| `GuiButton.java` | Кнопка GUI |
| `GUIListener.java` | Слушатель кликов GUI |
| `TrackGui.java` | Меню выбора трассы |
| `TrackPageGui.java` | Постраничный список трасс |
| `TimeTrialGui.java` | Меню тайм-трайла |
| `SettingsGui.java` | Меню настроек |
| `BoatSettingsGui.java` | Настройки лодки |
| `ColorSettingsGui.java` | Настройки цветов |
| `FilterGui.java` | Фильтр трасс |
| `TrackFilter.java` | Логика фильтрации |
| `TrackSort.java` | Логика сортировки |

### Пакет: `me.makkuusen.timing.system.api`
| Файл | Назначение |
|------|-----------|
| `TimingSystemAPI.java` | Основной публичный API |
| `QuickRaceAPI.java` | API для быстрых гонок |
| `EventResultsAPI.java` | API результатов событий |
| `DriverDetails.java` | DTO деталей гонщика |

### Пакет: `me.makkuusen.timing.system.api.event`
| Файл | Назначение |
|------|-----------|
| `EventResult.java` | Результат события |
| `DriverResult.java` | Результат гонщика |
| `RoundResult.java` | Результат раунда |
| `HeatResult.java` | Результат заезда |
| `LapResult.java` | Результат круга |

### Пакет: `me.makkuusen.timing.system.api.events`
| Файл | Назначение |
|------|-----------|
| `HeatFinishEvent.java` | Событие завершения заезда |
| `BoatSpawnEvent.java` | Событие спавна лодки (Cancellable) |
| `TimeTrialStartEvent.java` | Событие начала тайм-трайла |
| `TimeTrialFinishEvent.java` | Событие завершения тайм-трайла |
| `TimeTrialAttemptEvent.java` | Событие попытки тайм-трайла |
| `GuiOpenEvent.java` | Событие открытия GUI (Cancellable) |
| `BoatUtilsAppliedEvent.java` | Событие применения режима BoatUtils |

### Пакет: `me.makkuusen.timing.system.api.events.driver`
| Файл | Назначение |
|------|-----------|
| `DriverStartEvent.java` | Старт гонщика |
| `DriverFinishLapEvent.java` | Завершение круга |
| `DriverFinishHeatEvent.java` | Финиш гонщика в заезде |
| `DriverPassCheckpointEvent.java` | Прохождение чекпоинта |
| `DriverPassPitEvent.java` | Прохождение питстопа |
| `DriverDisqualifyEvent.java` | Дисквалификация |
| `DriverNewLapEvent.java` | Начало нового круга |
| `DriverSwapEvent.java` | Смена гонщика |
| `DriverScoreboardTitleUpdateEvent.java` | Обновление заголовка скорборда гонщика |
| `SpectatorScoreboardTitleUpdateEvent.java` | Обновление заголовка скорборда зрителя |
| `DriverPlacedOnGrid.java` | Размещение на стартовой решётке |

### Прочие пакеты
| Пакет / Файл | Назначение |
|------|-----------|
| `permissions/Permissions.java` | Интерфейс разрешений |
| `permissions/PermissionTimingSystem.java` | Разрешения системы |
| `permissions/PermissionTrack.java` | Разрешения трасс |
| `permissions/PermissionTrackEdit.java` | Разрешения редактирования |
| `permissions/PermissionHeat.java` | Разрешения заездов |
| `permissions/PermissionRound.java` | Разрешения раундов |
| `permissions/PermissionRace.java` | Разрешения гонок |
| `permissions/PermissionEvent.java` | Разрешения событий |
| `permissions/PermissionTeam.java` | Разрешения команд |
| `permissions/PermissionTimeTrial.java` | Разрешения тайм-трайлов |
| `permissions/PermissionBoatUtilsMode.java` | Разрешения режимов BoatUtils |
| `theme/Theme.java` | Система тем (цвета) |
| `theme/Text.java` | Отправка сообщений |
| `theme/TSColor.java` | Enum цветов темы |
| `theme/MessageParser.java` | Парсер сообщений |
| `theme/messages/Message.java` | Базовый класс сообщения |
| `theme/messages/Info.java` | Информационное сообщение |
| `theme/messages/Warning.java` | Предупреждение |
| `theme/messages/Error.java` | Ошибка |
| `theme/messages/Success.java` | Успех |
| `theme/messages/Broadcast.java` | Широковещательное |
| `theme/messages/Word.java` | Слово |
| `theme/messages/ActionBar.java` | Action bar |
| `theme/messages/TextButton.java` | Текстовая кнопка |
| `theme/messages/Hover.java` | Hover-текст |
| `theme/messages/Gui.java` | GUI-текст |
| `theme/messages/ScoreBoard.java` | Текст скорборда |
| `theme/messages/MessageNoColor.java` | Бесцветное сообщение |
| `tplayer/TPlayer.java` | Данные игрока TimingSystem |
| `tplayer/Settings.java` | Настройки игрока |
| `team/Team.java` | Команда |
| `team/TeamManager.java` | Менеджер команд |
| `drs/DrsManager.java` | Система DRS |
| `loneliness/LonelinessController.java` | Контроллер ghosting |
| `loneliness/DeltaGhostingController.java` | Контроллер delta ghosting |
| `boat/BoatSpawnManager.java` | Менеджер спавна лодок |
| `boat/BoatSpawner.java` | Интерфейс спавнера лодок |
| `boat/DefaultBoatSpawner.java` | Реализация спавнера (8 типов дерева) |
| `listeners/GSitListener.java` | Слушатель GSit |
| `listeners/DriverSwapListener.java` | Слушатель смены гонщика |
| `listeners/ReadyCheckListener.java` | Слушатель готовности |
| `listeners/DrsListener.java` | Слушатель DRS |
| `logger/LogEntry.java` | Запись лога |
| `logger/LogEntryBuilder.java` | Builder для логов |
| `network/UUIDFetcher.java` | Получение UUID по нику |
| `network/UUIDFetcherCallback.java` | Callback для UUIDFetcher |
| `sounds/PlaySound.java` | Воспроизведение звуков |
| `papi/TimingSystemPlaceholder.java` | Интеграция с PlaceholderAPI |

### Ресурсы плагина
| Файл | Назначение |
|------|-----------|
| `plugin.yml` | Метаданные плагина |
| `config.yml` | Основная конфигурация |
| `lang/en_us.yml` | Английская локализация |
| `lang/de_de.yml` | Немецкая локализация |
| `lang/zh_cn.yml` | Китайская локализация |
| `lang/id_id.yml` | Индонезийская локализация |

---

## Документация (корень репозитория)
| Файл | Назначение |
|------|-----------|
| `README.md` | Основное описание проекта |
| `PLAN.md` | План реализации (чек-лист фаз) |
| `DOCS_REALISTIC_PHYSICS.md` | Документация по физике |
| `CHANGES_copilot_instructions.md` | Описание изменений инструкций |
