import os
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, AdaBoostClassifier
from xgboost import XGBClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, accuracy_score
import joblib
import skl2onnx
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

# SETTINGS
DATASET_PATH = "dataset"
ACTIVITIES = ["downstairs", "running", "standing", "upstairs", "walking"]
SEQUENCE_LENGTH = 50
FEATURES_PER_SAMPLE = 9

def extract_features(sample_path):
    def load_sensor(file):
        return pd.read_csv(file, sep='\t', header=None, names=['time', 'x', 'y', 'z'])[['x', 'y', 'z']]
    
    try:
        acc = load_sensor(os.path.join(sample_path, 'accelerometer.txt'))
        gyro = load_sensor(os.path.join(sample_path, 'gyroscope.txt'))
        mag = load_sensor(os.path.join(sample_path, 'magnetometer.txt'))
        df = pd.concat([acc, mag, gyro], axis=1)
        if len(df) < SEQUENCE_LENGTH:
            return None
        df = df[:SEQUENCE_LENGTH]  # fix length
        features = df.agg(['mean', 'std', 'min', 'max']).values.flatten()
        return features
    except Exception:
        return None

def load_dataset(path):
    X, y = [], []
    for label, activity in enumerate(ACTIVITIES):
        activity_path = os.path.join(path, activity)
        if not os.path.isdir(activity_path):
            continue
        for sample_id in os.listdir(activity_path):
            sample_path = os.path.join(activity_path, sample_id)
            if not os.path.isdir(sample_path):
                continue
            features = extract_features(sample_path)
            if features is not None:
                X.append(features)
                y.append(label)
    return np.array(X), np.array(y)

print("Loading and extracting features...")
X, y = load_dataset(DATASET_PATH)
print("Total samples:", X.shape[0])

# Normalize features
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# Split
X_train, X_test, y_train, y_test = train_test_split(X_scaled, y, test_size=0.2, random_state=42, stratify=y)

# Store ONNX export function
def export_model(model, model_name):
    initial_type = [('float_input', FloatTensorType([None, X.shape[1]]))]
    onnx_model = convert_sklearn(model, initial_types=initial_type)
    with open(f"{model_name}.onnx", "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"Exported {model_name}.onnx")

# ========== RANDOM FOREST ==========
print("\\nTraining Random Forest...")
rf = RandomForestClassifier(n_estimators=100, random_state=42)
rf.fit(X_train, y_train)
print("RF Accuracy:", accuracy_score(y_test, rf.predict(X_test)))
print(classification_report(y_test, rf.predict(X_test)))
joblib.dump(rf, "rf_model.pkl")
export_model(rf, "rf_model")

# ========== ADA BOOST ==========
print("\\nTraining AdaBoost...")
ab = AdaBoostClassifier(n_estimators=100, random_state=42)
ab.fit(X_train, y_train)
print("AdaBoost Accuracy:", accuracy_score(y_test, ab.predict(X_test)))
print(classification_report(y_test, ab.predict(X_test)))
joblib.dump(ab, "adaboost_model.pkl")
export_model(ab, "adaboost_model")

# ========== XGBOOST ==========
print("\\nTraining XGBoost...")
xgb = XGBClassifier(n_estimators=100, use_label_encoder=False, eval_metric='mlogloss')
xgb.fit(X_train, y_train)
print("XGBoost Accuracy:", accuracy_score(y_test, xgb.predict(X_test)))
print(classification_report(y_test, xgb.predict(X_test)))
joblib.dump(xgb, "xgboost_model.pkl")
export_model(xgb, "xgboost_model")

# Save normalization parameters
joblib.dump(scaler, "feature_scaler.pkl")
print("\\nAll models trained and exported to ONNX successfully.")
