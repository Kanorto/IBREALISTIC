---
description: "Real-world examples, common mistakes, best practices / Реальные примеры, типичные ошибки, лучшие практики"
sidebar_position: 6
---

# Examples & Best Practices / Примеры и лучшие практики

---

## Complete Collection File Examples / Полные примеры файлов коллекций

### Example 1: Simple Spigot Collection / Пример 1: Простая коллекция Spigot

A basic collection file for a standalone Spigot server with text and sign translations:

Базовый файл коллекции для автономного сервера Spigot с текстовыми переводами и табличками:

```json title="plugins/Triton/translations/default.json"
[
  {
    "type": "text",
    "key": "welcome.join",
    "languages": {
      "en_GB": "&6Welcome to the server, &e%1&6!",
      "ru_RU": "&6Добро пожаловать на сервер, &e%1&6!"
    }
  },
  {
    "type": "text",
    "key": "welcome.first_join",
    "languages": {
      "en_GB": "&a&l★ &e%1 &ajoined for the first time! &a&l★",
      "ru_RU": "&a&l★ &e%1 &aвпервые зашёл на сервер! &a&l★"
    }
  },
  {
    "type": "text",
    "key": "error.no_permission",
    "languages": {
      "en_GB": "&cYou don't have permission to do that!",
      "ru_RU": "&cУ вас нет прав для этого действия!"
    }
  },
  {
    "type": "text",
    "key": "error.player_not_found",
    "languages": {
      "en_GB": "&cPlayer &e%1 &cnot found!",
      "ru_RU": "&cИгрок &e%1 &cне найден!"
    }
  },
  {
    "type": "sign",
    "key": "sign.spawn",
    "lines": {
      "en_GB": ["&6&l═══════", "&a&lSPAWN", "&fRight-click", "&6&l═══════"],
      "ru_RU": ["&6&l═══════", "&a&lСПАВН", "&fПКМ для телепортации", "&6&l═══════"]
    },
    "locations": [
      { "world": "world", "x": 0, "y": 65, "z": 0 }
    ]
  }
]
```

### Example 2: Plugin-Specific Collection / Пример 2: Коллекция для плагина

A dedicated collection for translating the CMI plugin:

Выделенная коллекция для перевода плагина CMI:

```json title="plugins/Triton/translations/cmi.json"
[
  {
    "type": "text",
    "key": "cmi.info.NoPermission",
    "languages": {
      "en_GB": "&cYou don't have permission!",
      "ru_RU": "&cУ вас нет прав!"
    }
  },
  {
    "type": "text",
    "key": "cmi.info.NoPlayerPermission",
    "languages": {
      "en_GB": "&c%1 doesn't have permission for: %2",
      "ru_RU": "&c%1 не имеет прав на: %2"
    }
  },
  {
    "type": "text",
    "key": "cmi.info.Ingame",
    "languages": {
      "en_GB": "&cYou can only use this in game!",
      "ru_RU": "&cЭту команду можно использовать только в игре!"
    }
  },
  {
    "type": "text",
    "key": "cmi.info.NoInformation",
    "languages": {
      "en_GB": "&cNo information found!",
      "ru_RU": "&cИнформация не найдена!"
    }
  },
  {
    "type": "text",
    "key": "cmi.teleport.success",
    "languages": {
      "en_GB": "&aTeleported to &e%1&a!",
      "ru_RU": "&aТелепортирован к &e%1&a!"
    }
  },
  {
    "type": "text",
    "key": "cmi.home.set",
    "languages": {
      "en_GB": "&aHome &e%1 &ahas been set!",
      "ru_RU": "&aДом &e%1 &aбыл установлен!"
    }
  }
]
```

### Example 3: BungeeCord Network Collection / Пример 3: Коллекция для сети BungeeCord

A collection for a BungeeCord network with server-specific messages:

Коллекция для сети BungeeCord с серверо-специфичными сообщениями:

