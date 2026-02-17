# Изменения: Полностью конфигурируемый гараж и магазин

## Дата
2026-02-17

## Краткое описание
Все пресеты гаража (имена, цены, уровни) перенесены из хардкода в config.yml. Конфигурация автоматически обновляется при запуске — недостающие ключи добавляются без перезаписи пользовательских настроек.

## Изменённые файлы

### Плагин (TimingSystem)
- `TimingSystem/src/main/resources/config.yml` — Добавлена секция `garage:` с полной конфигурацией пресетов
- `TimingSystem/src/main/java/me/makkuusen/timing/system/economy/GarageManager.java` — Переписан для чтения из конфига
- `TimingSystem/src/main/java/me/makkuusen/timing/system/TimingSystem.java` — Вызов `ensureConfigDefaults()` при старте

## Детальное описание

### 1. Секция `garage:` в config.yml
```yaml
garage:
   max_cars: 5               # Макс. машин на игрока
   extra_slot_cost: 2000      # Цена доп. слота
   car_creation_level: 0      # Мин. уровень для создания машины
   presets:
      tire:
         0: { name: "STANDARD", price: 0, level: 0 }
         1: { name: "SOFT", price: 500, level: 3 }
         ...
      engine:
         0: { name: "STOCK", price: 0, level: 0 }
         ...
```
Администратор может менять цены, уровни, имена, добавлять или убирать пресеты.

### 2. Автообновление конфигурации
Метод `GarageManager.ensureConfigDefaults()`:
- Вызывается при каждом запуске плагина
- Проверяет наличие каждого ключа в `garage:` секции
- Если ключ отсутствует — добавляет дефолтное значение
- Если ключ уже есть — НЕ перезаписывает (пользовательские изменения сохраняются)
- Сохраняет config.yml только если были изменения

### 3. GarageManager — чтение из конфига
Все методы `getPresetName()`, `getPresetPrice()`, `getPresetLevel()`, `getNamesForComponent()` и др.:
- Сначала ищут значение в `garage.presets.<component>.<id>` из config.yml
- Если конфиг пуст/отсутствует — используют хардкодированные дефолты (обратная совместимость)

## Тестирование
- [x] Плагин компилируется (Maven)
- [ ] Ручное тестирование на сервере
