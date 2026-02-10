---
description: "Complete overview of Triton's JSON translation configuration system / Полный обзор системы JSON-конфигураций переводов Triton"
sidebar_position: 1
---

# JSON Translation Configs Overview

# Обзор JSON-конфигураций переводов

---

## Introduction / Введение

Triton stores all translations in JSON files located in the `translations/` folder inside the `plugins/Triton` directory. Each JSON file represents a **collection** — a logical group of translations that can be managed independently. This is the core mechanism that allows Triton to serve multilingual content to players in real-time.

Triton хранит все переводы в JSON-файлах, расположенных в папке `translations/` внутри директории `plugins/Triton`. Каждый JSON-файл представляет собой **коллекцию** — логическую группу переводов, которой можно управлять независимо. Это основной механизм, позволяющий Triton выдавать многоязычный контент игрокам в реальном времени.

This documentation section provides a comprehensive reference for creating, structuring, and managing these JSON configuration files manually. If you prefer a visual editor, consider using [TWIN](../concepts/twin.md) instead.

Данный раздел документации представляет собой полное руководство по созданию, структурированию и управлению этими JSON-файлами конфигурации вручную. Если вы предпочитаете визуальный редактор, рассмотрите использование [TWIN](../concepts/twin.md).

---

## File Location / Расположение файлов

```
plugins/
└── Triton/
    ├── config.yml                  ← main config / главный конфиг
    ├── players.json                ← player language prefs / языковые настройки игроков
    └── translations/               ← translation collections / коллекции переводов
        ├── default.json            ← default collection (auto-created) / коллекция по умолчанию (создаётся автоматически)
        ├── chat.json               ← custom collection / пользовательская коллекция
        ├── signs.json              ← custom collection / пользовательская коллекция
        ├── essentials.json         ← plugin-specific collection / коллекция для плагина
        └── ...
```

:::tip
**EN:** When using BungeeCord, the `translations/` folder is only loaded from the **proxy server**. Translation files on individual Spigot servers are ignored.

**RU:** При использовании BungeeCord папка `translations/` загружается только с **прокси-сервера**. Файлы переводов на отдельных Spigot-серверах игнорируются.
:::

:::info
**EN:** When using non-local storage (MySQL/MongoDB), translations are stored in the database, not in JSON files. You can use `/triton database download` to export them to JSON and `/triton database upload` to import them back. See [Storage](../concepts/storage.md) for details.

**RU:** При использовании удалённого хранилища (MySQL/MongoDB) переводы хранятся в базе данных, а не в JSON-файлах. Вы можете использовать `/triton database download` для экспорта в JSON и `/triton database upload` для импорта обратно. Подробнее в разделе [Хранилище](../concepts/storage.md).
:::

---

## Key Concepts / Ключевые понятия

### Collections / Коллекции

A **collection** is a single JSON file in the `translations/` folder. Collections let you organize translations logically — for example, by plugin, feature, or language group.

**Коллекция** — это один JSON-файл в папке `translations/`. Коллекции позволяют логически организовать переводы — например, по плагинам, функциям или языковым группам.

- Triton automatically creates a `default.json` collection on first run. / Triton автоматически создаёт коллекцию `default.json` при первом запуске.
- You can create as many collections as you need. / Вы можете создавать столько коллекций, сколько нужно.
- Collection names are derived from the file name (e.g., `chat.json` → collection `chat`). / Имена коллекций берутся из имени файла (напр., `chat.json` → коллекция `chat`).
- Collections can contain both `text` and `sign` items mixed together. / Коллекции могут содержать элементы `text` и `sign` одновременно.
- Each collection is loaded independently; an error in one file won't affect others. / Каждая коллекция загружается независимо; ошибка в одном файле не повлияет на другие.

### Translatable Items / Переводимые элементы

Each collection contains **translatable items** — individual translation entries. There are exactly two types:

Каждая коллекция содержит **переводимые элементы** — отдельные записи переводов. Существует ровно два типа:

| Type / Тип | Purpose / Назначение | Required Fields / Обязательные поля | Optional Fields / Необязательные поля |
|---|---|---|---|
| `text` | Chat messages, titles, action bars, items, bossbars, tab, kick messages, holograms, GUIs, books / Сообщения чата, тайтлы, экшн-бар, предметы, боссбары, табы, кик, голограммы, GUI, книги | `type`, `key`, `languages` | `patterns`, `servers`, `blacklist` |
| `sign` | In-game signs / Таблички в игре | `type`, `key`, `lines`, `locations` | — |

### Language IDs / Идентификаторы языков

Language IDs are the identifiers you define in your `config.yml` under the `languages` section. These **must match exactly** between your `config.yml` and your JSON translation files.

Идентификаторы языков — это идентификаторы, которые вы определяете в `config.yml` в секции `languages`. Они **должны точно совпадать** между `config.yml` и JSON-файлами переводов.

