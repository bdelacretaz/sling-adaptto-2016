# docker-sling-hosting
This is a prototype Docker hosting of Sling instances.

For now, it connects a number of Sling instances to a common MongoDB database, and automatically makes
them available via a front-end HAProxy server.

The goals are to experiment with Sling in Docker and demonstrate automatic reconfiguration of related services
like HAProxy when Sling instances appear or disappear.

Apart from some glue scripts to setup the containers there's little or no code here, the goal is to use off-the-shelf
tools as much as possible, to concentrate on the proof of concept aspects. 

This is obviously not production-ready.

## Prerequisites
You need a Docker server and `docker-compose` setup to run this.

Unless you already have such a setup, https://www.docker.com/docker-toolbox is a good way to get started.

After installing `docker-compose`, you can test it from this folder, as follows:

	$ docker-compose -f docker-compose-test.yml up
	
	Starting dockerslinghosting_docker-setup-test_1...
	Attaching to dockerslinghosting_docker-setup-test_1
	docker-setup-test_1 | Congratulations, your docker-compose setup works. See the docker subfolder for the actual Sling hosting setup.
	dockerslinghosting_docker-setup-test_1 exited with code 0
	Gracefully stopping... (press Ctrl+C again to force)
   
If you see the _Congratulations, ..._ message it means your `docker-compose` setup works, and
this prototype should work as well.

## /etc/hosts setup
To access the virtual hosts that this prototype sets up, you'll need entries like this in your /etc/hosts:

    192.168.99.100 alpha.example.com
    192.168.99.100 bravo.example.com
    192.168.99.100 charlie.example.com
    ...

matching the `SLING_DOMAIN` values defined in the `docker/docker-compose.yml` file.

Replace 192.168.99.100 with the address of your Docker host if needed. `docker-machine ip default` provides
that value if you are using `docker-machine`.

## Starting the cluster
To start the cluster, for now it's safer to start the `mongo` and `etcd` containers first, as (pending more
testing) there might be startup timing issues otherwise.

So, from the `docker` folder found under this `README` file:


    # build the Docker images - might Download the Web (tm) the first time it runs
	docker-compose build

    # start the infrastructure containers	
	docker-compose up -d mongo etcd
	
	# wait a few seconds for those to start up, and start the other containers
	docker-compose up -d

After a few seconds, tests hosts like http://alpha.example.com should be proxied to the Sling container instances.

The HAProxy stats are available at http://alpha.example.com:81

## Adding more Sling hosts
Copying and adapting the `sling_001` section of the `docker/docker-compose.yml` file and running `docker-compose up SSS` where
SSS is the name of the new container should start a new Sling instance.

Make sure to give a unique port number and `SLING_DB` to each Sling instance if you do that.

Starting a new instance should cause it to be registered automatically in the HAproxy container, so the corresponding host (as
defined by the `SLING_DOMAIN` variable in the `docker-compose.yml`) should become available, provided you have added the 
corresponding `/etc/hosts` entry.

## TODO - known issues
I'm getting HTTP connection resets from HAproxy as I write this. Running the `util/check-sling.sh` shows about 10% failures.
When testing from a browser this translates into missing CSS or image resources, for example, from time to time.

The internal Sling instance port numbers should be assigned dynamically.

The cluster probably only works on a single Docker host so far, we'll need ambassador containers (or Mesos/Kubernetes) to make it multi-host.

## Tips & tricks (aka notes to self)
This rebuilds and runs a single container (here `haproxy`) in interactive mode, for debugging:

    docker run -it --link docker_etcd_1:etcd $(docker-compose build haproxy | grep "Successfully" | cut -d' ' -f3) bash
