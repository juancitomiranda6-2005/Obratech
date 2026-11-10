"""Genera un dataset semirreal, coherente y reproducible para ObraTech.

No escribe en MongoDB. Los CSV resultantes se pueden entrenar con:
``python -m src.entrenar_retrasos --reales`` y
``python -m src.entrenar_seleccion --reales``.
"""
from __future__ import annotations

import argparse
import math
import re
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

from src.clima import MAX_DIAS_LLUVIA_ESTIMADOS, cargar_climatologia
from src.config import PROCESSED_DATA_DIR, RAW_DATA_DIR
from src.geo import MUNICIPIOS_COORDENADAS, haversine

TIPOS_OBRA = ["vivienda", "vial", "acueducto", "edificio", "remodelacion", "espacio_publico"]
DEPARTAMENTO = "Atlántico"
CAUSAS = ["lluvia", "materiales", "permisos", "mano_de_obra", "financiacion", "diseno", "ninguna"]
TAMAÑOS = ["pequeño", "mediano", "grande"]
CAPACIDAD_MILLONES = {"pequeño": 1200.0, "mediano": 6000.0, "grande": 25000.0}
OBRAS_SIMULTANEAS_MAX = {"pequeño": 2, "mediano": 4, "grande": 8}
SENSIBILIDAD_LLUVIA = {
    "vivienda": 0.8, "vial": 1.4, "acueducto": 1.2,
    "edificio": 0.7, "remodelacion": 0.3, "espacio_publico": 1.1,
}
PREFIJOS = ["Constructora", "Ingeniería y Obras", "Proyectos", "Soluciones", "Edificaciones"]
SUFIJOS = ["Caribe Sur S.A.S.", "del Atlántico Ltda.", "Costa Norte S.A.S.", "Mar Abierto S.A.S."]
NOMBRES = ["Alborada", "Brisas", "Horizonte", "Mangle", "Ceiba", "Ribera", "Terracota", "Viento"]
APELLIDOS = ["Pardo", "Mendoza", "Salcedo", "Cárdenas", "Varela", "Pineda", "Acosta", "Bermúdez"]


def _fecha_aleatoria(rng: np.random.Generator) -> date:
    inicio = date(2019, 1, 1)
    fin = date(2026, 10, 31)
    return inicio + timedelta(days=int(rng.integers(0, (fin - inicio).days + 1)))


def _nit_inventado(indice: int) -> str:
    base = 900_000_000 + indice
    pesos = [71, 67, 59, 53, 47, 43, 41, 37, 29, 23, 19, 17, 13, 7, 3]
    digitos = [int(d) for d in str(base)]
    total = sum(digito * peso for digito, peso in zip(reversed(digitos), pesos))
    resto = total % 11
    verificador = 0 if resto in (0, 1) else 11 - resto
    return f"{base}-{verificador}"


def _normalizar(texto: str) -> str:
    return re.sub(r"\s+", " ", texto.lower().strip())


def _haversine(municipio_a: str, municipio_b: str) -> float:
    return haversine(*MUNICIPIOS_COORDENADAS[municipio_a], *MUNICIPIOS_COORDENADAS[municipio_b])


def _nombre_contratista(rng: np.random.Generator, indice: int) -> tuple[str, str]:
    if indice % 3:
        return (
            f"{rng.choice(PREFIJOS)} {rng.choice(NOMBRES)} {rng.choice(SUFIJOS)}",
            "empresa",
        )
    return f"{rng.choice(NOMBRES)} {rng.choice(APELLIDOS)}", "persona_natural"


