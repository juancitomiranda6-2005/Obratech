from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health() -> None:
    # TODO: Añadir pruebas de modelos cargados y dependencias.
    response = client.get("/health")
    assert response.status_code == 200


def test_peticion_vacia_rechazada() -> None:
    response = client.post("/predecir-retraso", json={})
    assert response.status_code == 422
    assert "Faltan campos clave para retrasos" in response.json()["detail"]


def test_retraso_con_secundarios_faltantes() -> None:
    response = client.post(
        "/predecir-retraso",
        json={
            "obra": "obra-demo",
            "presupuesto": 1000,
            "duracionPlanificadaDias": 120,
            "ubicacion": "Barranquilla",
            "factores": {
                "tipo_obra": "vial",
                "mes_inicio": 1,
                "experiencia_contratista_anios": 8,
            },
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["mock"] is False
    assert 0.0 <= body["probabilidad"] <= 1.0
    assert body["diasRetrasoEstimados"] >= 0
    assert "num_trabajadores" in body["camposPorDefecto"]
    assert "lluvia_promedio_mm" in body["camposPorDefecto"]
    assert "pct_obras_a_tiempo_contratista" in body["camposPorDefecto"]


def test_predecir_seleccion_entrenado() -> None:
    # TODO: Añadir casos de borde con ofertas y calificaciones fuera de rango.
    response = client.post(
        "/predecir-seleccion",
        json={
            "obra": "obra-demo",
            "contratista": "contratista-demo",
            "presupuesto": 1000,
            "calificacionPromedio": 4.5,
            "compatibilidad": 80,
            "factores": {
                "pct_obras_a_tiempo": 0.8,
                "distancia_km": 10,
            },
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["mock"] is False
    assert 0.0 <= body["probabilidad"] <= 1.0
    assert 0.0 <= body["porcentaje"] <= 100.0
    assert "experiencia_anios" in body["camposPorDefecto"]


def test_rechaza_datos_invalidos() -> None:
    response = client.post(
        "/predecir-seleccion",
        json={
            "obra": "obra-demo",
            "contratista": "contratista-demo",
            "presupuesto": 1000,
            "compatibilidad": 120,
        },
    )
    assert response.status_code == 422


def test_seleccion_calcula_distancia_y_rellena_historial() -> None:
    response = client.post(
        "/predecir-seleccion",
        json={
            "presupuesto": 1000,
            "calificacionPromedio": 4.0,
            "compatibilidad": 75,
            "factores": {
                "tipo_obra": "vial",
                "municipio_proyecto": "Soledad",
                "ciudadContratista": "Barranquilla",
                "latitud": 10.9184,
                "longitud": -74.7646,
            },
        },
    )
    assert response.status_code == 200
    defaults = response.json()["camposPorDefecto"]
    assert "distancia_km" in defaults
    assert "pct_obras_a_tiempo" in defaults
