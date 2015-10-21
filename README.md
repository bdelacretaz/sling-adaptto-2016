# docker-sling-hosting
Experiments with Docker hosting multiple Sling instances

## Usage notes
Might need to start "docker-compose up -d etcd mongo" first, to avoid startup timing issues.
Wait a few seconds and later start everything with "docker-compose up -d".

To access the virtual hosts you'll need entries like this in your /etc/hosts:

    192.168.99.100 alpha.example.com
    192.168.99.100 bravo.example.com
    192.168.99.100 charlie.example.com
    ...

matching the SLING_DOMAIN values defined in your docker-compose.yml file.

Replace 192.168.99.100 with the address of your Docker host if needed.

URLs like http://alpha.example.com should then reach your Sling instances.

The HAPRoxy stats are available at http://192.168.99.100:81/
