# Proyecto DP1 - Sistema MoraPack con Algoritmos de Optimización

Sistema completo de optimización logística para MoraPack usando **Algoritmos Genéticos** y **Colonia de Hormigas**, organizado en una arquitectura modular de 4 componentes independientes.

## 🏗️ Arquitectura Modular

Este proyecto está dividido en **4 carpetas independientes**:

### 📊 **1. `datos/` - Gestión de Datos**
Manejo de datos y generación de archivos CSV
- **`generar_datos_csv.py`** - Script Python para generar datos limpios
- **`aeropuertos_simple.csv`** - Base de datos de aeropuertos
- **`vuelos_simple.csv`** - Base de datos de vuelos
- **`README.md`** - Documentación de datos

### 🧬 **2. `algoritmo_genetico/` - Framework Genético**
Framework de algoritmos genéticos reutilizable
- **Clases base**: `Individuo`, `Poblacion`, `AlgoritmoGenetico`
- **Operadores**: Cruce, mutación, selección
- **Independiente del dominio** - puede usarse para cualquier problema
- **`README.md`** - Documentación del framework

### 🐜 **3. `algoritmo_colonia/` - Framework Colonia de Hormigas**
Framework de Ant Colony Optimization (ACO) reutilizable
- **Clases base**: `Hormiga`, `Colonia`, `Feromona`, `Heuristica`
- **Algoritmo ACO**: Optimización basada en comportamiento de hormigas
- **Independiente del dominio** - aplicable a problemas de ruteo y asignación
- **`README.md`** - Documentación del framework

### 🚛 **4. `morapack/` - Aplicación MoraPack**
Implementación específica del problema logístico
- **Modelos de dominio**: Aeropuerto, Vuelo, Pedido
- **Optimizador específico** para logística con algoritmo genético
- **Scripts de compilación y ejecución**
- **`README.md`** - Documentación de la aplicación

## 🚀 Ejecución Rápida

```bash
# 1. Generar datos CSV actualizados
cd datos/
python generar_datos_csv.py

# 2. Compilar frameworks de optimización
cd ../algoritmo_genetico/
./compilar.sh

cd ../algoritmo_colonia/
./compilar.sh

# 3. Ejecutar optimización MoraPack
cd ../morapack/
./compilar.sh
./ejecutar.sh
```

## 📋 Problema MoraPack

**Empresa**: MoraPack (Logística Internacional)  
**Sedes**: Lima (Perú), Bruselas (Bélgica), Bakú (Azerbaiyán)  
**Objetivo**: Optimizar asignación de pedidos a sedes minimizando costos

### Restricciones del Negocio
- ⏱️ **Plazos**: 2 días mismo continente, 3 días intercontinental
- 💰 **Costos**: +50% para envíos intercontinentales
- 📦 **Prioridades**: Alta, media, baja (afectan costo)
- ⚖️ **Balance**: Distribución equitativa entre sedes

## 🧠 Algoritmos de Optimización

### 🧬 **Algoritmo Genético**
- **Inspiración**: Evolución natural y selección
- **Operadores**: Selección, cruce, mutación
- **Aplicación**: Asignación de pedidos a sedes
- **Ventajas**: Exploración amplia del espacio de soluciones

### 🐜 **Colonia de Hormigas (ACO)**
- **Inspiración**: Comportamiento de hormigas reales
- **Mecanismo**: Feromonas para marcar buenos caminos
- **Aplicación**: Ruteo y problemas de asignación
- **Ventajas**: Convergencia hacia óptimos, memoria distribuida

## 🎯 Beneficios de la Modularización

1. **🔄 Reutilización**: Frameworks funcionan para otros problemas
2. **🛠️ Mantenimiento**: Código organizado por responsabilidades
3. **📈 Escalabilidad**: Fácil agregar nuevas funcionalidades
4. **🔒 Independencia**: Cada módulo funciona autónomamente
5. **📚 Claridad**: Documentación específica por componente
6. **⚡ Flexibilidad**: Elegir algoritmo según el problema

## 📊 Resultados Esperados

El sistema típicamente logra:
- **Mejora de costos**: 30-70% vs asignación aleatoria
- **Convergencia**: ~1000 generaciones/iteraciones
- **Factibilidad**: 0 violaciones de restricciones
- **Balance**: Distribución equitativa entre sedes

## 🔧 Tecnologías

- **Java 8+** - Implementación principal
- **Python 3** - Generación de datos
- **CSV** - Formato de intercambio de datos
- **Bash** - Scripts de automatización

## 📁 Estructura Completa

```
dp1/
├── datos/                    # 📊 Gestión de datos
│   ├── generar_datos_csv.py
│   ├── aeropuertos_simple.csv
│   ├── vuelos_simple.csv
│   └── README.md
├── algoritmo_genetico/       # 🧬 Framework genético
│   ├── src/genetico/
│   │   ├── Individuo.java
│   │   ├── Poblacion.java
│   │   └── algoritmo/
│   ├── compilar.sh
│   └── README.md
├── algoritmo_colonia/        # 🐜 Framework colonia hormigas
│   ├── src/colonia/
│   │   ├── Hormiga.java
│   │   ├── Colonia.java
│   │   ├── Feromona.java
│   │   └── algoritmo/
│   ├── compilar.sh
│   └── README.md
├── morapack/                 # 🚛 Aplicación MoraPack
│   ├── src/morapack/
│   │   ├── modelo/
│   │   ├── datos/
│   │   ├── optimizacion/
│   │   └── main/
│   ├── compilar.sh
│   ├── ejecutar.sh
│   └── README.md
└── README.md                 # Este archivo
```

## 🔬 Investigación y Extensión

### Posibles Mejoras
- **Hibridación**: Combinar algoritmo genético con ACO
- **Paralelización**: Ejecutar algoritmos en paralelo
- **Aprendizaje**: Ajustar parámetros dinámicamente
- **Multiobjetivo**: Optimizar múltiples criterios simultáneamente

### Nuevas Aplicaciones
- **Ruteo de vehículos** con ACO
- **Scheduling** con algoritmos genéticos
- **Diseño de redes** con ambos algoritmos
- **Planificación de recursos** híbrida

---

**Desarrollado para el curso de Diseño de Programas 1**  
*Optimización logística con metaheurísticas bio-inspiradas*
