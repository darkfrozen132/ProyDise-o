#!/usr/bin/env python3
"""
Script simple para convertir datos MoraPack a CSV
"""

import re
import csv
import os

def convertir_dms_a_decimal(coord_dms):
    """Convierte DMS a decimal"""
    if not coord_dms or coord_dms.strip() == "":
        return "0.0"
    
    try:
        patron = r'(\d+)°\s*(\d+)\'\s*(\d+)"\s*([NSEW])'
        match = re.search(patron, coord_dms)
        
        if not match:
            return "0.0"
        
        grados = int(match.group(1))
        minutos = int(match.group(2))
        segundos = int(match.group(3))
        direccion = match.group(4)
        
        decimal = grados + (minutos / 60.0) + (segundos / 3600.0)
        
        if direccion in ['S', 'W']:
            decimal = -decimal
        
        return f"{decimal:.6f}"
        
    except Exception as e:
        print(f"Error convirtiendo '{coord_dms}': {e}")
        return "0.0"

print("🔄 CONVERTIDOR DE DATOS MORAPACK A CSV")
print("=" * 50)

# 1. AEROPUERTOS (detección dinámica de continente)
archivo_aeropuertos = "c.1inf54.25.2.Aeropuerto.husos.v1.20250818__estudiantes (2).txt"
if os.path.exists(archivo_aeropuertos):
    print("📍 Procesando aeropuertos (detección de continente)...")
    try:
        with open(archivo_aeropuertos, 'r', encoding='utf-16') as f:
            lineas = f.readlines()
        print(f"  ✓ Archivo leído: {len(lineas)} líneas")

        aeropuertos = []
        contador = 1
        continente_actual = None

        # Patrones para encabezados de continente
        patron_continente = re.compile(r'(america\s+del\s+sur|europa|asia)', re.IGNORECASE)
        patron_aeropuerto = re.compile(r'^(\d+)\s+([A-Z]{4})\s+(.+?)\s+([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)\s+([a-záéíóúñ]+)\s+([+\-]?\d+)\s+(\d+)\s+Latitude:\s*(.+?)\s+Longitude:\s*(.+)$')

        for linea in lineas:
            cruda = linea
            linea = linea.strip()
            if not linea:
                continue

            # Detectar encabezado de continente (línea sin dígitos iniciales de aeropuerto)
            if patron_continente.search(linea) and not linea[:3].strip().isdigit():
                encontrado = patron_continente.search(linea).group(1).lower()
                if 'america' in encontrado:
                    continente_actual = 'America_del_Sur'
                elif 'europa' in encontrado:
                    continente_actual = 'Europa'
                elif 'asia' in encontrado:
                    continente_actual = 'Asia'
                print(f"  ↪ Detectado encabezado de continente: {continente_actual}")
                continue

            match = patron_aeropuerto.match(linea)
            if not match:
                continue

            _, codigo, ciudad, pais, codigo_ciudad, huso, capacidad, lat_original, lon_original = match.groups()

            # Limpieza adicional (acentos mal codificados podrían aparecer)
            ciudad = ciudad.replace('�', 'o')
            pais = pais.replace('�', 'n')

            lat_decimal = convertir_dms_a_decimal(lat_original)
            lon_decimal = convertir_dms_a_decimal(lon_original)

            continente = continente_actual or 'America_del_Sur'

            aeropuerto = {
                'Numero': f"{contador:02d}",
                'CodigoICAO': codigo,
                'Ciudad': ciudad,
                'Pais': pais,
                'CodigoCiudad': codigo_ciudad,
                'HusoHorario': huso,
                'Capacidad': capacidad,
                'Latitud': lat_decimal,
                'Longitud': lon_decimal,
                'Continente': continente
            }
            aeropuertos.append(aeropuerto)
            print(f"  ✓ {codigo} [{continente}] - {ciudad} {pais} ({lat_decimal}, {lon_decimal})")
            contador += 1

        with open('aeropuertos_completo.csv', 'w', newline='', encoding='utf-8') as csvfile:
            fieldnames = ['Numero', 'CodigoICAO', 'Ciudad', 'Pais', 'CodigoCiudad', 'HusoHorario', 'Capacidad', 'Latitud', 'Longitud', 'Continente']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(aeropuertos)

        print(f"✅ Aeropuertos: {len(aeropuertos)} registros → aeropuertos_completo.csv")
    except Exception as e:
        print(f"❌ Error procesando aeropuertos: {e}")

