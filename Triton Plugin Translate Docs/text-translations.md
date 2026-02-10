---
description: "Complete reference for text translation items in JSON / Полный справочник по текстовым переводам в JSON"
sidebar_position: 3
---

# Text Translations / Текстовые переводы

---

## Introduction / Введение

A text translation item represents a single translatable message. This is the most commonly used translation type in Triton. It covers chat messages, titles, action bars, item names, lore, bossbars, tab headers/footers, kick messages, holograms, GUI titles, books, entities, and MOTD.

Текстовый перевод представляет собой одно переводимое сообщение. Это самый часто используемый тип перевода в Triton. Он охватывает сообщения чата, тайтлы, экшн-бар, названия предметов, описания, боссбары, шапки/подвалы таба, сообщения кика, голограммы, заголовки GUI, книги, сущности и MOTD.

---

## Basic Structure / Базовая структура

```json
{
  "type": "text",
  "key": "my.translation.key",
  "languages": {
    "en_GB": "English text here",
    "ru_RU": "Русский текст здесь"
  }
}
```

This is the minimal valid text translation item. Every text item must have exactly these three required fields.

Это минимально валидный текстовый переводимый элемент. Каждый текстовый элемент должен иметь ровно эти три обязательных поля.

---

## Required Fields / Обязательные поля

### `type`

**Type / Тип:** `String`
**Value / Значение:** `"text"`
**Required / Обязательное:** ✅ Always / Всегда

Must always be set to `"text"` for text translations. This field tells Triton how to interpret the rest of the fields. If you set it to `"sign"`, Triton will expect `lines` and `locations` instead of `languages`.

Должно всегда быть `"text"` для текстовых переводов. Это поле говорит Triton, как интерпретировать остальные поля. Если установить `"sign"`, Triton будет ожидать `lines` и `locations` вместо `languages`.

```json
"type": "text"
```

### `key`

**Type / Тип:** `String`
**Required / Обязательное:** ✅ Always / Всегда

A unique identifier for this translation. This key is used in [Triton placeholders](../concepts/placeholders.md) to reference this translation in plugin configuration files, items, holograms, etc.

Уникальный идентификатор для этого перевода. Этот ключ используется в [плейсхолдерах Triton](../concepts/placeholders.md) для ссылки на этот перевод в конфигурационных файлах плагинов, предметах, голограммах и т.д.

**Placeholder usage / Использование плейсхолдера:**
```
[lang]my.translation.key[/lang]
```

**PlaceholderAPI usage / Использование PlaceholderAPI:**
```
%triton_my.translation.key%
```

#### Key Naming Conventions / Соглашения об именовании ключей

| Rule (EN) | Правило (RU) | Good Example / Хороший | Bad Example / Плохой |
|---|---|---|---|
| Use dot-separated names | Используйте точечную нотацию | `cmi.info.NoPermission` | `cmiInfoNoPermission` |
| Keep descriptive but concise | Описательные, но краткие | `shop.buy.success` | `msg1` |
| Use lowercase and dots | Только строчные и точки | `death.killed_by_player` | `My Translation Key` |
| Follow pattern: `plugin.category.message` | Следуйте шаблону: `plugin.category.message` | `essentials.teleport.success` | `a` |
| Avoid special characters except `.` and `_` | Избегайте спецсимволов кроме `.` и `_` | `menu.main.title` | `menu/main/title!` |

:::warning
**EN:** Keys must be **unique across ALL collection files**. If two items in different files share the same key, only one will be used and behavior is unpredictable.

**RU:** Ключи должны быть **уникальными среди ВСЕХ файлов коллекций**. Если два элемента в разных файлах имеют одинаковый ключ, будет использован только один, и поведение станет непредсказуемым.
:::

:::warning
**EN:** Various kinds of text in Minecraft have length limits. Most plugins truncate texts that are too long, which means Triton might not detect a placeholder if it has been truncated. If this happens, shorten the key name.

