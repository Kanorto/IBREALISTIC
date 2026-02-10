---
description: "Advanced features: patterns, server filtering, disabled lines, formatting / Продвинутые возможности: паттерны, фильтрация серверов, отключённые строки, форматирование"
sidebar_position: 5
---

# Advanced Features / Продвинутые возможности

---

## Patterns / Паттерны

:::note[Requirements / Требования]
Requires Triton v2.0.0 or later. / Требуется Triton v2.0.0 или новее.
:::

Patterns allow you to translate messages **without** editing the original plugin configuration. This is useful when a plugin doesn't allow you to edit its messages, when messages are hardcoded, or when you want to automatically intercept and replace any matching message.

Паттерны позволяют переводить сообщения **без** изменения оригинальной конфигурации плагина. Это полезно, когда плагин не позволяет редактировать сообщения, когда сообщения захардкодены, или когда вы хотите автоматически перехватывать и заменять любые совпадающие сообщения.

Each pattern is a [regular expression](https://regexr.com/) (regex) that is checked against every outgoing message. When a match is found, the message is replaced with the corresponding translation.

Каждый паттерн — это [регулярное выражение](https://regexr.com/) (regex), которое проверяется для каждого исходящего сообщения. При совпадении сообщение заменяется соответствующим переводом.

:::danger[Performance / Производительность]
**EN:** Each pattern is checked against **every** message sent to players. Use patterns only when you cannot use [Triton placeholders](../concepts/placeholders.md). Too many patterns can severely impact server performance.

**RU:** Каждый паттерн проверяется для **каждого** сообщения, отправляемого игрокам. Используйте паттерны только когда нельзя использовать [плейсхолдеры Triton](../concepts/placeholders.md). Слишком много паттернов может серьёзно повлиять на производительность сервера.
:::

### Basic Pattern Usage / Базовое использование паттернов

Add the `patterns` field to any text translation item:

Добавьте поле `patterns` к любому текстовому переводу:

```json
{
  "type": "text",
  "key": "no.permission",
  "languages": {
    "en_GB": "&cYou don't have permission!",
    "ru_RU": "&cУ вас нет прав!"
  },
  "patterns": [
    "&cYou do not have access to that command\\."
  ]
}
```

When any player receives the message `&cYou do not have access to that command.`, it will be replaced with the translation in their language.

Когда любой игрок получает сообщение `&cYou do not have access to that command.`, оно будет заменено на перевод на его языке.

### Escaping Special Characters / Экранирование специальных символов

Since patterns are regex, special characters must be escaped. In JSON, backslashes must also be escaped, resulting in **double backslashes**:

Поскольку паттерны — это regex, специальные символы нужно экранировать. В JSON обратные косые черты тоже нужно экранировать, что приводит к **двойным обратным косым**:

| Char / Символ | Meaning (EN) | Значение (RU) | Regex Escape | JSON String |
|---|---|---|---|---|
| `.` | Any character | Любой символ | `\.` | `\\.` |
| `(` `)` | Group | Группа | `\(` `\)` | `\\(` `\\)` |
| `[` `]` | Character class | Класс символов | `\[` `\]` | `\\[` `\\]` |
| `+` | One or more | Один или более | `\+` | `\\+` |
| `*` | Zero or more | Ноль или более | `\*` | `\\*` |
| `?` | Optional | Необязательный | `\?` | `\\?` |
| `{` `}` | Quantifier | Квантификатор | `\{` `\}` | `\\{` `\\}` |
| `^` | Start of string | Начало строки | `\^` | `\\^` |
| `$` | End of string | Конец строки | `\$` | `\\$` |
| `\|` | Alternation (OR) | Альтернация (ИЛИ) | `\\|` | `\\\\|` |
| `\` | Escape char | Символ экранирования | `\\` | `\\\\` |

### Using Anchors / Использование якорей

To prevent patterns from matching text typed by players in chat, use beginning `^` and end `$` anchors:

Чтобы паттерны не совпадали с текстом, набранным игроками в чате, используйте якоря начала `^` и конца `$`:

```json
{
  "patterns": [
    "^&cYou do not have access to that command\\.$"
  ]
}
```

This ensures the pattern only matches the **exact** message.

Это гарантирует, что паттерн совпадёт только с **точным** сообщением.

:::tip
**EN:** Always use `^` and `$` anchors for security. Without them, a player could type matching text in chat and trigger an unintended translation.

**RU:** Всегда используйте якоря `^` и `$` для безопасности. Без них игрок может набрать совпадающий текст в чате и вызвать непреднамеренный перевод.
:::

### Patterns with Variables / Паттерны с переменными

Capture dynamic parts using regex groups `(...)` — they become `%1`, `%2`, etc. in the translation:

Захватывайте динамические части с помощью групп regex `(...)` — они становятся `%1`, `%2` и т.д. в переводе:

```json
{
  "type": "text",
  "key": "pvp.kill.pattern",
  "languages": {
    "en_GB": "&c%1 &fwas slain by &c%2",
    "ru_RU": "&c%1 &fбыл убит игроком &c%2"
  },
  "patterns": [
    "^(\\w+) was killed by (\\w+)$"
  ]
}
```

**How it works / Как это работает:**
- `(\\w+)` captures one or more word characters / захватывает один или более символов слова
- First capture group `(...)` → `%1` / Первая группа захвата → `%1`
- Second capture group `(...)` → `%2` / Вторая группа захвата → `%2`

### Multiple Patterns / Несколько паттернов

A single item can have multiple patterns. If **any** pattern matches, the message is replaced:

Один элемент может иметь несколько паттернов. Если **любой** паттерн совпадёт, сообщение заменяется:

```json
{
  "type": "text",
  "key": "no.permission.generic",
  "languages": {
    "en_GB": "&cYou don't have permission!",
    "ru_RU": "&cУ вас нет прав!"
  },
  "patterns": [
    "^&cYou do not have access to that command\\.$",
    "^&cInsufficient permissions\\.$",
    "^&cNo permission\\.$",
    "^&cI'm sorry, but you do not have permission to perform this command\\.$"
  ]
}
```

### Color Code Variants in Patterns / Варианты цветовых кодов в паттернах

Depending on the plugin, messages might use `&` or `§` for color codes. You may need to try both:

В зависимости от плагина сообщения могут использовать `&` или `§`. Может понадобиться попробовать оба:

```json
{
  "patterns": [
    "&cNo permission!",
    "§cNo permission!"
  ]
}
```

:::tip
**EN:** Use regex alternation to match both at once: `[&§]cNo permission!`

**RU:** Используйте regex-альтернацию для совпадения обоих: `[&§]cNo permission!`
:::

### Common Regex Patterns / Распространённые регулярные выражения

| Pattern / Паттерн | Matches (EN) | Совпадает (RU) |
|---|---|---|
| `\\w+` | One or more word characters | Один или более символ слова |
| `\\d+` | One or more digits | Одна или более цифра |
| `\\S+` | One or more non-whitespace | Один или более непробельный символ |
| `.+` | One or more any characters | Один или более любой символ |
| `.*` | Zero or more any characters | Ноль или более любой символ |
| `\\d{1,3}` | 1 to 3 digits | От 1 до 3 цифр |
| `[&§]` | Either `&` or `§` | `&` или `§` |

### Where Patterns Work / Где работают паттерны

Patterns work everywhere **except** scoreboards:

Паттерны работают везде, **кроме** скорбордов:

- ✅ Chat messages / Сообщения чата
- ✅ Action bars / Экшн-бар
- ✅ Titles & subtitles / Тайтлы и субтайтлы
- ✅ Items / Предметы
- ✅ Bossbars / Боссбары
- ✅ Tab list / Таб-лист
- ✅ Signs (text content) / Таблички (текстовый контент)
- ❌ Scoreboards / Скорборды (use PlaceholderAPI / используйте PlaceholderAPI)

---

## Server Filtering (BungeeCord) / Фильтрация серверов (BungeeCord)

:::note
**EN:** This feature is only available on BungeeCord or Velocity networks.

**RU:** Эта функция доступна только на сетях BungeeCord или Velocity.
:::

You can restrict individual text translation items to specific servers using `servers` and `blacklist` fields. This is separate from collection-level metadata filtering.

Можно ограничить отдельные текстовые переводы конкретными серверами с помощью полей `servers` и `blacklist`. Это отдельно от фильтрации на уровне метаданных коллекции.

### Whitelist Mode / Режим белого списка

Show this translation **only** on listed servers:

Показывать перевод **только** на перечисленных серверах:

```json
{
  "type": "text",
  "key": "lobby.welcome",
  "languages": {
    "en_GB": "&6Welcome to the lobby!",
    "ru_RU": "&6Добро пожаловать в лобби!"
  },
  "servers": ["lobby-1", "lobby-2"],
  "blacklist": false
}
```

### Blacklist Mode / Режим чёрного списка

Show this translation on **all servers except** listed ones:

Показывать перевод на **всех серверах, кроме** перечисленных:

```json
{
  "type": "text",
  "key": "survival.tip",
  "languages": {
    "en_GB": "&eTip: Watch out for creepers!",
    "ru_RU": "&eСовет: Остерегайтесь криперов!"
  },
  "servers": ["creative", "lobby"],
  "blacklist": true
}
```

### All Scenarios / Все сценарии

| `blacklist` | `servers` | Result (EN) | Результат (RU) |
|---|---|---|---|
| `true` | `[]` | Available everywhere | Доступно везде |
| `true` | `["creative"]` | Everywhere except creative | Везде, кроме creative |
| `false` | `["lobby"]` | Only on lobby | Только на lobby |
| `false` | `[]` | Available nowhere | Недоступно нигде |
| _(not set)_ | _(not set)_ | Available everywhere (defaults) | Доступно везде (по умолчанию) |

:::warning
**EN:** Collection-level metadata and item-level filtering are **independent**. If a collection excludes a server via metadata, items inside that collection won't be available on that server, even if the item's own `servers` field includes it.

**RU:** Фильтрация на уровне метаданных коллекции и на уровне элемента **независимы**. Если коллекция исключает сервер через метаданные, элементы внутри неё не будут доступны на этом сервере, даже если поле `servers` самого элемента включает его.
:::

---

## Disabled Line Feature / Функция отключённой строки

The `disabled-line` feature (configured in `config.yml`) allows you to suppress specific messages entirely. When a message contains the disabled line placeholder, it is hidden or removed depending on the context.

Функция `disabled-line` (настраивается в `config.yml`) позволяет полностью подавить определённые сообщения. Когда сообщение содержит плейсхолдер отключённой строки, оно скрывается или удаляется в зависимости от контекста.

### Setup / Настройка

**Step 1:** Create a translation with an empty value / Создайте перевод с пустым значением:

```json
{
  "type": "text",
  "key": "disabled.line",
  "languages": {
    "en_GB": "",
    "ru_RU": ""
  }
}
```

**Step 2:** Configure in `config.yml` / Настройте в `config.yml`:

```yaml
disabled-line: 'disabled.line'
```

**Step 3:** Use in a plugin config / Используйте в конфиге плагина:

```yaml
unwanted-message: '[lang]disabled.line[/lang]'
```

### Behavior by Context / Поведение по контексту

| Context (EN) | Контекст (RU) | Behavior (EN) | Поведение (RU) |
|---|---|---|---|
| Chat | Чат | Message not sent at all | Сообщение не отправляется вообще |
| Action bar | Экшн-бар | Not displayed; doesn't hide current one | Не отображается; не скрывает текущий |
| Item titles | Названия предметов | Shows default material name (e.g., "Block of Diamond") | Показывает название материала по умолчанию |
| Item lores | Описания предметов | The line is removed from lore | Строка удаляется из описания |
| Books | Книги | The page is deleted | Страница удаляется |
| Titles | Тайтлы | Neither title nor subtitle sent | Ни тайтл, ни субтайтл не отправляются |
| Subtitles | Субтайтлы | Only subtitle not sent | Только субтайтл не отправляется |
| Tab header/footer | Шапка/подвал таба | Header/footer removed | Шапка/подвал удаляются |
| Bossbars | Боссбары | Text becomes blank | Текст становится пустым |
| GUI titles | Заголовки GUI | Title becomes blank | Заголовок становится пустым |
| Entities | Сущности | Display name is hidden | Отображаемое имя скрывается |
| Custom TABs | Пользовательские ТАБы | Display name swapped for real name | Отображаемое имя заменяется на реальное |
| Kick | Кик | Kick message becomes blank | Сообщение кика становится пустым |

---

## Formatting Prefixes / Префиксы форматирования

Triton supports special prefixes that change how translation text is processed. These are placed at the **very beginning** of each language's translation value.

Triton поддерживает специальные префиксы, изменяющие обработку текста перевода. Они размещаются в **самом начале** значения перевода каждого языка.

| Prefix / Префикс | Min Version / Мин. версия | Server / Сервер | Description (EN) | Описание (RU) |
|---|---|---|---|---|
| `[triton_json]` | v3.1.0 | Spigot/Paper/Forks | Minecraft JSON chat component | JSON-компонент чата Minecraft |
| `[minimsg]` | v3.5.1 | PaperMC and forks only | Kyori MiniMessage format | Формат MiniMessage от Kyori |
| _(none/empty)_ | Any | Any | Plain text with `&` color codes | Простой текст с цветовыми кодами `&` |

### How Prefixes Work / Как работают префиксы

- Prefix must be the **first characters** of the translation value. / Префикс должен быть **первыми символами** значения перевода.
- No spaces before the prefix. / Без пробелов перед префиксом.
- The rest of the string after the prefix is the content. / Остальная часть строки после префикса — это содержимое.
- Different languages in the same item can use different prefixes (but not recommended). / Разные языки в одном элементе могут использовать разные префиксы (но не рекомендуется).

```json
{
  "languages": {
    "en_GB": "[triton_json]{\"text\":\"Hello!\",\"bold\":true}",
    "ru_RU": "[minimsg]<bold>Привет!</bold>"
  }
}
```

See [Text Translations — JSON Chat Components](./text-translations.md#json-chat-components--json-компоненты-чата) and [Text Translations — MiniMessage](./text-translations.md#kyoris-minimessage--minimessage-от-kyori) for detailed syntax.

Подробный синтаксис в [Текстовые переводы — JSON-компоненты](./text-translations.md#json-chat-components--json-компоненты-чата) и [Текстовые переводы — MiniMessage](./text-translations.md#kyoris-minimessage--minimessage-от-kyori).

---

## Terminal Translations / Переводы в терминале

The `terminal` setting in `config.yml` controls whether Triton translates placeholders that appear in the console/terminal output. When enabled, console messages with Triton placeholders will be translated to the default language.

Настройка `terminal` в `config.yml` определяет, переводит ли Triton плейсхолдеры, которые появляются в консоли/терминале. При включении консольные сообщения с плейсхолдерами Triton будут переведены на язык по умолчанию.

```yaml
# In config.yml / В config.yml:
terminal: true
```

---

## Prevent Placeholders in Chat / Предотвращение плейсхолдеров в чате

The `prevent-placeholders-in-chat` setting (default: `true`) prevents players from typing Triton placeholders in chat that would get translated. When enabled, only placeholders in the message prefix or system messages are translated — not the actual player message.

Настройка `prevent-placeholders-in-chat` (по умолчанию: `true`) предотвращает набор игроками плейсхолдеров Triton в чате, которые были бы переведены. При включении переводятся только плейсхолдеры в префиксе сообщения или системных сообщениях — но не в самом сообщении игрока.

```yaml
# In config.yml / В config.yml:
prevent-placeholders-in-chat: true
```

:::tip
**EN:** Always keep this enabled for security. If disabled, players could insert `[lang]...[/lang]` tags in their messages to trigger translations.

**RU:** Всегда держите это включённым для безопасности. При отключении игроки могут вставлять теги `[lang]...[/lang]` в свои сообщения для срабатывания переводов.
:::

---

## Customizable Placeholder Tags / Настраиваемые теги плейсхолдеров

In `config.yml`, you can customize the tags used for Triton placeholders per context type:

В `config.yml` можно настроить теги, используемые для плейсхолдеров Triton для каждого типа контекста:

Default tags / Теги по умолчанию:
- `[lang]...[/lang]` — main tag / основной тег
- `[args]...[/args]` — arguments wrapper / обёртка аргументов
- `[arg]...[/arg]` — single argument / один аргумент

You can change these in the `language-creation` section of `config.yml` to avoid conflicts with other plugins.

Вы можете изменить их в секции `language-creation` файла `config.yml` для избежания конфликтов с другими плагинами.

---

## Complete Field Reference (All Types) / Полный справочник полей (Все типы)

### Text Translation Fields / Поля текстового перевода

| Field / Поле | Required / Обяз. | Type / Тип | Default | Description (EN) | Описание (RU) |
|---|---|---|---|---|---|
| `type` | ✅ | `"text"` | — | Item type identifier | Идентификатор типа |
| `key` | ✅ | `String` | — | Unique key for placeholders | Уникальный ключ для плейсхолдеров |
| `languages` | ✅ | `Object` | — | Translations per language | Переводы по языкам |
| `patterns` | ❌ | `String[]` | `[]` | Regex auto-matching patterns | Паттерны автоматического совпадения |
| `servers` | ❌ | `String[]` | `[]` | Server filter list (BungeeCord) | Список фильтрации серверов |
| `blacklist` | ❌ | `Boolean` | `true` | Server list mode (BungeeCord) | Режим списка серверов |

### Sign Translation Fields / Поля перевода табличек

| Field / Поле | Required / Обяз. | Type / Тип | Default | Description (EN) | Описание (RU) |
|---|---|---|---|---|---|
| `type` | ✅ | `"sign"` | — | Item type identifier | Идентификатор типа |
| `key` | ✅ | `String` | — | Unique key for `/triton sign` | Уникальный ключ для `/triton sign` |
| `lines` | ✅ | `Object` | — | 4 sign lines per language | 4 строки таблички на каждый язык |
| `locations` | ✅ | `Array` | — | Sign coordinates | Координаты табличек |

### Collection Metadata Fields (BungeeCord) / Поля метаданных коллекции

| Field / Поле | Required / Обяз. | Type / Тип | Default | Description (EN) | Описание (RU) |
|---|---|---|---|---|---|
| `metadata.blacklist` | ❌ | `Boolean` | `true` | Collection server filter mode | Режим фильтрации серверов коллекции |
| `metadata.servers` | ❌ | `String[]` | `[]` | Collection server list | Список серверов коллекции |

### Location Object Fields / Поля объекта местоположения

| Field / Поле | Required / Обяз. | Type / Тип | Description (EN) | Описание (RU) |
|---|---|---|---|---|
| `world` | ✅ | `String` | World name | Имя мира |
| `x` | ✅ | `Integer` | X coordinate | Координата X |
| `y` | ✅ | `Integer` | Y coordinate | Координата Y |
| `z` | ✅ | `Integer` | Z coordinate | Координата Z |
| `server` | BungeeCord | `String` | Server name | Имя сервера |
