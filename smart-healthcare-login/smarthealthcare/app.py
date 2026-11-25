import streamlit as st
import pandas as pd
import plotly.express as px
import joblib
from pathlib import Path

from datetime import date

from auth import JAVA_LOGOUT_URL, require_java_login
from database.db import (
    get_all_patients,
    add_patient,
    get_patient,
    add_vitals,
    get_patient_vitals,
    get_all_vitals,
    delete_patient,
    add_prediction,
    get_predictions
)


# --------------------------------------------------
# PAGE CONFIGURATION
# --------------------------------------------------

st.set_page_config(
    page_title="Smart Healthcare System",
    page_icon="🏥",
    layout="wide"
)


# --------------------------------------------------
# CUSTOM DESIGN
# --------------------------------------------------

st.markdown("""
<style>

/* Main application background */
.stApp {
    background-color: #f5f7fb;
}

/* Main content spacing */
.block-container {
    padding-top: 2rem;
}

/* Headings */
h1, h2, h3 {
    color: #183153 !important;
}

/* Metric cards */
div[data-testid="stMetric"] {
    background-color: #ffffff !important;
    padding: 20px !important;
    border-radius: 14px !important;
    border: 1px solid #dce4ef !important;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

/* Metric labels */
div[data-testid="stMetricLabel"],
div[data-testid="stMetricLabel"] p {
    color: #475569 !important;
    opacity: 1 !important;
}

/* Metric values */
div[data-testid="stMetricValue"] {
    color: #183153 !important;
}

/* Metric delta text */
div[data-testid="stMetricDelta"] {
    color: #334155 !important;
}

/* Sidebar */
section[data-testid="stSidebar"] {
    background-color: #eaf0f8;
}

/* General text */
.stMarkdown p {
    color: #334155;
}

/* Dataframes */
div[data-testid="stDataFrame"] {
    border: 1px solid #dce4ef;
    border-radius: 10px;
}
/* ===== GLOBAL TEXT VISIBILITY FIX ===== */

/* Main application text */
.stApp,
.stApp p,
.stApp label,
.stApp span {
    color: #263445;
}

/* Sidebar text and navigation */
section[data-testid="stSidebar"] * {
    color: #263445 !important;
}

/* Sidebar headings */
section[data-testid="stSidebar"] h1,
section[data-testid="stSidebar"] h2,
section[data-testid="stSidebar"] h3 {
    color: #183153 !important;
}

/* Radio buttons and navigation labels */
div[data-testid="stRadio"] label,
div[data-testid="stRadio"] label p,
div[data-testid="stRadio"] label span {
    color: #263445 !important;
    opacity: 1 !important;
}

/* Metric card labels */
div[data-testid="stMetricLabel"],
div[data-testid="stMetricLabel"] *,
div[data-testid="stMetricValue"],
div[data-testid="stMetricValue"] * {
    color: #183153 !important;
    opacity: 1 !important;
}

/* Main headings */
.stApp h1,
.stApp h2,
.stApp h3 {
    color: #183153 !important;
}

/* Normal text and captions */
.stApp [data-testid="stCaptionContainer"],
.stApp [data-testid="stMarkdownContainer"] p {
    color: #334155 !important;
}

/* Input labels */
.stApp [data-testid="stWidgetLabel"],
.stApp [data-testid="stWidgetLabel"] p {
    color: #263445 !important;
    opacity: 1 !important;
}
</style>
""", unsafe_allow_html=True)


# --------------------------------------------------
# AUTHENTICATION (HANDLED BY JAVA LOGIN PAGE)
# --------------------------------------------------

current_user = require_java_login()


# --------------------------------------------------
# SIDEBAR
# --------------------------------------------------

st.sidebar.title("🏥 Smart Healthcare")
st.sidebar.caption("Patient Risk & Analytics System")

if current_user.get("mode") == "join":
    st.sidebar.success(f"Session: {current_user.get('code')} ({current_user.get('name')})")
else:
    st.sidebar.success(f"{current_user.get('name')} ({current_user.get('role')})")

if st.sidebar.button("Sign Out", use_container_width=True):
    st.session_state.pop("smarthealth_auth", None)
    st.markdown(f'<meta http-equiv="refresh" content="0;url={JAVA_LOGOUT_URL}">', unsafe_allow_html=True)
    st.stop()

st.sidebar.divider()

page = st.sidebar.radio(
    "Navigation",
    [
        "Dashboard",
        "Register Patient",
        "Record Health Data",
        "Patient Records",
        "Analytics",
        "Diabetes risk prediction"
    ]
)

