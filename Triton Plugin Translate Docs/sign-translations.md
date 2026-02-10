---
description: "Complete reference for sign translation items / Полный справочник по переводам табличек"
sidebar_position: 4
---

# Sign Translations / Переводы табличек

---

## Introduction / Введение

Sign translations (also called **Sign Groups**) allow you to translate the text on in-game Minecraft signs. Unlike text translations, sign translations are tied to specific block locations in the world and consist of exactly 4 lines per language.

Переводы табличек (также называемые **Группами табличек**) позволяют переводить текст на игровых табличках Minecraft. В отличие от текстовых переводов, переводы табличек привязаны к конкретным координатам блоков в мире и состоят ровно из 4 строк на каждый язык.

---

## Basic Structure / Базовая структура

```json
{
  "type": "sign",
  "key": "sign.welcome",
  "lines": {
    "en_GB": ["&6Welcome", "&fto the", "&bServer", "&aEnjoy!"],
    "ru_RU": ["&6Добро", "&fпожаловать на", "&bСервер", "&aПриятной игры!"]
  },
  "locations": [
    {
      "world": "world",
      "x": 100,
      "y": 65,
      "z": 200
    }
  ]
}
```

---

## Required Fields / Обязательные поля

### `type`

**Type / Тип:** `String`
**Value / Значение:** `"sign"`
**Required / Обязательное:** ✅ Always / Всегда

Must always be set to `"sign"` for sign translations. This tells Triton to expect `lines` and `locations` fields.

Должно всегда быть `"sign"` для переводов табличек. Это говорит Triton ожидать поля `lines` и `locations`.

### `key`

**Type / Тип:** `String`
**Required / Обязательное:** ✅ Always / Всегда

A unique identifier for this sign group. Used with the `/triton sign` command to manage signs.

Уникальный идентификатор для этой группы табличек. Используется с командой `/triton sign` для управления табличками.

```
/triton sign <key>
```

:::info
**EN:** Unlike text translations, sign keys are **not** used in `[lang]` placeholders. They are only used for the `/triton sign` command.

**RU:** В отличие от текстовых переводов, ключи табличек **не** используются в плейсхолдерах `[lang]`. Они используются только для команды `/triton sign`.
:::

**Key naming examples / Примеры именования ключей:**
- `sign.spawn` — Spawn sign / Табличка спавна
- `sign.shop.entrance` — Shop entrance sign / Табличка входа в магазин
- `sign.rules.0` — First rules sign / Первая табличка правил
- `sign.game.skywars.1` — SkyWars game sign #1 / Табличка SkyWars #1

### `lines`

**Type / Тип:** `Object` (JSON Object — keys are language IDs (`String`), values are arrays of exactly 4 strings (`String[]`))
**Required / Обязательное:** ✅ Always (for `sign` type) / Всегда (для типа `sign`)

Contains exactly **4 strings** per language, representing the 4 lines of a Minecraft sign from top to bottom.

Содержит ровно **4 строки** на каждый язык, представляющие 4 строки таблички Minecraft сверху вниз.

```json
{
  "lines": {
    "en_GB": ["Line 1 (top)", "Line 2", "Line 3", "Line 4 (bottom)"],
    "ru_RU": ["Строка 1 (верх)", "Строка 2", "Строка 3", "Строка 4 (низ)"]
  }
}
```

:::warning
**EN:** Each language **must** have exactly **4 strings** in its array, even if some lines are empty. Use an empty string `""` for blank lines. Using fewer or more than 4 strings will cause errors.

**RU:** Каждый язык **должен** иметь ровно **4 строки** в массиве, даже если некоторые строки пустые. Используйте пустую строку `""` для пустых строк. Использование меньше или больше 4 строк приведёт к ошибкам.
:::

**Example with empty lines / Пример с пустыми строками:**

```json
{
  "lines": {
    "en_GB": ["&6Shop", "", "&aOpen!", ""],
    "ru_RU": ["&6Магазин", "", "&aОткрыт!", ""]
  }
}
```

**Sign lines and color codes / Строки табличек и цветовые коды:**

Signs support the same `&` color codes as text translations:

Таблички поддерживают те же цветовые коды `&`, что и текстовые переводы:

```json
{
  "lines": {
    "en_GB": ["&a&lGreen Bold", "&6Gold", "&b&oAqua Italic", "&cRed"],
    "ru_RU": ["&a&lЗелёный Жирный", "&6Золотой", "&b&oБирюз. Курсив", "&cКрасный"]
  }
}
```