```json title="plugins/Triton/translations/network.json"
{
  "metadata": {
    "blacklist": true,
    "servers": []
  },
  "items": [
    {
      "type": "text",
      "key": "network.motd",
      "languages": {
        "en_GB": "&6&lMy Network &8- &aOnline!",
        "ru_RU": "&6&lМоя Сеть &8- &aОнлайн!"
      }
    },
    {
      "type": "text",
      "key": "network.server_switch",
      "languages": {
        "en_GB": "&aSending you to &e%1&a...",
        "ru_RU": "&aОтправляем вас на &e%1&a..."
      }
    },
    {
      "type": "text",
      "key": "lobby.welcome",
      "languages": {
        "en_GB": "&bWelcome to the &6&lLobby&b!",
        "ru_RU": "&bДобро пожаловать в &6&lЛобби&b!"
      },
      "servers": ["lobby-1", "lobby-2"],
      "blacklist": false
    },
    {
      "type": "text",
      "key": "survival.welcome",
      "languages": {
        "en_GB": "&2Welcome to &a&lSurvival&2! Good luck!",
        "ru_RU": "&2Добро пожаловать на &a&lВыживание&2! Удачи!"
      },
      "servers": ["survival"],
      "blacklist": false
    },
    {
      "type": "text",
      "key": "creative.welcome",
      "languages": {
        "en_GB": "&eWelcome to &6&lCreative&e! Build freely!",
        "ru_RU": "&eДобро пожаловать на &6&lТворческий&e! Стройте свободно!"
      },
      "servers": ["creative"],
      "blacklist": false
    }
  ]
}
```

### Example 4: Advanced Features Collection / Пример 4: Коллекция с продвинутыми возможностями

Patterns, JSON chat, MiniMessage, dynamic signs, PlaceholderAPI:

Паттерны, JSON-чат, MiniMessage, динамические таблички, PlaceholderAPI:

```json title="plugins/Triton/translations/advanced.json"
[
  {
    "type": "text",
    "key": "pattern.death.generic",
    "languages": {
      "en_GB": "&7%1 &fdied",
      "ru_RU": "&7%1 &fпогиб"
    },
    "patterns": [
      "^(\\w+) died$",
      "^(\\w+) was killed$"
    ]
  },
  {
    "type": "text",
    "key": "pattern.death.pvp",
    "languages": {
      "en_GB": "&c%1 &fwas slain by &c%2",
      "ru_RU": "&c%1 &fбыл убит игроком &c%2"
    },
    "patterns": [
      "^(\\w+) was slain by (\\w+)$"
    ]
  },
  {
    "type": "text",
    "key": "pattern.death.fall",
    "languages": {
      "en_GB": "&7%1 &ffell to their death",
      "ru_RU": "&7%1 &fразбился насмерть"
    },
    "patterns": [
      "^(\\w+) fell from a high place$",
      "^(\\w+) hit the ground too hard$"
    ]
  },
  {
    "type": "text",
    "key": "clickable.website",
    "languages": {
      "en_GB": "[triton_json]{\"text\":\"§6§l[Click] §fVisit our website!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://example.com\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"§eClick to open\"}}",
      "ru_RU": "[triton_json]{\"text\":\"§6§l[Клик] §fПосетите наш сайт!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://example.com\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"§eНажмите для открытия\"}}"
    }
  },
  {
    "type": "text",
    "key": "clickable.discord",
    "languages": {
      "en_GB": "[triton_json]{\"text\":\"§9§l[Discord] §fJoin our community!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://discord.gg/example\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"§eClick to join\"}}",
      "ru_RU": "[triton_json]{\"text\":\"§9§l[Дискорд] §fПрисоединяйтесь к сообществу!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://discord.gg/example\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"§eНажмите для входа\"}}"
    }
  },
  {
    "type": "text",
    "key": "minimsg.announcement",
    "languages": {
      "en_GB": "[minimsg]<gradient:#ff0000:#0000ff><bold>SERVER ANNOUNCEMENT</bold></gradient><newline><yellow>%1",
      "ru_RU": "[minimsg]<gradient:#ff0000:#0000ff><bold>ОБЪЯВЛЕНИЕ СЕРВЕРА</bold></gradient><newline><yellow>%1"
    }
  },
  {
    "type": "text",
    "key": "minimsg.vote",
    "languages": {
      "en_GB": "[minimsg]<green>%1</green> voted and received <gold>%2 coins</gold>! <click:open_url:'https://example.com/vote'><aqua>[Vote Now]</aqua></click>",
      "ru_RU": "[minimsg]<green>%1</green> проголосовал и получил <gold>%2 монет</gold>! <click:open_url:'https://example.com/vote'><aqua>[Голосовать]</aqua></click>"
    }
  },
  {
    "type": "text",
    "key": "papi.scoreboard.header",
    "languages": {
      "en_GB": "&6&l%player_name%'s Stats &7| &aPing: %player_ping%ms",
      "ru_RU": "&6&lСтатистика %player_name% &7| &aПинг: %player_ping%мс"
    }
  },
  {
    "type": "sign",
    "key": "sign.game.skywars",
    "lines": {
      "en_GB": ["&b&l[SkyWars]", "%use_line_default%", "%use_line_default%", "%use_line_default%"],
      "ru_RU": ["&b&l[НебоВойны]", "%use_line_default%", "%use_line_default%", "%use_line_default%"]
    },
    "locations": [
      { "world": "lobby", "x": 10, "y": 64, "z": 20 },
      { "world": "lobby", "x": 12, "y": 64, "z": 20 }
    ]
  },
  {
    "type": "sign",
    "key": "sign.game.bedwars",
    "lines": {
      "en_GB": ["&c&l[BedWars]", "%use_line_default%", "%use_line_default%", "%use_line_default%"],
      "ru_RU": ["&c&l[БедВарс]", "%use_line_default%", "%use_line_default%", "%use_line_default%"]
    },
    "locations": [
      { "world": "lobby", "x": 14, "y": 64, "z": 20 }
    ]
  }
]
```

