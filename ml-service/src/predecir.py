"""
Carga los modelos entrenados y predice. Lo usa la API (app/main.py).

    from src.predecir import predecir_retraso, predecir_seleccion

Ambas funciones reciben un dict (el JSON de la petición) y devuelven un dict.
Si los modelos aún no se han entrenado lanzan FileNotFoundError; la API puede
atraparlo y responder con el mock mientras tanto.
"""
from __future__ import annotations

from functools import lru_cache

import joblib

from src.features import (
    MODELOS,
    RETRASO_CAT,
    RETRASO_NUM,
    SELECCION_CAT,
    SELECCION_NUM,
    a_dataframe,
)


@lru_cache(maxsize=None)
def _cargar(nombre_archivo: str) -> dict:
    ruta = MODELOS / nombre_archivo
    if not ruta.exists():
        raise FileNotFoundError(
            f"No existe {ruta}. Entrena primero: python -m src.entrenar_retrasos "
            "y python -m src.entrenar_seleccion"
        )
    return joblib.load(ruta)


def recargar_modelos() -> None:
    """Úsalo después de reentrenar sin reiniciar la API."""
    _cargar.cache_clear()


def modelos_disponibles() -> dict:
    return {
        "retrasos": (MODELOS / "modelo_retrasos.joblib").exists(),
        "seleccion": (MODELOS / "modelo_seleccion.joblib").exists(),
    }


def _nivel(prob: float, bajo: float, alto: float) -> str:
    return "bajo" if prob < bajo else ("medio" if prob < alto else "alto")


# ----------------------------------------------------------------------------
# Modelo 1: retrasos
# ----------------------------------------------------------------------------
def predecir_retraso(datos: dict, campos_por_defecto: list[str] | None = None) -> dict:
    m = _cargar("modelo_retrasos.joblib")
    X = a_dataframe(datos, RETRASO_NUM, RETRASO_CAT)

    prob = float(m["clasificador"].predict_proba(X)[0, 1])
    dias_si_hay = float(max(m["regresor_dias"].predict(X)[0], 0.0))

    pipe_causa = m["clasificador_causa"]
    p_causas = pipe_causa.predict_proba(X)[0]
    causas = sorted(zip(pipe_causa.classes_, p_causas), key=lambda t: t[1], reverse=True)[:3]

    return {
        "retrasoProbable": prob >= 0.5,
        "probabilidad": round(prob, 4),
        "diasRetrasoEstimados": round(prob * dias_si_hay),
        "modelo": "modelo_retrasos",
        "probabilidad_retraso": round(prob, 4),
        "nivel_riesgo": _nivel(prob, 0.35, 0.60),
        "dias_retraso_esperados": round(prob * dias_si_hay, 1),
        "dias_retraso_si_ocurre": round(dias_si_hay, 1),
        "causas_probables": [
            {"causa": str(c), "probabilidad": round(float(p), 4)} for c, p in causas
        ],
        "mock": False,
        "camposPorDefecto": campos_por_defecto or [],
    }


# ----------------------------------------------------------------------------
# Modelo 2: selección del contratista
# ----------------------------------------------------------------------------
# (columna, etiqueta, "mayor" si más alto es mejor / "menor" si más bajo es mejor)
_FACTORES = [
    ("compatibilidad", "Tu compatibilidad con el proyecto", "mayor"),
    ("calificacion", "Tu calificación", "mayor"),
    ("pct_obras_a_tiempo", "Tu porcentaje de obras entregadas a tiempo", "mayor"),
    ("distancia_km", "Tu distancia a la obra (km)", "menor"),
]

_ETIQUETAS_AMIGABLES = {
    "compatibilidad": "Tu compatibilidad con el proyecto",
    "calificacion": "Tu calificación",
    "pct_obras_a_tiempo": "Tus obras entregadas a tiempo",
    "distancia_km": "Tu distancia a la obra",
}


def _formatear_factor(col: str, valor: float) -> str:
    if col in {"compatibilidad", "pct_obras_a_tiempo"}:
        porcentaje = valor if col == "compatibilidad" else valor * 100
        return f"{porcentaje:.1f}%"
    if col == "distancia_km":
        return f"{valor:.1f} km"
    if col == "calificacion":
        return f"{valor:.1f}"
    return f"{valor:.1f}"


def _mensaje_factor(col: str, valor: float, referencia: float) -> str:
    etiqueta = _ETIQUETAS_AMIGABLES[col]
    valor_formateado = _formatear_factor(col, valor)
    referencia_formateada = _formatear_factor(col, referencia)
    if valor > referencia:
        relacion = "por encima del promedio"
    elif valor < referencia:
        relacion = "por debajo del promedio"
    else:
        relacion = "igual al promedio"
    return (
        f"{etiqueta} ({valor_formateado}) está {relacion} "
        f"de los contratistas escogidos ({referencia_formateada})"
    )


def _explicar(datos_df, referencia: dict) -> list[dict]:
    fila = datos_df.iloc[0]
    candidatos = []
    for col, etiqueta, mejor in _FACTORES:
        ref = referencia["media_escogidos"][col]
        desviacion = max(abs(float(referencia["desv_general"].get(col, 1.0))), 1e-9)
        z = (float(fila[col]) - ref) / desviacion  # distancia en desviaciones
        if abs(z) < 0.25:
            impacto = "neutral"
        else:
            favorable = (z > 0) if mejor == "mayor" else (z < 0)
            impacto = "favorable" if favorable else "desfavorable"
        candidatos.append(
            {
                "factor": col,
                "impacto": impacto,
                "mensaje": _mensaje_factor(col, float(fila[col]), ref),
                "_magnitud": abs(z),
            }
        )
    candidatos.sort(key=lambda c: c["_magnitud"], reverse=True)
    for c in candidatos:
        c.pop("_magnitud")
    return candidatos[:3]


def predecir_seleccion(datos: dict, campos_por_defecto: list[str] | None = None) -> dict:
    m = _cargar("modelo_seleccion.joblib")
    X = a_dataframe(datos, SELECCION_NUM, SELECCION_CAT)

    prob = float(m["modelo"].predict_proba(X)[0, 1])
    return {
        "probabilidad": round(prob, 4),
        "recomendacion": "recomendado" if prob >= 0.5 else "evaluar-manualmente",
        "modelo": "modelo_seleccion",
        "probabilidad_seleccion": round(prob, 4),
        "porcentaje": round(prob * 100, 1),
        "nivel": _nivel(prob, 0.15, 0.35),
        "factores": _explicar(X, m["referencia"]),
        "mock": False,
        "camposPorDefecto": campos_por_defecto or [],
    }