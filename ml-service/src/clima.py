"""Climatología histórica de municipios del Atlántico.

La consulta remota es opcional. El generador usa ``--sin-red`` en entornos sin
internet y conserva la misma interfaz para que la fuente pueda cambiarse luego.
"""
from __future__ import annotations

import json
import time
from datetime import date
from pathlib import Path
from typing import Any

import numpy as np
import requests

from src.geo import MUNICIPIOS_COORDENADAS

CLIMATOLOGIA_MENSUAL_MM = {
    1: 5.0, 2: 5.0, 3: 10.0, 4: 45.0, 5: 100.0, 6: 90.0,
    7: 80.0, 8: 120.0, 9: 150.0, 10: 190.0, 11: 130.0, 12: 30.0,
}
DIAS_PROMEDIO_MES = 30.4
UMBRAL_DIA_LLUVIA_MM = 5.0
MAX_DIAS_LLUVIA_ESTIMADOS = 25.0


def lluvia_para_fecha(
    latitud: float | None,
    longitud: float | None,
    fecha_inicio: str | None,
    cache_dir: Path,
    timeout: int = 4,
) -> tuple[float, float, bool]:
    """Obtiene lluvia mensual y días lluviosos; el tercer valor indica fallback."""
    try:
        fecha = date.fromisoformat(str(fecha_inicio))
        mes = fecha.month
    except (TypeError, ValueError):
        return CLIMATOLOGIA_MENSUAL_MM[1], CLIMATOLOGIA_MENSUAL_MM[1] / 12, True

    if latitud is not None and longitud is not None:
        hoy = date.today()
        ultimo_anio = hoy.year - 1
        inicio = date(ultimo_anio - 4, 1, 1).isoformat()
        fin = date(ultimo_anio, 12, 31).isoformat()
        cache_dir.mkdir(parents=True, exist_ok=True)
        clave = f"{float(latitud):.4f}_{float(longitud):.4f}_{inicio}_{fin}".replace("-", "")
        cache = cache_dir / f"prediccion_{clave}.json"
        try:
            if cache.exists():
                datos = json.loads(cache.read_text(encoding="utf-8"))
            else:
                respuesta = consultar_clima(float(latitud), float(longitud), inicio, fin, timeout)
                datos = respuesta.get("daily", {})
                cache.write_text(json.dumps(datos), encoding="utf-8")
            patron_mes = fecha.strftime("%m")
            valores = [
                float(lluvia or 0.0)
                for fecha_texto, lluvia in zip(datos.get("time", []), datos.get("precipitation_sum", []))
                if fecha_texto[5:7] == patron_mes
            ]
            if valores:
                return (
                    float(np.mean(valores) * DIAS_PROMEDIO_MES),
                    float(
                        np.clip(
                            sum(v >= UMBRAL_DIA_LLUVIA_MM for v in valores)
                            / len(valores)
                            * DIAS_PROMEDIO_MES,
                            0,
                            MAX_DIAS_LLUVIA_ESTIMADOS,
                        )
                    ),
                    False,
                )
        except (OSError, requests.RequestException, TypeError, ValueError, KeyError):
            pass
    base = CLIMATOLOGIA_MENSUAL_MM[mes]
    return base, float(np.clip(max(0.0, base / 12), 0, MAX_DIAS_LLUVIA_ESTIMADOS)), True


def _cache_path(cache_dir: Path, municipio: str) -> Path:
    return cache_dir / f"{municipio.lower().replace(' ', '_').replace('ó', 'o')}.json"


def consultar_clima(
    latitud: float,
    longitud: float,
    fecha_inicio: str = "2019-01-01",
    fecha_fin: str = "2026-12-31",
    timeout: int = 20,
) -> dict[str, Any]:
    """Consulta Open-Meteo una vez para el rango completo solicitado."""
    response = requests.get(
        "https://archive-api.open-meteo.com/v1/archive",
        params={
            "latitude": latitud,
            "longitude": longitud,
            "start_date": fecha_inicio,
            "end_date": fecha_fin,
            "daily": "precipitation_sum",
            "timezone": "America/Bogota",
        },
        timeout=timeout,
    )
    response.raise_for_status()
    return response.json()


def cargar_climatologia(
    municipio: str,
    rng: np.random.Generator,
    cache_dir: Path,
    sin_red: bool = False,
) -> tuple[dict[int, tuple[float, float]], str]:
    """Devuelve climatología mensual y su fuente, usando caché o fallback."""
    cache_dir.mkdir(parents=True, exist_ok=True)
    cache = _cache_path(cache_dir, municipio)
    if not sin_red and cache.exists():
        try:
            data = json.loads(cache.read_text(encoding="utf-8"))
            fuente = data.get("fuente")
            if fuente in {"open-meteo", "respaldo"}:
                return {int(k): tuple(v) for k, v in data["mensual"].items()}, fuente
            cache.unlink(missing_ok=True)
        except (OSError, KeyError, TypeError, ValueError):
            cache.unlink(missing_ok=True)

    if not sin_red:
        try:
            lat, lon = MUNICIPIOS_COORDENADAS[municipio]
            ultimo_anio = date.today().year - 1
            remoto = consultar_clima(
                lat,
                lon,
                date(ultimo_anio - 4, 1, 1).isoformat(),
                date(ultimo_anio, 12, 31).isoformat(),
            )
            fechas = remoto.get("daily", {}).get("time", [])
            lluvias = remoto.get("daily", {}).get("precipitation_sum", [])
            mensual: dict[int, tuple[float, float]] = {}
            for mes in range(1, 13):
                valores = [
                    float(lluvia or 0.0) for fecha, lluvia in zip(fechas, lluvias)
                    if int(fecha[5:7]) == mes
                ]
                mensual[mes] = (
                    float(np.mean(valores) * 30.4) if valores else CLIMATOLOGIA_MENSUAL_MM[mes],
                    float(
                        np.clip(
                            sum(valor >= UMBRAL_DIA_LLUVIA_MM for valor in valores)
                            / max(1, len(valores))
                            * DIAS_PROMEDIO_MES,
                            0,
                            MAX_DIAS_LLUVIA_ESTIMADOS,
                        )
                    ),
                )
            cache.write_text(
                json.dumps(
                    {"municipio": municipio, "mensual": mensual, "fuente": "open-meteo"},
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            time.sleep(0.25)
            return mensual, "open-meteo"
        except (KeyError, OSError, requests.RequestException, ValueError):
            pass

    mensual = {}
    for mes, base in CLIMATOLOGIA_MENSUAL_MM.items():
        factor = float(rng.uniform(0.85, 1.15))
        mensual[mes] = (
            round(base * factor, 2),
            round(float(np.clip(max(0.0, base / 12 * factor), 0, MAX_DIAS_LLUVIA_ESTIMADOS)), 2),
        )
    cache.write_text(
        json.dumps({"municipio": municipio, "mensual": mensual, "fuente": "respaldo"}, ensure_ascii=False),
        encoding="utf-8",
    )
    return mensual, "respaldo"