st.sidebar.divider()
st.sidebar.info(
    "Academic demonstration project. "
    "Risk indicators are not medical diagnoses."
)


# --------------------------------------------------
# HELPER FUNCTIONS
# --------------------------------------------------

def calculate_bmi(weight, height):
    if not weight or not height:
        return None

    height_m = height / 100

    if height_m <= 0:
        return None

    return round(weight / (height_m ** 2), 2)


def assess_demo_risk(bp_sys, bp_dia, glucose):
    """
    Simple demonstration rules.
    These are not clinically validated predictions.
    """

    score = 0

    if bp_sys is not None and bp_sys >= 140:
        score += 1

    if bp_dia is not None and bp_dia >= 90:
        score += 1

    if glucose is not None and glucose >= 200:
        score += 1

    if score == 0:
        return "Lower indicator", score

    elif score == 1:
        return "Moderate indicator", score

    return "Higher indicator", score


def get_latest_reading(patient_id):
    readings = get_patient_vitals(patient_id)

    if readings:
        return readings[0]

    return None


# --------------------------------------------------
# DASHBOARD
# --------------------------------------------------

if page == "Dashboard":

    st.title("🏥 Smart Healthcare Dashboard")

    st.write(
        "Monitor patient information, health records "
        "and healthcare analytics."
    )

    patients = get_all_patients()
    vitals = get_all_vitals()

    total_patients = len(patients)
    total_readings = len(vitals)

    bmi_values = []

    for reading in vitals:
        bmi = calculate_bmi(
            reading["weight_kg"],
            reading["height_cm"]
        )

        if bmi is not None:
            bmi_values.append(bmi)

    avg_bmi = (
        round(sum(bmi_values) / len(bmi_values), 2)
        if bmi_values else 0
    )

    col1, col2, col3 = st.columns(3)

    col1.metric("Total Patients", total_patients)
    col2.metric("Health Records", total_readings)
    col3.metric("Average Recorded BMI", avg_bmi)

    st.divider()

    st.subheader("Recent Patient Registrations")

    if patients:
        patient_df = pd.DataFrame(patients)

        st.dataframe(
            patient_df[
                ["id", "full_name", "age", "gender", "phone"]
            ],
            use_container_width=True,
            hide_index=True
        )

    else:
        st.info("No patients registered yet.")

    st.subheader("Recent Health Readings")

    if vitals:
        readings_df = pd.DataFrame(vitals)
        

        st.dataframe(
            readings_df[
                [
                    "full_name",
                    "recorded_on",
                    "systolic_bp",
                    "diastolic_bp",
                    "glucose"
                ]
            ].head(10),
            use_container_width=True,
            hide_index=True
        )

    else:
        st.info("No health readings available yet.")


# --------------------------------------------------
# REGISTER PATIENT
# --------------------------------------------------

elif page == "Register Patient":

    st.title("👤 Patient Registration")

    st.write("Enter the patient's basic information.")

    with st.form("patient_registration"):

        name = st.text_input("Full Name")

        age = st.number_input(
            "Age",
            min_value=1,
            max_value=120,
            value=25
        )

        gender = st.selectbox(
            "Gender",
            ["Male", "Female", "Other"]
        )

        phone = st.text_input("Phone Number")

        submitted = st.form_submit_button(
            "Register Patient",
            use_container_width=True
        )

        if submitted:

            if not name.strip():
                st.error("Please enter the patient's name.")

            else:

                patient_id = add_patient(
                    name.strip(),
                    int(age),
                    gender,
                    phone.strip()
                )

                st.success(
                    f"Patient registered successfully! "
                    f"Patient ID: {patient_id}"
                )


# --------------------------------------------------
# RECORD HEALTH DATA
# --------------------------------------------------

