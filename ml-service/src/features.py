"""
Variables (features) y utilidades compartidas por el entrenamiento y la predicción.

Si cambias una columna aquí, hay que reentrenar los modelos
(python -m src.entrenar_retrasos y python -m src.entrenar_seleccion).
"""
from __future__ import annotations

from pathlib import Path

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder, StandardScaler

# ----------------------------------------------------------------------------
# Rutas (relativas a la carpeta ml-service)
# ----------------------------------------------------------------------------
RAIZ = Path(__file__).resolve().parent.parent
DATOS_SINTETICOS = RAIZ / "data" / "synthetic"
DATOS_REALES = RAIZ / "data" / "processed"
MODELOS = RAIZ / "models"

# ----------------------------------------------------------------------------
# Modelo 1: retrasos de obra  (archivo: obras.csv)
# ----------------------------------------------------------------------------
RETRASO_NUM = [
    "presupuesto_millones_cop",
    "duracion_planificada_dias",
    "num_trabajadores",
    "mes_inicio",
    "lluvia_promedio_mm",
    "dias_lluvia_estimados",
    "permisos_complejos",
    "experiencia_contratista_anios",
    "pct_obras_a_tiempo_contratista",
    "obras_simultaneas_contratista",
]
RETRASO_CAT = ["tipo_obra", "municipio"]

# Variables objetivo (target) del modelo 1
TARGET_HUBO_RETRASO = "hubo_retraso"
TARGET_DIAS_RETRASO = "dias_retraso"
TARGET_CAUSA = "causa_principal_retraso"

# ----------------------------------------------------------------------------
# Modelo 2: probabilidad de que escojan al contratista  (archivo: postulaciones.csv)
# ----------------------------------------------------------------------------
SELECCION_NUM = [
    "compatibilidad",
    "experiencia_anios",
    "calificacion",
    "pct_obras_a_tiempo",
    "obras_completadas",
    "distancia_km",
    "num_postulantes",
    "presupuesto_millones_cop",
]
SELECCION_CAT = ["tipo_obra", "municipio_proyecto"]
TARGET_ESCOGIDO = "fue_escogido"
COLUMNA_GRUPO_SELECCION = "proyecto_id"  # para no mezclar un proyecto entre train y test


# ----------------------------------------------------------------------------
# Carga de datos
# ----------------------------------------------------------------------------
def _ruta_datos(nombre: str, usar_reales: bool) -> Path:
    carpeta = DATOS_REALES if usar_reales else DATOS_SINTETICOS
    ruta = carpeta / nombre
    if not ruta.exists():
        raise FileNotFoundError(
            f"No existe {ruta}. Genera los datos con: python -m src.generar_datos_sinteticos"
        )
    return ruta


def cargar_obras(usar_reales: bool = False) -> pd.DataFrame:
    return pd.read_csv(_ruta_datos("obras.csv", usar_reales))


def cargar_postulaciones(usar_reales: bool = False) -> pd.DataFrame:
    return pd.read_csv(_ruta_datos("postulaciones.csv", usar_reales))


# ----------------------------------------------------------------------------
# Preprocesamiento
# ----------------------------------------------------------------------------
def preprocesador(numericas: list[str], categoricas: list[str], escalar: bool = False) -> ColumnTransformer:
    """Convierte las categorías en columnas 0/1 (y opcionalmente escala los números)."""
    parte_num = StandardScaler() if escalar else "passthrough"
    return ColumnTransformer(
        [
            ("num", parte_num, numericas),
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), categoricas),
        ]
    )


def a_dataframe(datos: dict, numericas: list[str], categoricas: list[str]) -> pd.DataFrame:
    """
    Convierte el JSON que llega de la API en el DataFrame que espera el modelo.
    - Si falta 'mes_inicio' pero viene 'fecha_inicio' (YYYY-MM-DD), se deduce el mes.
    - Si falta alguna columna obligatoria, lanza ValueError indicando cuáles.
    """
    datos = dict(datos)
    if "mes_inicio" not in datos and datos.get("fecha_inicio"):
        datos["mes_inicio"] = pd.to_datetime(datos["fecha_inicio"]).month

    columnas = numericas + categoricas
    faltantes = [c for c in columnas if datos.get(c) is None]
    if faltantes:
        raise ValueError(f"Faltan campos obligatorios: {', '.join(faltantes)}")

    fila = {c: datos[c] for c in columnas}
    df = pd.DataFrame([fila])
    df[numericas] = df[numericas].astype(float)
    return df