"""Coordenadas y cálculos geográficos compartidos por generación y predicción."""
from __future__ import annotations

import math
import re
import unicodedata

MUNICIPIOS_COORDENADAS = {
    "Barranquilla": (10.9685, -74.7813), "Soledad": (10.9184, -74.7646),
    "Malambo": (10.8595, -74.7739), "Puerto Colombia": (11.0136, -74.8860),
    "Sabanalarga": (10.6378, -74.9200), "Baranoa": (10.7947, -74.9160),
    "Galapa": (10.8969, -74.8860), "Sabanagrande": (10.7917, -74.7556),
    "Santo Tomás": (10.7578, -74.7544), "Juan de Acosta": (10.8293, -75.0347),
    "Tubará": (10.8744, -74.9781), "Luruaco": (10.6086, -75.1511),
    "Repelón": (10.4930, -75.1286), "Campo de la Cruz": (10.3789, -74.8839),
    "Candelaria": (10.4594, -74.8797), "Manatí": (10.4453, -74.9581),
    "Palmar de Varela": (10.7406, -74.7547), "Polonuevo": (10.7761, -74.8539),
    "Ponedera": (10.6428, -74.7533), "Santa Lucía": (10.3247, -74.9608),
    "Suan": (10.3331, -74.8803), "Usiacurí": (10.7422, -75.0106),
    "Piojó": (10.7492, -75.1078),
}


def normalizar_municipio(valor: str | None) -> str:
    texto = unicodedata.normalize("NFKD", str(valor or ""))
    texto = "".join(c for c in texto if not unicodedata.combining(c))
    return re.sub(r"\s+", " ", texto.lower().strip())


def coordenadas_municipio(valor: str | None) -> tuple[float, float] | None:
    buscado = normalizar_municipio(valor)
    for municipio, coordenadas in MUNICIPIOS_COORDENADAS.items():
        if normalizar_municipio(municipio) == buscado:
            return coordenadas
    return None


def haversine(
    latitud_a: float, longitud_a: float, latitud_b: float, longitud_b: float
) -> float:
    radio = 6371.0
    phi_a, phi_b = math.radians(latitud_a), math.radians(latitud_b)
    dphi = math.radians(latitud_b - latitud_a)
    dlambda = math.radians(longitud_b - longitud_a)
    valor = (
        math.sin(dphi / 2) ** 2
        + math.cos(phi_a) * math.cos(phi_b) * math.sin(dlambda / 2) ** 2
    )
    return radio * 2 * math.atan2(math.sqrt(valor), math.sqrt(1 - valor))


def distancia_municipio_punto(
    municipio: str | None, latitud: float | None, longitud: float | None
) -> float | None:
    coordenadas = coordenadas_municipio(municipio)
    if coordenadas is None or latitud is None or longitud is None:
        return None
    return haversine(coordenadas[0], coordenadas[1], float(latitud), float(longitud))
