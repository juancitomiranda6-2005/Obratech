from pathlib import Path

import pandas as pd

from src.features import (
    RETRASO_CAT,
    RETRASO_NUM,
    SELECCION_CAT,
    SELECCION_NUM,
    TARGET_CAUSA,
    TARGET_DIAS_RETRASO,
    TARGET_ESCOGIDO,
    TARGET_HUBO_RETRASO,
)
from src.generar_dataset_semireal import generar_contratistas
import numpy as np

BASE = Path(__file__).resolve().parents[1] / "data" / "processed"


def _cargar(nombre: str) -> pd.DataFrame:
    return pd.read_csv(BASE / nombre)


def test_columnas_y_sin_nulos() -> None:
    obras = _cargar("obras.csv")
    postulaciones = _cargar("postulaciones.csv")
    assert set(RETRASO_NUM + RETRASO_CAT + [TARGET_HUBO_RETRASO, TARGET_DIAS_RETRASO, TARGET_CAUSA]) <= set(obras)
    assert set(SELECCION_NUM + SELECCION_CAT + [TARGET_ESCOGIDO, "proyecto_id"]) <= set(postulaciones)
    assert not obras.isna().any().any()
    assert not postulaciones.isna().any().any()


def test_fechas_y_un_ganador() -> None:
    obras = _cargar("obras.csv")
    postulaciones = _cargar("postulaciones.csv")
    inicio = pd.to_datetime(obras["fecha_inicio"])
    planeada = pd.to_datetime(obras["fecha_fin_planeada"])
    real = pd.to_datetime(obras["fecha_fin_real"])
    assert (real >= planeada).all()
    assert ((real - planeada).dt.days == obras["dias_retraso"]).all()
    assert (postulaciones.groupby("proyecto_id")[TARGET_ESCOGIDO].sum() == 1).all()
    assert inicio.is_monotonic_increasing


def test_reproducibilidad() -> None:
    obras = _cargar("obras.csv")
    postulaciones = _cargar("postulaciones.csv")
    assert len(obras) > 0 and len(postulaciones) > 0
    assert obras["obra_id"].is_unique
    assert postulaciones["postulacion_id"].is_unique
    primero = generar_contratistas(np.random.default_rng(42), 20)
    segundo = generar_contratistas(np.random.default_rng(42), 20)
    pd.testing.assert_frame_equal(primero, segundo)
