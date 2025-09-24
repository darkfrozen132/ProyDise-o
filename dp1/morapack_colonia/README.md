# MoraPack - Algoritmo Genético para Optimización de Rutas

## 🎯 Descripción
Sistema de optimización de rutas de entrega usando algoritmos genéticos para la empresa MoraPack. El sistema planifica rutas eficientes desde las sedes principales hacia destinos internacionales.

## 🏢 Sedes MoraPack
- **SPIM** - Lima, Perú (Sede principal Sudamérica)
- **EBCI** - Bruselas, Bélgica (Sede principal Europa)  
- **UBBB** - Bakú, Azerbaiyán (Sede principal Asia)

## 🚀 Ejecución Rápida
```bash
# Compilar
javac -d bin -cp bin src/morapack/main/MainRapido.java

# Ejecutar
java -cp bin morapack.main.MainRapido
```

## 📁 Estructura del Proyecto

### Core del Sistema
- **`MainRapido.java`** - Programa principal optimizado
- **`AlgoritmoGeneticoIntegrado.java`** - Algoritmo genético principal
- **`IndividuoIntegrado.java`** - Representación de soluciones
- **`PlanificadorAvanzadoEscalas.java`** - Planificador de rutas con escalas

### Datos y Modelos
- **`CargadorDatosCSV.java`** - Carga vuelos desde CSV
- **`CargadorPedidosSimple.java`** - Carga pedidos desde CSV
- **`Vuelo.java`** - Modelo de vuelo
- **`Pedido.java`** - Modelo de pedido
- **`RutaCompleta.java`** - Representación de rutas planificadas

### Optimización
- **`FuncionObjetivoOptimizada.java`** - Función de fitness optimizada

## 📊 Datos de Entrada
- **Vuelos:** `datos/vuelos_simple.csv` (2,866 vuelos)
- **Pedidos:** `datos/pedidos/pedidos_02.csv` (100 pedidos)

## 🎯 Resultados Típicos
- **Tiempo de ejecución:** ~2.2 segundos
- **Porcentaje de éxito:** ~94%
- **Rutas directas:** ~92
- **Rutas con escalas:** ~0-3
- **Sin planificar:** ~6

## 🧬 Parámetros del Algoritmo
- **Población:** 25 individuos
- **Generaciones:** 30
- **Cruce:** 80%
- **Mutación:** 10%
- **Máximo escalas:** 2
- **Máximo candidatos por escala:** 5

## ✅ Características
- ✅ Planificación de rutas directas y con escalas
- ✅ Filtrado automático de pedidos problemáticos
- ✅ Validación de capacidad de vuelos
- ✅ Optimización de tiempo y costo
- ✅ Sin rutas circulares
- ✅ Análisis detallado de resultados

## 🔧 Mantenimiento
El código ha sido limpiado eliminando:
- Archivos main obsoletos
- Algoritmos genéticos antiguos
- Directorios no utilizados (dao, service, test, demo)
- Funciones objetivo duplicadas
- Planificadores obsoletos
