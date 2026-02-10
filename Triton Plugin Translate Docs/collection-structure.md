---
description: "How to create, structure, and manage translation collection files / Как создавать, структурировать и управлять файлами коллекций переводов"
sidebar_position: 2
---

# Collection Structure / Структура коллекций

---

## What Is a Collection? / Что такое коллекция?

A collection is a JSON file inside the `translations/` folder that groups multiple translatable items together. Think of it as a container of related translations. Every translation item in Triton must belong to exactly one collection.

Коллекция — это JSON-файл внутри папки `translations/`, группирующий несколько переводимых элементов вместе. Думайте о ней как о контейнере связанных переводов. Каждый переводимый элемент в Triton должен принадлежать ровно одной коллекции.

**Key facts / Ключевые факты:**
- The file name determines the collection name (e.g., `chat.json` → `chat`). / Имя файла определяет имя коллекции (напр., `chat.json` → `chat`).
- File extension **must** be `.json`. / Расширение файла **должно** быть `.json`.
- Triton auto-creates `default.json` on first startup. / Triton автоматически создаёт `default.json` при первом запуске.
- Collections are loaded independently — a syntax error in one file will not prevent other collections from loading. / Коллекции загружаются независимо — синтаксическая ошибка в одном файле не помешает загрузке других коллекций.
- You can have as many collections as you want. / Вы можете иметь сколько угодно коллекций.
- Collections can contain both `text` and `sign` items in the same file. / Коллекции могут содержать элементы `text` и `sign` в одном файле.

---

## Creating a Collection / Создание коллекции

To create a new collection, create a new `.json` file inside the `translations/` folder.

Чтобы создать новую коллекцию, создайте новый файл `.json` в папке `translations/`.

### Empty Collection (Spigot) / Пустая коллекция (Spigot)

```json
[]
```

This is a valid JSON array with no items. You can start adding translation items into this array.

Это валидный JSON-массив без элементов. Вы можете начать добавлять переводимые элементы в этот массив.

### Empty Collection (BungeeCord with Metadata) / Пустая коллекция (BungeeCord с метаданными)

```json
{
  "metadata": {
    "blacklist": true,
    "servers": []
  },
  "items": []
}
```

This is a valid BungeeCord collection with metadata and an empty items list.

Это валидная коллекция BungeeCord с метаданными и пустым списком элементов.

---

## Spigot Format (Detailed) / Формат Spigot (Подробно)

On standalone Spigot servers, a collection is a JSON **array** `[...]` of translatable item objects `{...}`:

На автономных серверах Spigot коллекция — это JSON-**массив** `[...]` объектов переводимых элементов `{...}`:

```json
[
  {
    "type": "text",
    "key": "first.translation",
    "languages": {
      "en_GB": "Hello!",
      "ru_RU": "Привет!"
    }
  },
  {
    "type": "text",
    "key": "second.translation",
    "languages": {
      "en_GB": "Goodbye!",
      "ru_RU": "До свидания!"
    }
  },
  {
    "type": "sign",
    "key": "sign.0",
    "lines": {
      "en_GB": ["&aLine 1", "&bLine 2", "&cLine 3", "Line 4"],
      "ru_RU": ["&aСтрока 1", "&bСтрока 2", "&cСтрока 3", "Строка 4"]
    },
    "locations": [
      { "world": "world", "x": 10, "y": 64, "z": 20 }
    ]
  }
]
```

### Rules / Правила

- The root element **must** be a JSON array (`[...]`). / Корневой элемент **должен** быть JSON-массивом (`[...]`).
- Each element in the array is a translatable item (either `text` or `sign`). / Каждый элемент массива — это переводимый элемент (`text` или `sign`).
- You can mix `text` and `sign` items in the same collection. / Можно смешивать элементы `text` и `sign` в одной коллекции.
- Items are separated by commas `,`. / Элементы разделяются запятыми `,`.
- No trailing comma after the last item. / Нет висячей запятой после последнего элемента.
- The file **must** be valid JSON — use a validator if unsure. / Файл **должен** быть валидным JSON — используйте валидатор, если не уверены.

---

## BungeeCord Format (Detailed) / Формат BungeeCord (Подробно)

On BungeeCord/Velocity networks, you can optionally use an extended format with a `metadata` section for collection-level server filtering:

В сетях BungeeCord/Velocity можно использовать расширенный формат с секцией `metadata` для фильтрации серверов на уровне коллекции:

```json
{
  "metadata": {
    "blacklist": true,
    "servers": ["lobby-1", "lobby-2"]
  },
  "items": [
    {
      "type": "text",
      "key": "lobby.welcome",
      "languages": {
        "en_GB": "Welcome to the lobby!",
        "ru_RU": "Добро пожаловать в лобби!"
      }
    },
    {
      "type": "text",
      "key": "lobby.rules",
      "languages": {
        "en_GB": "&cPlease follow the rules!",
        "ru_RU": "&cПожалуйста, соблюдайте правила!"
      }
    }
  ]
}
```

