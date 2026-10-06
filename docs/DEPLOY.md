# Deploy

> Cómo está publicado el curso y cómo se opera ([ADR 0008](adr/0008-hosting-railway-cloudflare.md)):
> api y runner en **Railway**, la web en **Cloudflare** (un Worker que sirve los archivos
> estáticos; Cloudflare Pages quedó integrado a Workers). Todo sale de los Dockerfiles y del
> `pnpm build` del repo: no hay pasos manuales de compilación.

## Mapa

| Componente | Dónde | Cómo llega | URL |
|---|---|---|---|
| `services/runner` | Railway, servicio `runner`, **sin dominio público** | `railway up` desde `services/runner` | `http://runner.railway.internal:8081` (red privada) |
| `services/api` | Railway, servicio `api`, dominio público | `railway up` desde la raíz del repo | `https://api-production-c28c0.up.railway.app` |
| `apps/web` | Cloudflare Workers (assets estáticos), proyecto `ljbu` | `pnpm build` + `wrangler deploy` desde `apps/web` | `https://ljbu.balbiano06.workers.dev` |

Proyecto de Railway: `ljbu`, en el workspace de Luciano. El CLI (`railway`) está enlazado
desde la raíz del repo (`railway status`).

## Variables

| Variable | Servicio | Valor en producción |
|---|---|---|
| `RUNNER_TOKEN` | runner y api | Secreto compartido, 64 hex. Se genera con `openssl rand -hex 32` y se carga en los dos servicios; **nunca** en el repo |
| `PORT` | runner | `8081` (fijo, para que la api lo encuentre en la red privada) |
| `RUNNER_URL` | api | `http://runner.railway.internal:8081` |
| `ALLOWED_ORIGINS` | api | El origen de la web: `https://ljbu.balbiano06.workers.dev` (varios, separados por coma) |
| `RUNNER_MAX_CONCURRENT`, `RUNNER_QUEUE_CAPACITY` | runner | Opcionales (defaults 2 y 8) |
| `RATE_LIMIT_RUNS_PER_MINUTE` | api | Opcional (default 10 por IP) |
| `VITE_API_URL` | web (al hacer `pnpm build`) | `https://api-production-c28c0.up.railway.app` |

La api recibe su `PORT` de Railway. Detrás del proxy de Railway, la IP del alumno llega en
`X-Forwarded-For` (`server.forward-headers-strategy: native`), que es lo que usa el rate limit.

## Railway: primera vez

```sh
railway login
railway init --name ljbu --workspace "<workspace>"
TOKEN=$(openssl rand -hex 32)
railway add --service runner --variables "RUNNER_TOKEN=$TOKEN" --variables "PORT=8081"
railway add --service api --variables "RUNNER_TOKEN=$TOKEN" \
  --variables "RUNNER_URL=http://runner.railway.internal:8081"
railway up services/runner --path-as-root --service runner --detach
railway up --service api --detach                               # contexto: la raíz del repo
railway domain --service api                                    # dominio público de la api
railway variables --service api --set "ALLOWED_ORIGINS=https://<la web>"
```

- La infraestructura está **como código** en `.railway/railway.ts` (SDK `railway`, dev
  dependency de la raíz): servicios, healthchecks, variables no secretas y `preserve()` para
  los secretos. `railway config plan` muestra las diferencias; `railway config apply --yes`
  las aplica. La api elige su Dockerfile con la variable `RAILWAY_DOCKERFILE_PATH`
  (`services/api/Dockerfile`); el runner se sube con `--path-as-root` desde `services/runner`,
  donde su `Dockerfile` está en la raíz. `.railwayignore` deja fuera de la subida de la api lo
  que no necesita.
- El runner **no** tiene dominio: solo la api lo alcanza, por la red privada (IPv6). No hay
  que crearle uno.
- Memoria: el runner lanza JVM hijas de 64 MB, hasta `RUNNER_MAX_CONCURRENT` a la vez; con
  512 MB–1 GB alcanza. Si Railway lo reinicia por memoria, subir el límite del servicio.

## Railway: cada deploy

Desde `main` actualizado:

```sh
railway up services/runner --path-as-root --service runner --detach
railway up --service api --detach
railway deployment list --service api      # estado
railway logs --service api                 # logs en vivo (nunca incluyen código del alumno)
```

Para volver a una versión anterior: `railway deployment redeploy --service api` sobre el
deployment elegido (`railway deployment list` muestra los ids).

## Cloudflare: la web

`apps/web/wrangler.jsonc` describe el Worker `ljbu`: sirve `apps/web/dist` como assets
estáticos y manda toda ruta desconocida a `index.html` (`single-page-application`, porque
la app usa React Router). Primera vez: `npx wrangler login` (abre el navegador).

Cada deploy, desde `main` actualizado:

```sh
cd apps/web
VITE_API_URL=https://api-production-c28c0.up.railway.app pnpm build
npx wrangler deploy
```

`VITE_API_URL` se fija **al compilar** (Vite la incrusta en el bundle): si cambia la api,
hay que volver a compilar y desplegar. Si cambia el origen de la web, cargarlo en
`ALLOWED_ORIGINS` de la api.

Pendiente: un workflow de GitHub Actions que haga estos dos pasos en cada push a `main`
(necesita un token de API de Cloudflare como secreto del repositorio).

## Comprobación después de un deploy

```sh
curl https://api-production-c28c0.up.railway.app/api/v1/health      # {"status":"ok"}
curl https://api-production-c28c0.up.railway.app/api/v1/modules     # los 4 módulos
```

Y en la web (`https://ljbu.balbiano06.workers.dev`): abrir el 1.3, ejecutar la solución, ver
la maqueta. Una ejecución tarda < 4 s en p95 (criterio de M7; el primer deploy midió 1,1 s);
si tarda más, revisar `railway logs --service runner`.

## Rotar el token del runner

```sh
TOKEN=$(openssl rand -hex 32)
railway variables --service runner --set "RUNNER_TOKEN=$TOKEN"
railway variables --service api --set "RUNNER_TOKEN=$TOKEN"
```

Railway redespliega los dos servicios; hay unos segundos de `503` entre uno y otro.

## Dominio propio (opcional)

- Web: en el panel de Cloudflare → Workers → `ljbu` → Domains & Routes, agregar el dominio;
  Cloudflare crea el DNS si el dominio está en la misma cuenta.
- Api: `railway domain --service api <subdominio>` y un `CNAME` al valor que indica Railway.
  Actualizar `VITE_API_URL` (rebuild de Pages) y `ALLOWED_ORIGINS`.