Common examples / Типичные примеры:

| Language ID | Language / Язык |
|---|---|
| `en_GB` | English (UK) / Английский (Великобритания) |
| `en_US` | English (US) / Английский (США) |
| `ru_RU` | Russian / Русский |
| `uk_UA` | Ukrainian / Украинский |
| `de_DE` | German / Немецкий |
| `fr_FR` | French / Французский |
| `es_ES` | Spanish / Испанский |
| `pt_PT` | Portuguese / Португальский |
| `zh_CN` | Chinese (Simplified) / Китайский (упрощённый) |
| `ja_JP` | Japanese / Японский |
| `ko_KR` | Korean / Корейский |

:::warning
**EN:** If a language ID in a translation file doesn't match any language in `config.yml`, that translation entry for that language will simply be ignored. Triton won't throw an error — it will silently skip it.

**RU:** Если идентификатор языка в файле перевода не совпадает ни с одним языком в `config.yml`, эта запись перевода для данного языка будет просто проигнорирована. Triton не выдаст ошибку — он тихо пропустит её.
:::

### Fallback Mechanism / Механизм запасного языка

When a player requests a translation that doesn't exist in their language, Triton follows this fallback chain:

Когда игрок запрашивает перевод, которого нет на его языке, Triton следует цепочке запасных языков:

1. Player's selected language / Выбранный язык игрока
2. Languages in the `fallback-languages` list (from `config.yml`) / Языки из списка `fallback-languages` (из `config.yml`)
3. The `main-language` (from `config.yml`) / Основной язык `main-language` (из `config.yml`)

---

## File Formats / Форматы файлов

Triton supports two JSON file formats depending on your server setup:

Triton поддерживает два формата JSON-файлов в зависимости от конфигурации сервера:

### Spigot Format (Simple Array) / Формат Spigot (Простой массив)

