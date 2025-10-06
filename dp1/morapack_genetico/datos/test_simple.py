#!/usr/bin/env python3
import re
import csv
import os

print("Iniciando script...")

# Test básico
print("Verificando archivos...")

archivos_aeropuertos = [
    "c.1inf54.25.2.Aeropuerto.husos.v1.20250818__estudiantes (2).txt",
]

for archivo in archivos_aeropuertos:
    if os.path.exists(archivo):
        print(f"✓ Encontrado: {archivo}")
    else:
        print(f"✗ No encontrado: {archivo}")

print("Script completado.")
