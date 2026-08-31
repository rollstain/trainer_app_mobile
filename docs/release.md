# Релизы и сборки

## Как выпустить релиз

```bash
git checkout main && git pull
git tag -a v0.2.0 -m "Короткое описание релиза"
git push origin v0.2.0
```

Дальше `release.yml` сам:

1. гоняет юнит-тесты;
2. собирает `assembleRelease`, подписывая ключом из секретов;
3. проверяет подпись через `apksigner` — неподписанный APK дальше не проходит;
4. считает изменения от прошлого тега: `git log <прошлый тег>..<новый тег>`, одна строка на коммит;
5. шлёт APK в Telegram с заголовком `trainer_app <версия> · релиз` и этим списком;
6. создаёт GitHub Release с тем же текстом и APK.

Версия берётся из тега: `v0.2.0` → `versionName = 0.2.0`. `versionCode` — номер прогона Actions,
он монотонно растёт и не требует ручного учёта.

## Отладочная сборка на любой ветке

Actions → **Release** → **Run workflow**, ветка на выбор, тип сборки `debug`. Версия получается
вида `0.2.0-dev.57`, подпись — отладочная, APK ставится поверх только других debug-сборок.

## Секреты репозитория

| Секрет | Что это |
|---|---|
| `KEYSTORE_BASE64` | keystore, закодированный base64 |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | имя ключа внутри хранилища |
| `KEY_PASSWORD` | пароль ключа |
| `TELEGRAM_BOT_TOKEN` | токен бота, который шлёт сборки |
| `TELEGRAM_CHAT_ID` | чат или канал, куда бот шлёт |

Без пары `TELEGRAM_*` сборка проходит, а шаг отправки пропускается — APK остаётся артефактом
прогона. Без `KEYSTORE_BASE64` релизная сборка падает сразу, до Gradle: подписать нечем.

## Как заводится ключ подписи

Один раз на всю жизнь приложения. Потеряете ключ — обновить приложение в Google Play под тем же
`applicationId` станет невозможно, только выпускать новое.

```bash
keytool -genkeypair -v \
  -keystore trainer-release.jks -alias trainer \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
base64 -w0 trainer-release.jks > trainer-release.jks.base64
```

Файл кладётся вне репозитория и в бэкап (менеджер паролей или зашифрованное хранилище),
а его base64 и пароли — в секреты репозитория:

```bash
gh secret set KEYSTORE_BASE64 < trainer-release.jks.base64
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS
gh secret set KEY_PASSWORD
```

## Как заводится бот для сборок

1. В Telegram у `@BotFather` — `/newbot`, имя вида `trainer_builds_bot`, на выходе токен.
2. Написать боту любое сообщение (или добавить его в канал администратором).
3. Узнать `chat_id`: открыть `https://api.telegram.org/bot<токен>/getUpdates` и взять `chat.id`
   из последнего апдейта. У канала он отрицательный, вида `-100…`.
4. Положить оба значения в секреты:

```bash
gh secret set TELEGRAM_BOT_TOKEN
gh secret set TELEGRAM_CHAT_ID
```

Бот для сборок отдельный от бота входа: у них разные сроки жизни и разная цена компрометации.

## Ограничения

Telegram не принимает файлы больше 50 МБ. Если APK перерастёт этот размер, в чат уйдёт сообщение
со ссылкой на артефакт прогона вместо файла — сборка при этом считается успешной.
