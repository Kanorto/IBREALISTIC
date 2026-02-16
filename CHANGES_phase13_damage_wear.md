# Изменения: Фаза 13 — Система поломок и износа

## Дата
2026-02-15

## Краткое описание
Реализована полная система износа шин, перегрева двигателя, повреждений кузова с серверным расчётом, единой HUD-системой уведомлений, визуальными эффектами и ремонтом в сервис-зонах.

## Изменённые файлы

### Мод (IBRealistic)

#### Новые файлы
- `src/main/java/dev/kanorto/ibrealistic/client/HudNotificationRenderer.java` — Единая система HUD-уведомлений
  - Отображение под прицелом (НЕ action bar)
  - Временные уведомления с плавным исчезновением
  - Постоянные полоски: износ шин (FL/FR/RL/RR), температура двигателя, повреждения кузова
  - Индикатор ремонта в сервис-зоне
  - Цветовые градиенты: зелёный → жёлтый → красный
- `src/main/java/dev/kanorto/ibrealistic/physics/DamageState.java` — Состояние повреждений
  - tireWear[4]: износ 4 шин (0.0–1.0)
  - engineTemp: температура двигателя (0.0–1.0)
  - bodyDamage: повреждения кузова (0.0–1.0)
  - Методы расчёта эффектов: getTireGripMultiplier(), getEngineForceMultiplier(), getSteeringMultiplier(), getMaxSpeedMultiplier()
  - Пороги: минорные (0.3), умеренные (0.6), критические (0.8)
- `src/main/java/dev/kanorto/ibrealistic/client/DamageParticleRenderer.java` — Визуальные эффекты
  - Дым при bodyDamage > 0.5 (CAMPFIRE_COSY_SMOKE)
  - Искры при столкновениях (CRIT)
  - Мерцание при перегреве двигателя (SMOKE)

#### Изменённые файлы
- `src/main/java/dev/kanorto/ibrealistic/IBRealistic.java`
  - Добавлено поле `damageState` (DamageState)
  - Сеттеры: setDamageEnabled(), syncTireWear(), syncEngineTemp(), syncBodyDamage(), setServiceZoneState()
  - resetRealisticState() сбрасывает damageState
  - VERSION: 21 → 22
- `src/main/java/dev/kanorto/ibrealistic/ClientboundPackets.java`
  - 7 новых пакетов (70-76): SET_DAMAGE_ENABLED, SYNC_TIRE_WEAR, SYNC_ENGINE_TEMP, SYNC_BODY_DAMAGE, SET_SERVICE_ZONE, SET_DAMAGE_CONFIG, DAMAGE_NOTIFICATION
  - Обработчики для всех новых пакетов
- `src/main/java/dev/kanorto/ibrealistic/physics/FourWheelPhysicsEngine.java`
  - Поле damageState + сеттер
  - Секция 3 (EFFECTIVE MU): применение getTireGripMultiplier() к муке каждого колеса
  - Секция 6 (LONGITUDINAL FORCES): применение getEngineForceMultiplier()
  - Секция 1 (STEERING): применение getSteeringMultiplier()
  - Секция 12 (HIGH-SPEED SAFETY): применение getMaxSpeedMultiplier()
- `src/main/java/dev/kanorto/ibrealistic/client/IBRealisticClient.java`
  - Регистрация HudRenderCallback (с version-guard для 1.20.4 vs 1.21+)
  - Тик обновления damage HUD и частиц
  - Сброс HudNotificationRenderer при подключении
- `src/main/java/dev/kanorto/ibrealistic/SingleplayerCommands.java`
  - Новые команды: damageenabled, tirewear, enginetemp, bodydamage, servicezone

### Плагин (TimingSystem)

#### Новые файлы
- `src/main/java/me/makkuusen/timing/system/boatutils/DamageWearManager.java` — Серверный менеджер (~450 строк)
  - Серверный расчёт износа шин, температуры двигателя, повреждений кузова
  - Периодический таск (каждые 5 тиков)
  - Обнаружение столкновений по потере скорости
  - Определение поверхности по блоку под машиной
  - Ремонт в сервис-зонах (REPAIR_RATE_PER_TICK)
  - Уведомления при критических порогах
  - Синхронизация с клиентом (каждые 20 тиков)
  - Публичные геттеры/сеттеры для команд

#### Изменённые файлы
- `src/main/java/me/makkuusen/timing/system/boatutils/CustomBoatUtilsMode.java`
  - PACKET_ID 70-76 для damage/wear
  - @Expose поле `damageEnabled`
  - Обновлены: resetToVanilla(), finallyApplyToPlayer(), getNonDefaultSettings()
  - getVersionRequirementFromSettingName(): "damageEnabled" → version 22
