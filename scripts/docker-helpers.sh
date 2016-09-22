#!/bin/bash
# helpers for docker-compose work, created for adaptTo 2016
alias dc="docker-compose $*"

function getContainer() {
	docker ps | grep $1 | cut -d' ' -f1
}

function dockerLog() {
	docker logs -f $(getContainer $1)
}

alias dl="dockerLog $*"

function slingLog() {
	docker exec $(getContainer $1) tail -F /opt/sling/sling/logs/error.log
}

alias sl="slingLog $*"