# 2. PLANES DE VUELO
archivo_vuelos = "c.1inf54.25.2.planes_vuelo.v4.20250818 (1).txt"
if os.path.exists(archivo_vuelos):
    print(f"\n✈️  Procesando planes de vuelo...")
    
    try:
        with open(archivo_vuelos, 'r', encoding='utf-8') as f:
            lineas = f.readlines()
        print(f"  ✓ Archivo leído: {len(lineas)} líneas")
        
        vuelos = []
        
        for linea in lineas:
            linea = linea.strip()
            if not linea or linea.startswith('#'):
                continue
            
            partes = linea.split('-')
            if len(partes) >= 5:
                origen = partes[0].strip()
                destino = partes[1].strip()
                salida_raw = partes[2].strip()
                llegada_raw = partes[3].strip()
                capacidad_raw = partes[4].strip()
                
                # Formatear hora
                def formatear_hora(hora_raw):
                    hora = re.sub(r'[^\d:]', '', hora_raw)
                    if ':' not in hora and len(hora) == 4:
                        return f"{hora[:2]}:{hora[2:]}"
                    return hora if len(hora) == 5 else "00:00"
                
                salida = formatear_hora(salida_raw)
                llegada = formatear_hora(llegada_raw)
                
                capacidad = re.sub(r'[^\d]', '', capacidad_raw) or "300"
                
                vuelo = {
                    'Origen': origen,
                    'Destino': destino,
                    'HoraSalida': salida,
                    'HoraLlegada': llegada,
                    'Capacidad': capacidad
                }
                
                vuelos.append(vuelo)
                if len(vuelos) <= 10:  # Solo mostrar primeros 10
                    print(f"  ✓ {origen}-{destino} {salida}-{llegada} Cap:{capacidad}")
        
        # Escribir CSV vuelos (SIN IdPlan)
        with open('planes_vuelo_completo.csv', 'w', newline='', encoding='utf-8') as csvfile:
            fieldnames = ['Origen', 'Destino', 'HoraSalida', 'HoraLlegada', 'Capacidad']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(vuelos)
        
        print(f"✅ Planes de vuelo: {len(vuelos)} registros → planes_vuelo_completo.csv")
        
    except Exception as e:
        print(f"❌ Error procesando vuelos: {e}")

# 3. PEDIDOS
archivo_pedidos = "pedidoUltrafinal.txt"
if os.path.exists(archivo_pedidos):
    print(f"\n📦 Procesando pedidos...")
    
    try:
        with open(archivo_pedidos, 'r', encoding='utf-8') as f:
            lineas = f.readlines()
        print(f"  ✓ Archivo leído: {len(lineas)} líneas")
        
        pedidos = []
        
        for linea in lineas:
            linea = linea.strip()
            if not linea or linea.startswith('#'):
                continue
            
            pedido_id = linea.strip()
            
            patron = r'^(\d{2})-(\d{2})-(\d{2})-([A-Z]{4})-(\d{3,4})-(\d{7})$'
            match = re.match(patron, pedido_id)
            
            if match:
                dia = match.group(1)
                hora = match.group(2) 
                minuto = match.group(3)
                destino = match.group(4)
                cantidad = match.group(5)
                cliente_id = match.group(6)
                
                # Generar registro sin columna IdPedido
                pedido = {
                    'Dia': dia,
                    'Hora': hora,
                    'Minuto': minuto,
                    'AeropuertoDestino': destino,
                    'Cantidad': cantidad,
                    'ClienteId': cliente_id
                }
                
                pedidos.append(pedido)
                if len(pedidos) <= 10:  # Solo mostrar primeros 10
                    print(f"  ✓ {pedido_id} → {destino} ({cantidad})")
        
        # Escribir CSV pedidos
        with open('pedidos_completo.csv', 'w', newline='', encoding='utf-8') as csvfile:
            fieldnames = ['Dia', 'Hora', 'Minuto', 'AeropuertoDestino', 'Cantidad', 'ClienteId']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(pedidos)
        
        print(f"✅ Pedidos: {len(pedidos)} registros → pedidos_completo.csv")
        
    except Exception as e:
        print(f"❌ Error procesando pedidos: {e}")

print("\n" + "=" * 50)
print("✅ CONVERSIÓN COMPLETADA")
print("📁 Archivos generados:")
for archivo in ['aeropuertos_completo.csv', 'planes_vuelo_completo.csv', 'pedidos_completo.csv']:
    if os.path.exists(archivo):
        tamano = os.path.getsize(archivo)
        print(f"   • {archivo} ({tamano:,} bytes)")

print("\n🎯 Uso: java MainFinal aeropuertos_completo.csv planes_vuelo_completo.csv pedidos_completo.csv")
