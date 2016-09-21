-- mod_lua handler, demonstrates proxying with an
-- additional header retrieved via HTTP
-- see docs at
-- https://httpd.apache.org/docs/2.4/mod/mod_lua.html
-- http://www.lua.org/docs.html

function remap(r)
  -- our config, generated from the container environment
  require("reddr.config")
  string = require "string"
  
  -- get role via HTTP
  local selectorUrl = REDDR_PROCESSOR_SELECTOR_URL .. r.uri
  local out = {}
  
  local H_CONTENT_TYPE = "Content-Type"
  local contentType = r.headers_in[H_CONTENT_TYPE]
  local selectorHeaders = {}
  if contentType ~= nil
  then
    selectorHeaders[H_CONTENT_TYPE] = contentType
  else
    contentType = ""	
  end
  
  require "socket.http".request{ 
    url=selectorUrl, 
    method=r.method, 
    headers=selectorHeaders,
    sink=ltn12.sink.table(out) 
  }
  
  local selectorString = table.concat(out)
  if selectorString==nil
  then
	error("No content returned from processor selector " .. REDDR_PROCESSOR_SELECTOR_URL)
  end
  	  
  local role = string.match(selectorString, REDDR_PROCESSOR_SELECTOR_REGEXP)
  if role==nil
  then
	role = 'NO_HEADER_PROVIDED, REGEXP=' .. REDDR_PROCESSOR_SELECTOR_REGEXP
  end

  r:notice(selectorUrl 
    .. " returned role " 
	.. role 
	.. ", proxying to "
        .. r.method .. " "
	.. REDDR_PROXY_TO_URL
	.. " with headers " 
	.. REDDR_ADD_HEADER_NAME .. "=" .. role
	.. " Content-Type="
	.. contentType
	)
  
  -- forward to mod_proxy with additional header
  r.handler = "proxy-server"
  r.proxyreq = apache2.PROXYREQ_REVERSE
  r.headers_in[REDDR_ADD_HEADER_NAME] = role
  r.filename = "proxy:" .. REDDR_PROXY_TO_URL .. r.uri

  -- we're done
  return apache2.OK
end
