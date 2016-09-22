export H=localhost
history -r load-history.sh 

echo "Please start show-processors.sh in a separate window"
echo "Make sure ~/.curlrc is correctly setup with user=admin:admin"

open http://localhost/bin/browser.html/cluster/routing
open http://localhost/haproxy/stats
open http://localhost:81/system/console
open http://localhost/tmp/test.tidy.json
open http://localhost/tmp/test.html
open http://localhost/slingshot.html
