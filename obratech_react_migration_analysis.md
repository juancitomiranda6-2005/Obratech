# ObraTech — Cierre de Migración a React

> **Estado:** React se sirve desde Spring Boot; las 45 vistas Thymeleaf y sus recursos exclusivos fueron retirados.
> **Fecha:** 2026-10-01

---

## 1. Inventario del frontend heredado

El frontend antiguo usaba **Thymeleaf + TailwindCSS CDN + Vanilla JS**. El inventario siguiente es histórico: sus templates ya no están en el proyecto.

```
src/main/resources/
├── templates/                        ← Vistas Thymeleaf (HTML)
│   ├── admin/
│   │   └── usuarios.html
│   ├── clientes/
│   │   ├── calificar.html
│   │   ├── contratistas-para-calificar.html
│   │   ├── mis-contratistas.html
│   │   ├── mis-postulaciones.html
│   │   ├── postulantes-proyecto.html
│   │   ├── proyectos-en-proceso.html
│   │   └── reportes.html
│   ├── contratistas/
│   │   ├── contratista-equipos.html
│   │   └── crear-equipo.html
│   ├── trabajadores/
│   │   ├── detalles-postulacion.html
│   │   ├── mis-postulaciones.html
│   │   ├── perfil-laboral.html
│   │   ├── proyectos-disponibles.html
│   │   └── trabajador-equipo.html
│   ├── components/                   ← Vacío (sin componentes Thymeleaf reutilizables)
│   ├── admin-detalles-cliente.html
│   ├── admin-detalles-contratista.html
│   ├── completar-registro-oauth2.html
│   ├── contratista-historial-proyectos.html
│   ├── contratista-proyectos-asignados.html
│   ├── crear-trabajador.html
│   ├── desboard-admin.html
│   ├── desboard-contratista.html
│   ├── desboard-trabajador.html
│   ├── desboard.html
│   ├── detalles-contratista.html
│   ├── detalles-proyecto.html
│   ├── detalles-trabajador.html
│   ├── editar-contratista.html
│   ├── editar-perfil-cliente.html
│   ├── editar-proyecto.html
│   ├── editar-trabajador.html
│   ├── error.html
│   ├── index.html
│   ├── listar-clientes.html
│   ├── listar-contratistas.html
│   ├── listar-trabajadores.html
│   ├── login.html
│   ├── mis-proyectos.html
│   ├── perfil-cliente.html
│   ├── perfil-contratista.html
│   ├── publicar-proyecto-new.html
│   ├── registro.html
│   ├── seleccionar-perfil.html
│   └── seleccionar-proyecto-contrato.html
│
└── static/
    ├── img/
    │   ├── logo3.png
    │   └── image.png
    ├── js/
    │   ├── obratech.js              ← JS global compartido
    │   └── document-preview.js     ← Visor de documentos PDF
    └── styles/
        ├── scrollbar.css
        └── document-preview.css
```

---

## 2. Funcionalidades encontradas

| # | Funcionalidad | Rol(es) que la usan |
|---|---|---|
| 1 | **Autenticación** | Todos |
| 2 | **Registro** | Público |
| 3 | **Landing / Index** | Público |
| 4 | **Dashboard (rol-aware)** | Cliente, Contratista, Trabajador, Admin |
| 5 | **Proyectos** | Cliente, Contratista, Trabajador |
| 6 | **Postulaciones** | Trabajador, Cliente |
| 7 | **Perfil** | Cliente, Contratista, Trabajador |
| 8 | **Equipos de trabajo** | Contratista, Trabajador |
| 9 | **Calificaciones** | Cliente → Contratista |
| 10 | **Admin Panel** | Admin |
| 11 | **Contratistas** | Cliente, Admin |
| 12 | **Trabajadores** | Contratista, Admin |
| 13 | **Reportes** | Cliente |
| 14 | **Errores / Misc** | Todos |

---

## 3. Archivos agrupados por funcionalidad