### Example 5: Multi-Language Collection / Пример 5: Мультиязычная коллекция

Supporting many languages on a large international server:

Поддержка множества языков на крупном международном сервере:

```json title="plugins/Triton/translations/multilang.json"
[
  {
    "type": "text",
    "key": "menu.language.title",
    "languages": {
      "en_GB": "&6Select your language",
      "ru_RU": "&6Выберите язык",
      "uk_UA": "&6Виберіть мову",
      "de_DE": "&6Wähle deine Sprache",
      "fr_FR": "&6Choisissez votre langue",
      "es_ES": "&6Selecciona tu idioma",
      "pt_PT": "&6Selecione seu idioma",
      "it_IT": "&6Scegli la tua lingua",
      "zh_CN": "&6选择你的语言",
      "ja_JP": "&6言語を選択してください",
      "ko_KR": "&6언어를 선택하세요"
    }
  },
  {
    "type": "text",
    "key": "menu.language.selected",
    "languages": {
      "en_GB": "&aLanguage changed to &eEnglish&a!",
      "ru_RU": "&aЯзык изменён на &eРусский&a!",
      "uk_UA": "&aМова змінена на &eУкраїнську&a!",
      "de_DE": "&aSprache auf &eDeutsch &ageändert!",
      "fr_FR": "&aLangue changée en &eFrançais &a!",
      "es_ES": "&a¡Idioma cambiado a &eEspañol&a!",
      "pt_PT": "&aIdioma alterado para &ePortuguês&a!",
      "it_IT": "&aLingua cambiata in &eItaliano&a!",
      "zh_CN": "&a语言已更改为&e中文&a！",
      "ja_JP": "&a言語が&e日本語&aに変更されました！",
      "ko_KR": "&a언어가 &e한국어&a로 변경되었습니다!"
    }
  },
  {
    "type": "text",
    "key": "chat.global.format",
    "languages": {
      "en_GB": "&7[Global] &f%1",
      "ru_RU": "&7[Глобальный] &f%1",
      "uk_UA": "&7[Глобальний] &f%1",
      "de_DE": "&7[Global] &f%1",
      "fr_FR": "&7[Global] &f%1",
      "es_ES": "&7[Global] &f%1",
      "pt_PT": "&7[Global] &f%1"
    }
  }
]
```

