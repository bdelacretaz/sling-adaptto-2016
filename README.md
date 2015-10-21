# docker-sling-hosting
This is a prototype Docker hosting of Sling instances.

For now, it connects a number of Sling instances to a common MongoDB database, and automatically makes
them available via a front-end HAProxy server.

The goals are to experiment with Sling in Docker and demonstrate automatic reconfiguration of related services
like HAProxy when Sling instances appear or disappear.

## Prerequisites
You need a Docker server and docker-compose setup to run this.

Unless you already have such a setup, https://www.docker.com/docker-toolbox is a good way to get started.

After installing `docker-compose`, you can test it from this folder, as follows:

	docker-compose -f docker-compose-test.yml up
	Starting dockerslinghosting_docker-setup-test_1...
	Attaching to dockerslinghosting_docker-setup-test_1
	docker-setup-test_1 | Congratulations, your docker-compose setup works. See the docker subfolder for the actual Sling hosting setup.
	dockerslinghosting_docker-setup-test_1 exited with code 0
	Gracefully stopping... (press Ctrl+C again to force)
   
If you see the "Congratulations, ..." message it means your docker-compose setup works, and
this prototype should work as well.

## /etc/hosts setup
To access the virtual hosts that this prototype sets up, you'll need entries like this in your /etc/hosts:

    192.168.99.100 alpha.example.com
    192.168.99.100 bravo.example.com
    192.168.99.100 charlie.example.com
    ...

matching the SLING_DOMAIN values defined in the `docker/docker-compose.yml` file.

Replace 192.168.99.100 with the address of your Docker host if needed. `docker-machine ip default` provides
that value if you are using `docker-machine`.

## Starting the cluster
To start the cluster, for now it's safer to start the `mongo` and `etcd` containers first, as (pending more
testing) there might be startup timing issues otherwise.

So, from the `docker` folder found under this `README` file:


    # build the Docker images - might Download the Web (tm) the first time it runs
	docker-compose build

    # start the infrastructure containers	
	docker-compose up mongo etcd
	
	# wait a few seconds for those to start up, and start the other containers
	docker-compose up

After a few seconds, tests hosts like http://alpha.example.com should then be proxied to the Sling container instances.

The HAPRoxy stats are available at http://alpha.example.com:81

## Adding more Sling hosts
Copying and adapting the `sling_001` section of the `docker/docker-compose.yml` file and running `docker-compose up SSS` where
SSS is the name of the new container should start a new Sling instance.

Starting a new instance should cause it to be registered automatically in the HAproxy container, so the corresponding host (as
defined by the SLING_DOMAIN variable in the `docker-compose.yml`) should become available, provided you have added the 
corresponding `/etc/hosts` entry.
