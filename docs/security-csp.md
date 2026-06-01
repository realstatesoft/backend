# Content-Security-Policy (CSP)

OpenRoof aplica CSP en dos capas: el **frontend (SPA)** en Vercel y la **API REST** en Spring Boot (solo perfil `prod`).

## Dónde está configurado

| Capa | Archivo | Activo en |
|------|---------|-----------|
| Frontend SPA | `frontend/vercel.json` → header `Content-Security-Policy` | Deploy Vercel (prod y previews) |
| API REST | `backend/.../SecurityHeadersFilter.java` | `SPRING_PROFILES_ACTIVE=prod` (`security.headers.csp-enabled=true`) |

La CSP del navegador la impone **Vercel** al servir `index.html` y assets estáticos. La CSP del backend protege respuestas de la API (`default-src 'none'`) por defensa en profundidad.

---

## Policy del frontend (SPA)

Directiva | Valor | Motivo
----------|-------|--------
`default-src` | `'self'` | Base restrictiva |
`script-src` | `'self' 'wasm-unsafe-eval'` | Bundle Vite; WebGL/WASM de `@google/model-viewer` / Three.js |
`style-src` | `'self' 'unsafe-inline' https://fonts.googleapis.com` | Bootstrap, SweetAlert2, estilos inline de React; hoja Google Fonts |
`font-src` | `'self' https://fonts.gstatic.com data:` | Poppins (Google Fonts) |
`img-src` | `'self' data: blob:` + dominios listados + `https:` | Fotos Supabase, tiles OSM, placeholders Unsplash, avatares URL externas |
`connect-src` | `'self' https://*.supabase.co https://nominatim.openstreetmap.org https: wss:` | API backend, geocoding, storage |
`media-src` | `'self' blob: https://*.supabase.co https:` | Modelos `.glb` / video |
`worker-src` | `'self' blob:` | Photo Sphere Viewer, model-viewer |
`frame-src` | `'self' https://www.google.com https://*.google.com https://*.supabase.co https:` | Mapas Google embed, PDFs, tours 360 externos (solo HTTPS) |
`object-src` | `'none'` | Sin Flash/plugins |
`base-uri` | `'self'` | Anti-injection |
`form-action` | `'self'` | Formularios al mismo origen |
`frame-ancestors` | `'none'` | Anti-clickjacking del SPA |
`upgrade-insecure-requests` | (activo) | Fuerza HTTPS en recursos mixtos |

### Dominios explícitos en `img-src`

- `https://*.supabase.co` — imágenes, PDFs y modelos en Storage
- `https://*.tile.openstreetmap.org` — mapas Leaflet
- `https://unpkg.com` — iconos marker Leaflet (fallback en código)
- `https://images.unsplash.com` — placeholders de demo
- `https://modelviewer.dev` — icono AR en model-viewer
- `https:` — avatares y fotos con URL externa ingresada por usuarios/agentes

### Excepciones documentadas (relajaciones necesarias)

1. **`style-src 'unsafe-inline'`** — React, Bootstrap y SweetAlert2 inyectan estilos inline. Endurecer requeriría nonces/hashes en build.
2. **`script-src 'wasm-unsafe-eval'`** — Visor 3D (`model-viewer` + Three.js). Sin esto el modelo GLB no renderiza.
3. **`connect-src https:` / `img-src https:` / `frame-src https:`** — API en dominio distinto al frontend (Render, etc.), avatares URL libre, tours/PDF en iframe HTTPS dinámico.
4. **`frame-src https:`** — `PropertyVirtualTour` acepta cualquier tour HTTPS validado en app.

---

## Policy de la API (backend)

```
default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'
```

Configurable con `security.headers.csp-api-policy` si hiciera falta ampliar (p. ej. Swagger en staging).

Activación: `application-prod.yml` → `security.headers.csp-enabled: true`.

---

## Verificación en producción

```bash
# Frontend (Vercel)
curl -I https://<frontend-domain>/

# API
curl -I https://<api-domain>/api/actuator/health
```

Comprobar presencia de `Content-Security-Policy` y que la consola del navegador no reporte violaciones en:

- Login / listado de propiedades
- Ficha de propiedad (galería, mapa OSM, modelo 3D, tour 360, planos PDF)
- Subida de documentos KYC
- Mantenimiento (imágenes)

---

## Ampliar la policy

1. Identificar el origen bloqueado en DevTools → Console (`Refused to load...`).
2. Preferir añadir el dominio concreto antes que relajar toda la directiva.
3. Actualizar `frontend/vercel.json` y esta documentación.
4. Redeploy frontend en Vercel.

Si el backend cambia de host, no hace falta tocar CSP del frontend gracias a `connect-src https:` (CORS sigue siendo la restricción principal para XHR).
