for i in /cluster /tmp; do curl -X DELETE http://localhost/$i; done
export H=localhost
curl -H Sling-Processor-Role:selector http://${H}:81/tmp/test.tidy.json
curl -H Sling-Processor-Role:selector http://${H}:81/tmp/test.html
curl http://${H}/tmp/fakeimage.jpg
curl http://${H}/some/other/path/somefile.txt
curl http://${H}/tmp/test.tidy.json ; echo
curl http://${H}/tmp/test.html