### 🔐 Autenticación (Login / OAuth2)
- `templates/login.html`
- `templates/completar-registro-oauth2.html`
- **Controlador:** `LoginControllers.java`
- **API REST:** `POST /api/auth/login` → `AuthApiController.java`
- **Lógica JS:** `obratech.js` (bloque login-form)

### 📝 Registro
- `templates/registro.html`
- `templates/seleccionar-perfil.html`
- **Controlador:** `RegistroControllers.java`
- **Lógica JS:** `obratech.js` (bloque registro-form + password strength)

### 🏠 Landing / Página pública
- `templates/index.html`
- **Controlador:** `IndexControllers.java`

### 📊 Dashboard (por rol)
- `templates/desboard.html` ← Dashboard del **Cliente**
- `templates/desboard-contratista.html` ← Dashboard del **Contratista**
- `templates/desboard-trabajador.html` ← Dashboard del **Trabajador**
- `templates/desboard-admin.html` ← Dashboard del **Admin**
- **Controlador:** `DesboardController.java`

### 📁 Proyectos
- `templates/mis-proyectos.html` ← Vista cliente (sus proyectos)
- `templates/publicar-proyecto-new.html` ← Crear proyecto
- `templates/editar-proyecto.html` ← Editar proyecto
- `templates/detalles-proyecto.html` ← Ver proyecto
- `templates/contratista-proyectos-asignados.html` ← Proyectos asignados al contratista
- `templates/contratista-historial-proyectos.html` ← Historial del contratista
- `templates/clientes/proyectos-en-proceso.html` ← Proyectos activos del cliente
- `templates/trabajadores/proyectos-disponibles.html` ← Bolsa de proyectos
- `templates/seleccionar-proyecto-contrato.html` ← Seleccionar proyecto para contratar
- **Controladores:** `ProyectoControllers.java`, `MisProyectosController.java`, `ClienteControllers.java`
- **API REST:** `GET/POST/PUT/DELETE /api/proyectos`, `/api/proyectos/{id}`

### 📨 Postulaciones
- `templates/clientes/postulantes-proyecto.html` ← Lista de postulantes al proyecto
- `templates/clientes/mis-postulaciones.html` ← Postulantes vistos por cliente
- `templates/trabajadores/mis-postulaciones.html` ← Postulaciones del trabajador
- `templates/trabajadores/detalles-postulacion.html` ← Detalle de una postulación
- **Controlador:** `PostulacionController.java`

### 👤 Perfil
- `templates/perfil-cliente.html` ← Perfil del cliente
- `templates/editar-perfil-cliente.html` ← Editar perfil del cliente
- `templates/perfil-contratista.html` ← Perfil del contratista
- `templates/editar-contratista.html` ← Editar contratista
- `templates/trabajadores/perfil-laboral.html` ← Perfil laboral del trabajador
- `templates/editar-trabajador.html` ← Editar trabajador
- **API REST:** `GET/POST/PUT/DELETE /api/perfiles`

### 👷 Equipos de Trabajo
- `templates/contratistas/contratista-equipos.html` ← Gestión de equipos
- `templates/contratistas/crear-equipo.html` ← Crear equipo
- `templates/trabajadores/trabajador-equipo.html` ← Vista equipo del trabajador

### ⭐ Calificaciones
- `templates/clientes/calificar.html` ← Formulario de calificación
- `templates/clientes/contratistas-para-calificar.html` ← Lista de contratistas a calificar
- **Controlador:** `CalificacionController.java`
- **API REST:** `GET /api/calificaciones`, `POST /api/calificaciones`

