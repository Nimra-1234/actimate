import joblib
import numpy as np
from hummingbird.ml import convert
import onnx

# Load your trained XGBoost model
xgb_model = joblib.load("xgboost_model.pkl")

# Match this to your actual feature count
n_features = 54
dummy_input = np.random.rand(1, n_features).astype(np.float32)

# Convert to ONNX model using Hummingbird
onnx_model = convert(xgb_model, 'onnx', dummy_input)

# Access raw ONNX proto
onnx_bytes = onnx_model.model.SerializeToString()

# Save as actual .onnx file
with open("xgboost_model.onnx", "wb") as f:
    f.write(onnx_bytes)

print("✅ Real .onnx file saved as xgboost_model.onnx")