def generar_contratistas(rng: np.random.Generator, cantidad: int) -> pd.DataFrame:
    filas = []
    municipios = list(MUNICIPIOS_COORDENADAS)
    for indice in range(1, cantidad + 1):
        nombre, tipo = _nombre_contratista(rng, indice)
        experiencia = int(rng.integers(1, 25))
        municipio = str(rng.choice(municipios))
        calificacion = float(np.clip(3.2 + experiencia * 0.045 + rng.normal(0, 0.25), 1, 5))
        filas.append({
            "contratista_id": f"contratista-{indice:05d}",
            "nombre": nombre,
            "tipo_persona": tipo,
            "nit": _nit_inventado(indice),
            "municipio_base": municipio,
            "ciudad": municipio,
            "departamento": DEPARTAMENTO,
            "especialidad": str(rng.choice(TIPOS_OBRA)),
            "subespecialidades": str(rng.choice(TIPOS_OBRA)),
            "anio_fundacion": 2026 - experiencia,
            "experiencia_anios": experiencia,
            "calificacion_promedio": round(calificacion, 3),
            "tamano": str(rng.choice(TAMAÑOS, p=[0.48, 0.38, 0.14])),
        })
    return pd.DataFrame(filas)


def _historial(estado: dict, contratista_id: str) -> dict[str, float]:
    historial = estado[contratista_id]["historial"]
    if not historial:
        return {
            "experiencia_anios": estado[contratista_id]["experiencia_anios"],
            "pct_obras_a_tiempo": 0.70,
            "calificacion": 3.8,
            "obras_previas": 0,
            "presupuesto_medio": 0.0,
        }
    return {
        "experiencia_anios": estado[contratista_id]["experiencia_anios"],
        "pct_obras_a_tiempo": sum(not h["retraso"] for h in historial) / len(historial),
        "calificacion": float(np.mean([h["calificacion"] for h in historial])),
        "obras_previas": len(historial),
        "presupuesto_medio": float(np.mean([h["presupuesto"] for h in historial])),
    }


def _compatibilidad(
    tipo_obra: str,
    municipio: str,
    presupuesto: float,
    contratista: pd.Series,
    historial: dict[str, float],
) -> float:
    """Replica ProjectFitSimulationService: 0.50 técnico + 0.25 presupuesto + 0.25 ubicación."""
    especialidades = _normalizar(f"{contratista.especialidad} {contratista.subespecialidades}")
    tipo = _normalizar(tipo_obra)
    experiencia = min(1.0, historial["experiencia_anios"] / 10.0)
    tecnico = min(1.0, (0.70 if tipo in especialidades or any(
        token in especialidades for token in tipo.split() if len(token) > 3
    ) else 0.35) + experiencia * 0.30)
    presupuesto_medio = historial["presupuesto_medio"]
    presupuesto_fit = 0.50 if presupuesto_medio <= 0 else min(
        1.0, math.exp(-abs(math.log((presupuesto + 1) / (presupuesto_medio + 1))))
    )
    ubicacion = 1.0 if municipio == contratista.municipio_base else 0.75
    return float(np.clip(tecnico * 0.50 + presupuesto_fit * 0.25 + ubicacion * 0.25, 0, 1) * 100)


