from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT_DIR / "data"
RAW_DATA_DIR = DATA_DIR / "raw"
PROCESSED_DATA_DIR = DATA_DIR / "processed"
SYNTHETIC_DATA_DIR = DATA_DIR / "synthetic"
MODELS_DIR = ROOT_DIR / "models"

# TODO: Mover parámetros de entrenamiento y nombres de artefactos a configuración externa.
RETRASOS_MODEL_PATH = MODELS_DIR / "modelo_retrasos.joblib"
SELECCION_MODEL_PATH = MODELS_DIR / "modelo_seleccion.joblib"
