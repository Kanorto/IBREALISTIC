# GUI, Темы и Визуал

---

## GUI (me.makkuusen.timing.system.gui)

### BaseGui.java
Базовый класс для всех GUI-меню. Использует Bukkit `Inventory`.

### TrackGui.java (extends TrackPageGui)
Меню выбора трассы с постраничной навигацией.

### TimeTrialGui.java
Меню тайм-трайла — список трасс для одиночных попыток.

### SettingsGui.java (extends BaseGui)
Меню настроек игрока: звук, verbose, тайм-трайл, override, подменю лодки и цветов.

### BoatSettingsGui.java (extends BaseGui)
Выбор типа лодки (8 типов дерева).

### ColorSettingsGui.java (extends BaseGui)
Настройки цвета для скорборда и отображения.

### FilterGui.java, TrackFilter.java, TrackSort.java
Фильтрация и сортировка трасс в меню.

### GuiCommon.java, GuiButton.java
Общие элементы и кнопки GUI.

### GUIListener.java (Listener)
Обработка кликов по GUI-элементам.

---

## Темы (me.makkuusen.timing.system.theme)

### Theme.java
**Назначение:** Система цветовых тем для сообщений.
**Аннотации:** `@Getter`, `@Setter`

| Цвет | Описание |
|------|----------|
| `primary` | Основной цвет |
| `secondary` | Вторичный |
| `award` / `awardSecondary` | Цвета наград |
| `error` | Ошибки |
| `warning` | Предупреждения |
| `success` | Успех |
| `broadcast` | Общие объявления |
| `title` | Заголовки |
| `button` / `buttonAdd` / `buttonRemove` | Кнопки |

### Text.java
Утилита для отправки форматированных сообщений игрокам.

### TSColor.java (enum)
12 именованных цветов: PRIMARY, SECONDARY, ERROR, WARNING, SUCCESS, BROADCAST, AWARD, TITLE, BUTTON, BUTTON_ADD, BUTTON_REMOVE, AWARD_SECONDARY.

### MessageParser.java
Парсер текстовых сообщений с поддержкой переменных и форматирования.

### Типы сообщений (theme/messages/)
| Класс | Описание |
|-------|----------|
| `Message` | Базовый класс |
| `Info` | Информационное |
| `Warning` | Предупреждение |
| `Error` | Ошибка |
| `Success` | Успех |
| `Broadcast` | Широковещательное |
| `Word` | Отдельное слово |
| `ActionBar` | Action bar сообщение |
| `TextButton` | Кликабельная кнопка |
| `Hover` | Hover-текст |
| `Gui` | Текст для GUI |
| `ScoreBoard` | Текст скорборда |
| `MessageNoColor` | Бесцветное |

---

## Звуки (me.makkuusen.timing.system.sounds)

### PlaySound.java
| Метод | Описание |
|-------|----------|
| `buttonClick()` | Клик по кнопке |
| `pageTurn()` | Переворот страницы |
| `boatUtilsEffect()` | Эффект BoatUtils |
| `countDownPling()` | Обратный отсчёт |

---

## Скорборды

### DriverScoreboard.java
Скорборд для гонщика (позиция, время, круги, gaps).

### SpectatorScoreboard.java
Скорборд для зрителей (все гонщики, позиции, времена).

### TimeTrialScoreboard.java
Скорборд тайм-трайла (попытки, лучшее время).

### ScoreboardUtils.java
Утилиты форматирования скорбордов (выравнивание, паддинг, цвета).
