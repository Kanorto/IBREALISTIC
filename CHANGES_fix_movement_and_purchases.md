# Изменения: Исправление движения в реалистичных режимах и инвентарь покупок

## Дата
2026-02-17

## Краткое описание
Исправлена критическая ошибка, из-за которой лодка не могла двигаться в реалистичных режимах. Добавлена система постоянного инвентаря покупок — купленные детали сохраняются навсегда и могут быть бесплатно переустановлены на любую машину.

## Изменённые файлы

### Мод (IBRealistic)
- `IBRealistic/src/main/java/dev/kanorto/ibrealistic/mixin/BoatMixin.java` — Исправлена проблема с определением земли и добавлена гравитация

### Плагин (TimingSystem)
- `TimingSystem/src/main/java/me/makkuusen/timing/system/database/updates/Version26.java` — Новая миграция БД
- `TimingSystem/src/main/java/me/makkuusen/timing/system/database/SQLiteDatabase.java` — Версия БД 25 → 26
- `TimingSystem/src/main/java/me/makkuusen/timing/system/database/MySQLDatabase.java` — Версия БД 25 → 26
- `TimingSystem/src/main/java/me/makkuusen/timing/system/economy/GarageManager.java` — Методы инвентаря покупок
- `TimingSystem/src/main/java/me/makkuusen/timing/system/gui/ShopGui.java` — Обновлён UI магазина
- `TimingSystem/src/main/java/me/makkuusen/timing/system/theme/messages/Gui.java` — Новые enum значения

### Переводы
- Все 10 языковых файлов (`en_us`, `de_de`, `id_id`, `zh_cn`, `pl_pl`, `nl_nl`, `es_es`, `pt_br`, `fr_fr`, `triton`) — Добавлены 2 новых ключа перевода

## Детальное описание изменений

### 1. Исправление движения в реалистичных режимах (КРИТИЧЕСКИЙ БАГ)

**Файл:** `IBRealistic/src/main/java/dev/kanorto/ibrealistic/mixin/BoatMixin.java`

**Корневая причина:**
Когда реалистичная физика активна, метод `updateVelocity()` отменяется миксином `cancelVanillaVelocityDecay`. Но этот метод также применяет гравитацию! Без гравитации:
1. Лодка не прижимается к земле
2. `Entity.move()` не обнаруживает контакт с землёй (нет столкновения по вертикали)
3. Флаг `onGround` становится `false` после первого тика
4. Физический движок переключается в режим полёта (`airborne = true`)
5. В режиме полёта нет тяги двигателя — лодка не может двигаться

**Исправления:**

1. **Fallback-проверка земли** (`hasSolidBlockBelow`): Проверяет наличие твёрдых блоков непосредственно под лодкой. Используется как резервный метод когда `isOnGround()` ненадёжен.

2. **Гравитация контакта с землёй** (`GROUND_SNAP_VELOCITY = -0.04f`): Когда лодка на земле, применяется малая нисходящая скорость чтобы `Entity.move()` обнаружил контакт с землёй и установил `onGround = true` для следующего тика.

### 2. Постоянный инвентарь покупок

**Проблема:** Когда игрок покупал деталь и потом менял её на другую, предыдущая покупка "терялась" — приходилось покупать снова.

**Решение:**

#### Таблица БД (`ts_player_purchases`)
- `uuid` — UUID игрока
- `component` — тип компонента (tire, engine, body, etc.)
- `preset_id` — ID пресета
- `purchased_at` — дата покупки
- UNIQUE constraint на (uuid, component, preset_id)

#### GarageManager (новые методы)
- `hasPurchased(uuid, component, presetId)` — проверяет, куплен ли пресет
- `recordPurchase(uuid, component, presetId)` — записывает покупку
- `getPurchasedPresets(uuid, component)` — получает все купленные пресеты

#### ShopGui (обновлённый UI)
Новые состояния для каждого пресета:
- 🟢 **INSTALLED** (LIME_DYE) — установлен на текущую машину
- 🔵 **OWNED** (LIGHT_BLUE_DYE) — куплен, можно бесплатно переустановить
- 🟡 **BUYABLE** (YELLOW_DYE) — доступен для покупки
- 🔴 **LOCKED** (RED_DYE) — недостаточно уровня или монет

## Изменения VERSION БД
- Старая версия: 25
- Новая версия: 26
- Причина: добавлена таблица `ts_player_purchases`

## Тестирование
- [x] Мод собирается успешно (Gradle, все MC версии: 1.20.4, 1.21, 1.21.3)
- [x] Плагин собирается успешно (Maven)
- [ ] Протестировано на MC сервере (требует ручного тестирования)

## Примечание о CODEBASE_INDEX.md
При наличии файла CODEBASE_INDEX.md необходимо обновить:
- Добавить `hasSolidBlockBelow()` в описание BoatMixin
- Добавить `GROUND_SNAP_VELOCITY` в описание BoatMixin
- Добавить `Version26.java` в раздел миграций БД
- Добавить методы `hasPurchased`, `recordPurchase`, `getPurchasedPresets` в GarageManager
- Добавить `equipOwnedPreset()` в ShopGui
