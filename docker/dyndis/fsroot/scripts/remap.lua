-- mod_lua handler, demonstrates proxying with an
-- additional header retrieved via HTTP
-- see docs at
-- https://httpd.apache.org/docs/2.4/mod/mod_lua.html
-- http://www.lua.org/docs.html

function remap(r)
  -- our config, generated from the container environment
  require("dyndis.config")
  
  -- get w via HTTP
  p = require "socket.http".request(DYNDIS_WORKER_SELECTOR_URL)
  if p==nil
  then
	error("No content returned from worker selector " .. DYNDIS_WORKER_SELECTOR_URL)
  end
  	  
  string = require "string"
  w = string.match(p, DYNDIS_WORKER_SELECTOR_REGEXP)
  if w==nil
  then
	w='NO_HEADER_PROVIDED, REGEXP=' .. DYNDIS_WORKER_SELECTOR_REGEXP
  end

  -- forward to mod_proxy with additional header
  -- using echo service so that we see the results
  r.handler = "proxy-server"
  r.proxyreq = apache2.PROXYREQ_REVERSE
  r.headers_in[DYNDIS_ADD_HEADER_NAME] = w
  r.filename = "proxy:" .. DYNDIS_PROXY_TO_URL .. r.uri

  -- we're done
  return apache2.OK
end
