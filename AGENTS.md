# ObraTech - AGENTS.md

Este archivo contiene directivas de alto nivel para agentes OpenCode que trabajan en este repositorio Spring Boot. No todos los elementos aplicarán en cada caso, pero leerlos primero evitará errores comunes.

## Compilación & Ejecución

| Comando | Descripción |
|---|---|
| `mvnw clean package` | Compilar y empaquetar el proyecto. **Usar `mvnw`, no `mvn`** — el wrapper incluye Maven 3.9.11 y respeta la configuración `.mvn/wrapper/`. |
| `mvnw -pl backend spring-boot:run` | Ejecutar el backend desde el código (plugin de Spring Boot). |
| `java -jar backend/target/obratech-0.0.1-SNAPSHOT.jar` | Ejecutar el JAR compilado directamente. |
| `run.bat` | Ejecutador para Windows: compila (si el JAR falta) y luego inicia la app. |

**Importante**: La primera ejecución de `mvnw` descargará Maven si no está en caché. Ejecuciones posteriores son rápidas si `.m2` está poblado.

## Entorno & Requisitos

- **MongoDB** debe estar en ejecución para las pruebas de integración y la funcionalidad de la app. El proyecto usa `spring-boot-starter-data-mongodb` y `SeedService` requiere una instancia activa de MongoDB.
- Perfil: la app lee `application.yml` / `application.properties`. No es necesario un perfil especial para la operación básica, pero `app.seed.enabled` (en `SeedService`) controla si el runner de datos semilla se activa al inicio (por defecto: **desactivado**).
- Si `app.seed.enabled=true`, el `ApplicationStartupRunner` insertará admin + 100+ usuarios + 30 trabajadores + 50 proyectos + 500+ postulaciones + 1000+ calificaciones en MongoDB al iniciar cada vez. Tenerlo desactivado en desarrollo a menos que se quieran datos de prueba.

## Autenticación y Autorización

- **Tokens JWT** son necesarios para la mayoría de las llamadas a la API. El filtro `JwtAuthFilter` valida el header `Authorization: Bearer <token>`.
- Endpoints públicos (sin token necesario): `GET /`, `GET /login`, `GET /registro`, `GET /css/**`, `GET /js/**`, `GET /img/**`, `GET /styles/**`, `GET /error`, `GET /oauth2/**`, `GET /login/oauth2/**`, `GET /completar-registro-oauth2`, `GET /api/auth/**`.
- **Login OAuth2 Google** está configurado. El Client ID/Secret deben establecerse en `application.yml`/`application.properties` bajo `spring.security.oauth2.client.registration.google.*`. Sin credenciales, la página `/login` se mostrará pero el login de Google fallará.
- Control de acceso por roles (en `SecurityConfig.java`):
  - `ROLE_ADMIN` → `/admin/**`
  - `ROLE_CLIENT` → `/perfil-cliente/**`, `/mis-proyectos/**`, `/desboard`
  - `ROLE_CONTRACTOR` → `/desboard-contratista/`, `/perfil-contratista/**`, `/contratista-proyectos/**`
  - `ROLE_WORKER` → `/desboard-trabajador/`, `/perfil-laboral/**`

## Convenios Clave

- **`@EnableCaching`** está en la clase principal. Si agregas anotaciones de caching, asegúrate de tener un bean `CacheManager` configurado o Spring Boot lo hará por auto-configuración (default Guava).
- **`SpaRoutingInterceptor`** reenvía navegaciones `GET` que aceptan HTML a la SPA en `backend/src/main/resources/static/index.html`. Omite API, OAuth, assets y documentos binarios.
- **`ActiveUserInterceptor`** permanece para controles de sesión heredados; React usa JWT para API y no depende de atributos de modelo Thymeleaf.
- **`GlobalControllerAdvice`** (referenciado en los controladores) centraliza el manejo de excepciones. Si agregas nuevos controladores, revisa allí primero antes de añadir try/catch en métodos individuales.
- **`SeedService`** tiene dos modos:
  - Normal: crea admin + 100 usuarios + 30 trabajadores + 50 proyectos + postulaciones + calificaciones (controlado por `app.seed.enabled`).
  - `seed15kData()`: elimina todas las colecciones e inserta 15,000 registros en usuarios, perfiles, proyectos, postulaciones, calificaciones y equipos_trabajo. Usar solo para reset fresco de BD.
- **`mapearRol(String)`** en `UsuarioService.java` mapea nombres de roles en español (`"contratista"` → `ROLE_CONTRACTOR`, `"cliente"` → `ROLE_CLIENT`, `"trabajador"` → `ROLE_WORKER`). Si agregas nuevos roles, actualiza este método.

## Testing

| Comando | Descripción |
|---|---|
| `mvnw -pl backend test` | Ejecuta las pruebas del backend; MongoDB debe estar disponible para los tests de contexto. |
| Casos de prueba | Incluye pruebas de contexto, API, servicios y routing SPA. No se usan Testcontainers; MongoDB debe estar en ejecución para las pruebas de contexto. |
| Datos de prueba | `SeedService.seed15kData()` se puede llamar manualmente (via Spring `ApplicationRunner` o llamada directa al bean) para poblar un dataset grande de prueba. |

## Comandos que Más Usarás

```bash
# Compilar
./mvnw clean package

# Ejecutar la app
./mvnw -pl backend spring-boot:run

# O ejecutar el JAR directamente
java -jar backend/target/obratech-0.0.1-SNAPSHOT.jar

# Ejecutar pruebas (requiere MongoDB)
./mvnw test
```

## Lista Rápida para Evitar Errores

- [ ] Usé `mvnw` y no `mvn` directamente?
- [ ] MongoDB está corriendo antes de ejecutar pruebas?
- [ ] Estado de `app.seed.enabled` revisado (para no saturar la BD de dev con 15k registros)?
- [ ] Token JWT incluido en los headers de la petición API?
- [ ] Credenciales OAuth2 Google configuradas si se usa el flow `/login/oauth2`?
- [ ] Nuevos roles agregados a `mapearRol()` en `UsuarioService.java`?