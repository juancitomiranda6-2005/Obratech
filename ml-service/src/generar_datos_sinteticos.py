"""
Genera datos sintéticos para entrenar los dos modelos de ObraTech:

  1. obras.csv          -> modelo de retrasos de obra
  2. postulaciones.csv  -> modelo de probabilidad de ser escogido

IMPORTANTE: son datos inventados con lógica de negocio (más lluvia = más
retraso, mejor precio y compatibilidad = más probabilidad de ganar). Sirven
para desarrollar y hacer demos, NO para producción. Cuando tengas datos reales
(SECOP II o tu propia base de datos) reemplaza estos CSV con el mismo formato.

Uso (desde la carpeta ml-service):
    python -m src.generar_datos_sinteticos
    python -m src.generar_datos_sinteticos --obras 3000 --proyectos 1500 --seed 7
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

# Carpeta destino: ml-service/data/synthetic
DESTINO = Path(__file__).resolve().parent.parent / "data" / "synthetic"
MAX_POSTULANTES_POR_PROYECTO = 10

TIPOS_OBRA = ["vivienda", "vial", "acueducto", "edificio", "remodelacion", "espacio_publico"]
# Qué tanto afecta cada tipo de obra a la lluvia (obras al aire libre sufren más)
SENSIBILIDAD_LLUVIA = {
    "vivienda": 0.8, "vial": 1.4, "acueducto": 1.2,
    "edificio": 0.7, "remodelacion": 0.3, "espacio_publico": 1.1,
}
MUNICIPIOS = ["Barranquilla", "Soledad", "Malambo", "Puerto Colombia", "Sabanalarga", "Baranoa"]
CAUSAS = ["lluvia", "materiales", "permisos", "mano_de_obra", "financiacion", "diseno"]

# Lluvia mensual aproximada (mm) para el Caribe colombiano: temporada seca
# de dic a abr y lluvias fuertes de ago a nov.
LLUVIA_MENSUAL_MM = {
    1: 5, 2: 5, 3: 10, 4: 45, 5: 100, 6: 90,
    7: 80, 8: 120, 9: 150, 10: 190, 11: 130, 12: 30,
}


def _sigmoide(x: np.ndarray) -> np.ndarray:
    # TODO: Mantener la transformación estable si se incorporan factores de mayor escala.
    resultado = np.empty_like(x, dtype=float)
    positivos = x >= 0
    resultado[positivos] = 1.0 / (1.0 + np.exp(-x[positivos]))
    exponencial = np.exp(x[~positivos])
    resultado[~positivos] = exponencial / (1.0 + exponencial)
    return resultado


def _validar_cantidad(nombre: str, cantidad: int, minimo: int = 1) -> None:
    if cantidad < minimo:
        raise ValueError(f"{nombre} debe ser mayor o igual que {minimo}; recibido: {cantidad}")


def generar_contratistas(rng: np.random.Generator, n: int) -> pd.DataFrame:
    """Perfil fijo de cada contratista (se reutiliza en obras y postulaciones)."""
    _validar_cantidad("n", n, MAX_POSTULANTES_POR_PROYECTO)
    experiencia = rng.gamma(shape=3.0, scale=3.0, size=n).clip(0.5, 35)
    # Los contratistas con más experiencia suelen cumplir mejor, con ruido
    pct_a_tiempo = (0.45 + 0.012 * experiencia + rng.normal(0, 0.12, n)).clip(0.1, 0.99)
    calificacion = (3.0 + 1.5 * pct_a_tiempo + rng.normal(0, 0.35, n)).clip(1, 5)
    return pd.DataFrame({
        "contratista_id": np.arange(1, n + 1),
        "experiencia_anios": experiencia.round(1),
        "pct_obras_a_tiempo": pct_a_tiempo.round(3),
        "calificacion": calificacion.round(2),
        "obras_completadas": (experiencia * rng.uniform(1.0, 3.0, n)).astype(int) + 1,
        "municipio_base": rng.choice(MUNICIPIOS, n),
        "especialidad": rng.choice(TIPOS_OBRA, n),
    })


def generar_obras(rng: np.random.Generator, contratistas: pd.DataFrame, n: int) -> pd.DataFrame:
    _validar_cantidad("n", n)
    if len(contratistas) == 0:
        raise ValueError("contratistas no puede estar vacío")
    tipo = rng.choice(TIPOS_OBRA, n, p=[0.28, 0.15, 0.12, 0.15, 0.20, 0.10])
    municipio = rng.choice(MUNICIPIOS, n)
    presupuesto_millones = rng.lognormal(mean=6.0, sigma=0.9, size=n).clip(30, 8000)  # COP millones
    duracion_plan = (presupuesto_millones ** 0.45 * rng.uniform(6, 14, n)).clip(30, 720).astype(int)
    trabajadores = (presupuesto_millones ** 0.4 * rng.uniform(1.5, 4, n)).clip(3, 250).astype(int)

    inicio = pd.to_datetime("2021-01-01") + pd.to_timedelta(rng.integers(0, 365 * 5, n), unit="D")
    mes = inicio.month.to_numpy()

    # Lluvia promedio durante la obra (mm/mes) con ruido, según el mes de inicio
    lluvia_base = np.array([LLUVIA_MENSUAL_MM[m] for m in mes], dtype=float)
    lluvia_mm = (lluvia_base * rng.uniform(0.7, 1.3, n)).round(1)
    dias_lluvia = np.clip(lluvia_mm / 12 + rng.normal(0, 1.5, n), 0, 25).round(0)

    idx = rng.integers(0, len(contratistas), n)
    c = contratistas.iloc[idx].reset_index(drop=True)
    obras_simultaneas = rng.integers(1, 6, n)

    sens = np.array([SENSIBILIDAD_LLUVIA[t] for t in tipo])
    permisos_complejos = rng.binomial(1, 0.25, n)

    # --- Lógica del retraso: mezcla de factores con peso razonable ---
    z = (
        -1.6
        + 0.012 * lluvia_mm * sens           # lluvia pesa más en obras al aire libre
        + 0.0018 * duracion_plan             # obras largas se retrasan más
        + 0.25 * (obras_simultaneas - 1)     # contratista sobrecargado
        - 2.2 * (c["pct_obras_a_tiempo"] - 0.6)  # buen historial protege
        - 0.04 * c["experiencia_anios"]
        + 0.6 * permisos_complejos
        + rng.normal(0, 0.5, n)
    )
    prob_retraso = _sigmoide(z)
    hay_retraso = rng.binomial(1, prob_retraso)

    # Días de retraso: proporcional a la duración y a la probabilidad
    dias_retraso = np.where(
        hay_retraso == 1,
        (duracion_plan * prob_retraso * rng.uniform(0.10, 0.45, n)).astype(int).clip(1),
        0,
    )

    # Causa principal: depende de qué factor pesó más
    causas = []
    for i in range(n):
        if hay_retraso[i] == 0:
            causas.append("ninguna")
            continue
        pesos = np.array([
            0.012 * lluvia_mm[i] * sens[i] + 0.05,                    # lluvia
            0.25,                                                      # materiales
            0.6 * permisos_complejos[i] + 0.05,                       # permisos
            0.25 * (obras_simultaneas[i] - 1) + 0.05,                 # mano de obra
            0.15,                                                      # financiación
            0.10,                                                      # diseño
        ])
        causas.append(rng.choice(CAUSAS, p=pesos / pesos.sum()))

    return pd.DataFrame({
        "obra_id": np.arange(1, n + 1),
        "tipo_obra": tipo,
        "municipio": municipio,
        "presupuesto_millones_cop": presupuesto_millones.round(1),
        "duracion_planificada_dias": duracion_plan,
        "num_trabajadores": trabajadores,
        "fecha_inicio": inicio.strftime("%Y-%m-%d"),
        "mes_inicio": mes,
        "lluvia_promedio_mm": lluvia_mm,
        "dias_lluvia_estimados": dias_lluvia,
        "permisos_complejos": permisos_complejos,
        "contratista_id": c["contratista_id"],
        "experiencia_contratista_anios": c["experiencia_anios"],
        "pct_obras_a_tiempo_contratista": c["pct_obras_a_tiempo"],
        "obras_simultaneas_contratista": obras_simultaneas,
        # --- Variables objetivo (target) ---
        "hubo_retraso": hay_retraso,
        "dias_retraso": dias_retraso,
        "causa_principal_retraso": causas,
    })


def generar_postulaciones(rng: np.random.Generator, contratistas: pd.DataFrame, n_proyectos: int) -> pd.DataFrame:
    _validar_cantidad("n_proyectos", n_proyectos)
    if len(contratistas) < MAX_POSTULANTES_POR_PROYECTO:
        raise ValueError(
            "Se requieren al menos "
            f"{MAX_POSTULANTES_POR_PROYECTO} contratistas para generar postulaciones"
        )
    filas = []
    for proyecto_id in range(1, n_proyectos + 1):
        tipo = rng.choice(TIPOS_OBRA)
        municipio = rng.choice(MUNICIPIOS)
        presupuesto = float(np.clip(rng.lognormal(6.0, 0.9), 30, 8000))
        k = int(rng.integers(2, 11))  # número de postulantes
        cand = contratistas.sample(k, random_state=int(rng.integers(0, 1_000_000)))

        scores, registros = [], []
        for _, c in cand.iterrows():
            # Compatibilidad 0-100: especialidad + zona + experiencia
            comp = (
                45 * (c["especialidad"] == tipo)
                + 20 * (c["municipio_base"] == municipio)
                + min(c["experiencia_anios"], 20) * 1.5
                + rng.normal(0, 6)
            )
            comp = float(np.clip(comp + 10, 0, 100))

            # Precio ofertado como proporción del presupuesto (1.0 = igual)
            ratio_precio = float(np.clip(rng.normal(0.97, 0.12), 0.65, 1.4))
            distancia = float(np.clip(rng.gamma(2.0, 12.0) * (0.3 if c["municipio_base"] == municipio else 1.0), 1, 300))

            # Puntaje real con el que "gana" (el modelo debe descubrir esto)
            score = (
                0.045 * comp
                - 4.0 * (ratio_precio - 0.95)
                + 0.9 * (c["calificacion"] - 3.5)
                + 1.5 * (c["pct_obras_a_tiempo"] - 0.6)
                - 0.004 * distancia
                + rng.normal(0, 0.5)
            )
            scores.append(score)
            registros.append({
                "proyecto_id": proyecto_id,
                "tipo_obra": tipo,
                "municipio_proyecto": municipio,
                "presupuesto_millones_cop": round(presupuesto, 1),
                "contratista_id": int(c["contratista_id"]),
                "compatibilidad": round(comp, 1),
                "experiencia_anios": c["experiencia_anios"],
                "calificacion": c["calificacion"],
                "pct_obras_a_tiempo": c["pct_obras_a_tiempo"],
                "obras_completadas": int(c["obras_completadas"]),
                "distancia_km": round(distancia, 1),
                "num_postulantes": k,
            })

        # Un solo ganador por proyecto, elegido con probabilidad según el puntaje
        s = np.array(scores)
        p = np.exp(s - s.max())
        p /= p.sum()
        ganador = rng.choice(k, p=p)
        for j, r in enumerate(registros):
            r["fue_escogido"] = int(j == ganador)  # target
            filas.append(r)

    df = pd.DataFrame(filas)
    df.insert(0, "postulacion_id", np.arange(1, len(df) + 1))
    return df


def main() -> None:
    ap = argparse.ArgumentParser(description="Genera datos sintéticos de ObraTech")
    ap.add_argument("--obras", type=int, default=2500, help="cantidad de obras")
    ap.add_argument("--proyectos", type=int, default=1200, help="cantidad de proyectos con postulaciones")
    ap.add_argument("--contratistas", type=int, default=300, help="cantidad de contratistas")
    ap.add_argument("--seed", type=int, default=42, help="semilla para reproducir los datos")
    args = ap.parse_args()
    _validar_cantidad("obras", args.obras)
    _validar_cantidad("proyectos", args.proyectos)
    _validar_cantidad("contratistas", args.contratistas, MAX_POSTULANTES_POR_PROYECTO)

    rng = np.random.default_rng(args.seed)
    DESTINO.mkdir(parents=True, exist_ok=True)

    contratistas = generar_contratistas(rng, args.contratistas)
    obras = generar_obras(rng, contratistas, args.obras)
    postulaciones = generar_postulaciones(rng, contratistas, args.proyectos)

    contratistas.to_csv(DESTINO / "contratistas.csv", index=False)
    obras.to_csv(DESTINO / "obras.csv", index=False)
    postulaciones.to_csv(DESTINO / "postulaciones.csv", index=False)

    print(f"Archivos guardados en: {DESTINO}")
    print(f"  contratistas.csv   : {len(contratistas):>6} filas")
    print(f"  obras.csv          : {len(obras):>6} filas | retraso en {obras['hubo_retraso'].mean():.1%} de las obras")
    print(f"  postulaciones.csv  : {len(postulaciones):>6} filas | escogidos {postulaciones['fue_escogido'].mean():.1%}")


if __name__ == "__main__":
    main()