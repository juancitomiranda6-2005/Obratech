from __future__ import annotations

from datetime import date
from pathlib import Path
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, model_validator
from src.clima import lluvia_para_fecha
from src.geo import distancia_municipio_punto


class PrediccionRetrasoRequest(BaseModel):
    """Entrada de negocio adaptada a las variables del modelo de retrasos."""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    obra: str | None = None
    presupuesto: float | None = None
    duracion_planificada_dias: int | None = Field(default=None, alias="duracionPlanificadaDias")
    ubicacion: str | None = None
    factores: dict[str, Any] = Field(default_factory=dict)

    @model_validator(mode="after")
    def validar_datos(self) -> "PrediccionRetrasoRequest":
        if self.presupuesto is not None and self.presupuesto < 0:
            raise ValueError("presupuesto no puede ser negativo")
        if self.duracion_planificada_dias is not None and self.duracion_planificada_dias <= 0:
            raise ValueError("duracionPlanificadaDias debe ser mayor que cero")
        return self

    def to_model_input(self) -> tuple[dict[str, Any], list[str]]:
        datos = {**self.model_extra, **self.factores}
        valores = {
            "tipo_obra": datos.get("tipo_obra", datos.get("tipoObra")),
            "municipio": self.ubicacion or datos.get("municipio"),
            "presupuesto_millones_cop": self.presupuesto or datos.get("presupuesto_millones_cop"),
            "duracion_planificada_dias": self.duracion_planificada_dias
            or datos.get("duracion_planificada_dias"),
            "mes_inicio": datos.get("mes_inicio")
            or (
                date.fromisoformat(str(datos["fecha_inicio"])).month
                if datos.get("fecha_inicio")
                else None
            ),
            "experiencia_contratista_anios": datos.get("experiencia_contratista_anios"),
        }
        nombres_clave = {
            "tipo_obra": "tipo_obra",
            "municipio": "municipio",
            "presupuesto_millones_cop": "presupuesto",
            "duracion_planificada_dias": "duracion_planificada_dias",
            "mes_inicio": "mes_inicio_o_fecha_inicio",
            "experiencia_contratista_anios": "experiencia_contratista_anios",
        }
        faltantes = [nombres_clave[campo] for campo, valor in valores.items() if valor is None]
        if faltantes:
            raise ValueError(f"Faltan campos clave para retrasos: {', '.join(faltantes)}")

        datos.update(valores)
        secundarios = {
            "num_trabajadores": 10,
            "permisos_complejos": 0,
            "obras_simultaneas_contratista": 1,
            "pct_obras_a_tiempo_contratista": 0.70,
        }
        campos_por_defecto = []
        if datos.get("lluvia_promedio_mm") is None or datos.get("dias_lluvia_estimados") is None:
            lluvia, dias, respaldo = lluvia_para_fecha(
                datos.get("latitud"), datos.get("longitud"), datos.get("fecha_inicio"),
                Path(__file__).resolve().parents[1] / "data" / "raw" / "clima_cache",
            )
            datos["lluvia_promedio_mm"] = lluvia
            datos["dias_lluvia_estimados"] = dias
            campos_por_defecto.extend(["lluvia_promedio_mm", "dias_lluvia_estimados"])
            if respaldo:
                campos_por_defecto.append("lluvia_climatologia_respaldo")
        for campo, valor in secundarios.items():
            if datos.get(campo) is None:
                datos[campo] = valor
                campos_por_defecto.append(campo)
        return datos, campos_por_defecto


class PrediccionRetrasoResponse(BaseModel):
    retrasoProbable: bool
    probabilidad: float
    diasRetrasoEstimados: int
    modelo: str
    mock: bool
    camposPorDefecto: list[str] = Field(default_factory=list)
    nivel_riesgo: str | None = None
    dias_retraso_esperados: float | None = None
    dias_retraso_si_ocurre: float | None = None
    causas_probables: list[dict[str, Any]] = Field(default_factory=list)


class PrediccionSeleccionRequest(BaseModel):
    """Entrada de negocio adaptada a las variables del modelo de selección."""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    obra: str | None = None
    contratista: str | None = None
    presupuesto: float | None = None
    duracion_planificada_dias: int | None = Field(default=None, alias="duracionPlanificadaDias")
    experiencia_anios: float | None = Field(default=None, alias="experienciaAnios")
    calificacion: float | None = Field(default=None, alias="calificacionPromedio")
    compatibilidad: float | None = None
    factores: dict[str, Any] = Field(default_factory=dict)

    @model_validator(mode="after")
    def validar_datos(self) -> "PrediccionSeleccionRequest":
        if self.presupuesto is not None and self.presupuesto <= 0:
            raise ValueError("presupuesto debe ser mayor que cero")
        if self.compatibilidad is not None and not 0 <= self.compatibilidad <= 100:
            raise ValueError("compatibilidad debe estar entre 0 y 100")
        if self.calificacion is not None and not 0 <= self.calificacion <= 5:
            raise ValueError("calificacionPromedio debe estar entre 0 y 5")
        if self.experiencia_anios is not None and self.experiencia_anios < 0:
            raise ValueError("experienciaAnios no puede ser negativo")
        return self

    def to_model_input(self) -> tuple[dict[str, Any], list[str]]:
        datos = {**self.model_extra, **self.factores}
        presupuesto = self.presupuesto or datos.get("presupuesto_millones_cop")
        valores = {
            "compatibilidad": self.compatibilidad
            if self.compatibilidad is not None
            else datos.get("compatibilidad"),
            "calificacion": self.calificacion
            if self.calificacion is not None
            else datos.get("calificacion"),
        }
        faltantes = [campo for campo, valor in valores.items() if valor is None]
        if faltantes:
            raise ValueError(f"Faltan campos clave para seleccion: {', '.join(faltantes)}")

        datos.update(valores)
        datos["presupuesto_millones_cop"] = presupuesto or 0.0
        experiencia = (
            self.experiencia_anios
            if self.experiencia_anios is not None
            else datos.get("experiencia_anios")
        )
        datos.setdefault("tipo_obra", datos.get("tipoObra", "vivienda"))
        datos.setdefault("municipio_proyecto", datos.get("municipio", "Barranquilla"))
        campos_por_defecto = []
        if datos.get("distancia_km") is None:
            distancia = distancia_municipio_punto(
                datos.get("ciudadContratista"),
                datos.get("latitud"),
                datos.get("longitud"),
            )
            datos["distancia_km"] = 50.0 if distancia is None else distancia
            campos_por_defecto.append("distancia_km")
            if distancia is None:
                campos_por_defecto.append("distancia_km_no_disponible")
        secundarios = {
            "experiencia_anios": 0.0,
            "obras_completadas": 5,
            "num_postulantes": 5,
            "pct_obras_a_tiempo": 0.70,
        }
        for campo, valor in secundarios.items():
            if campo == "experiencia_anios" and experiencia is not None:
                datos[campo] = experiencia
                continue
            if campo not in datos or datos[campo] is None:
                datos[campo] = valor
                campos_por_defecto.append(campo)
        return datos, campos_por_defecto


class PrediccionSeleccionResponse(BaseModel):
    probabilidad: float
    recomendacion: str
    modelo: str
    mock: bool
    camposPorDefecto: list[str] = Field(default_factory=list)
    porcentaje: float | None = None
    nivel: str | None = None
    factores: list[dict[str, Any]] = Field(default_factory=list)
