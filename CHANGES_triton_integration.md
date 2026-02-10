# Изменения: Интеграция плагина Triton для мультиязычности

## Дата
2026-02-10

## Краткое описание
Добавлена полная интеграция с плагином Triton (com.rexcantor64.triton) для поддержки мультиязычности в TimingSystem. При обнаружении Triton на сервере все сообщения автоматически оборачиваются в Triton-плейсхолдеры, что позволяет каждому игроку видеть интерфейс на своём языке.

## Изменённые файлы

### Плагин (TimingSystem)
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — добавлена автодетекция Triton, флаг `tritonEnabled`
- `src/main/java/me/makkuusen/timing/system/theme/Text.java` — переключение на triton-локаль при активном Triton
- `src/main/resources/config.yml` — добавлены настройки `settings.triton.autodetect` и `settings.triton.enabled`
- `src/main/resources/plugin.yml` — Triton добавлен в softdepend
- `src/main/resources/lang/triton.yml` — новый файл локали с Triton-плейсхолдерами (388 ключей)
- `triton/timingsystem.json` — файл переводов для Triton с EN и RU (388 элементов)

## Детальное описание изменений

### 1. Автодетекция плагина Triton
**Файл:** `TimingSystem.java`
**Что сделано:**
- Добавлен статический флаг `tritonEnabled` с геттером через Lombok `@Getter`
- В `onEnable()` добавлена логика обнаружения Triton через `Bukkit.getPluginManager().getPlugin("Triton")`
- Поддержка ручного включения через конфигурацию `settings.triton.enabled`
- Поддержка автодетекции через `settings.triton.autodetect` (по умолчанию true)
- Флаг сбрасывается в `onDisable()`

**Причина:**
Необходимо автоматически определять наличие Triton для переключения системы сообщений.

### 2. Переключение локали в Text.java
**Файл:** `Text.java`
**Что сделано:**
- Модифицирован метод `getLocale(CommandSender)` — при `tritonEnabled == true` всегда возвращает `"triton"` вместо клиентской локали
- Это заставляет LanguageManager загрузить и использовать файл `triton.yml`

**Причина:**
Когда Triton активен, TimingSystem должен отправлять Triton-плейсхолдеры вместо обычного текста. Triton перехватывает эти плейсхолдеры и заменяет их на перевод на языке конкретного игрока.

### 3. Locale файл triton.yml
**Файл:** `src/main/resources/lang/triton.yml`
**Что сделано:**
- Создан полный файл локализации с 388 ключами
- Каждое значение — Triton-плейсхолдер формата `[lang]ts.category.key[/lang]`
- Для сообщений с переменными используется формат `[lang]ts.key[args][arg]%variable%[/arg][/args][/lang]`
- Переменные TimingSystem (`%player%`, `%track%` и т.д.) передаются как `[arg]` — TimingSystem подставляет значения, затем Triton обрабатывает плейсхолдер

**Причина:**
Этот файл является мостом между системой сообщений TimingSystem и системой переводов Triton.

### 4. JSON файл переводов для Triton
**Файл:** `triton/timingsystem.json`
**Что сделано:**
- Создан полный JSON-файл коллекции переводов для Triton (формат Spigot)
- 388 элементов перевода с ключами `ts.*`
- Два языка: `en_GB` (английский) и `ru_RU` (русский)
- Переменные используют позиционный формат Triton (`%1`, `%2`, `%3` и т.д.)
- Тексты без цветовых кодов TimingSystem (Triton обрабатывает цвета отдельно)

**Причина:**
Этот файл размещается в `plugins/Triton/translations/` на сервере и содержит фактические переводы.

### 5. Конфигурация
**Файл:** `config.yml`
**Что сделано:**
- Добавлен раздел `settings.triton`:
  - `autodetect: true` — автоматически обнаруживать плагин Triton
  - `enabled: false` — принудительно включить Triton-режим

### 6. Plugin.yml
**Файл:** `plugin.yml`
**Что сделано:**
- Triton добавлен в список `softdepend`

## Архитектура интеграции

### Поток данных
```
TimingSystem отправляет сообщение
  → Text.java определяет locale = "triton"
    → LanguageManager загружает triton.yml
      → Возвращает: [lang]ts.error.generic[args][arg]значение[/arg][/args][/lang]
        → TimingSystem подставляет переменные (%player% → "Steve")
          → Отправляет игроку: [lang]ts.error.generic[args][arg]Steve[/arg][/args][/lang]
            → Triton перехватывает пакет
              → Ищет ключ ts.error.generic в timingsystem.json
                → Подставляет %1 = "Steve" в перевод на языке игрока
                  → Игрок видит сообщение на своём языке
```

### Установка на сервере
1. Установить плагин Triton
2. Скопировать `triton/timingsystem.json` в `plugins/Triton/translations/`
3. Перезагрузить сервер — TimingSystem автоматически обнаружит Triton

## Тестирование
- [x] Мод собирается успешно (Gradle)
- [x] Плагин собирается успешно (Maven)
- [x] triton.yml содержит все 388 ключей из en_us.yml
- [x] timingsystem.json содержит все 388 элементов перевода
- [x] JSON валиден (проверено парсером)
- [x] Все переменные корректно маппятся на позиционные аргументы Triton

## Примечания
- CODEBASE_INDEX.md нужно будет обновить при его создании
- Цветовые коды TimingSystem (`&1`, `&2`, `&e`, `&s` и т.д.) НЕ включены в Triton JSON, так как они обрабатываются самим TimingSystem при рендеринге
