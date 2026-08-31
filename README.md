# trainer_app — приложение тренера и клиента

Мобильный клиент сервиса персональных тренировок: тренер ведёт подопечных, клиент — свой дневник.
Kotlin Multiplatform, общий код и UI на Compose Multiplatform; Android собран целиком, iOS
компилируется, но экранами не проверен.

Бэкенд живёт в отдельном репозитории — [trainer_app_backend](https://github.com/rollstain/trainer_app_backend),
прод `https://api.lyashukfit.ru`.

## Что умеет

| Раздел | Что там |
|---|---|
| Расписание | слоты тренера, запись клиента, групповые занятия, переносы, рабочие дни и часы |
| Чат | диалог клиента с тренером, чат-лист у тренера, вложения, WebSocket |
| Дневник тренировок | упражнения, подходы, вес и повторы, справочник на 124 упражнения плюс свои |
| Программы | конструктор недельного цикла, назначение клиенту |
| Прогресс | замеры, чек-ины с фото «до и после», привычки, графики динамики |
| Видеоразбор | клиент шлёт видео техники, у тренера очередь на разбор |
| Карточка клиента | заметки тренера, история, медицинские пометки (видит только тренер) |
| Вход | код от тренера, ссылка-приглашение, Telegram, почта с паролем |

Планы — `docs/roadmap.md`, что и почему уже сделано — `docs/changelog.md`.

## Стек

- **Kotlin 2.3**, Compose Multiplatform 1.10, Android minSdk 26 / targetSdk 36, JDK 17
- **Навигация** — Navigation 3 (`navigation3`), не Decompose
- **DI** — Koin
- **Сеть** — Ktor Client (OkHttp на Android, Darwin на iOS), WebSocket для чата
- **Хранилище** — SQLDelight, `multiplatform-settings` для настроек, `androidx.security.crypto` для токенов
- **Пуши и крэши** — Firebase Messaging, Crashlytics
- **Медиа** — Coil, media3 для видео
- **Статический анализ** — detekt с конфигом `config/detekt.yml`

## Карта модулей

Каждый `data_*` разбит на пару `api` (интерфейсы и модели домена) и `impl` (репозитории, DataSource,
Koin-модуль). Экран зависит только от `api`.

```
androidApp            точка входа Android: Activity, Firebase, BuildConfig
composeApp            общий App(), сборка графа Koin, платформенные actual
navigation            маршруты и стек Navigation 3
uikit                 дизайн-система: токены, кнопки, поля, скелетоны
core_strings          строковые ресурсы Compose (только здесь и в uikit)
core_entities         модели, общие для нескольких фич
core_network          Ktor-клиент, авторизация, обновление токена, пагинация
core_database         SQLDelight: схема, драйверы, миграции .sqm
core_media            выбор файла, загрузка, плеер
core_base_feature     базовый ViewModel и общие состояния экрана
core_logger           логи

data_auth             вход, сессии, ротация токена
data_chat             диалоги, сообщения, вложения
data_schedule         слоты, записи, рабочие часы
data_clients          список подопечных, заметки
data_profile          профиль, настройки, рабочие дни
data_program          программы тренировок
data_progress         замеры, чек-ины, привычки, видеоразбор
data_training_log     дневник и справочник упражнений
data_push             токен пуш-уведомлений

feature_home          главный экран и вкладки
feature_schedule      календарь и слоты
feature_chat          чат и чат-лист
feature_client_card   карточка подопечного
feature_training_log  дневник
feature_progress      прогресс и чек-ины
feature_account       профиль, вход, регистрация, настройки
```

## Сборка

Нужны JDK 17 и Android SDK. `local.properties` с `sdk.dir` создаёт Android Studio.

```bash
./gradlew :androidApp:assembleDebug          # APK в androidApp/build/outputs/apk/debug
./gradlew :androidApp:installDebug           # поставить на подключённое устройство
./gradlew detekt                             # статический анализ
./gradlew :androidApp:testDebugUnitTest      # юнит-тесты
```

iOS собирается с macOS через `iosApp` в Xcode; общий фреймворк линкуется и на Windows:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

### Куда смотрит debug-сборка

По умолчанию — на прод. Чтобы отладочная сборка ходила на локальный бэкенд, добавьте
в `local.properties` (файл не коммитится):

```properties
trainer.debugBaseUrl=http://10.0.2.2:8080/
trainer.debugChatWebSocketUrl=ws://10.0.2.2:8080/ws/chat
```

`10.0.2.2` — адрес хоста изнутри эмулятора Android. Release-сборка всегда идёт на прод.

### Версия сборки

`versionName` и `versionCode` приходят из свойств Gradle, значения по умолчанию — в
`androidApp/build.gradle.kts`. Релизный конвейер подставляет их из тега и номера прогона:

```bash
./gradlew :androidApp:assembleRelease -Ptrainer.versionName=0.2.0 -Ptrainer.versionCode=42
```

### Подпись релиза

Ключ в репозиторий не кладётся. Локально — файл `keystore.properties` в корне (тоже не коммитится):

```properties
storeFile=C:/Users/<имя>/keys/trainer-release.jks
storePassword=...
keyAlias=trainer
keyPassword=...
```

В CI те же значения приходят переменными окружения `TRAINER_KEYSTORE_PATH`,
`TRAINER_KEYSTORE_PASSWORD`, `TRAINER_KEY_ALIAS`, `TRAINER_KEY_PASSWORD`. Без ключа
`assembleRelease` собирает неподписанный APK — поставить его на телефон нельзя.

## Автоматика

| Workflow | Когда | Что делает |
|---|---|---|
| `ci.yml` | push и PR в `main` | detekt, юнит-тесты, debug APK, линковка iOS-фреймворка на macOS |
| `release.yml` | тег `v*` или кнопка в Actions | собирает APK, подписывает, шлёт в Telegram со списком изменений, на теге создаёт GitHub Release |

Как выпускать релиз и что настроить в секретах — `docs/release.md`.

## Как здесь работают

Правила ветвления, коммитов и релизов — `CLAUDE.md`. Коротко: задача → своя ветка → PR в `main`,
прямые коммиты в `main` не делаются.
