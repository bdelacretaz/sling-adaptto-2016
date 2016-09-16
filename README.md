# Sling adaptTo 2016 demo - can we run the whole Web on Sling?
This is the demo code for our http://adapt.to/2016/en/schedule/let_s-run-the-whole-web-on-apache-sling-and-oak-.html talk.

Very much a work in progress for now, we'll update this once it's ready to play with.

It's adapted from a previous https://github.com/bdelacretaz/docker-sling-hosting example, including some leftovers from that for now.

For now, it connects a number of Sling instances to a common MongoDB database, and automatically makes
them available via a front-end HAProxy server. Logs are collected in a Graylog instance. All these services
run in Docker containers.

Apart from some glue scripts to setup the containers there's little or no code here, the goal is to use off-the-shelf
tools as much as possible, to concentrate on the proof of concept aspects. 

This is obviously not production-ready.

## TODO
* POSTing to `/at16.txt` is much slower when more than one Sling instance is running, about 1 second vs. 10 msec on my box. Might be related to a once-per-second event as times vary a lot.
* Once we're happy with the setup, profile and tweak to increase overall throughput. 
* Review the load scenario and test servlets to make sure we are exposing realistic load paths.

## Prerequisites
You need a Docker server and `docker-compose` setup to run this.

Unless you already have such a setup, https://www.docker.com/docker-toolbox is a good way to get started.

You Docker host needs at least 4G of memory to run this cluster comfortably. See the `docker-machine` docs
for the relevant options for your machine backend, if applicable.

After installing `docker-compose`, you can test it from this folder, as follows:

	$ docker-compose -f docker-compose-test.yml up
	
	Starting dockerslinghosting_docker-setup-test_1...
	Attaching to dockerslinghosting_docker-setup-test_1
	docker-setup-test_1 | Congratulations, your docker-compose setup works. See the docker subfolder for the actual Sling hosting setup.
	dockerslinghosting_docker-setup-test_1 exited with code 0
	Gracefully stopping... (press Ctrl+C again to force)
   
If you see the _Congratulations, ..._ message it means your `docker-compose` setup works, and
this prototype should work as well.

## Sling Docker Images
To build the required OSGi bundles and Sling Docker images, run `mvn clean install` from this folder.

## Starting the cluster
To start the cluster, build the required Docker images as shown above and then, from the `docker` 
folder found under this `README` file , assuming `dockerhost` points to your Docker host:

    # Remove existing state, if any
    docker-compose kill
	docker-compose rm
	
	# IF DESIRED cleanup all your Docker state
	# (including ALL OTHER volumes and containers)
	docker volume rm $(docker volume ls -q)
	docker rm $(docker ps -q -a)

    # build the Docker images - might Download the Web (tm) the first time it runs
	docker-compose build

    # start the infrastructure containers	
	docker-compose up -d etcd mongo haproxy reddr
	
	# start the first Sling instance, which creates initial content
	docker-compose up -d default
	
	# Wait for http://dockerhost:81/ to show the Sling launchpad page
	# See also http://dockerhost/haproxy/stats for HTTP routing stats
	
	# start the remaining containers
	docker-compose up -d
	
After a few seconds http://dockerhost should show the Sling homepage, and the below routing
test scenario should work.

If things go wrong you can use `docker-compose logs S` where S is the name of a service
as defines in the `docker-compose.yml` file.

Later you can scale up the various containers using `docker-compose scale`, if desired.	See
the comments in the `docker-compose.yml` file for which ones make sense to scale.

## Routing test scenario
The following commands demonstrate the content-driven dynamic routing:

Create some content, with a .routing script that specifies the use of 
a backend worker with the 'fileserver' role for JSON rendering:

    export H=localhost
	curl -u admin:admin -Fsling:resourceType=test http://${H}/tmp/test
    curl -u admin:admin -X POST http://${H}/cluster/routing/scripts/test
    export W=fileserver
    echo $W > /tmp/1 && curl -u admin:admin -T /tmp/1 http://${H}/cluster/routing/scripts/test/json.routing
	
Note the `Sling-Instance-Info` header in the response to this request:

    curl -D - http://${H}/tmp/test.tidy.json
	
	...
	Sling-Instance-Info: SlingId:cd4374af-6192-4a31-9daa-17a016abebd6; sling.environment.info:"sling-role:fileserver"
	...
	
Requesting the same node with an `html` extension uses the `default` worker role, as it doesn't have a specific
`.routing` script:

    curl -D - http://${H}/tmp/test.html
	
    ...
	Sling-Instance-Info: SlingId:077141a0-63c0-4ab8-b5a4-b33782326000; sling.environment.info:"sling-role:default"	
	...
	
Routing can also be defined by HTTP method, here we route all PUT requests to the 'fileserver' processor, as well
as GET requests to `.jpg` files:

    curl -u admin:admin -F"sling:processorRole=fileserver" http://${H}/cluster/routing/methods/PUT
	echo fileserver > /tmp/1
	curl -u admin:admin -X POST http://${H}/cluster/routing/scripts/nt/file
	curl -u admin:admin -T /tmp/1 http://${H}/cluster/routing/scripts/nt/file/GET.routing
	
Now the following requests are handled by the 'fileserver' processor:

    echo "not an image but you get the idea" > /tmp/1
	curl -D - -u admin:admin -T /tmp/1 http://${H}/tmp/fakeimage.jpg
	...sling.environment.info:"sling-role:fileserver-6075c6d0b7c6"	
	
	curl -D - http://${H}/tmp/fakeimage.jpg
	...sling.environment.info:"sling-role:fileserver-6075c6d0b7c6"	

Finally, setting `sling:`processorRole` property in the content also defines a worker role, either on the resource or
its ancestors:

    curl -u admin:admin -F sling:processorRole=fileserver http://${H}/tmp
	
	curl -D - http://${H}/tmp/test.html
	...
	Sling-Instance-Info: SlingId:cd4374af-6192-4a31-9daa-17a016abebd6; sling.environment.info:"sling-role:fileserver"
	...	

## Load test scenario
The following requests can currently be used to generate load (example with the alpha.example.com test host):

    $ curl -u admin:admin http://dockerhost/at16.txt
    /at16 has 0 descendant nodes with an 'id' property.
    
    $ curl -u admin:admin -X POST http://dockerhost/at16.txt
    Added /at16/RootPostServlet/10/08/1008a470-42c8-432a-a7e3-73dd006e4497
    
    $ curl -u admin:admin -X POST http://dockerhost/at16.txt
    Added /at16/RootPostServlet/a4/f5/a4f5e869-9d99-4322-ba4c-0ffcc7474e1c
    
    $ curl -u admin:admin -X POST http://dockerhost/at16.txt
    Added /at16/RootPostServlet/e3/c1/e3c113c9-6374-48c6-b5b1-b3a42cdfb7dc
    
    $ curl -u admin:admin http://dockerhost/at16.txt
    /at16 has 3 descendant nodes with an 'id' property.
    
    $ ab -A admin:admin -p /dev/null -n 100 http://dockerhost/at16.txt
    This is ApacheBench, Version 2.3 <$Revision: 1706008 $>
	...
    Benchmarking alpha.example.com (be patient).....done
    Requests per second:    74.09 [#/sec] (mean)
	...
    
    $ curl -u admin:admin http://alpha.example.com/at16.txt
    /at16 has 103 descendant nodes with an 'id' property.

Metrics are available at http://dockerhost/system/console/slingmetrics - if several Sling instances are active this will hit each a different one every time due to the `haproxy` round-robin setup.
