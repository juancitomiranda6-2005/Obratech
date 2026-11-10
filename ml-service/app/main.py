from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.schemas import (
    PrediccionRetrasoRequest,
    PrediccionRetrasoResponse,
    PrediccionSeleccionRequest,
    PrediccionSeleccionResponse,
)
from src.predecir import modelos_disponibles, predecir_retraso, predecir_seleccion

app = FastAPI(title="ObraTech ML Service", version="0.1.0")


@app.exception_handler(FileNotFoundError)
async def modelo_no_disponible(_: Request, exception: FileNotFoundError) -> JSONResponse:
    return JSONResponse(status_code=503, content={"detail": str(exception)})


@app.exception_handler(ValueError)
async def entrada_no_valida(_: Request, exception: ValueError) -> JSONResponse:
    return JSONResponse(status_code=422, content={"detail": str(exception)})


@app.get("/health")
def health() -> dict[str, str]:
    # TODO: Añadir versión de cada modelo y estado de Open-Meteo.
    disponibles = modelos_disponibles()
    return {
        "status": "ok" if all(disponibles.values()) else "degraded",
        "modelos_retrasos": str(disponibles["retrasos"]).lower(),
        "modelos_seleccion": str(disponibles["seleccion"]).lower(),
    }


@app.post("/predecir-retraso", response_model=PrediccionRetrasoResponse)
def endpoint_retraso(request: PrediccionRetrasoRequest) -> dict:
    # TODO: Registrar métricas y trazabilidad de cada predicción.
    datos, campos_por_defecto = request.to_model_input()
    return predecir_retraso(datos, campos_por_defecto)


@app.post("/predecir-seleccion", response_model=PrediccionSeleccionResponse)
def endpoint_seleccion(request: PrediccionSeleccionRequest) -> dict:
    # TODO: Registrar métricas y trazabilidad de cada predicción.
    datos, campos_por_defecto = request.to_model_input()
    return predecir_seleccion(datos, campos_por_defecto)