### 🔧 Admin Panel
- `templates/desboard-admin.html` ← Dashboard admin
- `templates/admin/usuarios.html` ← Gestión de usuarios
- `templates/admin-detalles-cliente.html` ← Detalle cliente (admin)
- `templates/admin-detalles-contratista.html` ← Detalle contratista (admin)
- `templates/listar-clientes.html` ← Lista clientes
- `templates/listar-contratistas.html` ← Lista contratistas
- `templates/listar-trabajadores.html` ← Lista trabajadores
- `templates/detalles-contratista.html` ← Detalle contratista público
- `templates/detalles-trabajador.html` ← Detalle trabajador
- `templates/crear-trabajador.html` ← Crear trabajador (admin)
- **Controlador:** `AdminController.java`, `TrabajadorControllers.java`, `ContratistaControllers.java`

### 📊 Reportes
- `templates/clientes/reportes.html`
- **Controlador:** `ClienteControllers.java`

### 🛠️ Misceláneos / Compartidos
- `templates/error.html` ← Página de error
- `static/js/obratech.js` ← JS global
- `static/js/document-preview.js` ← Visor de documentos
- `static/styles/scrollbar.css` ← Estilos de scrollbar
- `static/styles/document-preview.css` ← Estilos del visor
- `static/img/logo3.png`, `static/img/image.png` ← Assets

---

## 4. API REST existente (usable por React)

El backend ya tiene endpoints REST listos:

| Endpoint | Método | Descripción |
|---|---|---|
| `POST /api/auth/login` | POST | Login con JWT |
| `GET /api/perfiles` | GET | Lista todos los perfiles |
| `GET /api/perfiles/{id}` | GET | Obtiene un perfil |
| `POST /api/perfiles` | POST | Crea perfil |
| `PUT /api/perfiles/{id}` | PUT | Actualiza perfil |
| `DELETE /api/perfiles/{id}` | DELETE | Elimina perfil |
| `GET /api/proyectos` | GET | Lista proyectos |
| `GET /api/proyectos/{id}` | GET | Obtiene proyecto |
| `POST /api/proyectos` | POST | Crea proyecto |
| `PUT /api/proyectos/{id}` | PUT | Actualiza proyecto |
| `DELETE /api/proyectos/{id}` | DELETE | Elimina proyecto |
| `POST /api/proyectos/{id}/asignar/{perfilId}` | POST | Asigna contratista |
| `GET /api/calificaciones` | GET | Lista calificaciones |
| `POST /api/calificaciones` | POST | Crea calificación |

Además de los endpoints anteriores, el backend ya expone APIs REST consumidas por React para:

- Dashboards de cliente, contratista y trabajador; reportes del cliente e historial del contratista.
- Postulaciones a proyectos, revisión de postulantes por proyecto y respuestas a invitaciones de trabajo.
- Gestión de equipos y perfiles profesionales, incluyendo archivos de perfil.
- Administración: métricas, validación de proyectos, verificación/activación de perfiles y creación de trabajadores.

Estas funciones ya no dependen exclusivamente de controladores Thymeleaf. Los templates antiguos se mantienen durante la coexistencia y no se eliminan en esta migración progresiva.

---

## 5. Problemas de organización del frontend heredado

Estos puntos describen las plantillas originales; no describen la app React nueva.

| Problema | Descripción |
|---|---|
| **Sin separación clara por dominio** | Los archivos están mezclados en la raíz de `templates/` junto con subcarpetas inconsistentes |
| **Carpeta `components/` vacía** | Existe pero no tiene nada — indicio de que la reutilización no está implementada |
| **JS global monolítico** | `obratech.js` mezcla lógica de login, registro, session warning y utilidades |
| **TailwindCSS vía CDN** | Cargado desde CDN en cada HTML individual — no hay un sistema de build |
| **Duplicación de estilos** | Cada template define su propio `tailwind.config` inline |
| **Sin sistema de rutas** | El enrutamiento es 100% servidor (Spring MVC) |
| **Assets sin estructura** | Solo 2 imágenes en `static/img/` sin carpetas por tipo |

---

## 6. Estructura de React propuesta

La app React vivirá en `frontend/` en la raíz del proyecto, **separada completamente del backend**.

