# Изменения: Фаза 15 — GUI магазина, гаража, профиля, заданий, треков

## Дата
2026-02-16

## Краткое описание
Реализованы GUI-интерфейсы для всех основных систем: магазин улучшений (ShopGui), гараж (GarageGui), профиль игрока (ProfileGui), ежедневные задания (DailyGui), расширенное отображение треков (TrackGui). Все команды (/shop, /garage, /daily, /profile) теперь открывают Chest Menu GUI вместо текстовых выводов.

## Изменённые файлы

### Плагин (TimingSystem) — Новые файлы

- `src/main/java/me/makkuusen/timing/system/gui/ShopGui.java` — GUI магазина (54 слота)
  - 8 категорий компонентов (шины, двигатель, кузов, подвеска, рулевое, тормоза, распр. веса, тип)
  - Цветовая индикация: зелёный = установлено, жёлтый = доступно, красный = заблокировано
  - Покупка по клику с проверкой монет и уровня
  - Навигация: баланс, уровень, кнопка назад
  
- `src/main/java/me/makkuusen/timing/system/gui/GarageGui.java` — GUI гаража (54 слота)
  - До 5 машин с выбором и подсветкой
  - Компоненты выбранной машины
  - Действия: выбрать активную, удалить, открыть магазин

- `src/main/java/me/makkuusen/timing/system/gui/ProfileGui.java` — GUI профиля (36 слотов)
  - Голова игрока, уровень с XP-баром, баланс монет
  - Статистика: треки, финиши, время
  - Текущая машина

- `src/main/java/me/makkuusen/timing/system/gui/DailyGui.java` — GUI заданий (27 слотов)
  - 3 задания с прогресс-барами
  - Награды (монеты + XP)
  - Таймер до сброса

- `src/main/java/me/makkuusen/timing/system/commands/CommandProfile.java` — команда /profile [player]

### Плагин (TimingSystem) — Изменённые файлы

- `src/main/java/me/makkuusen/timing/system/commands/CommandShop.java` — открывает ShopGui
- `src/main/java/me/makkuusen/timing/system/commands/CommandGarage.java` — открывает GarageGui
- `src/main/java/me/makkuusen/timing/system/commands/CommandDaily.java` — открывает DailyGui
- `src/main/java/me/makkuusen/timing/system/gui/TrackGui.java` — расширен: погода, сложность, рекорды, награды
- `src/main/java/me/makkuusen/timing/system/theme/messages/Gui.java` — 54 новых enum-константы
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java` — зарегистрирован CommandProfile

### Переводы

- `src/main/resources/lang/en_us.yml` — 54 новых GUI ключа
- `src/main/resources/lang/de_de.yml` — немецкие переводы
- `src/main/resources/lang/es_es.yml` — испанские переводы
- `src/main/resources/lang/fr_fr.yml` — французские переводы
- `src/main/resources/lang/id_id.yml` — индонезийские переводы
- `src/main/resources/lang/nl_nl.yml` — голландские переводы
- `src/main/resources/lang/pl_pl.yml` — польские переводы
- `src/main/resources/lang/pt_br.yml` — бразильские переводы
- `src/main/resources/lang/zh_cn.yml` — китайские переводы
- `src/main/resources/lang/triton.yml` — Triton обёртки
- `triton/timingsystem.json` — 48 новых Triton JSON записей

### Документация

- `PLAN.md` — обновлён: 15.1-15.5 отмечены как ✅

## Детальное описание изменений

### 1. ShopGui.java
**Паттерн:** Data-Driven GUI с категориями
- 8 категорий отображаются в верхнем ряду (слоты 0-7)
- Слот 8: активная машина игрока
- Пресеты отображаются в рядах 2-5 (слоты 9-44)
- Навигация в нижнем ряду (слоты 45-53): назад, баланс, уровень
- Используются статические массивы CATEGORY_KEYS, CATEGORY_MATERIALS, CATEGORY_LABELS
- Покупка: проверка монет → spendCoins() → upgradeComponent() → звук + рефреш GUI

### 2. GarageGui.java
**Паттерн:** Stateful GUI с выбранной машиной
- selectedCarIndex определяет какая машина отображается
- Клик на машину → обновление GUI с новым selectedCarIndex
- Пустые слоты создают машину с дефолтным именем
- Кнопки: selectCar, deleteCar, openShop

### 3. ProfileGui.java
**Паттерн:** Read-only GUI с PlayerHead
- Поддержка просмотра других игроков (targetUuid)
- Использует SkullMeta для отображения головы
- XP прогресс-бар через Unicode-символы (█░)
- Клик на машину → переход в GarageGui

### 4. DailyGui.java
**Паттерн:** Информационный GUI с таймером
- 3 задания расположены в слотах 11, 13, 15
- Прогресс-бар с Unicode-символами
- Таймер до полуночи UTC (Duration.between)

### 5. TrackGui — расширение
- Добавлены 5 новых строк в lore каждого трека:
  - Погода (WeatherCondition.getDisplayName())
  - Сложность (getDifficultyStars())
  - Персональный рекорд (getBestFinish)
  - Мировой рекорд (getTopList(1))
  - Награда (baseCoins × difficultyMultiplier)

## Новые GUI enum-константы (54 шт.)
- SHOP_*: 18 констант для магазина
- GARAGE_*: 7 констант для гаража
- PROFILE_*: 13 констант для профиля
- DAILY_*: 5 констант для заданий
- TRACK_*: 5 констант для расширенного трека

## Изменения версий
- VERSION протокола: без изменений (GUI не использует клиентские пакеты)
- realistic_version: без изменений (изменения только на сервере)

## Тестирование
- [x] Плагин собирается успешно (mvn compile)
- [x] Triton JSON валиден (python json.load)
- [ ] Протестировано в игре

## Примечание
- CODEBASE_INDEX.md необходимо обновить при создании (5 новых GUI файлов + CommandProfile)
