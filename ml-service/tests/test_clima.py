from pathlib import Path

import pytest
import numpy as np

from src import clima


def test_lluvia_para_fecha_agrega_el_mes_completo(monkeypatch, tmp_path) -> None:
    datos = {
        "daily": {
            "time": ["2021-02-01", "2021-02-02", "2021-10-01", "2021-10-02"],
            "precipitation_sum": [0.2, 0.4, 6.0, 8.0],
        }
    }

    monkeypatch.setattr(clima, "consultar_clima", lambda *args, **kwargs: datos)

    febrero = clima.lluvia_para_fecha(
        10.9685, -74.7813, "2026-02-01", tmp_path
    )
    octubre = clima.lluvia_para_fecha(
        10.9685, -74.7813, "2026-10-01", tmp_path
    )

    assert febrero == pytest.approx((9.12, 0.0, False))
    assert octubre == pytest.approx((212.8, 25.0, False))


def test_lluvia_para_fecha_aplica_umbral_y_limite(monkeypatch, tmp_path) -> None:
    datos = {
        "daily": {
            "time": [f"2021-10-{dia:02d}" for dia in range(1, 31)],
            "precipitation_sum": [6.0] * 30,
        }
    }
    monkeypatch.setattr(clima, "consultar_clima", lambda *args, **kwargs: datos)

    _, dias, respaldo = clima.lluvia_para_fecha(
        10.9685, -74.7813, "2026-10-01", tmp_path
    )

    assert respaldo is False
    assert dias == clima.MAX_DIAS_LLUVIA_ESTIMADOS
    assert clima.UMBRAL_DIA_LLUVIA_MM == 5.0


def test_cargar_climatologia_registra_fuente(monkeypatch, tmp_path) -> None:
    monkeypatch.setattr(
        clima,
        "consultar_clima",
        lambda *args, **kwargs: {
            "daily": {
                "time": ["2021-10-01"],
                "precipitation_sum": [6.0],
            }
        },
    )

    mensual, fuente = clima.cargar_climatologia(
        "Barranquilla", np.random.default_rng(42), tmp_path
    )

    assert fuente == "open-meteo"
    assert mensual[10][0] == pytest.approx(182.4)