```
frontend/                              ← Proyecto React (Vite)
├── public/
│   └── favicon.ico
├── src/
│   ├── main.jsx                      ← Punto de entrada
│   ├── App.jsx                       ← Router principal
│   │
│   ├── assets/                       ← Imágenes, fuentes, íconos
│   │   ├── img/
│   │   │   ├── logo3.png
│   │   │   └── image.png
│   │   └── styles/
│   │       └── globals.css           ← Reset + variables CSS
│   │
│   ├── config/
│   │   └── api.js                    ← Base URL y configuración de Axios
│   │
│   ├── hooks/
│   │   ├── useAuth.js                ← Hook de autenticación (JWT)
│   │   └── useApi.js                 ← Hook genérico para llamadas API
│   │
│   ├── services/                     ← Capa de comunicación con el backend
│   │   ├── authService.js            ← POST /api/auth/login
│   │   ├── perfilService.js          ← /api/perfiles
│   │   ├── proyectoService.js        ← /api/proyectos
│   │   ├── calificacionService.js    ← /api/calificaciones
│   │   └── postulacionService.js     ← (cuando haya endpoint REST)
│   │
│   ├── context/
│   │   └── AuthContext.jsx           ← Contexto global de autenticación
│   │
│   ├── components/                   ← Componentes reutilizables
│   │   ├── ui/
│   │   │   ├── Button.jsx
│   │   │   ├── Input.jsx
│   │   │   ├── Badge.jsx
│   │   │   ├── Card.jsx
│   │   │   └── Modal.jsx
│   │   ├── layout/
│   │   │   ├── Sidebar.jsx           ← Sidebar reutilizable por rol
│   │   │   ├── AppLayout.jsx         ← Layout con sidebar + main
│   │   │   └── PublicLayout.jsx      ← Layout sin sidebar (login/registro)
│   │   └── shared/
│   │       ├── PasswordStrengthBar.jsx
│   │       ├── DocumentPreview.jsx
│   │       └── SessionWarning.jsx
│   │
│   └── pages/                        ← Páginas por dominio
│       ├── auth/
│       │   ├── LoginPage.jsx
│       │   ├── RegistroPage.jsx
│       │   └── CompletarRegistroPage.jsx
│       ├── landing/
│       │   └── LandingPage.jsx
│       ├── dashboard/
│       │   ├── DashboardCliente.jsx
│       │   ├── DashboardContratista.jsx
│       │   ├── DashboardTrabajador.jsx
│       │   └── DashboardAdmin.jsx
│       ├── proyectos/
│       │   ├── MisProyectos.jsx
│       │   ├── PublicarProyecto.jsx
│       │   ├── EditarProyecto.jsx
│       │   ├── DetallesProyecto.jsx
│       │   ├── ProyectosDisponibles.jsx
│       │   └── ProyectosEnProceso.jsx
│       ├── postulaciones/
│       │   ├── MisPostulaciones.jsx
│       │   ├── PostulantesProyecto.jsx
│       │   └── DetallesPostulacion.jsx
│       ├── perfil/
│       │   ├── PerfilCliente.jsx
│       │   ├── PerfilContratista.jsx
│       │   └── PerfilLaboral.jsx
│       ├── equipos/
│       │   ├── ContratistaEquipos.jsx
│       │   ├── CrearEquipo.jsx
│       │   └── TrabajadorEquipo.jsx
│       ├── calificaciones/
│       │   ├── CalificarContratista.jsx
│       │   └── ContratistasParaCalificar.jsx
│       ├── admin/
│       │   ├── DashboardAdmin.jsx
│       │   ├── Usuarios.jsx
│       │   ├── ListarClientes.jsx
│       │   ├── ListarContratistas.jsx
│       │   └── ListarTrabajadores.jsx
│       ├── reportes/
│       │   └── ReportesPage.jsx
│       └── misc/
│           └── ErrorPage.jsx
│
├── index.html
├── vite.config.js
├── package.json
└── .env                               ← VITE_API_URL=http://localhost:9999
```