### Example 6: EssentialsX Translation / Пример 6: Перевод EssentialsX

Translating EssentialsX messages with variables:

Перевод сообщений EssentialsX с переменными:

```json title="plugins/Triton/translations/essentials.json"
[
  {
    "type": "text",
    "key": "essentials.teleport.success",
    "languages": {
      "en_GB": "&6Teleporting to &c%1&6...",
      "ru_RU": "&6Телепортация к &c%1&6..."
    }
  },
  {
    "type": "text",
    "key": "essentials.home.set",
    "languages": {
      "en_GB": "&6Home &c%1 &6set.",
      "ru_RU": "&6Дом &c%1 &6установлен."
    }
  },
  {
    "type": "text",
    "key": "essentials.balance",
    "languages": {
      "en_GB": "&6Balance: &a$%1",
      "ru_RU": "&6Баланс: &a$%1"
    }
  },
  {
    "type": "text",
    "key": "essentials.pay.sent",
    "languages": {
      "en_GB": "&6You sent &a$%1 &6to &c%2&6.",
      "ru_RU": "&6Вы отправили &a$%1 &6игроку &c%2&6."
    }
  },
  {
    "type": "text",
    "key": "essentials.pay.received",
    "languages": {
      "en_GB": "&6You received &a$%1 &6from &c%2&6.",
      "ru_RU": "&6Вы получили &a$%1 &6от игрока &c%2&6."
    }
  },
  {
    "type": "text",
    "key": "essentials.msg.to",
    "languages": {
      "en_GB": "&6[&cme &6→ &c%1&6] &f%2",
      "ru_RU": "&6[&cя &6→ &c%1&6] &f%2"
    }
  },
  {
    "type": "text",
    "key": "essentials.msg.from",
    "languages": {
      "en_GB": "&6[&c%1 &6→ &cme&6] &f%2",
      "ru_RU": "&6[&c%1 &6→ &cмне&6] &f%2"
    }
  }
]
```

---

## Best Practices / Лучшие практики

### 1. Key Naming Convention / Соглашение об именовании ключей

Use a consistent, hierarchical naming scheme:

Используйте единообразную иерархическую схему именования:

```
<plugin>.<category>.<message>
```

| Key | Description (EN) | Описание (RU) |
|---|---|---|
| `cmi.info.NoPermission` | CMI no permission | CMI: нет прав |
| `essentials.teleport.success` | Essentials teleport success | Essentials: успешная телепортация |
| `shop.buy.confirm` | Shop purchase confirm | Магазин: подтверждение покупки |
| `menu.main.title` | Main menu title | Главное меню: заголовок |
| `sign.spawn.welcome` | Spawn welcome sign | Табличка приветствия спавна |
| `death.pvp.kill` | PvP kill message | Сообщение убийства PvP |
| `chat.format.global` | Global chat format | Формат глобального чата |

### 2. One Collection Per Plugin / Одна коллекция на плагин

```
translations/
├── default.json          ← core server messages / основные сообщения
├── essentials.json       ← EssentialsX
├── cmi.json              ← CMI
├── shops.json            ← Shop plugin / магазин
├── signs.json            ← All signs / все таблички
├── minigames.json        ← Minigames / мини-игры
└── custom.json           ← Custom messages / свои сообщения
```

### 3. Always Include the Main Language / Всегда включайте основной язык

Always include a translation for your `main-language` (from `config.yml`). It serves as the final fallback:

Всегда включайте перевод для `main-language` (из `config.yml`). Он является последним запасным:

```json
{
  "type": "text",
  "key": "example",
  "languages": {
    "en_GB": "This is required if en_GB is your main language",
    "ru_RU": "Это обязательно, если en_GB — ваш основной язык"
  }
}
```

### 4. Consistent Color Codes / Единообразные цветовые коды

Keep the same color codes across all languages for visual consistency:

Сохраняйте одинаковые цветовые коды для всех языков:

