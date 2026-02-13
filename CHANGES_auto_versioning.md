# Изменения: Автоматическое версионирование из тегов

## Дата
2026-02-12

## Краткое описание
Реализовано автоматическое определение версии из git-тегов в CI/CD. Версия больше не берётся из файлов gradle.properties — она извлекается из тега релиза и подставляется при сборке.

## Изменённые файлы

### Мод (OBURealistic)
- `build.gradle` — `mod_version` теперь вычисляется динамически из `obu_version`, `realistic_version` и `mc_suffix`. Поддерживает CI-override через `-Prealistic_version_override=X.Y.Z`
- `gradle.properties` — `mod_version` заменён на `mc_suffix`. Добавлены комментарии о CI
- `versions/1.21/gradle.properties` — аналогично, `mod_version` → `mc_suffix`, `realistic_version` обновлён до 1.0.6
- `versions/1.21.3/gradle.properties` — аналогично, `mod_version` → `mc_suffix`, `realistic_version` обновлён до 1.0.6

### Плагин (TimingSystem)
- `pom.xml` — добавлены комментарии о том, что версия определяется автоматически в CI

### CI/CD
- `.github/workflows/build-release.yml` — при полном релизе (тег `vX.X.X`) версия извлекается из тега и передаётся в Gradle (`-Prealistic_version_override`) и Maven (`sed` замена в pom.xml)
- `.github/workflows/prerelease.yml` — при пререлизе базовая версия определяется из последнего полного релиз-тега (не из gradle.properties), пререлизная версия передаётся в оба билдера

## Детальное описание изменений

### 1. Динамический расчёт mod_version в Gradle
**Файл:** `OBURealistic/build.gradle`
**Что сделано:**
- Заменён `version = project.mod_version` на динамический расчёт: `version = "${project.obu_version}-${effectiveRealisticVersion}_${project.mc_suffix}"`
- Добавлена поддержка CI-override: `project.findProperty('realistic_version_override')` позволяет переопределить версию из командной строки

### 2. Замена mod_version на mc_suffix в gradle.properties
**Файлы:** Все три `gradle.properties` (корневой, 1.21, 1.21.3)
**Что сделано:**
- Свойство `mod_version` удалено (теперь вычисляется динамически)
- Добавлено свойство `mc_suffix` — суффикс версии MC для имени файла (например `1.20.4`, `1.21-1.21.1`, `1.21.3-1.21.4`)
- `realistic_version` теперь используется только как fallback для локальных сборок

### 3. Автоматическое версионирование в build-release.yml
**Файл:** `.github/workflows/build-release.yml`
**Что сделано:**
- Шаг `check-tag` теперь извлекает версию из тега (например `v1.0.7` → `1.0.7`)
- Gradle: `./gradlew chiseledBuild -Prealistic_version_override=1.0.7`
- Maven: `sed` заменяет `<version>` и `<realistic.version>` в pom.xml перед сборкой

### 4. Автоматическое версионирование в prerelease.yml
**Файл:** `.github/workflows/prerelease.yml`
**Что сделано:**
- Базовая версия определяется из последнего полного релиз-тега (`git tag -l 'v[0-9]*.[0-9]*.[0-9]*'`), а не из gradle.properties
- Пререлизный номер инкрементируется как и раньше
- Версия передаётся в оба билдера (Gradle и Maven)

## Как это работает

### Полный релиз (push тега v1.0.7)
1. CI извлекает `1.0.7` из тега
2. Gradle получает `-Prealistic_version_override=1.0.7` → JAR-файлы: `OpenBoatUtils-0.4.10-1.0.7_1.20.4.jar`
3. Maven: sed подставляет `1.0.7` → JAR-файл: `TimingSystem-3.2-1.0.7.jar`

### Пререлиз (merge PR в main)
1. CI находит последний полный тег: `v1.0.7`
2. CI находит последний пререлиз: `v1.0.7.3` → следующий: `v1.0.7.4`
3. Gradle получает `-Prealistic_version_override=1.0.7.4` → JAR-файлы: `OpenBoatUtils-0.4.10-1.0.7.4_1.20.4.jar`
4. Maven: sed подставляет `1.0.7.4` → JAR-файл: `TimingSystem-3.2-1.0.7.4.jar`

### Локальная разработка
- Используется `realistic_version` из gradle.properties (fallback)
- Разработчику не нужно ничего менять для обычной работы

## Тестирование
- [x] Gradle properties корректно резолвятся (default: `0.4.10-1.0.6_1.20.4`)
- [x] Gradle override работает (`-Prealistic_version_override=1.0.6.1` → `0.4.10-1.0.6.1_1.20.4`)
- [x] Все три MC версии получают правильные суффиксы
- [x] JAR-файлы именуются корректно
- [x] fabric.mod.json внутри JAR содержит правильную версию
- [x] Maven sed-замена работает корректно
- [x] Сборка мода проходит успешно (Gradle chiseledBuild)

## Примечания
- **CODEBASE_INDEX.md** нужно обновить при его создании: добавить описание CI/CD workflow, новых свойств `mc_suffix` и механизма override
