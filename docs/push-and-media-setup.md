# Пуши и медиа: что нужно завести снаружи

Код готов и собирается. Здесь — то, что кодом не решается: аккаунты, ключи, файлы конфигурации.
Пока этого нет, пуши уходят в no-op (пишется строка в лог), а ручка вложений отдаёт `503`.

## 1. Пуши

### Схема

Единый канал — **FCM для обеих платформ**, как в `urentbike-technic`. На iOS Firebase SDK
обменивает APNs-токен на FCM-токен, бэк работает только с FCM и не знает про APNs напрямую.

### Firebase-проект

1. Создать проект в Firebase Console.
2. Добавить **Android-приложение** с package `app.trainer.android`, скачать `google-services.json`,
   положить в `androidApp/`.
3. Подключить плагин `com.google.gms.google-services` в `androidApp/build.gradle.kts`.
   **Сейчас он намеренно не подключён**: без `google-services.json` сборка с ним падает.
4. Добавить **iOS-приложение** с его bundle id, скачать `GoogleService-Info.plist`, положить в `iosApp`.

### APNs (только iOS)

1. **Apple Developer Program** — платный, без него ни APNs-ключа, ни сборки на устройство.
2. В Apple Developer → Keys создать **APNs Auth Key** (`.p8`). Записать **Key ID** и **Team ID**.
   Ключ скачивается один раз, второй раз его не выдадут.
3. Загрузить `.p8` в Firebase Console → Project Settings → Cloud Messaging → APNs Authentication Key,
   указать Key ID и Team ID.
4. В Xcode для таргета включить capability **Push Notifications** и **Background Modes → Remote notifications**.
5. Entitlements: `aps-environment` = `development` для отладки, `production` для релиза.
   Это разные среды APNs — токен из dev-сборки не работает в prod.

### Бэкенд

1. Firebase Console → Project Settings → Service accounts → Generate new private key → JSON.
2. Положить файл на сервер, задать переменную `FIREBASE_CREDENTIALS_PATH`.
3. Без переменной поднимается `NoOpPushSender` — приложение работает, пуши молча не уходят,
   в логе видно сколько получателей пропущено.

### iOS-код, которого ещё нет

`iosApp` не создан вообще. Когда появится, по образцу донора нужно:

- `AppDelegate`:
  - `FirebaseApp.configure()`;
  - `UNUserNotificationCenter.current().delegate = …`, `Messaging.messaging().delegate = …`;
  - `requestAuthorization([.alert, .badge, .sound])`, затем `registerForRemoteNotifications()`;
  - `didRegisterForRemoteNotificationsWithDeviceToken` → `Messaging.messaging().apnsToken = deviceToken`;
  - `didReceiveRemoteNotification` → `Messaging.messaging().appDidReceiveMessage(userInfo)`.
- Swift-класс реализует Kotlin-интерфейс `NotificationsUtils` и отдаёт себя в общий код:
  `IosNotificationsUtilsHolder().setInstance(notificationsUtils: …)`.
- **Грабля из донора:** `getMessagingToken` обязан вернуть `nil`, пока `Messaging.messaging().apnsToken == nil`.
  Если запросить FCM-токен раньше — Firebase его не выдаст.

### Android-код — сделано

- `NotificationChannel` создаётся в `TrainerApplication.onCreate`.
- Разрешение `POST_NOTIFICATIONS` объявлено в манифесте и запрашивается в `MainActivity` на Android 13+.
- `TrainerMessagingService` принимает сообщения и обновляет токен по `onNewToken`.
- `AppStartup` вызывает `refreshToken()` при старте, если пользователь авторизован,
  и через `onSignedIn()` — сразу после входа.

Остаётся только положить `google-services.json` и подключить плагин: без них сервис не поднимется.

## 2. Медиа (вложения в чат)

### Хранилище

S3-совместимое: Yandex Object Storage, VK Cloud, MinIO для локальной разработки. Нужны бакет и пара ключей.

Переменные окружения бэкенда:

| Переменная | Назначение |
|---|---|
| `MEDIA_ENDPOINT` | адрес S3 API; **пока пусто — вложения отключены** |
| `MEDIA_BUCKET` | имя бакета |
| `MEDIA_REGION` | регион (для Yandex — `ru-central1`) |
| `MEDIA_ACCESS_KEY` / `MEDIA_SECRET_KEY` | ключи сервисного аккаунта |

### Настройки бакета

- Доступ **закрытый**. Файлы отдаются только по presigned-ссылкам, срок жизни задан
  в `trainer.media.download-url-lifetime-minutes` (сейчас 60 минут).
- Ограничения на стороне бэка: 25 МБ на файл, белый список типов — jpeg, png, heic, webp, mp4,
  quicktime, pdf. Меняются в `application.yml`, отдельной настройки в бакете не требуют.
- CORS понадобится, только если появится веб-клиент, который грузит файлы из браузера.

### Незакрытое в коде

- **Размер не проверяется по факту.** Бэк верит числу `sizeBytes` из запроса при выдаче ссылки;
  реально залитый объём не сверяется. Ограничение обязательно продублировать политикой бакета,
  иначе заявленные 25 МБ обходятся одной подменой числа в запросе.
- **Файл держится в памяти целиком.** Событие `OnFileAttached` несёт `ByteArray`; для 25 МБ видео
  это заметный пик. Понадобится потоковая загрузка — переделывать придётся и контракт репозитория.

Сделано: чистка осиротевших вложений (`OrphanAttachmentCleaner`, раз в час, старше 24 часов),
отдельная ручка за свежей ссылкой, клиентский слой вложений целиком.
