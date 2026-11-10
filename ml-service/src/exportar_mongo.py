"""Exporta CSV semirreales a JSON compatible con mongoimport --jsonArray.

No conecta ni inserta documentos en MongoDB. Las referencias entre documentos
se dejan como IDs para que la importación pueda adaptarse a la estrategia de
DBRef del backend.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import pandas as pd

from src.config import PROCESSED_DATA_DIR


def _leer(nombre: str) -> pd.DataFrame:
    ruta = PROCESSED_DATA_DIR / nombre
    if not ruta.exists():
        raise FileNotFoundError(f"No existe {ruta}; genera primero el dataset semirreal")
    return pd.read_csv(ruta)


def _limpiar(valor):
    if pd.isna(valor):
        return None
    if hasattr(valor, "item"):
        return valor.item()
    return valor


def _guardar(nombre: str, documentos: list[dict], destino: Path) -> None:
    (destino / nombre).write_text(
        json.dumps(documentos, ensure_ascii=False, indent=2, default=str),
        encoding="utf-8",
    )


def exportar(destino: Path) -> list[str]:
    destino.mkdir(parents=True, exist_ok=True)
    contratistas = _leer("contratistas.csv")
    obras = _leer("obras.csv")
    postulaciones = _leer("postulaciones.csv")
    archivos = []

    usuarios = [
        {"_id": row.contratista_id, "username": row.nit, "roles": ["ROLE_CONTRACTOR"],
         "activo": True, "verificado": True}
        for row in contratistas.itertuples()
    ]
    perfiles = [{
        "_id": row.contratista_id, "username": row.nit, "nombre": row.nombre,
        "roles": ["ROLE_CONTRACTOR"], "activo": True, "verificado": True,
        "contratista": {
            "especialidad": row.especialidad, "ubicacion": row.municipio_base,
            "experiencia": int(row.experiencia_anios),
            "calificacionPromedio": float(row.calificacion_promedio),
            "ciudad": row.ciudad,
            "departamento": row.departamento, "subespecialidades": [row.subespecialidades],
        },
    } for row in contratistas.itertuples()]
    proyectos = [{
        "_id": row.obra_id, "titulo": row.titulo, "tipoProyecto": row.tipo_obra,
        "ubicacion": row.municipio, "ubicacionGeografica": {
            "departamento": row.departamento, "municipio": row.municipio,
        }, "presupuesto": float(row.presupuesto_millones_cop),
        "plazoEstimado": int(row.duracion_planificada_dias),
        "fechaInicio": row.fecha_inicio, "fechaEntrega": row.fecha_fin_planeada,
        "contratistaAsignado": row.contratista_id,
    } for row in obras.itertuples()]
    aplicaciones = [{
        "_id": row.postulacion_id, "proyecto": row.proyecto_id,
        "usuario": row.contratista_id, "estado": "ACCEPTED" if row.fue_escogido else "PENDING",
        "fechaPostulacion": row.fecha_postulacion,
    } for row in postulaciones.itertuples()]

    for nombre, documentos in (
        ("usuarios.json", usuarios), ("perfiles.json", perfiles),
        ("proyectos.json", proyectos), ("postulaciones.json", aplicaciones),
    ):
        _guardar(nombre, documentos, destino)
        archivos.append(nombre)
    extras = {
        "campos_no_presentes_en_entidades_java.json": {
            "descripcion": "Variables necesarias para entrenamiento que no son campos directos de las entidades revisadas.",
            "campos": [
                "fecha_fin_planeada", "fecha_fin_real", "hubo_retraso", "dias_retraso",
                "causa_principal_retraso", "lluvia_promedio_mm", "dias_lluvia_estimados",
                "permisos_complejos", "obras_previas", "obras_simultaneas_contratista",
                "pct_obras_a_tiempo", "compatibilidad",
                "distancia_km", "num_postulantes", "fue_escogido",
            ],
        }
    }
    _guardar("campos_no_presentes_en_entidades_java.json", extras["campos_no_presentes_en_entidades_java.json"], destino)
    archivos.append("campos_no_presentes_en_entidades_java.json")
    return archivos


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--destino", type=Path, default=PROCESSED_DATA_DIR / "mongo")
    args = parser.parse_args()
    print("Archivos generados:", ", ".join(exportar(args.destino)))
    print(f"No se insertó nada en MongoDB. Revisa: {args.destino}")


if __name__ == "__main__":
    main()