### `locations`

**Type / Тип:** `Array` (JSON Array of Location objects)
**Required / Обязательное:** ✅ Always (for `sign` type) / Всегда (для типа `sign`)

Defines which signs in the world belong to this sign group. Each location object specifies the exact position of a sign block.

Определяет, какие таблички в мире принадлежат этой группе. Каждый объект местоположения указывает точную позицию блока таблички.

#### Spigot Location Format / Формат местоположения Spigot

```json
{
  "locations": [
    {
      "world": "world",
      "x": 100,
      "y": 65,
      "z": 200
    }
  ]
}
```

#### BungeeCord Location Format / Формат местоположения BungeeCord

When using BungeeCord, you **must** include the `server` field:

При использовании BungeeCord **необходимо** включить поле `server`:

```json
{
  "locations": [
    {
      "server": "lobby",
      "world": "world",
      "x": 100,
      "y": 65,
      "z": 200
    }
  ]
}
```

### Location Fields Reference / Справочник полей местоположения

| Field / Поле | Type / Тип | Required (EN) | Обяз. (RU) | Description (EN) | Описание (RU) |
|---|---|---|---|---|---|
| `world` | `String` | Always | Всегда | World name where the sign exists | Имя мира, где находится табличка |
| `x` | `Integer` | Always | Всегда | X coordinate of the sign block | Координата X блока таблички |
| `y` | `Integer` | Always | Всегда | Y coordinate of the sign block | Координата Y блока таблички |
| `z` | `Integer` | Always | Всегда | Z coordinate of the sign block | Координата Z блока таблички |
| `server` | `String` | BungeeCord only | Только BungeeCord | Server name where the sign exists | Имя сервера, где находится табличка |

:::warning
**EN:** Coordinates must be **integers** (whole numbers), not decimals. They represent the block position, not the player's position. Use `F3` in-game to find the exact block coordinates.

**RU:** Координаты должны быть **целыми числами**, не дробными. Они представляют позицию блока, а не игрока. Используйте `F3` в игре для нахождения точных координат блока.
:::

:::warning
**EN:** The `world` name must match **exactly** with the actual world folder name on the server (case-sensitive). Common names: `world`, `world_nether`, `world_the_end`.

**RU:** Имя мира `world` должно **точно совпадать** с именем папки мира на сервере (с учётом регистра). Типичные имена: `world`, `world_nether`, `world_the_end`.
:::

---

## Multiple Signs in One Group / Несколько табличек в одной группе

A single sign group can contain multiple signs that share the same translated text. This is useful for signs that appear in multiple locations but show the same content.

Одна группа табличек может содержать несколько табличек с одинаковым переведённым текстом. Это полезно для табличек в разных местах с одинаковым содержимым.

```json
{
  "type": "sign",
  "key": "sign.rules",
  "lines": {
    "en_GB": ["&c&lRules", "&f1. Be nice", "&f2. No griefing", "&f3. Have fun"],
    "ru_RU": ["&c&lПравила", "&f1. Будьте добры", "&f2. Без гриферства", "&f3. Играйте"]
  },
  "locations": [
    {
      "world": "world",
      "x": 100,
      "y": 65,
      "z": 200
    },
    {
      "world": "world",
      "x": 105,
      "y": 65,
      "z": 200
    },
    {
      "world": "world_nether",
      "x": 50,
      "y": 70,
      "z": 50
    }
  ]
}
```

All three signs will display the same translated content based on the player's language.

Все три таблички будут отображать одинаковый переведённый контент в зависимости от языка игрока.

---

## Dynamic Signs / Динамические таблички

:::note[Requirements / Требования]
Requires Triton v2.3.0 or later. / Требуется Triton v2.3.0 или новее.
:::

Some signs display dynamic information (player counts, game status, etc.). You can preserve dynamic lines by using the special placeholder `%use_line_default%`.

Некоторые таблички отображают динамическую информацию (количество игроков, статус игры и т.д.). Вы можете сохранить динамические строки с помощью специального плейсхолдера `%use_line_default%`.

When a line is set to `%use_line_default%`, Triton will **not** translate that line and instead show the original sign content, allowing other plugins to update it dynamically.

Когда строка установлена в `%use_line_default%`, Triton **не** будет переводить эту строку, а вместо этого покажет оригинальное содержимое таблички, позволяя другим плагинам динамически обновлять его.

### Example: Game Join Sign / Пример: табличка входа в игру