**RU:** Различные виды текста в Minecraft имеют ограничения по длине. Большинство плагинов обрезают слишком длинные тексты, из-за чего Triton может не обнаружить плейсхолдер. Если это произойдёт, сократите имя ключа.
:::

### `languages`

**Type / Тип:** `Object` (JSON Object — keys are language IDs (`String`), values are translation strings (`String`))
**Required / Обязательное:** ✅ Always (for `text` type) / Всегда (для типа `text`)

Contains the actual translation text for each language. The keys **must match** the language IDs defined in your `config.yml` under the `languages` section.

Содержит фактический текст перевода для каждого языка. Ключи **должны совпадать** с идентификаторами языков, определёнными в `config.yml` в секции `languages`.

```json
{
  "languages": {
    "en_GB": "Welcome, adventurer!",
    "ru_RU": "Добро пожаловать, искатель приключений!",
    "de_DE": "Willkommen, Abenteurer!",
    "fr_FR": "Bienvenue, aventurier !",
    "es_ES": "¡Bienvenido, aventurero!",
    "zh_CN": "欢迎，冒险者！",
    "ja_JP": "ようこそ、冒険者！",
    "ko_KR": "환영합니다, 모험가!"
  }
}
```

**Important nuances / Важные нюансы:**

- If a language ID used here doesn't exist in `config.yml`, it will be silently ignored. / Если идентификатор языка здесь не существует в `config.yml`, он будет тихо проигнорирован.
- If a player's language is not present in the `languages` object, Triton falls back through the `fallback-languages` chain and ultimately to the `main-language`. / Если язык игрока отсутствует в объекте `languages`, Triton откатится по цепочке `fallback-languages` и в конце — к `main-language`.
- You don't need to include all languages — just the ones you've translated. / Не обязательно включать все языки — только те, которые вы перевели.
- The value can be an empty string `""` — this is valid and will show nothing. / Значение может быть пустой строкой `""` — это валидно и ничего не покажет.
- The value can contain color codes, variables, PlaceholderAPI placeholders, and special prefixes. / Значение может содержать цветовые коды, переменные, плейсхолдеры PlaceholderAPI и специальные префиксы.

---

## Optional Fields / Необязательные поля

### `patterns`

**Type / Тип:** `String[]` (Array of regex strings)
**Default:** `[]`
**Required / Обязательное:** ❌ Optional / Необязательное
**Only for / Только для:** `text` type