- `src/main/java/me/makkuusen/timing/system/TimingSystem.java`
  - onEnable: DamageWearManager.start()
  - onDisable: DamageWearManager.stop()
- `src/main/java/me/makkuusen/timing/system/TSListener.java`
  - onPlayerQuit: DamageWearManager.removePlayer()

### Переводы
- `src/main/resources/lang/en_us.yml` — 14 новых ключей damage.*
- `src/main/resources/lang/de_de.yml` — немецкий
- `src/main/resources/lang/id_id.yml` — индонезийский
- `src/main/resources/lang/zh_cn.yml` — китайский
- `src/main/resources/lang/pl_pl.yml` — польский
- `src/main/resources/lang/nl_nl.yml` — голландский
- `src/main/resources/lang/es_es.yml` — испанский
- `src/main/resources/lang/pt_br.yml` — португальский
- `src/main/resources/lang/fr_fr.yml` — французский
- `src/main/resources/lang/triton.yml` — Triton обёртки
- `triton/timingsystem.json` — Triton JSON коллекция (8 языков)

### Версионирование
- `gradle.properties` (корень) — realistic_version: 1.0.6 → 1.1.0
- `versions/1.21/gradle.properties` — realistic_version: 1.0.6 → 1.1.0
- `versions/1.21.3/gradle.properties` — realistic_version: 1.0.6 → 1.1.0
- `pom.xml` — version: 3.2-1.0.6 → 3.2-1.1.0, realistic.version: 1.0.6 → 1.1.0

## Новые пакеты
| ID | Константа | Описание | Формат |
|----|-----------|----------|--------|
| 70 | SET_DAMAGE_ENABLED | Вкл/выкл систему повреждений | bool |
| 71 | SYNC_TIRE_WEAR | Авторитетный износ шин | float × 4 |
| 72 | SYNC_ENGINE_TEMP | Авторитетная температура двигателя | float |
| 73 | SYNC_BODY_DAMAGE | Авторитетные повреждения кузова | float |
| 74 | SET_SERVICE_ZONE | Состояние сервис-зоны | bool + float |
| 75 | SET_DAMAGE_CONFIG | Конфигурация (резерв) | — |
| 76 | DAMAGE_NOTIFICATION | Уведомление от сервера | string + int + long |

## Изменения VERSION
- Старая версия протокола: 21
- Новая версия протокола: 22
- Причина: добавлены пакеты 70-76 для системы повреждений/износа

## Изменения realistic_version
- Старая версия: 1.0.6
- Новая версия: 1.1.0
- Причина: новая функциональность (MINOR bump) — система поломок и износа

## Архитектура

### Поток данных
```
DamageWearManager (сервер, каждые 5 тиков)
  → Расчёт: скорость, поверхность, столкновения, температура
  → Обновление: tireWear[], engineTemp, bodyDamage
  → SYNC пакеты (каждые 20 тиков)
    → ClientboundPackets (мод)
      → DamageState (обновление)
        → FourWheelPhysicsEngine (эффекты на физику)
        → HudNotificationRenderer (отображение)
        → DamageParticleRenderer (частицы)
```

### Эффекты повреждений
| Параметр | Уровень | Эффект |
|----------|---------|--------|
| Износ шин | 0-100% | До -30% сцепления (μ) |
| Температура | 0-100% | До -50% мощности двигателя |
| Кузов 0-30% | Минорный | Нет эффекта |
| Кузов 30-60% | Умеренный | -10% руль |
| Кузов 60-80% | Серьёзный | -10% руль, -20% мощность |
| Кузов 80-100% | Критический | -15% руль, -20% мощность, -50% макс. скорость |

## Тестирование
- [x] Мод собирается успешно (Gradle) — все 3 версии MC
- [x] Плагин собирается успешно (Maven)
- [ ] Протестировано в игре на MC 1.20.4
- [ ] Протестировано в игре на MC 1.21
- [ ] Протестировано в игре на MC 1.21.3

## Примечание: CODEBASE_INDEX.md
Когда CODEBASE_INDEX.md будет создан, необходимо обновить:
- Добавить DamageState.java, DamageWearManager.java, HudNotificationRenderer.java, DamageParticleRenderer.java
- Обновить описание FourWheelPhysicsEngine.java (damage integration)
- Обновить описание ClientboundPackets.java (packets 70-76)
- Добавить поток данных для системы повреждений