Suppose a minigame plugin creates a sign: / Допустим, плагин мини-игр создаёт табличку:

```
[SkyWars]
Game #3
Waiting
12/16
```

You want to translate line 1 but keep the rest dynamic:

Вы хотите перевести строку 1, но оставить остальные динамическими:

```json
{
  "type": "sign",
  "key": "sign.skywars.join",
  "lines": {
    "en_GB": ["&b[SkyWars]", "%use_line_default%", "%use_line_default%", "%use_line_default%"],
    "ru_RU": ["&b[НебоВойны]", "%use_line_default%", "%use_line_default%", "%use_line_default%"]
  },
  "locations": [
    {
      "world": "lobby",
      "x": 10,
      "y": 64,
      "z": 20
    }
  ]
}
```

**Result / Результат:**
- **Line 1** — translated per player language / переводится по языку игрока
- **Lines 2, 3, 4** — show original content from the minigame plugin / показывают оригинальный контент от плагина мини-игр

### Combining Dynamic Signs with Patterns / Комбинирование динамических табличек с паттернами

You can combine dynamic signs with [patterns](../concepts/patterns.md) to translate the dynamic content itself:

Можно комбинировать динамические таблички с [паттернами](../concepts/patterns.md) для перевода самого динамического контента:

For example, translate game statuses like "Waiting" → "Ожидание":

Например, перевести статусы игры "Waiting" → "Ожидание":

```json
{
  "type": "text",
  "key": "game.status.waiting",
  "languages": {
    "en_GB": "&eWaiting",
    "ru_RU": "&eОжидание"
  },
  "patterns": ["^Waiting$"]
}
```

---

## Finding Sign Coordinates / Поиск координат табличек

To find the coordinates of a sign for the `locations` field:

Чтобы найти координаты таблички для поля `locations`:

### Method 1: F3 Debug Screen / Метод 1: Экран отладки F3

1. Stand near the sign in-game. / Встаньте рядом с табличкой в игре.
2. Look at the sign and press **F3** to open the debug screen. / Посмотрите на табличку и нажмите **F3** для открытия экрана отладки.
3. Find the **"Looking at"** or **"Targeted Block"** line showing coordinates. / Найдите строку **"Looking at"** или **"Targeted Block"** с координатами.
4. Use those X, Y, Z values in your location object. / Используйте эти значения X, Y, Z в объекте местоположения.

### Method 2: /triton sign Command / Метод 2: Команда /triton sign

Use the built-in command which can auto-detect the sign you're looking at:

Используйте встроенную команду, которая автоматически определяет табличку, на которую вы смотрите:

```
/triton sign
```

This is the easier method and avoids manual coordinate entry.

Это более простой метод, который избавляет от ручного ввода координат.

---

## Sign Translation Nuances / Нюансы переводов табличек

### Signs vs Text Differences / Различия табличек и текста

| Feature (EN) | Особенность (RU) | Text (`"type": "text"`) | Sign (`"type": "sign"`) |
|---|---|---|---|
| Content field | Поле контента | `languages` (strings) | `lines` (arrays of 4 strings) |
| Used in placeholders | В плейсхолдерах | Yes (`[lang]key[/lang]`) / Да | No / Нет |
| Requires locations | Нужны координаты | No / Нет | Yes / Да |
| Supports variables | Поддержка переменных | Yes (`%1`, `%2`) / Да | No / Нет |
| Supports patterns | Поддержка паттернов | Yes / Да | No / Нет |
| Supports server filtering | Фильтрация серверов | Yes / Да | No / Нет |
| Dynamic content | Динамический контент | Via variables / Через переменные | Via `%use_line_default%` |

### Line Length Limits / Ограничения длины строк

Minecraft signs have a character limit per line (approximately 15-16 characters depending on the font width). If your translation is too long, it will be visually truncated on the sign.

Таблички Minecraft имеют ограничение символов на строку (примерно 15-16 символов в зависимости от ширины шрифта). Если перевод слишком длинный, он будет визуально обрезан на табличке.

:::tip
**EN:** Keep sign translations short. If you need more text, use multiple signs in the same group.

**RU:** Делайте переводы табличек краткими. Если нужно больше текста, используйте несколько табличек в одной группе.
:::

### Sign Types / Типы табличек

Triton works with all sign types in Minecraft:

Triton работает со всеми типами табличек в Minecraft:

