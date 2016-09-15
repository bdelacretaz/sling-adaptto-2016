-- mod_lua handler, demonstrates proxying with an
-- additional header retrieved via HTTP
-- see docs at
-- https://httpd.apache.org/docs/2.4/mod/mod_lua.html
-- http://www.lua.org/docs.html

function remap(r)
  -- our config, generated from the container environment
  require("dyndis.config")
  
  -- get w via HTTP
  selectorUrl = DYNDIS_WORKER_SELECTOR_URL .. r.uri
  p = require "socket.http".request(selectorUrl)
  if p==nil
  then
	error("No content returned from worker selector " .. DYNDIS_WORKER_SELECTOR_URL)
  end
  	  
  string = require "string"
  role = string.match(p, DYNDIS_WORKER_SELECTOR_REGEXP)
  if role==nil
  then
	role = 'NO_HEADER_PROVIDED, REGEXP=' .. DYNDIS_WORKER_SELECTOR_REGEXP
  end

  r:notice(selectorUrl 
    .. " returned role " 
	.. role 
	.. ", proxying to "
	.. DYNDIS_PROXY_TO_URL
	.. " with header " 
	.. DYNDIS_ADD_HEADER_NAME .. "=" .. role
	)
  
  -- forward to mod_proxy with additional header
  -- using echo service so that we see the results
  r.handler = "proxy-server"
  r.proxyreq = apache2.PROXYREQ_REVERSE
  r.headers_in[DYNDIS_ADD_HEADER_NAME] = role
  r.filename = "proxy:" .. DYNDIS_PROXY_TO_URL .. r.uri

  -- we're done
  return apache2.OK
end