---

## 7. Arquitectura integrada

- El frontend fuente vive en `frontend/`; `npm run build` genera la SPA en `backend/src/main/resources/static/`.
- Spring Boot sirve React y reenvía navegaciones HTML profundas a `index.html` mediante `SpaRoutingInterceptor`.
- Las API `/api/**`, OAuth, uploads y documentos conservan sus handlers y autorizaciones; no caen en el fallback SPA.
- Las vistas heredadas se retiraron; `SpaViewResolver` convierte los nombres de vista que aún devuelven controladores MVC a la entrada React.
- El módulo Java/Maven vive en `backend/`; el POM raíz agrega el módulo.
- OAuth2 usa mismo origen por defecto; `APP_FRONTEND_URL` permite configurar un origen de desarrollo externo.

---

## 8. Estado de migración por fase

| Fase | Alcance | Estado actual |
|---|---|---|
| 0 | Base React, Vite, Router, Axios y contexto de autenticación | ✅ Implementada |
| 1 | Login JWT y flujo OAuth2 | ✅ Implementados |
| 2 | Registro y finalización de registro OAuth2 | ✅ Implementados |
| 3 | Landing pública | ✅ Implementada |
| 4 | Dashboards de cliente, contratista, trabajador y admin | ✅ Implementados; navegación de postulantes del cliente completada |
| 5 | Proyectos: publicar, listar, consultar, editar y eliminar | ✅ Implementados en React con APIs REST |
| 6 | Perfiles de cliente, contratista y trabajador | ✅ Implementados; incluyen edición y funciones de archivo donde aplica |
| 7 | Calificaciones | ✅ Flujo React disponible |
| 8 | Postulaciones: enviar, consultar y revisar por proyecto | ✅ Implementadas con APIs REST |
| 9 | Equipos de trabajo | ✅ Vistas y APIs REST disponibles |
| 10 | Reportes del cliente | ✅ Vista React y API REST disponibles |
| 11 | Panel de administración | ✅ Dashboard, usuarios, perfiles y acciones administrativas implementados |

**Verificación global:** el build React pasó y las 16 pruebas Java pasaron, incluidas pruebas del fallback SPA, API JSON y documentos protegidos. No se ejecutó E2E en navegador compartido.

---

## ⚠️ Señales de alerta que debes saber antes de empezar

1. **OAuth2 Google:** React ya inicia el flujo del servidor y recupera la sesión mediante `/oauth/callback` y `/api/auth/oauth-session`. Requiere credenciales Google configuradas y una sesión válida del backend.

2. **Autenticación:** React envía JWT en sus llamadas API; cerrar sesión también invalida la sesión Spring. OAuth2 vuelve a la SPA del mismo origen por defecto.

3. **Subida de archivos / documentos:** React ya usa `FormData` para los perfiles de contratista y trabajador, y dispone de acciones para abrir sus documentos. El botón de apertura del CV del trabajador se corrigió en esta auditoría.

4. **Rutas por rol:** El despacho de pantallas por rol ya está implementado en `frontend/src/App.jsx`; los endpoints API aplican además reglas de autoridad en Spring Security.

5. **Sesión expirada:** Durante esta revisión, el navegador compartido tenía un JWT vencido y una llamada API terminó redirigida al login con error CORS. Hace falta repetir E2E con sesiones válidas y comprobar el manejo de expiración.

---

## ✅ Resumen de avance

Las fases funcionales enumeradas arriba tienen vistas React y servicios REST implementados. Las vistas Thymeleaf y sus cuatro recursos exclusivos fueron retirados; las imágenes compartidas se conservaron.

### Checklist de avances realizados

