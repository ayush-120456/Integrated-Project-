# ============================================================
# SMART HEALTHCARE SYSTEM
# Diabetes Risk Prediction - Model Training
# MCA Generative AI Project
# ============================================================

import pandas as pd
import numpy as np
import joblib
import json

from pathlib import Path

from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier

from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report
)

# ------------------------------------------------------------
# 1. PROJECT PATHS
# ------------------------------------------------------------

PROJECT_DIR = Path(__file__).resolve().parent.parent

DATA_PATH = PROJECT_DIR / "data" / "diabetes.csv"

ARTIFACT_DIR = PROJECT_DIR / "models" / "artifacts"

ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)

print("\nSMART HEALTHCARE - ML TRAINING")
print("=" * 50)

# ------------------------------------------------------------
# 2. LOAD DATASET
# ------------------------------------------------------------

print("\nLoading diabetes dataset...")

df = pd.read_csv(DATA_PATH)

print("Dataset loaded successfully!")

print("\nFirst five records:")
print(df.head())

print("\nDataset shape:")
print(df.shape)

print("\nDataset information:")
print(df.info())

print("\nMissing values:")
print(df.isnull().sum())

# ------------------------------------------------------------
# 3. DATA CLEANING
# ------------------------------------------------------------

print("\nCleaning dataset...")

# In this dataset, zero is an invalid placeholder
# for certain medical measurements.

invalid_zero_columns = [
    "Glucose",
    "BloodPressure",
    "SkinThickness",
    "Insulin",
    "BMI"
]

for column in invalid_zero_columns:
    df[column] = df[column].replace(0, np.nan)

print("Invalid zero values converted to missing values.")

print("\nMissing values after cleaning:")
print(df.isnull().sum())

# ------------------------------------------------------------
# 4. SEPARATE FEATURES AND TARGET
# ------------------------------------------------------------

X = df.drop("Outcome", axis=1)

y = df["Outcome"]

print("\nInput features:")
print(X.columns.tolist())

print("\nTarget distribution:")
print(y.value_counts())

# ------------------------------------------------------------
# 5. TRAIN-TEST SPLIT
# ------------------------------------------------------------

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y
)

print("\nTraining records:", len(X_train))
print("Testing records:", len(X_test))

# ------------------------------------------------------------
# 6. LOGISTIC REGRESSION MODEL
# ------------------------------------------------------------

print("\nTraining Logistic Regression...")

logistic_model = Pipeline([
    ("imputer", SimpleImputer(strategy="median")),
    ("scaler", StandardScaler()),
    ("classifier", LogisticRegression(max_iter=2000))
])

logistic_model.fit(X_train, y_train)

print("Logistic Regression training completed!")

# ------------------------------------------------------------
# 7. RANDOM FOREST MODEL
# ------------------------------------------------------------

print("\nTraining Random Forest...")

random_forest_model = Pipeline([
    ("imputer", SimpleImputer(strategy="median")),
    ("classifier", RandomForestClassifier(
        n_estimators=200,
        random_state=42,
        class_weight="balanced"
    ))
])

random_forest_model.fit(X_train, y_train)

print("Random Forest training completed!")

# ------------------------------------------------------------
# 8. MODEL EVALUATION
# ------------------------------------------------------------

def evaluate_model(model, model_name):

    predictions = model.predict(X_test)

    probabilities = model.predict_proba(X_test)[:, 1]

    accuracy = accuracy_score(y_test, predictions)

    precision = precision_score(
        y_test, predictions, zero_division=0
    )

    recall = recall_score(
        y_test, predictions, zero_division=0
    )

    f1 = f1_score(
        y_test, predictions, zero_division=0
    )

    auc = roc_auc_score(y_test, probabilities)

    matrix = confusion_matrix(y_test, predictions)

    print("\n" + "=" * 50)
    print(model_name.upper())
    print("=" * 50)

    print(f"Accuracy:  {accuracy:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall:    {recall:.4f}")
    print(f"F1 Score:  {f1:.4f}")
    print(f"ROC-AUC:   {auc:.4f}")

    print("\nConfusion Matrix:")
    print(matrix)

    print("\nClassification Report:")
    print(classification_report(
        y_test,
        predictions,
        zero_division=0
    ))

    return {
        "model": model_name,
        "accuracy": float(accuracy),
        "precision": float(precision),
        "recall": float(recall),
        "f1_score": float(f1),
        "roc_auc": float(auc),
        "confusion_matrix": matrix.tolist()
    }


# Evaluate both models

logistic_results = evaluate_model(
    logistic_model,
    "Logistic Regression"
)

forest_results = evaluate_model(
    random_forest_model,
    "Random Forest"
)

# ------------------------------------------------------------
# 9. SAVE TRAINED MODELS
# ------------------------------------------------------------

print("\nSaving trained models...")

joblib.dump(
    logistic_model,
    ARTIFACT_DIR / "diabetes_logistic_regression.joblib"
)

joblib.dump(
    random_forest_model,
    ARTIFACT_DIR / "diabetes_random_forest.joblib"
)

# ------------------------------------------------------------
# 10. SAVE EVALUATION RESULTS
# ------------------------------------------------------------

results = {
    "logistic_regression": logistic_results,
    "random_forest": forest_results,
    "features": X.columns.tolist(),
    "training_records": len(X_train),
    "testing_records": len(X_test)
}

with open(ARTIFACT_DIR / "model_results.json", "w") as file:
    json.dump(results, file, indent=4)

# ------------------------------------------------------------
# 11. FINAL OUTPUT
# ------------------------------------------------------------

print("\n" + "=" * 50)
print("MODEL TRAINING COMPLETED SUCCESSFULLY!")
print("=" * 50)

print("\nSaved files:")

for file in ARTIFACT_DIR.iterdir():
    print(file.name)

print("\nYour ML models are ready for integration.")
print("SMART HEALTHCARE PROJECT - COMPLETE TRAINING")