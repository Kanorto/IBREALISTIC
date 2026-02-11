# Изменения: Проверка версии с учётом статуса реалистик мода

## Дата
2026-02-11

## Краткое описание
TimingSystem теперь работает с VERSION 18 для реалистик режимов, проверяя статус мода (isRealisticMod) вместо требования повышенной версии протокола. Если у игрока обычный OBU — требуется установка реалистик мода; если реалистик мод — всё работает.

## Изменённые файлы

### Плагин (TimingSystem)
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsMode.java` — добавлен флаг `requiresRealisticMod`, версия реалистик режимов изменена с 19 на 18
- `src/main/java/me/makkuusen/timing/system/boatutils/BoatUtilsManager.java` — добавлена проверка реалистик мода перед проверкой версии; обновлена сигнатура `getAvailableModes`
- `src/main/java/me/makkuusen/timing/system/ContextResolvers.java` — обновлён вызов `getAvailableModes` с передачей флага `isRealisticMod`

## Детальное описание изменений

### 1. Добавление флага requiresRealisticMod в BoatUtilsMode
**Файл:** `BoatUtilsMode.java`
**Что сделано:**
- Добавлен третий параметр `requiresRealisticMod` (boolean) в конструктор enum
- Все обычные режимы получили `false`, все реалистичные (REALISTIC_*) — `true`
- Версия реалистик режимов изменена с 19 на 18 (соответствует реальному VERSION в моде)
- Добавлен метод `requiresRealisticMod()` для получения значения флага

**Причина:**
Раньше реалистик режимы требовали version=19, но мод отправляет VERSION=18. Это блокировало доступ даже для пользователей с реалистик модом. Теперь проверка двухуровневая: сначала version, потом статус мода.

### 2. Обновление проверки в sendBoatUtilsModePluginMessage
**Файл:** `BoatUtilsManager.java`
**Строки:** 86-93
**Что сделано:**
- Добавлена проверка `mode.requiresRealisticMod() && !tPlayer.isRealisticMod()` перед проверкой версии
- Если режим требует реалистик мод, но у игрока его нет — показывается предупреждение со ссылкой на скачивание реалистик мода
- Если режим не требует реалистик мод — проверяется только версия (как раньше)

**Причина:**
Позволяет разделить логику: обычные режимы проверяются по версии, реалистичные — по наличию реалистик мода.

### 3. Обновление getAvailableModes
**Файл:** `BoatUtilsManager.java`
**Строки:** 208-212
**Что сделано:**
- Добавлен параметр `boolean isRealisticMod`
- Добавлен фильтр: режимы с `requiresRealisticMod=true` доступны только если `isRealisticMod=true`

### 4. Обновление вызова getAvailableModes
**Файл:** `ContextResolvers.java`
**Строки:** 168
**Что сделано:**
- Передаётся `tPlayer.isRealisticMod()` вторым аргументом в `getAvailableModes`

**Причина:**
Tab-completion теперь также учитывает наличие реалистик мода у игрока.

## Тестирование
- [ ] Протестировано на MC 1.20.4
- [ ] Протестировано на MC 1.21
- [ ] Протестировано на MC 1.21.3
- [x] Плагин собирается успешно (Maven)

## Примечания
- CODEBASE_INDEX.md необходимо обновить при его создании: добавить описание нового метода `requiresRealisticMod()` в `BoatUtilsMode` и обновить описание `getAvailableModes()` в `BoatUtilsManager`
- Версия протокола (VERSION=18) и `realistic_version` не изменены, так как это исправление логики на стороне плагина, не затрагивающее протокол