```json
{
  "languages": {
    "en_GB": "&c[Error] &fSomething went wrong!",
    "ru_RU": "&c[Ошибка] &fЧто-то пошло не так!"
  }
}
```

### 5. Unique Keys Across All Collections / Уникальные ключи среди всех коллекций

Keys must be unique across **all** collection files. Duplicate keys cause unpredictable behavior.

Ключи должны быть уникальными среди **всех** файлов коллекций. Дубликаты ведут к непредсказуемому поведению.

### 6. Validate JSON Before Deploying / Валидируйте JSON перед развёртыванием

| Tool / Инструмент | URL |
|---|---|
| JSONLint | https://jsonlint.com/ |
| JSON Formatter | https://jsonformatter.org/json-pretty-print |
| Visual Studio Code | https://code.visualstudio.com |

### 7. Use `/triton reload` After Changes / Используйте `/triton reload` после изменений

Always reload after editing translation files. Changes don't take effect until reload.

Всегда перезагружайте после редактирования файлов. Изменения не вступают в силу до перезагрузки.

### 8. Test with Multiple Languages / Тестируйте с несколькими языками

After adding translations, switch between languages in-game using `/triton` to verify all translations display correctly.

После добавления переводов переключайтесь между языками в игре с помощью `/triton`, чтобы проверить корректное отображение.

---

## Common Mistakes / Типичные ошибки

### ❌ Trailing Comma / Висячая запятая

```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } },
]
```

**Fix / Исправление:** Remove the comma after the last item. / Удалите запятую после последнего элемента.

```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } }
]
```

### ❌ Missing `type` Field / Отсутствует поле `type`

```json
{ "key": "welcome", "languages": { "en_GB": "Welcome!" } }
```

**Fix / Исправление:** Always include `"type": "text"` or `"type": "sign"`. / Всегда включайте `"type": "text"` или `"type": "sign"`.

```json
{ "type": "text", "key": "welcome", "languages": { "en_GB": "Welcome!" } }
```

### ❌ Using `languages` for Signs / Использование `languages` для табличек

```json
{ "type": "sign", "key": "sign.0", "languages": { "en_GB": "Line 1" } }
```

**Fix / Исправление:** Signs use `lines` (arrays of 4 strings) + `locations`. / Таблички используют `lines` (массивы из 4 строк) + `locations`.

```json
{
  "type": "sign",
  "key": "sign.0",
  "lines": { "en_GB": ["Line 1", "Line 2", "Line 3", "Line 4"] },
  "locations": [{ "world": "world", "x": 0, "y": 64, "z": 0 }]
}
```

### ❌ Wrong Number of Sign Lines / Неправильное количество строк таблички

```json
{ "type": "sign", "key": "sign.0", "lines": { "en_GB": ["Line 1", "Line 2"] } }
```

**Fix / Исправление:** Always exactly 4 lines. / Всегда ровно 4 строки.

```json
{ "type": "sign", "key": "sign.0", "lines": { "en_GB": ["Line 1", "Line 2", "", ""] } }
```

### ❌ Missing Sign Locations / Отсутствуют координаты таблички

```json
{ "type": "sign", "key": "sign.0", "lines": { "en_GB": ["1", "2", "3", "4"] } }
```

**Fix / Исправление:** Add `locations` field. / Добавьте поле `locations`.

```json
{
  "type": "sign",
  "key": "sign.0",
  "lines": { "en_GB": ["1", "2", "3", "4"] },
  "locations": [{ "world": "world", "x": 100, "y": 65, "z": 200 }]
}
```

### ❌ Wrong Language IDs / Неправильные идентификаторы языков

```json
{ "type": "text", "key": "test", "languages": { "english": "Hi!", "russian": "Привет!" } }
```

**Fix / Исправление:** Use IDs from `config.yml`. / Используйте ID из `config.yml`.

```json
{ "type": "text", "key": "test", "languages": { "en_GB": "Hi!", "ru_RU": "Привет!" } }
```

### ❌ Unescaped Regex in Patterns / Неэкранированный regex в паттернах

```json
{ "patterns": ["Player (someone) joined."] }
```

