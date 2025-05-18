import tensorflow as tf
import tf2onnx
import numpy as np

# Load your trained LSTM model
model = tf.keras.models.load_model("activity_lstm_model.h5")

# Set input shape: batch size = 1, timesteps = 50, features = 9
spec = (tf.TensorSpec([None, 50, 9], tf.float32, name="input"),)

# Convert to ONNX using tf2onnx
print("Converting to ONNX...")
onnx_model, _ = tf2onnx.convert.from_keras(model, input_signature=spec, opset=13)

# Save the ONNX model
with open("activity_lstm_model.onnx", "wb") as f:
    f.write(onnx_model.SerializeToString())

print("✅ Exported activity_lstm_model.onnx successfully.")
