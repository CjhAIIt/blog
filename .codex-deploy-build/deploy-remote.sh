set -euo pipefail

cd /home/lab/apps/blog

release_id=20260412-182053
newjar="/home/lab/apps/blog/releases/blog-${release_id}.jar"
ts="$(date +%Y%m%d-%H%M%S)"
backup="/home/lab/apps/blog/blog.jar.bak-${ts}"
deploytmp="/home/lab/apps/blog/blog.jar.deploying"

test -f "$newjar"

cp -p blog.jar "$backup"
cp -p "$newjar" "$deploytmp"

systemctl --user stop blog.service
mv -f "$deploytmp" /home/lab/apps/blog/blog.jar
systemctl --user start blog.service

ok=0
for i in $(seq 1 20); do
    state="$(systemctl --user is-active blog.service || true)"
    if [ "$state" = "active" ] && curl -fsS http://127.0.0.1:8012/ >/dev/null; then
        ok=1
        break
    fi
    sleep 2
done

if [ "$ok" -ne 1 ]; then
    echo "DEPLOY_FAILED"
    cp -p "$backup" /home/lab/apps/blog/blog.jar
    systemctl --user restart blog.service || true
    sleep 10
    echo "ROLLED_BACK_TO=$backup"
    systemctl --user status blog.service --no-pager -l || true
    journalctl --user -u blog.service -n 120 --no-pager || true
    exit 1
fi

printf '%s' "$release_id" > current-release.txt
echo "DEPLOYED_RELEASE=$release_id"
echo "BACKUP_JAR=$backup"
ls -lh blog.jar "$newjar" "$backup"
systemctl --user status blog.service --no-pager -l --lines=0
journalctl --user -u blog.service -n 50 --no-pager
