import sqlite3
from pathlib import Path

# Always use the existing project database
DB_PATH = Path(__file__).resolve().parent.parent / "healthcare.db"


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def get_all_patients():
    """Return all patients as dictionaries."""
    with get_connection() as conn:
        rows = conn.execute("""
            SELECT *
            FROM patients
            ORDER BY id DESC
        """).fetchall()

        return [dict(row) for row in rows]


def add_patient(name, age, gender, phone):
    """Register a patient and return their ID."""
    with get_connection() as conn:
        cursor = conn.execute("""
            INSERT INTO patients
            (full_name, age, gender, phone)
            VALUES (?, ?, ?, ?)
        """, (name, age, gender, phone))

        return cursor.lastrowid


def get_patient(patient_id):
    """Return one patient as a dictionary."""
    with get_connection() as conn:
        row = conn.execute("""
            SELECT *
            FROM patients
            WHERE id = ?
        """, (patient_id,)).fetchone()

        return dict(row) if row else None


def add_vitals(
    patient_id,
    recorded_on,
    weight,
    height,
    systolic,
    diastolic,
    glucose,
    notes
):
    """Save a patient's health readings."""
    with get_connection() as conn:
        cursor = conn.execute("""
            INSERT INTO vitals (
                patient_id,
                recorded_on,
                weight_kg,
                height_cm,
                systolic_bp,
                diastolic_bp,
                glucose,
                notes
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            patient_id,
            recorded_on,
            weight,
            height,
            systolic,
            diastolic,
            glucose,
            notes
        ))

        return cursor.lastrowid


def get_patient_vitals(patient_id):
    """Return all readings for a particular patient."""
    with get_connection() as conn:
        rows = conn.execute("""
            SELECT *
            FROM vitals
            WHERE patient_id = ?
            ORDER BY recorded_on DESC, id DESC
        """, (patient_id,)).fetchall()

        return [dict(row) for row in rows]


def get_all_vitals():
    """Return all patient health readings."""
    with get_connection() as conn:
        rows = conn.execute("""
            SELECT
                v.*,
                p.full_name
            FROM vitals v
            JOIN patients p
                ON v.patient_id = p.id
            ORDER BY v.recorded_on DESC, v.id DESC
        """).fetchall()

        return [dict(row) for row in rows]


def delete_patient(patient_id):
    """Delete a patient and associated records."""
    with get_connection() as conn:
        cursor = conn.execute("""
            DELETE FROM patients
            WHERE id = ?
        """, (patient_id,))

        return cursor.rowcount > 0


def add_prediction(
    patient_id,
    model_name,
    risk_probability,
    risk_category
):
    """Save a prediction for a patient."""
    with get_connection() as conn:
        cursor = conn.execute("""
            INSERT INTO predictions (
                patient_id,
                model_name,
                risk_probability,
                risk_category
            )
            VALUES (?, ?, ?, ?)
        """, (
            patient_id,
            model_name,
            risk_probability,
            risk_category
        ))

        return cursor.lastrowid


def get_predictions(patient_id=None):
    """Retrieve prediction history."""
    with get_connection() as conn:
        if patient_id is None:
            rows = conn.execute("""
                SELECT *
                FROM predictions
                ORDER BY predicted_at DESC, id DESC
            """).fetchall()
        else:
            rows = conn.execute("""
                SELECT *
                FROM predictions
                WHERE patient_id = ?
                ORDER BY predicted_at DESC, id DESC
            """, (patient_id,)).fetchall()

        return [dict(row) for row in rows]