Regex patterns for automatic message matching. See [Advanced Features — Patterns](./advanced-features.md#patterns--паттерны) for detailed documentation.

Регулярные выражения для автоматического сопоставления сообщений. Подробная документация в [Продвинутые возможности — Паттерны](./advanced-features.md#patterns--паттерны).

### `servers`

**Type / Тип:** `String[]` (Array of server name strings)
**Default:** `[]`
**Required / Обязательное:** ❌ Optional / Необязательное
**Only for / Только для:** `text` type, BungeeCord/Velocity only

List of server names for filtering. See [Advanced Features — Server Filtering](./advanced-features.md#server-filtering-bungeecord--фильтрация-серверов-bungeecord) for details.

Список имён серверов для фильтрации. Подробнее в [Продвинутые возможности — Фильтрация серверов](./advanced-features.md#server-filtering-bungeecord--фильтрация-серверов-bungeecord).

### `blacklist`

**Type / Тип:** `Boolean` (`true` or `false`)
**Default:** `true`
**Required / Обязательное:** ❌ Optional / Необязательное
**Only for / Только для:** `text` type, BungeeCord/Velocity only

Controls whether `servers` acts as a blacklist or whitelist. See [Advanced Features — Server Filtering](./advanced-features.md#server-filtering-bungeecord--фильтрация-серверов-bungeecord) for details.

Определяет, действует ли `servers` как чёрный или белый список. Подробнее в [Продвинутые возможности — Фильтрация серверов](./advanced-features.md#server-filtering-bungeecord--фильтрация-серверов-bungeecord).

---

## Color Codes & Formatting / Цветовые коды и форматирование

Triton supports Minecraft color codes using the `&` symbol (legacy color codes). These work in both `text` and `sign` translations.

Triton поддерживает цветовые коды Minecraft с использованием символа `&` (устаревшие цветовые коды). Они работают как в переводах `text`, так и `sign`.

### Color Codes / Цветовые коды

| Code / Код | Color (EN) | Цвет (RU) |
|---|---|---|
| `&0` | Black | Чёрный |
| `&1` | Dark Blue | Тёмно-синий |
| `&2` | Dark Green | Тёмно-зелёный |
| `&3` | Dark Aqua | Тёмно-бирюзовый |
| `&4` | Dark Red | Тёмно-красный |
| `&5` | Dark Purple | Тёмно-фиолетовый |
| `&6` | Gold | Золотой |
| `&7` | Gray | Серый |
| `&8` | Dark Gray | Тёмно-серый |
| `&9` | Blue | Синий |
| `&a` | Green | Зелёный |
| `&b` | Aqua | Бирюзовый |
| `&c` | Red | Красный |
| `&d` | Light Purple | Светло-фиолетовый |
| `&e` | Yellow | Жёлтый |
| `&f` | White | Белый |

### Formatting Codes / Коды форматирования

| Code / Код | Effect (EN) | Эффект (RU) |
|---|---|---|
| `&l` | **Bold** | **Жирный** |
| `&m` | ~~Strikethrough~~ | ~~Зачёркнутый~~ |
| `&n` | Underline | Подчёркнутый |
| `&o` | *Italic* | *Курсив* |
| `&k` | Obfuscated (random characters) | Обфускация (случайные символы) |
| `&r` | Reset all formatting | Сброс всего форматирования |

### Using § vs & / Использование § и &

Some plugins send messages using `§` instead of `&` for color codes. When using patterns, you may need to try both:

Некоторые плагины отправляют сообщения с `§` вместо `&`. При использовании паттернов может понадобиться попробовать оба варианта:

```json
{
  "languages": {
    "en_GB": "&c&lError: &fSomething went wrong!",
    "ru_RU": "&c&lОшибка: &fЧто-то пошло не так!"
  }
}
```

### Combining Codes / Комбинирование кодов

You can combine multiple codes. Formatting codes must come before text. A color code resets all previous formatting:

Можно комбинировать коды. Коды форматирования должны идти перед текстом. Цветовой код сбрасывает всё предыдущее форматирование:

```json
{
  "languages": {
    "en_GB": "&6&l&nGold Bold Underline &r&7then gray normal",
    "ru_RU": "&6&l&nЗолотой жирный подчёркнутый &r&7затем серый обычный"
  }
}
```

---

## Using Variables / Использование переменных

Variables allow you to insert dynamic values into translations. Variables are defined with `%1`, `%2`, `%3`, etc. (starting from 1, no upper limit).

Переменные позволяют вставлять динамические значения в переводы. Переменные определяются как `%1`, `%2`, `%3` и т.д. (начиная с 1, без верхнего предела).

### Basic Variable Usage / Базовое использование переменных

```json
{
  "type": "text",
  "key": "death.kill",
  "languages": {
    "en_GB": "&c%1 &fhas killed &c%2",
    "ru_RU": "&c%1 &fубил &c%2"
  }
}
```

Variables are passed through the [Triton placeholder](../concepts/placeholders.md) `[arg]` tags:

Переменные передаются через теги `[arg]` [плейсхолдера Triton](../concepts/placeholders.md):

```
[lang]death.kill[args][arg]Player1[/arg][arg]Player2[/arg][/args][/lang]
```

**Mapping / Соответствие:**
- `[arg]Player1[/arg]` → `%1`
- `[arg]Player2[/arg]` → `%2`

### Repeating Variables / Повторение переменных

You can use the same variable multiple times in a single translation:

Можно использовать одну и ту же переменную несколько раз:

```json
{
  "type": "text",
  "key": "pvp.announce",
  "languages": {
    "en_GB": "&c%1 &fkilled &c%2&f! &c%1 &fis on a streak!",
    "ru_RU": "&c%1 &fубил &c%2&f! &c%1 &fна серии убийств!"
  }
}
```

### Variables with Different Word Order / Переменные с другим порядком слов

Different languages may require different word order. Variables give you this flexibility:

Разные языки могут требовать различный порядок слов. Переменные дают эту гибкость:

```json
{
  "type": "text",
  "key": "event.give",
  "languages": {
    "en_GB": "&e%1 &fgave &a%2 &fto &e%3",
    "ru_RU": "&e%1 &fдал игроку &e%3 &fпредмет &a%2",
    "ja_JP": "&e%1 &fは &e%3 &fに &a%2 &fを渡しました"
  }
}
```

Note that `%2` and `%3` are in different order in Russian and Japanese — but the values are still assigned by position in the `[arg]` tags.

Обратите внимание: `%2` и `%3` в разном порядке в русском и японском — но значения назначаются по позиции в тегах `[arg]`.

### Using Plugin Variables Inside `[arg]` / Использование переменных плагинов внутри `[arg]`

When configuring a third-party plugin, pass that plugin's variables inside `[arg]` tags:

При настройке стороннего плагина передавайте его переменные внутри тегов `[arg]`:

```yaml
# In a plugin's config.yml / В config.yml плагина:
death-message: '[lang]death.kill[args][arg]{killer}[/arg][arg]{victim}[/arg][/args][/lang]'
```

### Nesting Placeholders / Вложенные плейсхолдеры

You can nest Triton placeholders inside `[arg]` tags:

Можно вкладывать плейсхолдеры Triton в теги `[arg]`:

```
[lang]economy.withdrawal[args][arg]10[/arg][arg][lang]currency.usd[/lang][/arg][/args][/lang]
```

This resolves `[lang]currency.usd[/lang]` first and passes the result as `%2`.

Сначала разрешается `[lang]currency.usd[/lang]`, и результат передаётся как `%2`.

---

## JSON Chat Components / JSON-компоненты чата

:::note[Requirements / Требования]
Requires Triton v3.1.0 or newer. / Требуется Triton v3.1.0 или новее.
Understanding of the [Minecraft JSON Text Format](https://minecraft.gamepedia.com/Raw_JSON_text_format) is required. / Необходимо понимание [формата JSON-текста Minecraft](https://minecraft.gamepedia.com/Raw_JSON_text_format).
:::

Minecraft supports rich text through JSON chat components since version 1.7. This enables click events, hover events, and native Minecraft translations. Prepend `[triton_json]` to the translation value.

Minecraft поддерживает форматированный текст через JSON-компоненты чата с версии 1.7. Это позволяет создавать события клика, наведения и нативные переводы. Добавьте `[triton_json]` в начало значения перевода.

### Basic JSON Chat Example / Базовый пример

```json
{
  "type": "text",
  "key": "click.website",
  "languages": {
    "en_GB": "[triton_json]{\"text\":\"Click here to visit our website!\",\"color\":\"gold\",\"bold\":true,\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://example.com\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Open website\"}}",
    "ru_RU": "[triton_json]{\"text\":\"Нажмите, чтобы посетить наш сайт!\",\"color\":\"gold\",\"bold\":true,\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://example.com\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Открыть сайт\"}}"
  }
}
```

### Supported JSON Chat Actions / Поддерживаемые действия

| Action (EN) | Действие (RU) | JSON Key | Example / Пример |
|---|---|---|---|
| Open a URL | Открыть URL | `clickEvent.open_url` | `"clickEvent":{"action":"open_url","value":"https://..."}` |
| Run a command | Выполнить команду | `clickEvent.run_command` | `"clickEvent":{"action":"run_command","value":"/spawn"}` |
| Suggest a command | Предложить команду | `clickEvent.suggest_command` | `"clickEvent":{"action":"suggest_command","value":"/msg "}` |
| Show text on hover | Показать текст при наведении | `hoverEvent.show_text` | `"hoverEvent":{"action":"show_text","contents":"Info"}` |
| Use MC translations | Использовать переводы MC | `translate` | `"translate":"gameMode.changed"` |

### Minecraft Native Translations / Нативные переводы Minecraft

Reference Minecraft's own localization keys:

Ссылайтесь на ключи локализации самого Minecraft:

```json
{
  "type": "text",
  "key": "gamemode.change",
  "languages": {
    "en_GB": "[triton_json]{\"translate\":\"gameMode.changed\",\"color\":\"gold\",\"bold\":true,\"with\":[{\"text\":\"%1\"}]}",
    "ru_RU": "[triton_json]{\"translate\":\"gameMode.changed\",\"color\":\"gold\",\"bold\":true,\"with\":[{\"text\":\"%1\"}]}"
  }
}
```

:::warning
**EN:** Variables inside JSON chat components (`%1`, `%2`, etc.) are **not** automatically escaped. If a variable contains `"` or `\`, it can break the JSON structure.

**RU:** Переменные внутри JSON-компонентов чата (`%1`, `%2` и т.д.) **не** экранируются автоматически. Если переменная содержит `"` или `\`, это может сломать JSON-структуру.
:::

---

## Kyori's MiniMessage / MiniMessage от Kyori

:::note[Requirements / Требования]
Requires Triton v3.5.1 or newer and PaperMC (or a fork). / Требуется Triton v3.5.1 или новее и PaperMC (или форк).
:::

[MiniMessage](https://docs.adventure.kyori.net/minimessage.html#format) is a simpler, more readable alternative to JSON chat components. Prepend `[minimsg]` to use it.

[MiniMessage](https://docs.adventure.kyori.net/minimessage.html#format) — более простая и читаемая альтернатива JSON-компонентам чата. Добавьте `[minimsg]` в начало.

### Basic MiniMessage Example / Базовый пример

```json
{
  "type": "text",
  "key": "pvp.kill.minimsg",
  "languages": {
    "en_GB": "[minimsg]<#006fff>PVP: <gold>%1</gold> has been killed by <#6100e9>%2",
    "ru_RU": "[minimsg]<#006fff>PVP: <gold>%1</gold> был убит игроком <#6100e9>%2"
  }
}
```

### MiniMessage Features / Возможности MiniMessage

| Feature (EN) | Возможность (RU) | Syntax / Синтаксис | Example / Пример |
|---|---|---|---|
| HEX colors | HEX-цвета | `<#rrggbb>text` | `<#ff5555>Red text` |
| Named colors | Именованные цвета | `<color>text</color>` | `<gold>Gold text</gold>` |
| Bold | Жирный | `<bold>text</bold>` | `<bold>Bold</bold>` |
| Italic | Курсив | `<italic>text</italic>` | `<italic>Italic</italic>` |
| Underlined | Подчёркнутый | `<underlined>text</underlined>` | `<underlined>Under</underlined>` |
| Strikethrough | Зачёркнутый | `<strikethrough>text</strikethrough>` | `<strikethrough>Struck</strikethrough>` |
| Gradient | Градиент | `<gradient:c1:c2>text</gradient>` | `<gradient:red:blue>Rainbow</gradient>` |
| Click events | Клик-события | `<click:action:'val'>text</click>` | `<click:open_url:'https://...'>Click</click>` |
| Hover events | Ховер-события | `<hover:show_text:'val'>text</hover>` | `<hover:show_text:'Info'>Hover</hover>` |
| Newline | Перенос строки | `<newline>` | `Line 1<newline>Line 2` |
| Reset | Сброс | `<reset>` | `<red>Red <reset>Normal` |

### Advanced MiniMessage Example / Продвинутый пример

```json
{
  "type": "text",
  "key": "announcement.event",
  "languages": {
    "en_GB": "[minimsg]<gradient:#ff0000:#0000ff><bold>SERVER EVENT</bold></gradient><newline><yellow>%1<newline><click:run_command:'/event join'><green>[Join Now]</green></click>",
    "ru_RU": "[minimsg]<gradient:#ff0000:#0000ff><bold>СОБЫТИЕ СЕРВЕРА</bold></gradient><newline><yellow>%1<newline><click:run_command:'/event join'><green>[Присоединиться]</green></click>"
  }
}
```

### MiniMessage vs JSON Chat / Сравнение MiniMessage и JSON Chat

| Aspect (EN) | Аспект (RU) | JSON Chat | MiniMessage |
|---|---|---|---|
| Readability | Читаемость | Hard / Сложно | Easy / Легко |
| HEX colors | HEX-цвета | Complex JSON | `<#ff5555>` |
| Gradients | Градиенты | Not supported / Нет | `<gradient:red:blue>` |
| Escaping | Экранирование | Double escaping / Двойное | Minimal / Минимальное |
| Server requirement | Требования | Any Spigot | PaperMC only |

---

## Using PlaceholderAPI / Использование PlaceholderAPI

:::note[Requirements / Требования]
Requires Triton v3.7.0 or newer. / Требуется Triton v3.7.0 или новее.
:::

Use any [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) placeholder directly inside translation values. Placeholders are resolved **per-player**.

Используйте любой плейсхолдер [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) прямо в значениях перевода. Плейсхолдеры разрешаются **для каждого игрока**.

```json
{
  "type": "text",
  "key": "welcome.personalized",
  "languages": {
    "en_GB": "&6Welcome, &e%player_name%&6! Balance: &a%vault_eco_balance%",
    "ru_RU": "&6Добро пожаловать, &e%player_name%&6! Баланс: &a%vault_eco_balance%"
  }
}
```

### Popular PlaceholderAPI Placeholders / Популярные плейсхолдеры

| Placeholder | Description (EN) | Описание (RU) |
|---|---|---|
| `%player_name%` | Player's name | Имя игрока |
| `%player_health%` | Player's health | Здоровье игрока |
| `%player_level%` | Player's XP level | Уровень опыта |
| `%vault_eco_balance%` | Economy balance | Баланс экономики |
| `%server_online%` | Online player count | Количество онлайн |
| `%server_tps%` | Server TPS | TPS сервера |

:::info
**EN:** PlaceholderAPI placeholders (`%player_name%`) are different from Triton variables (`%1`, `%2`). PAPI placeholders resolve automatically per-player; Triton variables are passed through `[arg]` tags.

**RU:** Плейсхолдеры PlaceholderAPI (`%player_name%`) отличаются от переменных Triton (`%1`, `%2`). PAPI разрешаются автоматически для каждого игрока; переменные Triton передаются через теги `[arg]`.
:::

---

## Special Prefixes Summary / Сводка специальных префиксов

| Prefix / Префикс | Version / Версия | Server / Сервер | Description (EN) | Описание (RU) |
|---|---|---|---|---|
| `[triton_json]` | v3.1.0+ | Spigot/Paper | JSON chat component | JSON-компонент чата |
| `[minimsg]` | v3.5.1+ | PaperMC only | Kyori MiniMessage format | Формат MiniMessage Kyori |
| _(none)_ | Any | Any | Plain text with `&` color codes | Простой текст с кодами `&` |

Prefixes are placed at the **very beginning** of each language value:

Префиксы размещаются в **самом начале** значения каждого языка:

```json
{
  "languages": {
    "en_GB": "[triton_json]{\"text\":\"Hello!\",\"bold\":true}",
    "ru_RU": "[minimsg]<bold>Привет!</bold>"
  }
}
```

:::info
**EN:** You can use different prefixes for different languages in the same item, but this is uncommon and not recommended for consistency.

**RU:** Можно использовать разные префиксы для разных языков в одном элементе, но это нетипично и не рекомендуется.
:::

---

## Complete Field Reference / Полный справочник полей

| Field / Поле | Required / Обяз. | Type / Тип | Default | Description (EN) | Описание (RU) |
|---|---|---|---|---|---|
| `type` | ✅ | `"text"` | — | Item type identifier | Идентификатор типа |
| `key` | ✅ | `String` | — | Unique translation key | Уникальный ключ перевода |
| `languages` | ✅ | `Object` | — | Translation strings per language | Переводы по языкам |
| `patterns` | ❌ | `String[]` | `[]` | Regex patterns for matching | Регулярные выражения |
| `servers` | ❌ | `String[]` | `[]` | Server filter (BungeeCord) | Фильтр серверов (BungeeCord) |
| `blacklist` | ❌ | `Boolean` | `true` | Blacklist/whitelist mode (BungeeCord) | Режим чёрного/белого списка |

---

## Complete Examples / Полные примеры

### Minimal / Минимальный

```json
{
  "type": "text",
  "key": "hello",
  "languages": {
    "en_GB": "Hello!",
    "ru_RU": "Привет!"
  }
}
```

### With Colors / С цветами

```json
{
  "type": "text",
  "key": "error.generic",
  "languages": {
    "en_GB": "&c&l[ERROR] &fSomething went wrong!",
    "ru_RU": "&c&l[ОШИБКА] &fЧто-то пошло не так!"
  }
}
```

### With Variables / С переменными

```json
{
  "type": "text",
  "key": "player.balance",
  "languages": {
    "en_GB": "&6Your balance: &a%1 coins",
    "ru_RU": "&6Ваш баланс: &a%1 монет"
  }
}
```

### With PlaceholderAPI / С PlaceholderAPI

```json
{
  "type": "text",
  "key": "scoreboard.header",
  "languages": {
    "en_GB": "&6&l%player_name%'s Stats",
    "ru_RU": "&6&lСтатистика %player_name%"
  }
}
```

### With All Optional Fields / Со всеми полями

```json
{
  "type": "text",
  "key": "shop.purchase.success",
  "languages": {
    "en_GB": "&a✔ &fBought &e%1x %2 &ffor &6%3 coins&f. Balance: &a%vault_eco_balance%",
    "ru_RU": "&a✔ &fКупили &e%1x %2 &fза &6%3 монет&f. Баланс: &a%vault_eco_balance%"
  },
  "patterns": [],
  "servers": ["survival-1", "survival-2"],
  "blacklist": false
}
```

### With MiniMessage / С MiniMessage

```json
{
  "type": "text",
  "key": "broadcast.gradient",
  "languages": {
    "en_GB": "[minimsg]<gradient:#ff0000:#00ff00><bold>ATTENTION!</bold></gradient> <yellow>%1",
    "ru_RU": "[minimsg]<gradient:#ff0000:#00ff00><bold>ВНИМАНИЕ!</bold></gradient> <yellow>%1"
  }
}
```

### With JSON Chat / С JSON-чатом

```json
{
  "type": "text",
  "key": "help.clickable",
  "languages": {
    "en_GB": "[triton_json][{\"text\":\"[Help] \",\"color\":\"gold\",\"bold\":true},{\"text\":\"Click for help\",\"color\":\"yellow\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/help\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Click to open\"}}]",
    "ru_RU": "[triton_json][{\"text\":\"[Помощь] \",\"color\":\"gold\",\"bold\":true},{\"text\":\"Нажмите для помощи\",\"color\":\"yellow\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/help\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Нажмите, чтобы открыть\"}}]"
  }
}
```
