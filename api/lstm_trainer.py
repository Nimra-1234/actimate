import os
import numpy as np
import pandas as pd
import pickle
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization, Bidirectional
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from tensorflow.keras.optimizers import Adam
import matplotlib.pyplot as plt

# Constants
DATASET_PATH = "dataset"
ACTIVITIES = ["downstairs", "running", "standing", "upstairs", "walking"]
SEQUENCE_LENGTH = 50  # Match app's requirements
RANDOM_SEED = 42
TRAIN_SIZE = 0.8
VALIDATION_SIZE = 0.2  # Percentage of training data to use for validation
BATCH_SIZE = 32
MAX_EPOCHS = 50
LEARNING_RATE = 0.001

def load_data_from_directory(dataset_path):
    """
    Load sensor data from the directory structure and create sequences for LSTM
    """
    sequences = []
    labels = []
    
    for class_idx, activity in enumerate(ACTIVITIES):
        activity_path = os.path.join(dataset_path, activity)
        print(f"Processing {activity} data...")
        
        # Get all sample directories
        sample_dirs = [d for d in os.listdir(activity_path) 
                      if os.path.isdir(os.path.join(activity_path, d))]
        
        for sample_dir in sample_dirs:
            sample_path = os.path.join(activity_path, sample_dir)
            
            # Check if all required sensor files exist
            sensor_files = {
                'accelerometer': os.path.join(sample_path, 'accelerometer.txt'),
                'gyroscope': os.path.join(sample_path, 'gyroscope.txt'),
                'magnetometer': os.path.join(sample_path, 'magnetometer.txt')
            }
            
            if not all(os.path.exists(file) for file in sensor_files.values()):
                continue
                
            # Read sensor data
            try:
                acc_data = pd.read_csv(sensor_files['accelerometer'], sep='\t', 
                                      names=['timestamp', 'acc_x', 'acc_y', 'acc_z'])
                gyro_data = pd.read_csv(sensor_files['gyroscope'], sep='\t', 
                                       names=['timestamp', 'gyro_x', 'gyro_y', 'gyro_z'])
                mag_data = pd.read_csv(sensor_files['magnetometer'], sep='\t', 
                                      names=['timestamp', 'mag_x', 'mag_y', 'mag_z'])
                
                # Check if we have enough data points
                min_length = min(len(acc_data), len(gyro_data), len(mag_data))
                if min_length < SEQUENCE_LENGTH:
                    continue
                    
                # Trim to ensure same length
                acc_data = acc_data.iloc[:min_length]
                gyro_data = gyro_data.iloc[:min_length]
                mag_data = mag_data.iloc[:min_length]
                
                # Merge sensor data, keeping only sensor values (removing timestamps)
                sensor_features = pd.concat([
                    acc_data[['acc_x', 'acc_y', 'acc_z']],
                    mag_data[['mag_x', 'mag_y', 'mag_z']],
                    gyro_data[['gyro_x', 'gyro_y', 'gyro_z']]
                ], axis=1)
                
                # Create sequences with 50% overlap for better training
                step_size = SEQUENCE_LENGTH // 2
                for i in range(0, len(sensor_features) - SEQUENCE_LENGTH + 1, step_size):
                    sequence = sensor_features.iloc[i:i+SEQUENCE_LENGTH].values
                    sequences.append(sequence)
                    labels.append(class_idx)
                    
            except Exception as e:
                print(f"Error processing {sample_path}: {e}")
                continue
    
    return np.array(sequences), np.array(labels)

def normalize_sequences(train_sequences, test_sequences):
    """
    Normalize each feature across the sequence length to have mean 0 and std 1
    """
    # Reshape to 2D for normalization
    train_shape = train_sequences.shape
    test_shape = test_sequences.shape
    
    train_reshaped = train_sequences.reshape(-1, train_shape[-1])
    test_reshaped = test_sequences.reshape(-1, test_shape[-1])
    
    # Calculate mean and std from training data
    train_mean = np.mean(train_reshaped, axis=0)
    train_std = np.std(train_reshaped, axis=0)
    
    # Handle zero std to avoid division by zero
    train_std[train_std == 0] = 1e-6
    
    # Normalize
    train_normalized = (train_reshaped - train_mean) / train_std
    test_normalized = (test_reshaped - train_mean) / train_std
    
    # Reshape back to original shape
    train_sequences_normalized = train_normalized.reshape(train_shape)
    test_sequences_normalized = test_normalized.reshape(test_shape)
    
    # Save normalization params for Android app
    normalization_params = {
        'mean': train_mean,
        'std': train_std
    }
    
    with open('normalization_params.pkl', 'wb') as f:
        pickle.dump(normalization_params, f)
    
    return train_sequences_normalized, test_sequences_normalized