elif page == "Record Health Data":

    st.title("🩺 Record Patient Health Data")

    patients = get_all_patients()

    if not patients:

        st.warning(
            "Please register a patient before recording health data."
        )

    else:

        patient_options = {
            f'{p["id"]} - {p["full_name"]}': p["id"]
            for p in patients
        }

        selected_patient = st.selectbox(
            "Select Patient",
            list(patient_options.keys())
        )

        patient_id = patient_options[selected_patient]

        st.divider()

        with st.form("health_data_form"):

            col1, col2 = st.columns(2)
           # Select registered patient
            patients = get_all_patients()

            
    if patients:
        selected_patient = st.selectbox(
            "Select Registered Patient",
            options=patients,
            format_func=lambda p: f"{p['full_name']} (ID: {p['id']})"
        )

        patient_id = selected_patient["id"]

    else:
        st.warning("Please register a patient first.")
        patient_id = None

            
    with col1:
            recorded_on = st.date_input(
                "Recording Date",
                value=date.today()
            )

            weight = st.number_input(
                "Weight (kg)",
                min_value=0.0,
                max_value=300.0,
                value=70.0
            )

            height = st.number_input(
                "Height (cm)",
                min_value=0.0,
                max_value=250.0,
                value=170.0
            )

            systolic = st.number_input(
                "Systolic Blood Pressure (mmHg)",
                min_value=0,
                max_value=300,
                value=120
            )


            with col2:

                diastolic = st.number_input(
                    "Diastolic Blood Pressure (mmHg)",
                    min_value=0,
                    max_value=200,
                    value=80
                )

                glucose = st.number_input(
                    "Blood Glucose (mg/dL)",
                    min_value=0.0,
                    max_value=1000.0,
                    value=100.0
                )

                notes = st.text_area(
                    "Additional Notes"
                )

            submitted = st.form_submit_button(
                "Save Health Record",
                use_container_width=True
            )

            if submitted:

                if (
                    weight <= 0
                    or height <= 0
                    or systolic <= 0
                    or diastolic <= 0
                    or glucose <= 0
                ):
                    st.error(
                        "Please enter valid positive health measurements."
                    )

                elif systolic <= diastolic:
                    st.error(
                        "Systolic pressure must be greater than diastolic pressure."
                    )

                else:

                    add_vitals(
                        patient_id,
                        recorded_on.isoformat(),
                        weight,
                        height,
                        systolic,
                        diastolic,
                        glucose,
                        notes
                    )

                    st.success("Health record saved successfully!")


# --------------------------------------------------
# PATIENT RECORDS
# --------------------------------------------------

elif page == "Patient Records":

    st.title("📋 Patient Records")

    patients = get_all_patients()

    if not patients:

        st.info("No patient records available.")

    else:

        patient_options = {
            f'{p["id"]} - {p["full_name"]}': p["id"]
            for p in patients
        }

        selected = st.selectbox(
            "Select Patient",
            list(patient_options.keys())
        )

        patient_id = patient_options[selected]

        patient = get_patient(patient_id)

        st.subheader("Patient Information")

        col1, col2, col3 = st.columns(3)

        col1.metric("Patient ID", patient["id"])
        col2.metric("Age", patient["age"])
        col3.metric("Gender", patient["gender"])

        st.write("**Name:**", patient["full_name"])
        st.write("**Phone:**", patient["phone"] or "Not provided")

        st.divider()

        st.subheader("Health History")

        readings = get_patient_vitals(patient_id)

        if readings:

            df = pd.DataFrame(readings)

            st.dataframe(
                df,
                use_container_width=True,
                hide_index=True
            )

            latest = readings[0]

            bmi = calculate_bmi(
                latest["weight_kg"],
                latest["height_cm"]
            )

            if bmi is not None:
                st.metric("Latest Recorded BMI", bmi)

            category, score = assess_demo_risk(
                latest["systolic_bp"],
                latest["diastolic_bp"],
                latest["glucose"]
            )

            st.write("**Demonstration indicator:**", category)

            st.caption(
                "This rule-based indicator is for demonstration only "
                "and is not a medical diagnosis."
            )

        else:

            st.info("No health readings for this patient yet.")

        st.divider()

        st.subheader("Delete Patient")

        st.warning(
            "Deleting a patient also deletes their associated "
            "health readings and saved predictions."
        )

        confirm = st.checkbox(
            "I confirm that I want to delete this patient."
        )

        if st.button(
            "Delete Patient",
            disabled=not confirm
        ):

            delete_patient(patient_id)

            st.success("Patient deleted successfully.")

            st.rerun()


# --------------------------------------------------
# ANALYTICS
# --------------------------------------------------

