# CORS 403 When Frontend Is Served Through nginx Proxy

## Problem
Dashboard worked correctly when run via `npm run dev` (Vite dev server, port 5173), but
returned a 403 Forbidden on every analysis request once the frontend was containerized
and accessed via the nginx-served build on port 3000.

## Root Cause
The Spring Boot `CorsFilter` whitelisted only `http://localhost:5173` (the Vite dev
server origin). When the containerized frontend is accessed at `http://localhost:3000`,
nginx proxies `/api/` requests to the backend container but forwards the original
`Origin: http://localhost:3000` header along with the request. Spring's CORS filter saw
an origin not in its whitelist and rejected the request with a 403, even though the
request was being proxied server-side, not made directly cross-origin by the browser in
the traditional sense.

## Fix
Added `http://localhost:3000` as a second allowed origin alongside the existing dev
server origin:

```java
config.addAllowedOrigin("http://localhost:5173");
config.addAllowedOrigin("http://localhost:3000");
```

Both origins are legitimate — 5173 for local frontend development outside Docker, 3000
for the containerized frontend accessed directly by a user's browser.

## Rule
When containerizing a frontend that proxies API calls through nginx, remember that the
browser's `Origin` header reflects where the user loaded the page from, not the internal
container-to-container hop. The backend's CORS whitelist must include every origin a real
browser will actually send, across every way the frontend can be accessed (dev server,
containerized build, any future production domain).