- [x] **Fase 0 — Setup base de React**
- [x] Crear la carpeta `frontend/` con Vite + React
- [x] Configurar Axios, React Router y contexto de autenticación
- [x] Definir estructura modular por dominio (`config`, `context`, `hooks`, `services`, `components`, `pages`)
- [x] Crear layouts públicos y protegidos
- [x] Implementar páginas base de login, registro, landing, dashboard y perfil
- [x] Migrar las opciones restantes de los dashboards por rol (cliente, contratista y trabajador) con navegación y estados parecidos a la versión anterior
- [x] Dar acceso directo a postulantes desde cada proyecto reciente del cliente y desde la lista de proyectos
- [x] Corregir la acción “Abrir CV” del perfil trabajador: el manejador estaba fuera del alcance del botón
- [x] Conservar la consulta y actualización del avance del trabajador desde “Mi equipo de trabajo”
- [x] Integrar resúmenes, invitaciones, proyectos activos, verificación y perfiles dentro de cada dashboard
- [x] Implementar las fases de proyectos, perfiles, calificaciones, postulaciones, equipos, reportes y administración en React con sus integraciones REST
- [x] Conectar la autenticación con el backend real (`/api/auth/login`)
- [x] Validar login end-to-end con usuario admin real
- [x] Validar el estado actual del proyecto con comprobación estática sin errores de edición en `frontend/src`
- [x] Mantener el backend sin cambios durante el scaffold inicial; las fases funcionales posteriores añadieron las APIs REST necesarias
- [x] Ejecutar `npm run build` en producción (Vite finalizó correctamente; reportó advertencia de chunk mayor a 500 kB)
- [x] Ejecutar `npm run lint` (sin errores; dos advertencias de React)
- [x] Implementar informes narrativos de avance del contratista con historial y autorización por proyecto asignado
- [x] Implementar solicitud y restablecimiento de contraseña con token aleatorio de un solo uso, hash persistido y expiración de 30 minutos
- [ ] Configurar y verificar entrega de correo SMTP real mediante `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `APP_MAIL_FROM` y `APP_FRONTEND_URL`
- [x] Probar en navegador solicitud genérica de recuperación (`200`) y rechazo de token inválido (`400`)
- [x] Ejecutar pruebas unitarias de recuperación y autorización de informes
- [ ] Ejecutar prueba E2E de los cuatro roles en un navegador compartido con credenciales válidas

### Estado de cierre de la fase de dashboards

La migración de las pantallas de dashboard quedó integrada en React con una estructura comparable a la anterior, manteniendo las secciones clave por rol:

- Cliente: resumen, proyectos recientes, estado de verificación, acceso a perfil y revisión directa de postulantes por proyecto.
- Contratista: resumen, proyectos asignados, historial, perfil, mercado de trabajadores, equipos e informes narrativos fechados por proyecto.
- Trabajador: resumen, registro del avance de sus equipos, invitaciones pendientes, proyectos asignados, disponibilidad, perfil laboral y acceso al CV desde el perfil.
- Admin: panel con métricas, validación de proyectos y verificación de usuarios.

Las funciones solicitadas están implementadas. Para habilitar el envío real de recuperación se deben configurar las variables SMTP indicadas; también queda la prueba E2E completa de los cuatro roles con sesiones válidas. Build y lint ya se ejecutaron en esta sesión.

---

## ✅ Base del proyecto React

Se dejó creada la base del proyecto React en la carpeta `frontend/` con:

- Vite + React
- estructura modular por dominio
- React Router para rutas públicas y protegidas
- contexto de autenticación
- servicios para auth, perfiles, proyectos y calificaciones
- layouts públicos y de app
- páginas base para login, registro, dashboard y perfil

La estructura principal queda bajo:

- `frontend/src/config/`
- `frontend/src/context/`
- `frontend/src/hooks/`
- `frontend/src/services/`
- `frontend/src/components/`
- `frontend/src/pages/`

En esta revisión, el build React y la suite Java finalizaron correctamente. Lint no reporta errores, aunque conserva dos advertencias de React; el bundle supera 500 kB. El envío real de correo depende de SMTP configurado y sigue pendiente la prueba E2E completa indicada en el checklist.