**Fix / Исправление:** Escape special regex characters with `\\`. / Экранируйте спецсимволы regex с `\\`.

```json
{ "patterns": ["Player \\(someone\\) joined\\."] }
```

### ❌ Duplicate Keys / Дублированные ключи

```json title="chat.json"
[{ "type": "text", "key": "welcome", "languages": { "en_GB": "Hi!" } }]
```
```json title="greetings.json"
[{ "type": "text", "key": "welcome", "languages": { "en_GB": "Hello!" } }]
```

**Fix / Исправление:** Use unique keys: `chat.welcome` and `greetings.welcome`. / Используйте уникальные ключи.

### ❌ Single Quotes in JSON / Одинарные кавычки в JSON

```json
[{ 'type': 'text', 'key': 'a', 'languages': { 'en_GB': 'Hi' } }]
```

**Fix / Исправление:** JSON only accepts double quotes `"`. / JSON принимает только двойные кавычки `"`.

```json
[{ "type": "text", "key": "a", "languages": { "en_GB": "Hi" } }]
```

### ❌ Missing Comma Between Items / Пропущена запятая между элементами

```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } }
  { "type": "text", "key": "b", "languages": { "en_GB": "Bye" } }
]
```

**Fix / Исправление:** Add a comma between items. / Добавьте запятую между элементами.

```json
[
  { "type": "text", "key": "a", "languages": { "en_GB": "Hi" } },
  { "type": "text", "key": "b", "languages": { "en_GB": "Bye" } }
]
```

### ❌ Using Decimal Coordinates for Signs / Использование дробных координат для табличек

```json
{ "locations": [{ "world": "world", "x": 10.5, "y": 64.0, "z": 20.3 }] }
```

**Fix / Исправление:** Use integers (whole numbers) only. / Используйте только целые числа.

```json
{ "locations": [{ "world": "world", "x": 10, "y": 64, "z": 20 }] }
```

### ❌ Forgetting `server` Field on BungeeCord Signs / Забыли поле `server` на табличках BungeeCord

```json
{ "locations": [{ "world": "world", "x": 10, "y": 64, "z": 20 }] }
```

**Fix / Исправление:** Add the `server` field on BungeeCord. / Добавьте поле `server` на BungeeCord.

```json
{ "locations": [{ "server": "lobby", "world": "world", "x": 10, "y": 64, "z": 20 }] }
```

---

## Workflow Checklist / Чеклист рабочего процесса

When creating translations manually, follow this checklist:

При создании переводов вручную следуйте этому чеклисту:

1. ✅ Create or choose a collection file in `translations/` / Создайте или выберите файл коллекции в `translations/`
2. ✅ Add the translation item with all required fields / Добавьте элемент перевода со всеми обязательными полями
3. ✅ Set correct `type`: `"text"` or `"sign"` / Установите правильный `type`: `"text"` или `"sign"`
4. ✅ Use unique `key` across all collections / Используйте уникальный `key` среди всех коллекций
5. ✅ Use language IDs matching your `config.yml` / Используйте ID языков из `config.yml`
6. ✅ Include the main language translation as fallback / Включите перевод основного языка
7. ✅ For signs: provide exactly 4 lines per language / Для табличек: ровно 4 строки на язык
8. ✅ For signs: provide all `locations` with correct coordinates / Для табличек: все `locations` с правильными координатами
9. ✅ For BungeeCord signs: include `server` in each location / Для BungeeCord табличек: включите `server` в каждую локацию
10. ✅ Replace variables with `%1`, `%2`, etc. / Замените переменные на `%1`, `%2` и т.д.
11. ✅ Validate JSON syntax with a validator tool / Проверьте синтаксис JSON валидатором
12. ✅ Save the file and run `/triton reload` / Сохраните файл и выполните `/triton reload`
13. ✅ Replace original plugin messages with `[lang]key[/lang]` / Замените оригинальные сообщения на `[lang]key[/lang]`
14. ✅ Test in-game with different languages / Протестируйте в игре с разными языками
15. ✅ Verify fallback behavior for missing translations / Проверьте работу запасного языка
