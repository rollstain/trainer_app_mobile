#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=${LC_ALL:-C.UTF-8}

if [ $# -ne 3 ]; then
    echo "Использование: $0 <файл сборки> <заголовок> <файл со списком изменений>" >&2
    exit 2
fi

build_file=$1
title=$2
notes_file=$3

: "${TELEGRAM_BOT_TOKEN:?не задан TELEGRAM_BOT_TOKEN}"
: "${TELEGRAM_CHAT_ID:?не задан TELEGRAM_CHAT_ID}"

api="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}"
caption_char_limit=900
message_char_limit=3900
telegram_upload_limit_bytes=$((50 * 1024 * 1024))
run_url="${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-}/actions/runs/${GITHUB_RUN_ID:-}"

notes=$(cat "$notes_file")
caption="$title"
notes_sent_with_file=false

if [ -n "$notes" ]; then
    combined=$(printf '%s\n\n%s' "$title" "$notes")
    if [ "$(printf '%s' "$combined" | wc -m)" -le "$caption_char_limit" ]; then
        caption="$combined"
        notes_sent_with_file=true
    fi
fi

build_size_bytes=$(stat -c %s "$build_file")
build_size_mb=$((build_size_bytes / 1024 / 1024))

if [ "$build_size_bytes" -le "$telegram_upload_limit_bytes" ]; then
    curl --silent --show-error --fail-with-body \
        --form "chat_id=${TELEGRAM_CHAT_ID}" \
        --form "document=@${build_file}" \
        --form "caption=${caption}" \
        "${api}/sendDocument" > /dev/null
else
    oversized_notice=$(printf '%s\n\nФайл %s МБ — больше лимита Telegram в 50 МБ.\nЗабрать в артефактах прогона: %s' \
        "$caption" "$build_size_mb" "$run_url")
    curl --silent --show-error --fail-with-body \
        --form "chat_id=${TELEGRAM_CHAT_ID}" \
        --form "text=${oversized_notice}" \
        "${api}/sendMessage" > /dev/null
fi

if [ "$notes_sent_with_file" = false ] && [ -n "$notes" ]; then
    message="$notes"
    if [ "$(printf '%s' "$notes" | wc -m)" -gt "$message_char_limit" ]; then
        trimmed=$(head -c "$message_char_limit" <<< "$notes" | iconv -c -f utf-8 -t utf-8)
        message=$(printf '%s\n\n…список обрезан, целиком — в прогоне: %s' "$trimmed" "$run_url")
    fi
    curl --silent --show-error --fail-with-body \
        --form "chat_id=${TELEGRAM_CHAT_ID}" \
        --form "text=${message}" \
        "${api}/sendMessage" > /dev/null
fi
