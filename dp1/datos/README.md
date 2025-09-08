# Datos CSV para Sistema MoraPack

Este directorio contiene todos los archivos de datos en formato CSV para el sistema de logística MoraPack.

## 📁 Archivos Incluidos

- **`aeropuertos_simple.csv`** - Datos de aeropuertos con coordenadas geográficas
- **`vuelos_simple.csv`** - Información de vuelos disponibles
- **`pedidos_simulacion.csv`** - Pedidos generados para simulación (generado por Python)

## 🐍 Generador Python

### `generar_datos_csv.py`
Script en Python para generar y convertir datos a formato CSV limpio.

**Uso:**
```bash
cd datos/
python3 generar_datos_csv.py
```

**Funciones:**
- `generar_aeropuertos_csv()` - Crea archivo de aeropuertos con coordenadas
- `generar_vuelos_csv()` - Genera rutas de vuelos realistas  
- `generar_pedidos_csv(n)` - Crea n pedidos aleatorios para simulación

## 📊 Formato de Datos

### Aeropuertos
```csv
ICAO,Ciudad,Pais,Codigo,Huso,Capacidad,Latitud,Longitud
SPIM,Lima,Peru,lima,-5,440,-12.0219,-77.1144
```

### Vuelos
```csv
Origen,Destino,HoraSalida,HoraLlegada,Capacidad
SKBO,SEQM,03:34,05:21,300
```

### Pedidos
```csv
PedidoID,ClienteID,DestinoICAO,Cantidad,Prioridad,FechaCreacion
P001,Cliente_001,SKBO,15,1,2025-09-07 10:30:00
```

## 🔧 Requisitos

- Python 3.6+
- Módulos estándar: csv, random, math, datetime

## 📈 Características

- **Datos geográficos reales** con coordenadas precisas
- **Rutas de vuelos lógicas** basadas en proximidad continental
- **Pedidos aleatorios** con distribución realista de prioridades
- **Formato CSV estándar** compatible con Java y otros lenguajes
