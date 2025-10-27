1) Objetivo

Orquestar la ejecución periódica del algoritmo de rutas sobre ventanas de tiempo simulado.

Controlar la velocidad con K (seg_sim/seg_real) y el salto con Sa (segundos simulados por tick).

Medir Ta (duración real del ciclo) y validar Sa > Ta con margen.

Derivar Sc = K * Sa (salto de consumo de datos por tick) y entregarlo al algoritmo.

Exponer APIs para estado/control y métricas para operación.

2) Variables y reglas

K (escala): seg_sim / seg_real. Ajustable en caliente.

Sa (salto simulado por tick): tamaño de ventana simulada [simStart, simEnd).

Ta (tiempo real de ejecución): medido en cada corrida; usar p50/p95 rolling.

Sc (salto de consumo): Sc = K * Sa (para decidir cuánto rango de datos consumir por tick).

Regla operativa: mantener Sa/K (intervalo real entre ticks) > Ta`.

Umbral de advertencia: Ta ≥ 0.7 * (Sa/K) → warning; Ta ≥ (Sa/K) → rechazar cambios de K/Sa.

3) Componentes a generar (paquetes sugeridos)
3.1 clock — Reloj de simulación (bean singleton)

Mantiene: k, simEpoch, realAnchor, paused, version.

Funciones: nowSimSeconds(), setK(), pause(), resume().

Persistencia: tabla simulation_control con un único registro.

3.2 scheduler — Planificador por tiempo simulado

Mantiene deltaSimSeconds = Sa, idleSleepMs, maxCatchUpWindows.

Bucle:

Calcula nextSim alineado a bordes de ventana.

Si paused o k<=0: espera corta y reintenta.

Sleep real hasta nextSim: (nextSim - nowSim)/K.

Emite evento de ventana [simStart=nextSim, simEnd=nextSim+Sa).

Avanza nextSim += Sa. Catch-up limitado si se acumuló atraso.

Publica un evento de dominio SimWindowEvent(simStart, simEnd, emittedAt).

3.3 routes — Orquestador del algoritmo de rutas

Interface del algoritmo (contrato):

Entrada: simStart, simEnd, consumptionHorizonSeconds=Sc, deadlineMs, runId.

Salida: resumen JSON y/o referencia a artefacto (blob), más métricas internas.

Handler de job (suscriptor de SimWindowEvent):

Idempotencia por ventana: clave routes:{simStart}-{simEnd}.

Concurrencia máx. configurable (default 1).

Deadline/cancelación respetando deadlineMs.

Medición de Ta y registro de estado.

Persistencia:

job_window: (job_type, sim_start, sim_end, status, attempts, duration_ms, last_error…).

routes_result: (window_id FK, summary JSON, artifact_ref…).

job_log: trazas con ts_real, ts_sim.

3.4 orders (opcional ahora)

Listener ligero para actualización de estados usando nowSimSeconds().

3.5 api — Endpoints REST + SSE

GET /sim/time: sim_now_seconds, k, sa_seconds, paused, version, server_*.

POST /sim/control (admin): action∈{set_k,set_sa,pause,resume}, k?, sa_seconds?, if_version, reason.

GET /sim/stream (SSE): tick con sim_now_seconds, k, paused, version.

GET /routes/status: lista ventanas (filtros from_sim, to_sim, limit).

GET /routes/result: por sim_start & sim_end.

GET /health, GET /metrics (si se integra Prometheus).

3.6 security

JWT o equivalente. Roles: viewer, admin.

POST /sim/control protegido (admin).

3.7 metrics

Gauges: sim_k, sim_sa_seconds, sim_paused, sim_now_seconds.

Histos/summary: routes_job_duration_ms (Ta), routes_deadlines_missed_total.

Gauges: routes_running, routes_queue_depth, sse_clients_connected.

4) Flujo por tick (paso a paso)

Planificador calcula sleep_real = (nextSim - nowSim)/K y duerme.

Emite ventana [simStart, simEnd), con simEnd = simStart + Sa.

RoutesJobHandler recibe el evento:

Calcula Sc = K * Sa.

Ensambla runId y verifica idempotencia (no ejecutar si ya success).

Verifica política de seguridad (Sa/K > Ta_p95 previo). Si no, marca queued_later o skipped_policy.

Ejecuta el algoritmo de rutas con deadlineMs y Sc.

Mide Ta (duración real), guarda resultado/errores y actualiza job_window.

Emite evento job-update por SSE.

El planificador avanza a la siguiente ventana. En reanudaciones puede
hacer catch-up de N ventanas (configurable) y, si excede, salta a la actual.

5) Validaciones operativas (al cambiar K o Sa)

Backend calcula intervalo real entre ticks: intervalo_real = Sa/K.

Si Ta_p95 ≥ 0.7 * intervalo_real → warning (permitir con confirmación).

Si Ta_p95 ≥ intervalo_real → rechazar (HTTP 400).

Guardar en auditoría: quién, cuándo, valores previos/nuevos y razón.

Ejemplos de payload (referencia):

POST /sim/control (cambiar K):
{"action":"set_k","k":14,"if_version":17,"reason":"Simulación 3 días"}

POST /sim/control (cambiar Sa):
{"action":"set_sa","sa_seconds":300,"if_version":17,"reason":"Ventana 5 min"}

6) Esquema de tablas (nombres y campos)

simulation_control
id(1), k, sim_epoch, real_anchor, paused, version, sa_seconds, updated_at.

job_window
id, job_type('routes'), sim_start, sim_end, status(queued|running|success|failed|timeout|skipped_policy),
attempts, duration_ms, last_error, created_at, updated_at.
Índices: (job_type, sim_start), (status, updated_at).

routes_result
id, window_id(FK), summary_json(TEXT/JSON), artifact_ref(VARCHAR), created_at.

job_log
id, window_id(FK), ts_real, ts_sim, level, message, payload_json.

(La IA ajusta tipos/DDL según la BD elegida.)

7) Configuración (application.yml sugerido)

sim.k (default: 14)

sim.sa-seconds (default: 300)

sim.idle-sleep-ms (default: 200)

sim.max-catchup-windows (default: 5)

routes.deadline-real-ms (default: 60000)

routes.max-concurrency (default: 1)

8) Estados del job y transiciones

queued → (tomado) → running

running → success (guardar resultado y Ta)

running → failed (registrar last_error)

running → timeout (si excede deadlineMs)

Cualquier → skipped_policy (si Sa/K <= Ta)

9) Pruebas manuales (E2E mínimas)

Arranque: GET /sim/time muestra k, sa_seconds y sim_now avanzando (si no pausado).

Ejecución automática: observar en BD o GET /routes/status cómo aparecen ventanas cada Sa/K segundos reales.

Cambiar K a 14 y Sa a 300 s: verificar que el intervalo real ≈ 21.43s.

Pausa/Reanuda: se detiene/reanuda la generación de ventanas (SSE debe reflejarlo).

Forzar deadline pequeño: ver timeout y conteo de “deadlines missed”.

Idempotencia: reintentar la misma ventana no duplica resultados.

10) Criterios de aceptación

El planificador emite ventanas alineadas y a la cadencia Sa/K (±tolerancia).

El algoritmo de rutas corre exactamente una vez por ventana (idempotente).

Ta queda registrado y visible en métricas; se aplican reglas Sa/K > Ta.

Cambios de K/Sa no rompen continuidad del tiempo simulado.

SSE refleja tick y job-update en tiempo real.

Logs y métricas incluyen timestamps real y simulado.

11) Notas de calibración (ejemplo de tu clase)

Ta = 1 min real, Sa = 5 min sim, K = 14

Intervalo real por tick = Sa/K = 5/14 ≈ 21.43 s.

Sc = 70 min → horizonte de consumo entregado al algoritmo por tick.

Verificar que Ta_p95 < 21.43 s para operar con holgura; si no, subir Sa o bajar K.