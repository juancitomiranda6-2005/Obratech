"""
Entrena el modelo 2: probabilidad de que un contratista sea escogido para un proyecto.

Se comparan dos candidatos y se guarda el que tenga menor log-loss en la prueba:
  - Regresión logística (simple y fácil de explicar)
  - Gradient Boosting calibrado (captura relaciones más complejas)

Se separa por PROYECTO (no por fila) para que las postulaciones de un mismo
proyecto no queden repartidas entre entrenamiento y prueba.

Uso (desde la carpeta ml-service):
    python -m src.entrenar_seleccion
    python -m src.entrenar_seleccion --reales
"""
from __future__ import annotations

import argparse
import json

import joblib
import numpy as np
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import brier_score_loss, log_loss, roc_auc_score
from sklearn.model_selection import GroupShuffleSplit
from sklearn.pipeline import Pipeline

from src.features import (
    COLUMNA_GRUPO_SELECCION,
    MODELOS,
    SELECCION_CAT,
    SELECCION_NUM,
    TARGET_ESCOGIDO,
    cargar_postulaciones,
    preprocesador,
)

SEMILLA = 42


def _top1_por_proyecto(df_te, prob) -> tuple[float, float]:
    """% de proyectos donde el contratista con mayor probabilidad fue el escogido,
    y el % esperado si se eligiera al azar (promedio de 1/k)."""
    tmp = df_te[[COLUMNA_GRUPO_SELECCION, TARGET_ESCOGIDO]].copy()
    tmp["prob"] = prob
    aciertos, azar = [], []
    for _, g in tmp.groupby(COLUMNA_GRUPO_SELECCION):
        aciertos.append(int(g.loc[g["prob"].idxmax(), TARGET_ESCOGIDO] == 1))
        azar.append(1 / len(g))
    return float(np.mean(aciertos)), float(np.mean(azar))


def main(usar_reales: bool = False) -> dict:
    df = cargar_postulaciones(usar_reales)
    columnas = SELECCION_NUM + SELECCION_CAT
    X, y = df[columnas], df[TARGET_ESCOGIDO]

    gss = GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=SEMILLA)
    idx_tr, idx_te = next(gss.split(X, y, groups=df[COLUMNA_GRUPO_SELECCION]))
    X_tr, X_te, y_tr, y_te = X.iloc[idx_tr], X.iloc[idx_te], y.iloc[idx_tr], y.iloc[idx_te]
    df_te = df.iloc[idx_te]

    candidatos = {
        "regresion_logistica": Pipeline(
            [
                ("prep", preprocesador(SELECCION_NUM, SELECCION_CAT, escalar=True)),
                ("modelo", LogisticRegression(max_iter=2000, C=1.0)),
            ]
        ),
        "gradient_boosting_calibrado": CalibratedClassifierCV(
            Pipeline(
                [
                    ("prep", preprocesador(SELECCION_NUM, SELECCION_CAT)),
                    (
                        "modelo",
                        HistGradientBoostingClassifier(
                            max_iter=150, learning_rate=0.05, max_depth=3,
                            l2_regularization=1.0, random_state=SEMILLA,
                        ),
                    ),
                ]
            ),
            method="sigmoid",
            cv=3,
        ),
    }

    tasa_base = float(y_tr.mean())
    resultados, entrenados = {}, {}
    for nombre, modelo in candidatos.items():
        modelo.fit(X_tr, y_tr)
        p = modelo.predict_proba(X_te)[:, 1]
        top1, azar = _top1_por_proyecto(df_te, p)
        resultados[nombre] = {
            "roc_auc": round(float(roc_auc_score(y_te, p)), 4),
            "log_loss": round(float(log_loss(y_te, p)), 4),
            "brier": round(float(brier_score_loss(y_te, p)), 4),
            "acierto_top1_por_proyecto": round(top1, 4),
            "acierto_top1_al_azar": round(azar, 4),
        }
        entrenados[nombre] = modelo

    ganador = min(resultados, key=lambda n: resultados[n]["log_loss"])
    modelo_final = entrenados[ganador]

    # Valores de referencia para explicar el "por qué" de cada predicción
    escogidos = df[df[TARGET_ESCOGIDO] == 1]
    referencia = {
        "media_escogidos": {c: float(escogidos[c].mean()) for c in SELECCION_NUM},
        "desv_general": {c: float(df[c].std() or 1.0) for c in SELECCION_NUM},
    }

    metricas = {
        "n_postulaciones": int(len(df)),
        "n_proyectos": int(df[COLUMNA_GRUPO_SELECCION].nunique()),
        "tasa_escogidos": round(float(df[TARGET_ESCOGIDO].mean()), 4),
        "brier_sin_modelo": round(float(brier_score_loss(y_te, np.full(len(y_te), tasa_base))), 4),
        "candidatos": resultados,
        "modelo_elegido": ganador,
    }

    MODELOS.mkdir(parents=True, exist_ok=True)
    joblib.dump(
        {
            "modelo": modelo_final,
            "nombre": ganador,
            "columnas": columnas,
            "referencia": referencia,
            "metricas": metricas,
            "datos_reales": usar_reales,
        },
        MODELOS / "modelo_seleccion.joblib",
    )
    (MODELOS / "metricas_seleccion.json").write_text(json.dumps(metricas, indent=2, ensure_ascii=False))

    print("Modelo de selección guardado en models/modelo_seleccion.joblib")
    print(json.dumps(metricas, indent=2, ensure_ascii=False))
    return metricas


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--reales", action="store_true", help="usar data/processed en vez de data/synthetic")
    main(ap.parse_args().reales)