### Metadata Fields (Detailed) / Поля метаданных (Подробно)

| Field / Поле | Type / Тип | Default | Description (EN) | Описание (RU) |
|---|---|---|---|---|
| `blacklist` | `Boolean` (`true`/`false`) | `true` | Controls how the `servers` list is interpreted. `true` = blacklist mode (items available everywhere **except** listed servers). `false` = whitelist mode (items available **only** on listed servers). | Определяет, как интерпретируется список `servers`. `true` = режим чёрного списка (элементы доступны везде, **кроме** перечисленных серверов). `false` = режим белого списка (элементы доступны **только** на перечисленных серверах). |
| `servers` | `String[]` (array of strings) | `[]` | List of server names (must match server names in BungeeCord's `config.yml`). | Список имён серверов (должны совпадать с именами серверов в `config.yml` BungeeCord). |

### How Server Filtering Works / Как работает фильтрация серверов

| Scenario / Сценарий | `blacklist` | `servers` | Result (EN) | Результат (RU) |
|---|---|---|---|---|
| Default (no filtering) | `true` | `[]` | Available on ALL servers | Доступно на ВСЕХ серверах |
| Blacklist specific servers | `true` | `["creative", "minigames"]` | Available everywhere EXCEPT creative and minigames | Доступно везде, КРОМЕ creative и minigames |
| Whitelist specific servers | `false` | `["lobby-1", "lobby-2"]` | Available ONLY on lobby-1 and lobby-2 | Доступно ТОЛЬКО на lobby-1 и lobby-2 |
| Empty whitelist | `false` | `[]` | Available NOWHERE (no servers match) | Недоступно НИГДЕ (ни один сервер не совпадает) |

:::tip
**EN:** You can also set `servers` and `blacklist` on **individual text items** to get per-item server filtering. See [Advanced Features](./advanced-features.md#server-filtering-bungeecord--фильтрация-серверов-bungeecord) for details.

**RU:** Вы также можете установить `servers` и `blacklist` на **отдельных текстовых элементах** для фильтрации по серверам на уровне элемента. Подробнее в [Продвинутые возможности](./advanced-features.md#server-filtering-bungeecord--фильтрация-серверов-bungeecord).
:::

:::warning
**EN:** Collection-level metadata filtering and item-level filtering work **independently**. If a collection's metadata excludes a server, individual items within that collection won't be available on that server regardless of their own `servers` setting.

**RU:** Фильтрация на уровне метаданных коллекции и на уровне элемента работают **независимо**. Если метаданные коллекции исключают сервер, отдельные элементы внутри этой коллекции не будут доступны на этом сервере, независимо от их собственных настроек `servers`.
:::

---

## Organizing Collections / Организация коллекций

There is no strict naming convention, but here are recommended approaches:

Строгой конвенции именования нет, но вот рекомендуемые подходы:

### By Plugin / По плагинам

```
translations/
├── default.json         ← general messages / общие сообщения
├── essentials.json      ← EssentialsX translations / переводы EssentialsX
├── cmi.json             ← CMI translations / переводы CMI
├── worldguard.json      ← WorldGuard translations / переводы WorldGuard
└── luckperms.json       ← LuckPerms translations / переводы LuckPerms
```

### By Feature / По функциям

```
translations/
├── chat.json            ← chat messages / сообщения чата
├── menus.json           ← GUI menus / GUI-меню
├── signs.json           ← sign translations / переводы табличек
├── items.json           ← item names and lores / названия и описания предметов
├── titles.json          ← titles and subtitles / тайтлы и субтайтлы
└── bossbars.json        ← bossbar messages / сообщения боссбаров
```

### By Server (BungeeCord) / По серверам (BungeeCord)

```
translations/
├── global.json          ← shared across all servers / общие для всех серверов
├── lobby.json           ← lobby-specific (use metadata filtering) / для лобби (с фильтрацией через метаданные)
├── survival.json        ← survival-specific / для сервера выживания
├── minigames.json       ← minigames-specific / для мини-игр
└── creative.json        ← creative-specific / для творческого режима
```

### Mixed Approach / Смешанный подход

```
translations/
├── default.json         ← core server messages / основные сообщения сервера
├── essentials.json      ← EssentialsX (all servers) / EssentialsX (все серверы)
├── lobby-signs.json     ← lobby sign translations / переводы табличек лобби
├── survival-chat.json   ← survival chat messages / сообщения чата выживания
└── minigames.json       ← minigame-specific / для мини-игр
```

---

## Deleting a Collection / Удаление коллекции

To delete a collection, simply delete the corresponding `.json` file from the `translations/` folder, then run `/triton reload`.

Чтобы удалить коллекцию, просто удалите соответствующий файл `.json` из папки `translations/`, затем выполните `/triton reload`.

:::warning
**EN:** Deleting a collection file permanently removes **all** translations inside it. Copy any translations you want to keep to another collection file before deleting.

**RU:** Удаление файла коллекции безвозвратно удаляет **все** переводы внутри неё. Скопируйте переводы, которые хотите сохранить, в другой файл коллекции перед удалением.
:::

---

## Renaming a Collection / Переименование коллекции

To rename a collection, simply rename the `.json` file. The collection name is derived from the file name. After renaming, run `/triton reload`.

Чтобы переименовать коллекцию, просто переименуйте файл `.json`. Имя коллекции берётся из имени файла. После переименования выполните `/triton reload`.

---

## Moving Translations Between Collections / Перемещение переводов между коллекциями

To move a translation from one collection to another / Чтобы переместить перевод из одной коллекции в другую:

1. Open the source collection file. / Откройте файл исходной коллекции.
2. Cut the translation item (the entire JSON object `{...}`). / Вырежьте переводимый элемент (весь JSON-объект `{...}`).
3. Open the destination collection file. / Откройте файл целевой коллекции.
4. Paste the translation item into the array (Spigot) or `items` array (BungeeCord). / Вставьте переводимый элемент в массив (Spigot) или массив `items` (BungeeCord).
5. Ensure commas between items are correct. / Убедитесь, что запятые между элементами расставлены правильно.
6. Save both files. / Сохраните оба файла.
7. Run `/triton reload`. / Выполните `/triton reload`.

---

## JSON Validation / Валидация JSON

Always ensure your JSON files are valid before saving. Invalid JSON will cause Triton to fail loading the collection.

Всегда проверяйте, что ваши JSON-файлы валидны перед сохранением. Невалидный JSON приведёт к тому, что Triton не сможет загрузить коллекцию.

### Recommended Tools / Рекомендуемые инструменты

| Tool / Инструмент | Description (EN) | Описание (RU) |
|---|---|---|
| [JSONLint](https://jsonlint.com/) | Online JSON validator | Онлайн-валидатор JSON |
| [JSON Formatter](https://jsonformatter.org/json-pretty-print) | Online formatter & validator | Онлайн-форматтер и валидатор |
| [Visual Studio Code](https://code.visualstudio.com) | IDE with built-in JSON validation and syntax highlighting | IDE с встроенной валидацией JSON и подсветкой синтаксиса |
| [Notepad++](https://notepad-plus-plus.org/) | Text editor with JSON plugin | Текстовый редактор с плагином JSON |
| [IntelliJ IDEA](https://www.jetbrains.com/idea/) | IDE with advanced JSON support | IDE с продвинутой поддержкой JSON |

### Common JSON Errors / Частые ошибки JSON

| Error (EN) | Ошибка (RU) | Cause (EN) | Причина (RU) | Fix (EN) | Исправление (RU) |
|---|---|---|---|---|---|
| Unexpected token | Неожиданный токен | Missing comma between items | Пропущена запятая между элементами | Add `,` between objects | Добавьте `,` между объектами |
| Unexpected end of input | Неожиданный конец ввода | Missing closing bracket | Пропущена закрывающая скобка | Ensure all `[`, `{` have matching `]`, `}` | Убедитесь, что все `[`, `{` имеют парные `]`, `}` |
| Duplicate key | Дублированный ключ | Same property name in same object | Одинаковое имя свойства в одном объекте | Use unique property names | Используйте уникальные имена свойств |
| Trailing comma | Висячая запятая | Extra `,` after the last item | Лишняя `,` после последнего элемента | Remove the trailing comma | Удалите висячую запятую |
| Single quotes used | Использованы одинарные кавычки | Using `'` instead of `"` | Использование `'` вместо `"` | Replace all `'` with `"` | Замените все `'` на `"` |
| Unescaped special char | Неэкранированный спецсимвол | Characters like `\`, `"`, tab/newline in strings | Символы `\`, `"`, табуляция/перенос строки в строках | Escape with `\\`, `\"`, `\t`, `\n` | Экранируйте через `\\`, `\"`, `\t`, `\n` |

### Examples of Invalid vs. Valid JSON / Примеры невалидного и валидного JSON

**❌ Invalid / Невалидный — trailing comma / висячая запятая:**
```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } },
]
```

**✅ Valid / Валидный:**
```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } }
]
```

**❌ Invalid / Невалидный — single quotes / одинарные кавычки:**
```json
[
  { 'type': 'text', 'key': 'a', 'languages': { 'en_GB': 'Hi' } }
]
```

**✅ Valid / Валидный:**
```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } }
]
```

**❌ Invalid / Невалидный — missing comma / пропущена запятая:**
```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } }
  { "type": "text", "key": "b", "languages": { "en_GB": "Bye" } }
]
```

**✅ Valid / Валидный:**
```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } },
  { "type": "text", "key": "b", "languages": { "en_GB": "Bye" } }
]
```

**❌ Invalid / Невалидный — unescaped quotes in string / неэкранированные кавычки в строке:**
```json
{
  "languages": {
    "en_GB": "He said "hello" to me"
  }
}
```

**✅ Valid / Валидный:**
```json
{
  "languages": {
    "en_GB": "He said \"hello\" to me"
  }
}
```