def generar_obras(
    rng: np.random.Generator,
    contratistas: pd.DataFrame,
    climatologia: dict[str, tuple[dict[int, tuple[float, float]], str]],
    cantidad: int,
) -> tuple[pd.DataFrame, dict[str, dict]]:
    estado = {
        row.contratista_id: {
            "experiencia_anios": int(row.experiencia_anios),
            "tamano": row.tamano,
            "historial": [],
            "activas": [],
        }
        for row in contratistas.itertuples()
    }
    municipios = list(MUNICIPIOS_COORDENADAS)
    obras = []
    fechas = sorted(_fecha_aleatoria(rng) for _ in range(cantidad))
    for indice in range(1, cantidad + 1):
        fecha_inicio = fechas[indice - 1]
        tipo = str(rng.choice(TIPOS_OBRA))
        municipio = str(rng.choice(municipios))
        presupuesto = float(np.clip(rng.lognormal(6.0, 0.85), 30, 18000))
        candidatos = contratistas[
            (contratistas.especialidad == tipo)
            & (contratistas.tamano.map(CAPACIDAD_MILLONES) >= presupuesto)
        ]
        if len(candidatos) < 1:
            candidatos = contratistas[contratistas.tamano.map(CAPACIDAD_MILLONES) >= presupuesto]
        candidato = candidatos.sample(1, random_state=int(rng.integers(1_000_000_000))).iloc[0]
        contratista_id = candidato.contratista_id
        h = _historial(estado, contratista_id)
        duracion = int(np.clip(presupuesto ** 0.43 * rng.uniform(7, 13), 30, 720))
        fin_plan = fecha_inicio + timedelta(days=duracion)
        clima_mensual, fuente_clima = climatologia[municipio]
        clima = clima_mensual[fecha_inicio.month]
        lluvia = float(max(0, clima[0] * rng.uniform(0.75, 1.25)))
        dias_lluvia = float(np.clip(clima[1] * rng.uniform(0.75, 1.25), 0, MAX_DIAS_LLUVIA_ESTIMADOS))
        permisos = int(rng.random() < (0.20 + 0.10 * (tipo in {"edificio", "acueducto"})))
        obras_simultaneas = len([
            f for f in estado[contratista_id]["activas"]
            if f >= fecha_inicio
        ]) + 1
        ruido = float(rng.normal(0, 0.55))
        z = (
            -1.35 + 0.010 * lluvia * SENSIBILIDAD_LLUVIA[tipo]
            + 0.0015 * duracion + 0.18 * (obras_simultaneas - 1)
            - 1.8 * (h["pct_obras_a_tiempo"] - 0.60) - 0.025 * h["experiencia_anios"]
            + 0.55 * permisos + ruido
        )
        prob = 1 / (1 + math.exp(-z))
        hubo_retraso = bool(rng.random() < prob)
        causa = "ninguna"
        retraso = 0
        if hubo_retraso:
            pesos = np.array([
                0.08 + lluvia / 450 * SENSIBILIDAD_LLUVIA[tipo],
                0.23 + max(0, presupuesto - 4000) / 30000,
                0.10 + permisos * 0.45,
                0.10 + max(0, obras_simultaneas - 1) * 0.08,
                0.10 + max(0, presupuesto - 7000) / 40000,
                0.10,
            ])
            causa = str(rng.choice(CAUSAS[:-1], p=pesos / pesos.sum()))
            retraso = int(max(1, duracion * prob * rng.uniform(0.08, 0.42) + rng.normal(0, 4)))
        fin_real = fin_plan + timedelta(days=retraso)
        registro = {
            "obra_id": f"obra-{indice:06d}", "titulo": f"Obra {tipo.replace('_', ' ')} {indice:05d}",
            "tipo_obra": tipo, "municipio": municipio, "departamento": DEPARTAMENTO,
            "presupuesto_millones_cop": round(presupuesto, 2),
            "duracion_planificada_dias": duracion, "num_trabajadores": int(np.clip(presupuesto ** 0.4 * rng.uniform(2, 5), 3, 300)),
            "fecha_inicio": fecha_inicio.isoformat(), "fecha_fin_planeada": fin_plan.isoformat(),
            "fecha_fin_real": fin_real.isoformat(), "mes_inicio": fecha_inicio.month,
            "lluvia_promedio_mm": round(lluvia, 2), "dias_lluvia_estimados": round(dias_lluvia, 2),
            "fuente_clima": fuente_clima,
            "permisos_complejos": permisos, "contratista_id": contratista_id,
            "experiencia_contratista_anios": h["experiencia_anios"],
            "pct_obras_a_tiempo_contratista": round(h["pct_obras_a_tiempo"], 4),
            "obras_simultaneas_contratista": obras_simultaneas, "obras_previas": h["obras_previas"],
            "hubo_retraso": int((fin_real - fin_plan).days > 0),
            "dias_retraso": (fin_real - fin_plan).days, "causa_principal_retraso": causa,
            "calificacion_contratista": round(float(np.clip(rng.normal(4.0, 0.55), 1, 5)), 3),
            "latitud": MUNICIPIOS_COORDENADAS[municipio][0], "longitud": MUNICIPIOS_COORDENADAS[municipio][1],
        }
        obras.append(registro)
        estado[contratista_id]["historial"].append({
            "retraso": registro["hubo_retraso"],
            "calificacion": registro["calificacion_contratista"],
            "presupuesto": presupuesto, "fecha_inicio": fecha_inicio,
        })
        estado[contratista_id]["activas"].append(fin_plan)
    return pd.DataFrame(obras).sort_values("fecha_inicio").reset_index(drop=True), estado


