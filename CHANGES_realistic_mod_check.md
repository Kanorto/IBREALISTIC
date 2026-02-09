# Изменения: Проверка наличия OpenBoatUtilsRealistic мода при входе на сервер

## Дата
2026-02-09

## Краткое описание
Добавлена система обнаружения и предупреждения игроков, которые заходят на сервер без модифицированной версии OpenBoatUtils (OpenBoatUtilsRealistic). Игрокам без мода каждые 60 секунд отправляется сообщение с предупреждением и ссылкой на скачивание.

## Изменённые файлы

### Мод (OpenBoatUtilsRealistic)
- `src/main/java/dev/o7moon/openboatutils/OpenBoatUtils.java` — добавлен флаг `realistic` (boolean true) в пакет версии

### Плагин (TimingSystem)
- `src/main/java/me/makkuusen/timing/system/tplayer/TPlayer.java` — добавлено поле `hasRealisticMod` с Lombok аннотациями
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsManager.java` — добавлена система предупреждений и чтение realistic-идентификатора
- `src/main/java/me/makkuusen/timing/system/TSListener.java` — добавлен сброс состояния и запуск задачи предупреждений при входе

## Детальное описание изменений

### 1. Идентификатор Realistic мода
**Файл:** `OpenBoatUtilsRealistic/.../OpenBoatUtils.java`
**Строка:** 283
**Что сделано:**
- В метод `sendVersionPacket()` добавлена отправка `boolean true` после VERSION
- Это позволяет серверу отличить нашу модифицированную версию от обычного OpenBoatUtils

**Формат пакета версии (до):**
```
[short: packetID=0] [int: VERSION]
```

**Формат пакета версии (после):**
```
[short: packetID=0] [int: VERSION] [boolean: isRealistic=true]
```

### 2. Поле hasRealisticMod в TPlayer
**Файл:** `TimingSystem/.../tplayer/TPlayer.java`
**Строки:** 50-52
**Что сделано:**
- Добавлено поле `private boolean hasRealisticMod = false` с аннотациями `@Getter` и `@Setter`
- По умолчанию `false` — игрок считается без realistic мода до получения подтверждения

### 3. Система предупреждений в BoatUtilsManager
**Файл:** `TimingSystem/.../boatutils/BoatUtilsManager.java`
**Что сделано:**
- Добавлены константы `INITIAL_CHECK_DELAY_TICKS` (10 сек), `WARNING_INTERVAL_TICKS` (60 сек), `REALISTIC_MOD_DOWNLOAD_URL`
- Добавлена карта `realisticModWarningTasks` для хранения задач предупреждений
- В `pluginMessageListener()` добавлено чтение boolean-флага realistic из пакета версии (с обратной совместимостью)
- Добавлен метод `startRealisticModWarningTask(Player)` — запускает периодическую проверку
- Добавлен метод `cancelRealisticModWarning(UUID)` — отменяет задачу при обнаружении мода или выходе
- Добавлен метод `sendRealisticModWarning(Player)` — отправляет красивое форматированное предупреждение
- В `clearPlayerModes()` добавлена отмена задачи предупреждения

### 4. Инициализация при входе в TSListener
**Файл:** `TimingSystem/.../TSListener.java`
**Что сделано:**
- При входе игрока сбрасываются `hasRealisticMod` и `boatUtilsVersion`
- Запускается задача `startRealisticModWarningTask(player)`

## Архитектура решения

### Поток данных
```
Игрок заходит на сервер
  → TSListener.onPlayerJoin()
    → Сброс hasRealisticMod = false, boatUtilsVersion = null
    → Запуск startRealisticModWarningTask() (первая проверка через 10 сек)
    
Если мод установлен:
  → Мод отправляет VERSION + true
    → BoatUtilsManager.pluginMessageListener()
      → tPlayer.setHasRealisticMod(true)
      → cancelRealisticModWarning() — предупреждения прекращаются

Если мод НЕ установлен:
  → Каждые 60 секунд sendRealisticModWarning()
    → Красное предупреждение в чат с кликабельной ссылкой
    
Игрок выходит:
  → clearPlayerModes() → cancelRealisticModWarning()
```

### Обратная совместимость
- Если обычный OpenBoatUtils отправляет пакет версии БЕЗ boolean, `try-catch` перехватывает исключение и ставит `hasRealisticMod = false`
- Если игрок вообще без мода — `boatUtilsVersion` остаётся `null`, `hasRealisticMod` остаётся `false`

## Тестирование
- [x] Мод собирается успешно (Gradle) — BUILD SUCCESSFUL
- [x] Плагин собирается успешно (Maven) — BUILD SUCCESSFUL
- [ ] Протестировано на MC 1.20.4
- [ ] Протестировано на MC 1.21
- [ ] Протестировано на MC 1.21.3

## Заметки
- `CODEBASE_INDEX.md` нужно будет обновить, когда он будет создан
- Версия протокола НЕ была изменена, т.к. изменение обратно совместимо (boolean добавлен в конец существующего пакета)