elif page == "Analytics":

    st.title("📊 Patient Analytics")

    vitals = get_all_vitals()

    if not vitals:

        st.info(
            "Register patients and record health readings "
            "to generate analytics."
        )

    else:

        df = pd.DataFrame(vitals)

        st.write("Total health records:", len(df))

        st.write("Health records preview:")
        st.dataframe(df)

        df["BMI"] = df.apply(
            lambda row: calculate_bmi(
                row["weight_kg"],
                row["height_cm"]
            ),
            axis=1
        )

        st.subheader("Blood Pressure Analysis")

        bp_df = df.dropna(
            subset=["systolic_bp", "diastolic_bp"]
        )

        if not bp_df.empty:

            fig = px.scatter(
                bp_df,
                x="systolic_bp",
                y="diastolic_bp",
                color="full_name",
                hover_data=["recorded_on"],
                title="Patient Blood Pressure Readings"
            )

            st.plotly_chart(
                fig,
                use_container_width=True
            )

        st.subheader("Blood Glucose Distribution")

        glucose_df = df.dropna(subset=["glucose"])

        if not glucose_df.empty:

            fig = px.histogram(
                glucose_df,
                x="glucose",
                nbins=15,
                title="Blood Glucose Distribution"
            )

            st.plotly_chart(
                fig,
                use_container_width=True
            )

        st.subheader("BMI Analysis")

        bmi_df = df.dropna(subset=["BMI"])

        if not bmi_df.empty:

            fig = px.histogram(
                bmi_df,
                x="BMI",
                nbins=15,
                title="BMI Distribution"
            )

            st.plotly_chart(
                fig,
                use_container_width=True
            )

        st.subheader("Patient Health Data")

        st.dataframe(
            df,
            use_container_width=True,
            hide_index=True
        )
# ==========================================
    # DIABETES RISK PREDICTION
    # ==========================================
elif page == "Diabetes risk prediction":

        st.title("🩺 Diabetes Risk Prediction")
        st.write(
            "Enter patient health parameters to estimate diabetes risk."
        )

        st.divider()

        # Load trained Random Forest model
        MODEL_PATH = (
            Path(__file__).resolve().parent
            / "models"
            / "artifacts"
            / "diabetes_random_forest.joblib"
        )

        @st.cache_resource
        def load_diabetes_model():
            return joblib.load(MODEL_PATH)

        try:
            model = load_diabetes_model()
        except Exception as e:
            st.error(f"Could not load prediction model: {e}")
            st.stop()

        st.subheader("Patient Health Parameters")

        with st.form("diabetes_prediction_form"):

            col1, col2 = st.columns(2)

            with col1:
                pregnancies = st.number_input(
                    "Pregnancies",
                    min_value=0,
                    max_value=20,
                    value=0
                )

                glucose = st.number_input(
                    "Glucose Level (mg/dL)",
                    min_value=0,
                    max_value=300,
                    value=120
                )

                blood_pressure = st.number_input(
                    "Blood Pressure (mm Hg)",
                    min_value=0,
                    max_value=200,
                    value=70
                )

                skin_thickness = st.number_input(
                    "Skin Thickness (mm)",
                    min_value=0,
                    max_value=100,
                    value=20
                )

            with col2:
                insulin = st.number_input(
                    "Insulin (mu U/ml)",
                    min_value=0,
                    max_value=900,
                    value=79
                )

                bmi = st.number_input(
                    "BMI",
                    min_value=0.0,
                    max_value=70.0,
                    value=25.0
                )

                diabetes_pedigree = st.number_input(
                    "Diabetes Pedigree Function",
                    min_value=0.0,
                    max_value=3.0,
                    value=0.5,
                    step=0.01
                )

                age = st.number_input(
                    "Age (years)",
                    min_value=1,
                    max_value=120,
                    value=30
                )

            submitted = st.form_submit_button(
                "Predict Diabetes Risk",
                use_container_width=True
            )

        if submitted:

            input_data = pd.DataFrame([[
                pregnancies,
                glucose,
                blood_pressure,
                skin_thickness,
                insulin,
                bmi,
                diabetes_pedigree,
                age
            ]], columns=[
                "Pregnancies",
                "Glucose",
                "BloodPressure",
                "SkinThickness",
                "Insulin",
                "BMI",
                "DiabetesPedigreeFunction",
                "Age"
            ])

            prediction = model.predict(input_data)[0]
            probability = model.predict_proba(input_data)[0][1]

            st.divider()
            st.subheader("Prediction Results")

            st.metric(
                "Estimated Diabetes Probability",
                f"{probability * 100:.2f}%"
            )

            st.progress(float(probability))

            if prediction == 1:
                st.error(
                    "Elevated diabetes risk indicator"
                )
            else:
                st.success(
                    "Lower diabetes risk indicator"
                )

            st.caption(
                "This is an academic machine-learning prediction, "
                "not a medical diagnosis. Consult a healthcare "
                "professional for clinical assessment."
            )

# --------------------------------------------------
# FOOTER
# --------------------------------------------------

st.sidebar.divider()

st.sidebar.caption(
    "Smart Healthcare System | MCA Project"
)