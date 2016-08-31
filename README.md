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

## Sling Launchpad
The demo uses its own custom launchpad, in a Docker image that needs to be built like this:

    cd slingbuild/
    mvn clean install
    docker build -t sling-at16 .
	
The `sling-at16` Docker image that this builds is used by the `docker-compose` setup later.	

## Starting the cluster
To start the cluster, for now it's safer to start the `mongo`, `graylog` and `etcd` containers first, as (pending more
testing) there might be startup timing issues otherwise.

Also, as all Sling instances are sharing the same Mongo database, for now it's safer to start one Sling instance first 
and wait for it to be ready before starting the others, to avoid any race conditions.

So, from the `docker` folder found under this `README` file (assuming `dockerhost` points to your Docker host - if
using `docker-machine` its IP address is provided by `docker-machine ip default`):

    # Remove existing state, if any
	docker-compose rm

    # build the Docker images - might Download the Web (tm) the first time it runs
	docker-compose build

    # start the infrastructure containers	
	docker-compose up -d mongo etcd haproxy graylog
	
	# wait for that to be ready (watch http://dockerhost/index.html)
	
    # start the other containers, if any
	docker-compose up -d
	
	# change the number of Sling instances, if desired
	docker-compose scale sling=3

After a few seconds, http://dockerhost should be proxied to the Sling container instances.

The HAProxy stats are available at http://dockerhost/haproxy/stats

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

## Configuring graylog
*TODO update this section*

Aggregated logs are provided by graylog at http://alpha.example.com:9000 . The initial credentials are _admin/admin_.

To collect them you need to configure an input at http://alpha.example.com:9000/system/inputs - create an input of 
type GELF TCP on port 12201.

Once that's done the search interface at http://alpha.example.com:9000/ should show messages emitted by the Sling
containers at regular intervals. For now these are simulated, like "Hello from delta.example.com/a96333b3f7b7...", 
we'll need to connect the Sling logging subsystem to graylog using a specific Logback GELF appender.

## Adding more Sling hosts
*TODO update this section*

Copying and adapting the `sling_001` section of the `docker/docker-compose.yml` file and running `docker-compose up SSS` where
SSS is the name of the new container should start a new Sling instance.

Make sure to give a unique port number and `SLING_DB` to each Sling instance if you do that.

Starting a new instance should cause it to be registered automatically in the HAproxy container, so the corresponding host (as
defined by the `SLING_DOMAIN` variable in the `docker-compose.yml`) should become available, provided you have added the 
corresponding `/etc/hosts` entry.

The logs of the new instance will also appear automatically in Graylog.

## TODO - known issues
*TODO update this section*

The internal Sling instance port numbers should be assigned dynamically. And we shouldn't need to expose them outside of
the Docker host, this is done for debugging purposes for now.

No real logs are sent to graylog yet, the current setup just demonstrates how container logs can
be aggregated in graylog.

The cluster probably only works on a single Docker host so far, we'll need ambassador containers (or something like Mesos or Kubernetes) to make it multi-host.

## Tips & tricks (aka notes to self)
This rebuilds and runs a single container (here `haproxy`) in interactive mode, for debugging:

    docker run -p 80:80 -p 81:81 -it --link docker_etcd_1:etcd $(docker-compose build haproxy | grep "Successfully" | cut -d' ' -f3) bash
