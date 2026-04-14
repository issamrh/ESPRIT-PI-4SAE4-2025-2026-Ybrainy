@echo off
echo Starting YBrainy Predict Service on port 5001...
set MODEL_PATH=C:\Users\hbaie\Downloads\ybrainy_model.pkl
py -3.12 app.py
pause
