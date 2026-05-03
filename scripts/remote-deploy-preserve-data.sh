#!/usr/bin/env bash
set -euo pipefail

remote_dir="${1:?remote_dir is required}"
release_id="${2:?release_id is required}"
service_name="${3:-blog.service}"
service_scope="${4:-user}"
health_url="${5:-http://127.0.0.1:8012/}"

case "$service_scope" in
    user)
        systemctl_cmd=(systemctl --user)
        journalctl_cmd=(journalctl --user)
        ;;
    system)
        systemctl_cmd=(systemctl)
        journalctl_cmd=(journalctl)
        ;;
    *)
        echo "Unsupported service scope: $service_scope" >&2
        exit 2
        ;;
esac

release_dir="$remote_dir/releases"
newjar="$release_dir/blog-$release_id.jar"
current_jar="$remote_dir/blog.jar"
backup="$remote_dir/blog.jar.bak-$(date +%Y%m%d-%H%M%S)"
deploying_jar="$remote_dir/blog.jar.deploying"
release_file="$remote_dir/current-release.txt"

test -d "$remote_dir"
test -f "$newjar"
test -f "$current_jar"

cleanup() {
    rm -f "$deploying_jar"
}

trap cleanup EXIT

cp -p "$current_jar" "$backup"
cp -p "$newjar" "$deploying_jar"

"${systemctl_cmd[@]}" stop "$service_name"
mv -f "$deploying_jar" "$current_jar"
"${systemctl_cmd[@]}" start "$service_name"

ok=0
for _ in $(seq 1 30); do
    state="$("${systemctl_cmd[@]}" is-active "$service_name" || true)"
    if [ "$state" = "active" ] && curl -fsS "$health_url" >/dev/null; then
        ok=1
        break
    fi
    sleep 2
done

if [ "$ok" -ne 1 ]; then
    echo "DEPLOY_FAILED"
    cp -p "$backup" "$current_jar"
    "${systemctl_cmd[@]}" restart "$service_name" || true
    sleep 10
    echo "ROLLED_BACK_TO=$backup"
    "${systemctl_cmd[@]}" status "$service_name" --no-pager -l || true
    "${journalctl_cmd[@]}" -u "$service_name" -n 120 --no-pager || true
    exit 1
fi

printf '%s' "$release_id" > "$release_file"
echo "DEPLOYED_RELEASE=$release_id"
echo "BACKUP_JAR=$backup"
ls -lh "$current_jar" "$newjar" "$backup"
"${systemctl_cmd[@]}" status "$service_name" --no-pager -l --lines=0
"${journalctl_cmd[@]}" -u "$service_name" -n 50 --no-pager