For standalone Spigot servers (or when you don't need collection-level server filtering), the collection file is a simple JSON array:

Для автономных серверов Spigot (или когда вам не нужна фильтрация серверов на уровне коллекции) файл коллекции — это простой JSON-массив:

```json
[
  {
    "type": "text",
    "key": "welcome.message",
    "languages": {
      "en_GB": "Welcome to the server!",
      "ru_RU": "Добро пожаловать на сервер!"
    }
  },
  {
    "type": "sign",
    "key": "sign.spawn",
    "lines": {
      "en_GB": ["&6═══════", "&aSPAWN", "&fRight-click", "&6═══════"],
      "ru_RU": ["&6═══════", "&aСПАВН", "&fПКМ", "&6═══════"]
    },
    "locations": [
      { "world": "world", "x": 0, "y": 65, "z": 0 }
    ]
  }
]
```

**Rules / Правила:**
- The root element **must** be a JSON array `[...]`. / Корневой элемент **должен** быть JSON-массивом `[...]`.
- Each element is a translatable item object `{...}`. / Каждый элемент — это объект переводимого элемента `{...}`.
- You can mix `text` and `sign` items freely. / Можно свободно смешивать элементы `text` и `sign`.

### BungeeCord Format (With Metadata) / Формат BungeeCord (С метаданными)

For BungeeCord/Velocity networks, you can use an extended format that includes server filtering metadata:

Для сетей BungeeCord/Velocity можно использовать расширенный формат с метаданными фильтрации серверов:

```json
{
  "metadata": {
    "blacklist": true,
    "servers": []
  },
  "items": [
    {
      "type": "text",
      "key": "welcome.message",
      "languages": {
        "en_GB": "Welcome to the server!",
        "ru_RU": "Добро пожаловать на сервер!"
      }
    }
  ]
}
```

**Metadata fields / Поля метаданных:**

| Field / Поле | Type / Тип | Default | Description (EN) | Описание (RU) |
|---|---|---|---|---|
| `blacklist` | `Boolean` | `true` | If `true`, `servers` is a blacklist; if `false`, it's a whitelist | Если `true`, `servers` — чёрный список; если `false` — белый |
| `servers` | `String[]` | `[]` | List of server names | Список имён серверов |

:::info
**EN:** Both formats work on BungeeCord. The Spigot format (simple array) is also valid on BungeeCord — it just lacks collection-level server filtering. The metadata format adds the ability to apply server filtering rules to an **entire** collection at once.

**RU:** Оба формата работают на BungeeCord. Формат Spigot (простой массив) также допустим на BungeeCord — он просто не имеет фильтрации серверов на уровне коллекции. Формат с метаданными добавляет возможность применить правила фильтрации серверов ко **всей** коллекции сразу.
:::

---

## JSON Data Types Used / Используемые типы данных JSON

Understanding JSON data types is essential for writing valid configuration files:

Понимание типов данных JSON важно для написания валидных файлов конфигурации:

| JSON Type / Тип JSON | Description (EN) | Описание (RU) | Example / Пример |
|---|---|---|---|
| `String` | Text enclosed in double quotes | Текст в двойных кавычках | `"Hello!"` |
| `Number` (Integer) | Whole number without quotes | Целое число без кавычек | `42`, `-10`, `0` |
| `Boolean` | `true` or `false` without quotes | `true` или `false` без кавычек | `true` |
| `Array` | Ordered list in square brackets | Упорядоченный список в квадратных скобках | `["a", "b", "c"]` |
| `Object` | Key-value pairs in curly braces | Пары ключ-значение в фигурных скобках | `{"key": "value"}` |
| `null` | Represents "no value" | Означает «нет значения» | `null` |

:::warning
**EN:** All strings in JSON **must** use double quotes `"..."`. Single quotes `'...'` are **not valid** JSON and will cause parsing errors.

**RU:** Все строки в JSON **обязаны** использовать двойные кавычки `"..."`. Одинарные кавычки `'...'` — это **невалидный** JSON и приведут к ошибкам парсинга.
:::

---

## Contexts Where Translations Work / Контексты, в которых работают переводы

Triton can intercept and translate text in the following game contexts (each can be enabled/disabled in `config.yml`):

Triton может перехватывать и переводить текст в следующих игровых контекстах (каждый можно включить/выключить в `config.yml`):

| Context (EN) | Контекст (RU) | Notes / Заметки |
|---|---|---|
| Chat messages | Сообщения чата | Regular chat, system messages / Обычный чат, системные сообщения |
| Action bars | Экшн-бар | Text above hotbar / Текст над хотбаром |
| Titles & subtitles | Тайтлы и субтайтлы | Full-screen text / Полноэкранный текст |
| GUIs (inventory titles) | GUI (названия инвентарей) | Chest/menu titles / Названия сундуков/меню |
| Holograms | Голограммы | Via hologram plugins / Через плагины голограмм |
| Kick messages | Сообщения кика | Kick/ban messages / Сообщения кика/бана |
| Tab list (header/footer) | Таб-лист (шапка/подвал) | Tab menu customization / Кастомизация таб-меню |
| Items (names & lore) | Предметы (названия и описания) | Item display names, lore lines / Названия предметов, строки описания |
| Signs | Таблички | Via sign groups / Через группы табличек |
| Bossbars | Боссбары | Boss bar text / Текст боссбара |
| Books | Книги | Written book pages / Страницы написанных книг |
| MOTD | MOTD | Server list description / Описание в списке серверов |
| Entities | Сущности | Entity display names / Отображаемые имена сущностей |
| Scoreboards | Скорборды | Limited support; use PlaceholderAPI / Ограниченная поддержка; используйте PlaceholderAPI |

---

## Documentation Structure / Структура документации

This documentation section is organized as follows / Раздел документации организован следующим образом:

- **[Collection Structure / Структура коллекций](./collection-structure.md)** — How to create and organize collection files / Как создавать и организовывать файлы коллекций
- **[Text Translations / Текстовые переводы](./text-translations.md)** — Complete reference for text translation items / Полный справочник по текстовым переводам
- **[Sign Translations / Переводы табличек](./sign-translations.md)** — Complete reference for sign translation items / Полный справочник по переводам табличек
- **[Advanced Features / Продвинутые возможности](./advanced-features.md)** — Patterns, server filtering, special formatting / Паттерны, фильтрация серверов, специальное форматирование
- **[Examples & Best Practices / Примеры и лучшие практики](./examples.md)** — Real-world examples, common mistakes, validation / Реальные примеры, типичные ошибки, валидация

---

## Quick Start / Быстрый старт

Here is the simplest possible translation file you can create:

Вот самый простой файл перевода, который можно создать:

**File / Файл:** `plugins/Triton/translations/default.json`

```json
[
  {
    "type": "text",
    "key": "hello",
    "languages": {
      "en_GB": "Hello!",
      "ru_RU": "Привет!"
    }
  }
]
```

**Steps / Шаги:**

1. Create the file in the `translations/` folder. / Создайте файл в папке `translations/`.
2. Run `/triton reload` in-game or from the console to load the translations. / Выполните `/triton reload` в игре или из консоли, чтобы загрузить переводы.
3. Replace the original message in a plugin config with a [Triton placeholder](../concepts/placeholders.md): / Замените оригинальное сообщение в конфиге плагина на [плейсхолдер Triton](../concepts/placeholders.md):

```
[lang]hello[/lang]
```

4. The message will now appear as "Hello!" for English players and "Привет!" for Russian players. / Теперь сообщение будет отображаться как "Hello!" для англоязычных игроков и "Привет!" для русскоязычных.

:::tip
**EN:** If you want to quickly translate many messages at once, check out the [Bulk Translate Tool](../guides/bulk-translate.md).

**RU:** Если хотите быстро перевести множество сообщений сразу, ознакомьтесь с [Инструментом массового перевода](../guides/bulk-translate.md).
:::
