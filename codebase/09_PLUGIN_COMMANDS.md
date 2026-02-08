# Команды плагина TimingSystem

Пакет: `me.makkuusen.timing.system.commands`

Все команды используют ACF (Annotation Command Framework) и extends `BaseCommand`.

---

## CommandTimingSystem.java
**Alias:** `/ts`
| Субкоманда | Описание |
|------------|----------|
| `tag create/delete/set` | Управление тегами трасс |
| `color set` | Установка цвета |
| `scoreboard set` | Настройки скорборда |
| `drs set` | Настройки DRS |

## CommandTrack.java
**Alias:** `/track`
| Субкоманда | Описание |
|------------|----------|
| `info` | Информация о трассе |
| `tp` | Телепортация на трассу |
| `tt` | Тайм-трайл |
| `leaderboard` | Таблица лидеров |
| `session` | Данные сессии |
| `delete` | Удаление результатов |
| `reload` | Перезагрузка |

## CommandTrackEdit.java
**Alias:** `/trackedit`
| Субкоманда | Описание |
|------------|----------|
| `create` | Создание трассы |
| `select` | Выбор трассы |
| `name/item/spawn/type` | Основные настройки |
| `region` | Управление регионами |
| `location` | Управление локациями |
| `tag/option` | Теги и опции |
| `boatutilsmode` | Режим BoatUtils |
| `owner/contributor` | Управление доступом |
| `open/close/timetrial` | Статус трассы |
| `move/delete` | Перемещение/удаление |

## CommandHeat.java
**Alias:** `/heat`
| Субкоманда | Описание |
|------------|----------|
| `setup` | Настройка заезда |
| `start/finish/reset` | Управление заездом |
| `add/remove/swap` | Управление гонщиками |
| `settings` | Настройки (laps, pits, time, etc.) |
| `results` | Результаты |

## CommandRound.java
**Alias:** `/round`
| Субкоманда | Описание |
|------------|----------|
| `create` | Создание раунда |
| `fill` | Заполнение гонщиками |
| `results` | Результаты |

## CommandRace.java
**Alias:** `/race`
| Субкоманда | Описание |
|------------|----------|
| `create` | Создание быстрой гонки |
| `join/leave` | Присоединение/выход |
| `start/end` | Старт/конец |

## CommandEvent.java
**Alias:** `/event`
| Субкоманда | Описание |
|------------|----------|
| `create/delete` | CRUD событий |
| `sign/unsign` | Подписка |
| `reserve/unreserve` | Резерв |
| `spectate` | Наблюдение |
| `start/finish` | Управление |

## CommandTeam.java
**Alias:** `/team`
| Субкоманда | Описание |
|------------|----------|
| `create/delete` | CRUD команд |
| `add/remove` | Управление участниками |
| `list/info` | Информация |

## CommandTimeTrial.java
**Alias:** `/tt`
Телепортация на трассу для тайм-трайла.

## CommandTimeTrialRandom.java
**Alias:** `/ttr`
Телепортация на случайную трассу.

## CommandTimeTrialCancel.java
**Alias:** `/ttcancel`
Отмена текущего тайм-трайла.

## CommandBoat.java
**Alias:** `/boat`
Спавн лодки с режимом.

## CommandBoatUtilsModeEdit.java
**Alias:** `/boatutilsmode`
| Субкоманда | Описание |
|------------|----------|
| `create` | Создание кастомного режима |
| `edit` | Редактирование существующего |
| `save` | Сохранение |
| `set` | Установка свойства |
| `addblockslip` | Добавление скользкости блоков |
| `clearblockslip` | Очистка скользкости |
| `addperblock` | Per-block настройка |
| `clearperblock` | Очистка per-block |
| `addsurfacetype` | Добавление типа поверхности |
| `clearsurfacetypes` | Очистка типов поверхности |
| `reset` | Сброс к ванильным |
| `info` | Информация о режиме |

## CommandGhost.java / CommandUnghost.java
**Alias:** `/ghost`, `/unghost`
Включение/выключение невидимости в заездах.

## CommandSettings.java
**Alias:** `/settings`
| Субкоманда | Описание |
|------------|----------|
| `shortname` | Короткое имя |
| `sound` | Звуки |
| `verbose` | Подробный вывод |
| `timetrial` | Тайм-трайл уведомления |
| `override` | Переопределение |
| `compactscoreboard` | Компактный скорборд |
| `color` | Настройки цвета |

## CommandReset.java
**Alias:** `/reset`
Сброс позиции гонщика в заезде.
