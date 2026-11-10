# ObraTech ML Service

Servicio FastAPI para los modelos predictivos de retrasos y selección de contratistas.

<!-- TODO: Documentar el contrato definitivo, versionado de modelos y despliegue. -->

## Desarrollo local

```powershell
cd ml-service
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## Dataset semirreal reproducible

Los datos son inventados y no deben presentarse como datos reales.

```powershell
cd ml-service
.\.venv\Scripts\Activate.ps1
python -m src.generar_dataset_semireal --contratistas 400 --obras 4000 --proyectos 2000 --seed 42 --sin-red
python -m src.exportar_mongo
```

El exportador solo crea JSON compatibles con `mongoimport --jsonArray`; nunca
inserta datos automáticamente. Las variables que no existen directamente en
las entidades Java quedan listadas en `data/processed/mongo/`.

## Entrenamiento

```powershell
python -m src.entrenar_retrasos --reales
python -m src.entrenar_seleccion --reales
python -m pytest -q
```

Para entrenar con el dataset semirreal usa `--reales` en ambos comandos.

El modelo de selección no usa precio ofertado. La distancia puede calcularse
con `ciudadContratista` y las coordenadas del proyecto; lluvia e historial
faltantes se completan con valores derivados o neutros y se informan en
`camposPorDefecto`.

## Prueba rápida

```powershell
curl.exe http://localhost:8000/health
curl.exe -X POST http://localhost:8000/predecir-retraso -H "Content-Type: application/json" -d "{\"obra\":\"obra-demo\"}"
```
