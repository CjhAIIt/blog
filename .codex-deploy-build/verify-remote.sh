set -e

cd /home/lab/apps/blog

echo "===RELEASE==="
cat current-release.txt
echo
echo "===SERVICE==="
systemctl --user is-active blog.service
echo "===PORT==="
ss -ltnp | grep :8012
echo "===HTTP==="
curl -I --max-time 10 http://127.0.0.1:8012/ | sed -n '1,5p'
