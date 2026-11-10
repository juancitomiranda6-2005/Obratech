"""
Entrena el modelo 1 (retrasos de obra). Se entrenan tres piezas:

  - clasificador       : probabilidad de que la obra tenga retraso (sí/no)
  - regresor_dias      : cuántos días de retraso, SI hay retraso
  - clasificador_causa : causa más probable (lluvia, materiales, permisos...), SI hay retraso

Uso (desde la carpeta ml-service):
    python -m src.entrenar_retrasos
    python -m src.entrenar_retrasos --reales      # usa data/processed en vez de data/synthetic
"""
from __future__ import annotations

import argparse
import json

import joblib
import numpy as np
from sklearn.ensemble import HistGradientBoostingClassifier, HistGradientBoostingRegressor
from sklearn.metrics import accuracy_score, brier_score_loss, mean_absolute_error, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

from src.features import (
    MODELOS,
    RETRASO_CAT,
    RETRASO_NUM,
    TARGET_CAUSA,
    TARGET_DIAS_RETRASO,
    TARGET_HUBO_RETRASO,
    cargar_obras,
    preprocesador,
)

SEMILLA = 42


def _pipeline(modelo) -> Pipeline:
    return Pipeline([("prep", preprocesador(RETRASO_NUM, RETRASO_CAT)), ("modelo", modelo)])


def main(usar_reales: bool = False) -> dict:
    df = cargar_obras(usar_reales)
    columnas = RETRASO_NUM + RETRASO_CAT
    X = df[columnas]

    # --- Separar entrenamiento (80%) y prueba (20%) ---
    X_tr, X_te, df_tr, df_te = train_test_split(
        X, df, test_size=0.2, random_state=SEMILLA, stratify=df[TARGET_HUBO_RETRASO]
    )

    # ------------------------------------------------------------------
    # 1) ¿Habrá retraso?
    # ------------------------------------------------------------------
    clf = _pipeline(
        HistGradientBoostingClassifier(
            max_iter=200, learning_rate=0.05, max_depth=4, l2_regularization=1.0, random_state=SEMILLA
        )
    )
    clf.fit(X_tr, df_tr[TARGET_HUBO_RETRASO])
    p_te = clf.predict_proba(X_te)[:, 1]
    y_te = df_te[TARGET_HUBO_RETRASO]
    tasa_base = float(df_tr[TARGET_HUBO_RETRASO].mean())

    metricas = {
        "n_obras": int(len(df)),
        "tasa_retraso": round(float(df[TARGET_HUBO_RETRASO].mean()), 4),
        "clasificador": {
            "roc_auc": round(float(roc_auc_score(y_te, p_te)), 4),
            "accuracy": round(float(accuracy_score(y_te, p_te >= 0.5)), 4),
            "brier": round(float(brier_score_loss(y_te, p_te)), 4),
            "brier_sin_modelo": round(float(brier_score_loss(y_te, np.full(len(y_te), tasa_base))), 4),
        },
    }

    # ------------------------------------------------------------------
    # 2) Días de retraso (solo obras que sí se retrasaron)
    # ------------------------------------------------------------------
    tr_ret = df_tr[df_tr[TARGET_HUBO_RETRASO] == 1]
    te_ret = df_te[df_te[TARGET_HUBO_RETRASO] == 1]

    reg = _pipeline(
        HistGradientBoostingRegressor(
            max_iter=200, learning_rate=0.05, max_depth=4, l2_regularization=1.0, random_state=SEMILLA
        )
    )
    reg.fit(tr_ret[columnas], tr_ret[TARGET_DIAS_RETRASO])
    pred_dias = np.clip(reg.predict(te_ret[columnas]), 0, None)
    mae_modelo = float(mean_absolute_error(te_ret[TARGET_DIAS_RETRASO], pred_dias))
    mae_base = float(
        mean_absolute_error(
            te_ret[TARGET_DIAS_RETRASO], np.full(len(te_ret), tr_ret[TARGET_DIAS_RETRASO].median())
        )
    )
    metricas["regresor_dias"] = {
        "mae_dias": round(mae_modelo, 2),
        "mae_dias_sin_modelo": round(mae_base, 2),
    }

    # ------------------------------------------------------------------
    # 3) Causa más probable (solo obras que sí se retrasaron)
    # ------------------------------------------------------------------
    clf_causa = _pipeline(
        HistGradientBoostingClassifier(
            max_iter=150, learning_rate=0.05, max_depth=3, l2_regularization=1.0, random_state=SEMILLA
        )
    )
    clf_causa.fit(tr_ret[columnas], tr_ret[TARGET_CAUSA])
    proba = clf_causa.predict_proba(te_ret[columnas])
    clases = clf_causa.classes_
    top1 = clases[proba.argmax(axis=1)]
    top2 = clases[np.argsort(proba, axis=1)[:, -2:]]
    acc_top1 = float((top1 == te_ret[TARGET_CAUSA].to_numpy()).mean())
    acc_top2 = float(np.mean([real in fila for real, fila in zip(te_ret[TARGET_CAUSA], top2)]))
    base_causa = float(tr_ret[TARGET_CAUSA].value_counts(normalize=True).iloc[0])
    metricas["clasificador_causa"] = {
        "accuracy_top1": round(acc_top1, 4),
        "accuracy_top2": round(acc_top2, 4),
        "accuracy_sin_modelo": round(base_causa, 4),
    }

    # ------------------------------------------------------------------
    # Guardar
    # ------------------------------------------------------------------
    MODELOS.mkdir(parents=True, exist_ok=True)
    joblib.dump(
        {
            "clasificador": clf,
            "regresor_dias": reg,
            "clasificador_causa": clf_causa,
            "columnas": columnas,
            "metricas": metricas,
            "datos_reales": usar_reales,
        },
        MODELOS / "modelo_retrasos.joblib",
    )
    (MODELOS / "metricas_retrasos.json").write_text(json.dumps(metricas, indent=2, ensure_ascii=False))

    print("Modelo de retrasos guardado en models/modelo_retrasos.joblib")
    print(json.dumps(metricas, indent=2, ensure_ascii=False))
    return metricas


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--reales", action="store_true", help="usar data/processed en vez de data/synthetic")
    main(ap.parse_args().reales)