def generar_postulaciones(
    rng: np.random.Generator,
    obras: pd.DataFrame,
    contratistas: pd.DataFrame,
    estado: dict[str, dict],
    cantidad_proyectos: int,
) -> pd.DataFrame:
    filas = []
    seleccionadas = obras.head(cantidad_proyectos).copy()
    for obra in seleccionadas.itertuples():
        candidatos = contratistas.sample(
            min(len(contratistas), int(rng.integers(2, 13))),
            random_state=int(rng.integers(1_000_000_000)),
        )
        registros, precios_ocultos = [], []
        for candidato in candidatos.itertuples():
            historial = [
                registro for registro in estado[candidato.contratista_id]["historial"]
                if registro["fecha_inicio"] < date.fromisoformat(obra.fecha_inicio)
            ]
            h = {
                "experiencia_anios": int(candidato.experiencia_anios),
                "pct_obras_a_tiempo": (
                    sum(not registro["retraso"] for registro in historial) / len(historial)
                    if historial else 0.70
                ),
                "calificacion": (
                    float(np.mean([registro["calificacion"] for registro in historial]))
                    if historial else 3.8
                ),
                "obras_previas": len(historial),
                "presupuesto_medio": (
                    float(np.mean([registro["presupuesto"] for registro in historial]))
                    if historial else 0.0
                ),
            }
            compat = _compatibilidad(obra.tipo_obra, obra.municipio, obra.presupuesto_millones_cop, candidato, h)
            ratio = float(np.clip(rng.lognormal(math.log(0.98), 0.13), 0.55, 1.45))
            distancia = _haversine(candidato.municipio_base, obra.municipio)
            registros.append({
                "proyecto_id": obra.obra_id, "obra_id": obra.obra_id,
                "tipo_obra": obra.tipo_obra, "municipio_proyecto": obra.municipio,
                "presupuesto_millones_cop": obra.presupuesto_millones_cop,
                "contratista_id": candidato.contratista_id, "compatibilidad": round(compat, 3),
                "experiencia_anios": h["experiencia_anios"], "calificacion": round(h["calificacion"], 3),
                "pct_obras_a_tiempo": round(h["pct_obras_a_tiempo"], 4),
                "obras_completadas": h["obras_previas"], "distancia_km": round(distancia, 3),
                "num_postulantes": len(candidatos), "fecha_postulacion": obra.fecha_inicio,
            })
            precios_ocultos.append(ratio)
        scores = np.array([
            0.45 * r["compatibilidad"] / 100 - 0.22 * abs(precio - 0.92)
            + 0.15 * r["calificacion"] / 5 + 0.12 * r["pct_obras_a_tiempo"]
            - 0.0007 * r["distancia_km"] + rng.normal(0, 0.08)
            for r, precio in zip(registros, precios_ocultos)
        ])
        ganador = int(np.argmax(scores))
        for indice, registro in enumerate(registros):
            registro["postulacion_id"] = f"postulacion-{len(filas) + 1:07d}"
            registro["fue_escogido"] = int(indice == ganador)
            filas.append(registro)
    return pd.DataFrame(filas)