- Oak signs / Дубовые таблички
- Spruce signs / Еловые таблички
- Birch signs / Берёзовые таблички
- Jungle signs / Тропические таблички
- Acacia signs / Акациевые таблички
- Dark Oak signs / Таблички из тёмного дуба
- Crimson signs / Багровые таблички
- Warped signs / Искажённые таблички
- Mangrove signs / Мангровые таблички
- Cherry signs / Вишнёвые таблички
- Bamboo signs / Бамбуковые таблички
- Wall signs (mounted on walls) / Настенные таблички
- Standing signs (on posts) / Стоячие таблички
- Hanging signs / Подвесные таблички

---

## Complete Field Reference / Полный справочник полей

| Field / Поле | Required / Обяз. | Type / Тип | Description (EN) | Описание (RU) |
|---|---|---|---|---|
| `type` | ✅ | `"sign"` | Item type — must be `"sign"` | Тип элемента — должен быть `"sign"` |
| `key` | ✅ | `String` | Unique sign group key | Уникальный ключ группы табличек |
| `lines` | ✅ | `Object` | Sign lines per language (4 strings per language) | Строки таблички по языкам (4 строки на язык) |
| `locations` | ✅ | `Array` | Sign block coordinates (with optional `server` for BungeeCord) | Координаты блоков табличек (`server` для BungeeCord) |

---

## Complete Examples / Полные примеры

### Simple Sign / Простая табличка

```json
{
  "type": "sign",
  "key": "sign.spawn",
  "lines": {
    "en_GB": ["&6═══════", "&a&lSPAWN", "&fRight-click", "&6═══════"],
    "ru_RU": ["&6═══════", "&a&lСПАВН", "&fПКМ", "&6═══════"]
  },
  "locations": [
    { "world": "world", "x": 0, "y": 65, "z": 0 }
  ]
}
```

### Multi-Location Sign / Табличка в нескольких местах

```json
{
  "type": "sign",
  "key": "sign.shop.entrance",
  "lines": {
    "en_GB": ["&6&l═══════", "&a&lSHOP", "&fClick to enter", "&6&l═══════"],
    "ru_RU": ["&6&l═══════", "&a&lМАГАЗИН", "&fНажмите для входа", "&6&l═══════"]
  },
  "locations": [
    { "world": "world", "x": 150, "y": 64, "z": -30 },
    { "world": "world", "x": 155, "y": 64, "z": -30 },
    { "world": "world", "x": 160, "y": 64, "z": -30 }
  ]
}
```

### BungeeCord Sign / Табличка BungeeCord

```json
{
  "type": "sign",
  "key": "sign.lobby.info",
  "lines": {
    "en_GB": ["&b&lINFO", "&fServer: Lobby", "&fPlayers online:", "&aEnjoy your stay!"],
    "ru_RU": ["&b&lИНФО", "&fСервер: Лобби", "&fИгроков онлайн:", "&aПриятной игры!"]
  },
  "locations": [
    { "server": "lobby", "world": "world", "x": 10, "y": 64, "z": 20 },
    { "server": "lobby", "world": "world", "x": 15, "y": 64, "z": 20 }
  ]
}
```

### Dynamic Sign / Динамическая табличка

```json
{
  "type": "sign",
  "key": "sign.bedwars.join",
  "lines": {
    "en_GB": ["&c&l[BedWars]", "%use_line_default%", "%use_line_default%", "%use_line_default%"],
    "ru_RU": ["&c&l[БедВарс]", "%use_line_default%", "%use_line_default%", "%use_line_default%"]
  },
  "locations": [
    { "world": "lobby", "x": 25, "y": 64, "z": 30 }
  ]
}
```

### Multi-Language Sign / Мультиязычная табличка

```json
{
  "type": "sign",
  "key": "sign.welcome.multilang",
  "lines": {
    "en_GB": ["&6Welcome!", "&fto our", "&bserver", "&aHave fun!"],
    "ru_RU": ["&6Добро", "&fпожаловать", "&bна сервер", "&aПриятной игры!"],
    "de_DE": ["&6Willkommen!", "&fauf unserem", "&bServer", "&aViel Spaß!"],
    "fr_FR": ["&6Bienvenue !", "&fsur notre", "&bserveur", "&aAmusez-vous !"],
    "es_ES": ["&6¡Bienvenido!", "&fa nuestro", "&bservidor", "&a¡Disfruta!"]
  },
  "locations": [
    { "world": "world", "x": 0, "y": 65, "z": 0 }
  ]
}
```
