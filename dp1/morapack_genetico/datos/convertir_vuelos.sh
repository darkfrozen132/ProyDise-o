#!/bin/bash

# Script para convertir todos los vuelos del archivo TXT al CSV
echo "Convirtiendo 2866 vuelos del archivo TXT al CSV..."

# Crear el nuevo archivo CSV con cabecera
echo "Origen,Destino,HoraSalida,HoraLlegada,Capacidad" > vuelos_completos.csv

# Procesar cada línea del archivo TXT
while IFS='-' read -r origen destino hora_salida hora_llegada capacidad; do
    # Limpiar la capacidad (quitar el 0 del inicio)
    capacidad_limpia=$(echo $capacidad | sed 's/^0*//')
    
    # Agregar la línea al CSV
    echo "$origen,$destino,$hora_salida,$hora_llegada,$capacidad_limpia" >> vuelos_completos.csv
done < "c.1inf54.25.2.planes_vuelo.v4.20250818 (1).txt"

echo "¡Conversión completada! Archivo: vuelos_completos.csv"
echo "Total de vuelos convertidos: $(wc -l < vuelos_completos.csv | tr -d ' ')"