def build_lstm_model(input_shape, num_classes):
    """
    Build an LSTM model for activity recognition
    """
    model = Sequential([
        # Input layer
        # Bidirectional LSTM to capture patterns in both directions
        Bidirectional(LSTM(64, return_sequences=True), input_shape=input_shape),
        BatchNormalization(),
        Dropout(0.3),
        
        # Second LSTM layer
        LSTM(32),
        BatchNormalization(),
        Dropout(0.3),
        
        # Dense layers for classification
        Dense(16, activation='relu'),
        BatchNormalization(),
        Dropout(0.2),
        
        # Output layer with softmax for multi-class classification
        Dense(num_classes, activation='softmax')
    ])
    
    # Use Adam optimizer with a specific learning rate
    optimizer = Adam(learning_rate=LEARNING_RATE)
    
    # Compile the model
    model.compile(
        optimizer=optimizer,
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model

def plot_training_history(history, save_path='training_history.png'):
    """
    Plot and save the training history
    """
    plt.figure(figsize=(12, 5))
    
    # Plot accuracy
    plt.subplot(1, 2, 1)
    plt.plot(history.history['accuracy'], label='Training Accuracy')
    plt.plot(history.history['val_accuracy'], label='Validation Accuracy')
    plt.title('Model Accuracy')
    plt.xlabel('Epoch')
    plt.ylabel('Accuracy')
    plt.legend()
    
    # Plot loss
    plt.subplot(1, 2, 2)
    plt.plot(history.history['loss'], label='Training Loss')
    plt.plot(history.history['val_loss'], label='Validation Loss')
    plt.title('Model Loss')
    plt.xlabel('Epoch')
    plt.ylabel('Loss')
    plt.legend()
    
    plt.tight_layout()
    plt.savefig(save_path)
    plt.close()

def main():
    # Set random seeds for reproducibility
    np.random.seed(RANDOM_SEED)
    tf.random.set_seed(RANDOM_SEED)
    
    print("Loading data from directory structure...")
    sequences, labels = load_data_from_directory(DATASET_PATH)
    
    print(f"Dataset loaded: {sequences.shape[0]} sequences, each with {sequences.shape[1]} timesteps and {sequences.shape[2]} features")
    print(f"Activity distribution: {np.bincount(labels)}")
    
    # Split data into training and testing sets
    train_sequences, test_sequences, train_labels, test_labels = train_test_split(
        sequences, labels, train_size=TRAIN_SIZE, random_state=RANDOM_SEED, stratify=labels
    )
    
    print("Normalizing sequences...")
    train_sequences_norm, test_sequences_norm = normalize_sequences(train_sequences, test_sequences)
    
    # Build model
    print("Building and compiling LSTM model...")
    model = build_lstm_model((SEQUENCE_LENGTH, sequences.shape[2]), len(ACTIVITIES))
    model.summary()
    
    # Callbacks for training
    callbacks = [
        # Early stopping to prevent overfitting
        EarlyStopping(
            monitor='val_loss',
            patience=15,
            restore_best_weights=True,
            verbose=1
        ),
        # Reduce learning rate when learning plateaus
        ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=8,
            min_lr=1e-6,
            verbose=1
        ),
        # Save best model during training
        ModelCheckpoint(
            'best_model.h5',
            monitor='val_accuracy',
            save_best_only=True,
            verbose=1
        )
    ]
    
    # Train model
    print("Training model...")
    history = model.fit(
        train_sequences_norm, train_labels,
        validation_split=VALIDATION_SIZE,
        epochs=MAX_EPOCHS,
        batch_size=BATCH_SIZE,
        callbacks=callbacks,
        verbose=1
    )
    
    # Evaluate final model
    print("Evaluating model on test data...")
    test_loss, test_accuracy = model.evaluate(test_sequences_norm, test_labels, verbose=1)
    print(f"Test accuracy: {test_accuracy:.4f}")
    
    # Plot training history
    plot_training_history(history)
    
    # Save final model
    model.save('activity_lstm_model.h5')
    print("Final model saved as 'activity_lstm_model.h5'")
    print("Best model (highest validation accuracy) saved as 'best_model.h5'")
    
    print("Training completed successfully!")

if __name__ == "__main__":
    main()