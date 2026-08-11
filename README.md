# Soup Visuals — исходники 3.1.0 / Source code 3.1.0

Русская версия

## Описание
Исходный код модификации **Soup Visuals 3.1.0** для Minecraft 1.21.4 (Fabric).

### 🔗 Ссылки
- Страница мода (Modrinth): https://modrinth.com/mod/soup-api/version/1.21.4-3.1.0
- Скачать .jar (Fabric 1.21.4): https://modrinth.com/mod/soup-api/version/1.21.4-3.1.0#download

---

## Требования
- Minecraft 1.21.4 (Fabric)  
- Fabric Loader, совместимый с 1.21.4  
- Fabric API (если требуется модом)  
- Java JDK 21 (в проекте настроен toolchain и компиляция с release = 21 — см. build.gradle)

> Примечание: если в вашей среде разработки используется другая версия JDK, подстройте её при необходимости, но для сборки и запуска рекомендуется JDK 21, как указано в конфигурации проекта.

## Сборка
Клонируйте репозиторий и соберите проект.

Unix / macOS (если есть Gradle Wrapper):

```bash
git clone https://github.com/zDEBRYrp/SoupVisuals-3.1.0-1.21.4-src.git
cd SoupVisuals-3.1.0-1.21.4-src
./gradlew build
```

Если Gradle Wrapper отсутствует или вы на Windows и в репозитории нет `gradlew.bat`, используйте установленный в системе Gradle:

Unix / macOS (без wrapper):

```bash
git clone https://github.com/zDEBRYrp/SoupVisuals-3.1.0-1.21.4-src.git
cd SoupVisuals-3.1.0-1.21.4-src
gradle build
```

Windows (если нет `gradlew.bat`):

- Установите Gradle локально и запустите в PowerShell или cmd.exe:
```powershell
git clone https://github.com/zDEBRYrp/SoupVisuals-3.1.0-1.21.4-src.git
cd SoupVisuals-3.1.0-1.21.4-src
gradle build
```
- Либо используйте WSL / Git Bash и выполните `./gradlew build`, если wrapper добавят в репозиторий.

Если возникают проблемы с зависимостями, выполните:

```bash
./gradlew --refresh-dependencies
# или, если wrapper отсутствует
gradle --refresh-dependencies
```

Собранный артефакт появится в `build/libs/`. Полученный `.jar` можно поместить в папку `mods/` клиента/сервера.

---

## Установка (для игроков)
1. Установите Fabric Loader и (при необходимости) Fabric API.  
2. Скопируйте полученный `SoupVisuals-*.jar` в `minecraft/mods/`.  
3. Запустите Minecraft с профилем Fabric.

---

## Разработка / запуск в IDE
- Откройте проект в IntelliJ IDEA или VSCode.  
- Импортируйте Gradle-проект.  
- Проект настроен для использования Fabric Loom: используйте конфигурации `runClient` / `runServer` для тестирования.

---

*Опубликовано автором:* [zDEBRY](https://github.com/zDEBRYrp)

---

English version

# Soup Visuals — source 3.1.0

## Overview
Source code for the Soup Visuals mod version 3.1.0 targeting Minecraft 1.21.4 (Fabric).

### Links
- Mod page (Modrinth): https://modrinth.com/mod/soup-api/version/1.21.4-3.1.0
- Download .jar (Fabric 1.21.4): https://modrinth.com/mod/soup-api/version/1.21.4-3.1.0#download

---

## Requirements
- Minecraft 1.21.4 (Fabric)  
- Fabric Loader compatible with 1.21.4  
- Fabric API (if required by the mod)  
- Java JDK 21 (project uses Java toolchain and compilation target release = 21 — see build.gradle)

Note: If your development environment uses a different JDK version, adjust as needed. For consistency with the project configuration, JDK 21 is recommended.

## Build
Clone the repository and build the project.

Unix / macOS (with Gradle Wrapper):

```bash
git clone https://github.com/zDEBRYrp/SoupVisuals-3.1.0-1.21.4-src.git
cd SoupVisuals-3.1.0-1.21.4-src
./gradlew build
```

If the Gradle Wrapper is not included or you're on Windows and `gradlew.bat` is absent, use your system Gradle:

Unix / macOS (without wrapper):

```bash
git clone https://github.com/zDEBRYrp/SoupVisuals-3.1.0-1.21.4-src.git
cd SoupVisuals-3.1.0-1.21.4-src
gradle build
```

Windows (if `gradlew.bat` is missing):

- Install Gradle and run in PowerShell or cmd.exe:
```powershell
git clone https://github.com/zDEBRYrp/SoupVisuals-3.1.0-1.21.4-src.git
cd SoupVisuals-3.1.0-1.21.4-src
gradle build
```
- Or use WSL / Git Bash and run `./gradlew build` if a wrapper is added.

If you have dependency issues run:

```bash
./gradlew --refresh-dependencies
# or, if wrapper is not present
gradle --refresh-dependencies
```

The built artifact will appear in `build/libs/`. Place the resulting `.jar` into the `mods/` folder of the client or server.

---

## Installation (for players)
1. Install Fabric Loader and Fabric API (if required).  
2. Copy the `SoupVisuals-*.jar` into your `minecraft/mods/` directory.  
3. Launch Minecraft using the Fabric profile.

---

## Development / IDE
- Open the project in IntelliJ IDEA or VSCode.  
- Import the Gradle project.  
- Use `runClient` / `runServer` (Fabric Loom) configurations for testing.

---

Published by: [zDEBRY](https://github.com/zDEBRYrp)
