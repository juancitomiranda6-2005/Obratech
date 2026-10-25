# ObraTech: desarrollo del frontend

## Para editar React y ver los cambios al guardar

Desde la carpeta principal del repositorio, ejecuta `run.bat`. Este inicia el
backend Spring Boot y Vite en dos ventanas y abre la aplicación en
`http://localhost:5173`.

Edita archivos dentro de `frontend/src` y guárdalos. Vite usa HMR (Hot Module
Replacement) para reflejar los cambios en el navegador sin crear un JAR ni
reconstruir el frontend. Si el navegador no actualiza un cambio de estilos,
recarga la página con `Ctrl+F5`.

El frontend envía las solicitudes `/api` al backend en `http://localhost:9999`.
MongoDB debe estar activo para las funciones de la aplicación que leen o guardan
datos.

`run.bat` necesita Node.js, las dependencias existentes en
`frontend/node_modules` y Java/Maven Wrapper para el backend. Si las
dependencias de Node todavía no están instaladas, abre una terminal en
`frontend`, ejecuta `npm install` una vez y vuelve a iniciar `run.bat`. El
script no instala paquetes automáticamente.

## Versión empaquetada

`run-production.bat` ejecuta la aplicación servida por Spring Boot desde el
JAR. Esa versión usa los recursos compilados: después de modificar el
frontend, hay que generar el bundle con `npm run build` antes de empaquetar o
ejecutar esa versión. Para aprender y trabajar con cambios inmediatos, usa
`run.bat`.
