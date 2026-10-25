# Datos de demostración

ObraTech puede cargar un conjunto pequeño de datos ficticios para revisar los
perfiles, proyectos asignados, equipos, reportes y el flujo de postulaciones.
No representa personas, empresas, obras, pagos ni calificaciones reales.
Los correos usan el dominio reservado `.test` y no reciben mensajes.

La carga está desactivada por defecto y no borra colecciones. Para activarla en
una terminal local de PowerShell, define `APP_SEED_ENABLED=true` y
`APP_SEED_PASSWORD` con una clave local fuerte antes de iniciar el backend:

```powershell
$env:APP_SEED_ENABLED = "true"
# Define APP_SEED_PASSWORD localmente; no la guardes en el repositorio.
.\mvnw.cmd -pl backend spring-boot:run
```

Todas las cuentas de demostración usan la clave que definas en
`APP_SEED_PASSWORD`. Cuentas creadas:

- Clientes: `camila.herrera@obratech.test`,
  `nicolas.pardo@obratech.test`
- Contratistas: `julian.restrepo@obratech.test`,
  `valentina.gomez@obratech.test`
- Trabajadores: `mateo.giraldo@obratech.test`,
  `laura.castano@obratech.test`, `david.ospina@obratech.test`,
  `karen.velez@obratech.test`

La carga es idempotente para sus identificadores y no modifica perfiles que ya
existan. No se generan registros de pagos ni calificaciones. No habilites esta
opción en producción.