def _validar(df_obras: pd.DataFrame, df_postulaciones: pd.DataFrame) -> None:
    if df_obras.isna().any().any() or df_postulaciones.isna().any().any():
        raise ValueError("El dataset contiene valores nulos")
    if not (df_obras["fecha_fin_real"] >= df_obras["fecha_fin_planeada"]).all():
        raise ValueError("Hay fechas reales anteriores a las fechas planeadas")
    derivados = (
        pd.to_datetime(df_obras["fecha_fin_real"]) - pd.to_datetime(df_obras["fecha_fin_planeada"])
    ).dt.days
    if not (derivados == df_obras["dias_retraso"]).all():
        raise ValueError("dias_retraso no coincide con las fechas")
    ganadores = df_postulaciones.groupby("proyecto_id")["fue_escogido"].sum()
    if not (ganadores == 1).all():
        raise ValueError("Cada proyecto debe tener exactamente un ganador")
    if not df_obras["obra_id"].is_unique or not df_postulaciones["postulacion_id"].is_unique:
        raise ValueError("Los identificadores deben ser únicos")
    if not df_postulaciones.groupby("proyecto_id")["contratista_id"].apply(lambda s: s.is_unique).all():
        raise ValueError("Un contratista no puede postular dos veces al mismo proyecto")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--contratistas", type=int, default=400)
    parser.add_argument("--obras", type=int, default=4000)
    parser.add_argument("--proyectos", type=int, default=2000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--sin-red", action="store_true")
    args = parser.parse_args()
    if min(args.contratistas, args.obras, args.proyectos) < 1 or args.contratistas < 12:
        raise ValueError("Las cantidades deben ser positivas y contratar al menos 12 contratistas")
    rng = np.random.default_rng(args.seed)
    RAW_DATA_DIR.joinpath("clima_cache").mkdir(parents=True, exist_ok=True)
    climatologia = {
        municipio: cargar_climatologia(municipio, rng, RAW_DATA_DIR / "clima_cache", args.sin_red)
        for municipio in MUNICIPIOS_COORDENADAS
    }
    contratistas = generar_contratistas(rng, args.contratistas)
    obras, estado = generar_obras(rng, contratistas, climatologia, args.obras)
    postulaciones = generar_postulaciones(rng, obras, contratistas, estado, args.proyectos)
    _validar(obras, postulaciones)
    PROCESSED_DATA_DIR.mkdir(parents=True, exist_ok=True)
    contratistas.to_csv(PROCESSED_DATA_DIR / "contratistas.csv", index=False)
    obras.to_csv(PROCESSED_DATA_DIR / "obras.csv", index=False)
    postulaciones.to_csv(PROCESSED_DATA_DIR / "postulaciones.csv", index=False)
    print(f"contratistas.csv: {len(contratistas)} filas")
    print(f"obras.csv: {len(obras)} filas | retraso: {obras.hubo_retraso.mean():.1%}")
    fuentes = obras["fuente_clima"].value_counts().to_dict()
    print("Fuente clima:", fuentes)
    if not args.sin_red:
        for municipio, grupo in obras.groupby("municipio"):
            respaldo = int((grupo["fuente_clima"] == "respaldo").sum())
            if respaldo:
                print(
                    f"ADVERTENCIA: Open-Meteo falló para {municipio}; "
                    f"{respaldo} obras usan respaldo."
                )
    print(f"postulaciones.csv: {len(postulaciones)} filas | proyectos: {postulaciones.proyecto_id.nunique()}")
    print("Retraso por mes:", obras.groupby("mes_inicio").hubo_retraso.mean().round(3).to_dict())
    print("Retraso por tipo:", obras.groupby("tipo_obra").hubo_retraso.mean().round(3).to_dict())
    print("Causas:", obras.causa_principal_retraso.value_counts().to_dict())
    print("Ganadores por proyecto:", postulaciones.groupby("proyecto_id").fue_escogido.sum().value_counts().to_dict())
    print("Compatibilidad:", postulaciones.compatibilidad.describe()[["min", "mean", "max"]].round(2).to_dict())
    print("\nObras de ejemplo:\n", obras.head(5).to_string(index=False))
    print("\nPostulaciones de ejemplo:\n", postulaciones.head(5).to_string(index=False))


if __name__ == "__main__":
    main()
