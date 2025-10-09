# MoraPack Dashboard — Frontend

Este repositorio contiene el frontend de "MoraPack Dashboard", una interfaz web construida con Create React App y React.

Este README está pensado para desarrolladores que quieran clonar, ejecutar y contribuir al frontend. Incluye instrucciones para Windows (cmd.exe) y buenas prácticas recomendadas.

## Tabla de contenidos

- [Resumen rápido](#resumen-rápido)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Scripts disponibles](#scripts-disponibles)
- [Desarrollo local](#desarrollo-local)
- [Estructura del proyecto (resumen)](#estructura-del-proyecto-resumen)
- [Descripción detallada de `src/`](#descripción-detallada-de-src)
- [Variables de entorno](#variables-de-entorno)
- [Cómo contribuir](#cómo-contribuir)
- [Pruebas](#pruebas)
- [Despliegue](#despliegue)
- [Dependencias principales](#dependencias-principales)


## Resumen rápido

- Nombre del paquete: `morapack-dashboard`
- Versión: `0.1.0`
- Tecnologías principales: React, React Router, Leaflet, React-Leaflet

## Requisitos

- Node.js (recomendado: 14.x / 16.x / 18.x)
- npm (v6+) o yarn
- Git

Comprueba tus versiones:

```cmd
node -v
npm -v
git --version
```

## Instalación

1. Clona el repositorio y sitúate en la carpeta `front` (o la raíz si ya estás en ella):

```cmd
git clone <repo-url>
cd <ruta-al-proyecto>\front
```

2. Instala dependencias con npm:

```cmd
npm install
```

## Scripts disponibles

Los scripts vienen definidos en `package.json` (Create React App):

- `npm start` — Ejecuta la app en modo desarrollo (http://localhost:3000).
- `npm run build` — Genera la versión optimizada para producción en la carpeta `build`.
- `npm test` — Ejecuta pruebas con el runner de Create React App.
- `npm run eject` — Ejecta la configuración (operación irreversible).

Ejemplo para arrancar en Windows (cmd.exe):

```cmd
npm start
```

## Desarrollo local

1. Arranca la aplicación:

```cmd
npm start
```

2. Abre un navegador en `http://localhost:3000`.
3. Modifica archivos en `src/` y la aplicación recargará automáticamente.

## Estructura del proyecto (resumen)

- `public/` — Archivos estáticos (index.html, manifest, iconos).
- `src/` — Código fuente
  - `App.js`, `index.js` — Entradas principales
  - `components/` — Componentes reutilizables y layouts
  - `pages/` — Vistas por funcionalidad (login, clientes, pedidos, simulación, etc.)
  - `services/` — Comunicación con APIs y lógica de servicios
  - `utils/` — Utilidades y helpers (ej. `flightSimulation.js`)
  - `assets/` — Imágenes y recursos
- `build/` — Salida de producción (generada por `npm run build`)

La carpeta `src/pages` contiene módulos como `clientes`, `simulacion`, `login`, `usuarios`, `vuelos`, entre otros.

## Descripción detallada de `src/`

A continuación se describe qué contiene y para qué sirve cada carpeta o archivo importante dentro de `src/`. Esta documentación ayuda a nuevos desarrolladores a entender rápidamente la organización del frontend.

- `assets/`
  - Recursos estáticos usados por la app.
  - `assets/data/` — Lugar para datos estáticos o archivos JSON de ejemplo (si se usan).
  - `assets/images/` — Imágenes e íconos (ej. `logo.svg`).

- `components/`
  - Componentes reutilizables y elementos UI que se usan en varias páginas.
  - `components/common/` — Componentes genéricos (botones, inputs, cards, loaders). (Vacío actualmente o con componentes generales según convención.)
  - `components/layout/` — Componentes relacionados con el layout de la aplicación:
    - `Header/` — Componentes y estilos para la cabecera de la aplicación (`Header.js`, `Header.css`).
    - `Navigation/` — Componentes y estilos para la navegación lateral o superior (`Navigation.js`, `Navigation.css`).

- `constants/`
  - Valores constantes y enums compartidos por la aplicación (URLs, claves, códigos de estado, mensajes, etc.).

- `hooks/`
  - Custom hooks reutilizables para lógica compartida (por ejemplo hooks para fetch, forms o manejo de estado local). (Vacío actualmente.)

- `pages/`
  - Vistas de nivel de aplicación — cada carpeta suele mapearse a una ruta.
  - `autenticacion/` — Módulos relacionados con autenticación (login, registro, recuperación de contraseña).
  - `clientes/` — Gestión de clientes (`Clientes.js`, `Clientes.css`): listados, creación/edición.
  - `configuracion/` — Ajustes de la aplicación.
  - `login/` — Pantalla de inicio de sesión (si existe separada de autenticación general).
  - `pedidos/` — Gestión de pedidos.
  - `planificador/` — Funcionalidad de planificación (si aplica).
  - `reportes/` — Vistas y componentes para generación y visualización de reportes.
  - `simulacion/` — Módulos del simulador y monitoreo:
    - `simulacion/Monitoreo/` — Componentes para supervisar la simulación (p. ej. `Simulador.css`, `Simulador.js`).
    - `simulacion/Simulador/` — Variantes del simulador:
      - `SimuladorColapso.js`, `SimuladorSemanal.js`, estilos asociados (`SImuladorColapso.css`, `SimuladorSemanl.css`).
  - `usuarios/` — Gestión de usuarios (`Usuarios.js`, `Usuarios.css`).
  - `vuelos/` — Módulos relacionados con vuelos (si están implementados).

- `services/`
  - Código responsable de la comunicación con APIs externas o internas. Aquí se colocan funciones que realizan fetch/axios a endpoints y encapsulan la lógica HTTP y manejo de errores.

- `styles/`
  - Estilos globales, variables CSS, temas o utilidades compartidas entre componentes.

- `utils/`
  - Funciones utilitarias y helpers del proyecto. Ejemplo: `flightSimulation.js` contiene lógica relacionada con la simulación de vuelos usada por los componentes de `simulacion/`.

- `index.js` — Punto de entrada: monta la aplicación en el DOM mediante `ReactDOM.

- `App.js` — Define rutas públicas y privadas, rutas de administración y simulación, y redirecciones.

- `setupTests.js`
  - Configuración global para el runner de pruebas (jest) — mocks, adaptadores o configuración antes de ejecutar tests.


## Variables de entorno

Si necesitas configurar endpoints o claves, usa archivos `.env` en la raíz del proyecto (Create React App exige que las variables que se exponen al cliente comiencen con `REACT_APP_`). Ejemplos:

- `.env` — variables locales
- `.env.production` — variables para producción

Ejemplo:

```
REACT_APP_API_URL=https://api.midominio.com
```

Recuerda no subir al repositorio variables sensibles (usa `.gitignore` si procede).


## Cómo contribuir

1. Crea una rama a partir de `main` o la rama de desarrollo:

```cmd
git checkout -b feature/mi-cambio
```

2. Haz commits pequeños y significativos. Incluye pruebas cuando sea posible.
3. Abre un Pull Request describiendo el objetivo, cambios y cómo probar.

## Pruebas

Ejecuta las pruebas con:

```cmd
npm test
```

Si añades pruebas, incluye al menos un caso de éxito y uno borde.

## Despliegue

Genera la versión de producción:

```cmd
npm run build
```

La carpeta `build/` contiene los archivos listos para servir. Puedes desplegarlos en un servicio de hosting estático (Netlify, Vercel, GitHub Pages, servidor Nginx/Apache, Azure Static Web Apps, etc.).

Si el frontend depende de un backend, asegúrate de configurar CORS y el history fallback para SPAs en el servidor.

## Dependencias principales

Las dependencias más relevantes incluidas en `package.json` son:

- `react`, `react-dom` — UI
- `react-router-dom` — Enrutado
- `leaflet`, `react-leaflet` — Mapas e interacción geoespacial
- `react-scripts` — Herramientas de build de Create React App



