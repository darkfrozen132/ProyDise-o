# 🧬 MORAPACK - Sistema Genético

> **Sistema de Optimización de Rutas usando Algoritmos Genéticos**  
> Planificación inteligente de envíos con análisis de consolidación

---

## 📋 Características

✅ **Algoritmo Genético Avanzado** - Optimización evolutiva de rutas  
✅ **Planificación Temporal** - Gestión UTC y plazos continentales  
✅ **Análisis de Consolidación** - Reporte de vuelos con múltiples pedidos  
✅ **Salida Limpia** - Sin iconos, formato profesional  
✅ **Multiplataforma** - Compatible con Linux y Windows  

---

## 🚀 Ejecución Rápida

### **Linux/Mac:**
```bash
chmod +x ejecutar_genetico_sin_iconos.sh
./ejecutar_genetico_sin_iconos.sh
```

### **Windows:**
```cmd
ejecutar_genetico_sin_iconos.bat
```

### **Demo Independiente:**
```bash
# Linux/Mac
cd morapack_genetico
javac -d bin src/morapack/main/DemoGeneticoSinIconos.java
java -cp bin morapack.main.DemoGeneticoSinIconos

```

```cmd
REM Windows
cd morapack_genetico
javac -d bin src\morapack\main\DemoGeneticoSinIconos.java
java -cp bin morapack.main.DemoGeneticoSinIconos
```

---

## � Reporte de Consolidación

El sistema genera un **análisis detallado** de eficiencia operativa:

```
================ REPORTE DE CONSOLIDACION ================
VUELOS CON CONSOLIDACION DE PEDIDOS:
Vuelo               Pedidos  Paquetes  Capacidad  Eficiencia  Estado
-------------------------------------------------------------------
SLLP-SABE          13       336       346        97.1%       OPTIMO
SLLP-SCEL          12       307       307        100.0%      OPTIMO
SPIM-SEQM          12       354       330        107.3%      OPTIMO

============= ESTADISTICAS DE CONSOLIDACION =============
Total de vuelos utilizados: 21
Vuelos con multiples pedidos: 8
Tasa de consolidacion: 38.1%
Vuelos ahorrados por consolidacion: 55

================ BENEFICIOS OBTENIDOS ================
✓ Reduccion de vuelos: 55 vuelos menos (72.4% menos operaciones)
✓ Mejor utilizacion de capacidad de aeronaves
✓ Reduccion significativa de costos operativos  
✓ Menor impacto ambiental por menos vuelos
```

---

## 🗂️ Estructura del Proyecto

```
morapack_genetico/
├── 📁 src/morapack/
│   ├── 📁 main/              # Clases principales
│   │   ├── DemoGeneticoSinIconos.java
│   │   └── MainGeneticoSinIconos.java
│   ├── 📁 modelo/            # Modelos de datos
│   ├── � datos/             # Cargadores de datos
│   ├── � genetico/          # Algoritmo genético
│   └── � planificacion/     # Planificador temporal
├── � bin/                   # Archivos compilados
├── � datos/                 # Archivos CSV
├── 🔧 ejecutar_genetico_sin_iconos.sh    # Script Linux
├── 🔧 ejecutar_genetico_sin_iconos.bat   # Script Windows  
└── 📖 README.md              # Este archivo
```

---

## 💡 Interpretación de Resultados

| Eficiencia | Estado | Descripción |
|-----------|--------|-------------|
| **≥ 90%** | 🟢 ÓPTIMO | Excelente utilización de capacidad |
| **70-89%** | 🟡 BUENO | Buena utilización, margen de mejora |
| **50-69%** | 🟠 REGULAR | Utilización moderada |
| **< 50%** | 🔴 BAJO | Baja utilización, revisar asignación |

### **Tasa de Consolidación:**
- **≥ 40%**: 🟢 Excelente consolidación
- **25-39%**: 🟡 Buena consolidación  
- **15-24%**: 🟠 Regular consolidación
- **< 15%**: 🔴 Baja consolidación

---

## ⚙️ Requisitos del Sistema

- **Java JDK 8+** (OpenJDK recomendado)
- **4GB RAM** mínimo
- **Sistema Operativo:** Linux, Windows, macOS

### **Verificar Instalación:**
```bash
java -version
javac -version
```

---

## � Resolución de Problemas

### **Error: "Java no encontrado"**
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-11-jdk

# Windows
# Descargar e instalar desde: https://adoptium.net/
```

### **Error de Compilación:**
```bash
# Limpiar y recompilar
rm -rf bin && mkdir bin
javac -d bin src/morapack/main/DemoGeneticoSinIconos.java
```

### **Problema de Permisos (Linux):**
```bash
chmod +x *.sh
```

---

## 📈 Métricas de Rendimiento

| Métrica | Valor Típico | Descripción |
|---------|--------------|-------------|
| **Tasa de Éxito** | 95-98% | Pedidos procesados exitosamente |
| **Rutas Directas** | 70-75% | Porcentaje de rutas sin escalas |
| **Consolidación** | 25-40% | Vuelos con múltiples pedidos |
| **Tiempo Procesamiento** | < 2 seg | Para 80 pedidos promedio |

---

## 🔄 Actualizaciones Recientes

### **v2.1** - Sistema Sin Iconos + Consolidación
- ✅ Eliminación completa de iconos en salidas
- ✅ Reporte detallado de consolidación de vuelos
- ✅ Análisis de eficiencia operativa
- ✅ Métricas de ahorro y beneficios ambientales

### **v2.0** - Planificador Temporal Mejorado
- ✅ Gestión UTC integrada
- ✅ Plazos continentales automáticos
- ✅ Validación de aeropuertos en tiempo real

---

## 📞 Soporte

Para reportar problemas o sugerencias:
- 📧 Crear issue en el repositorio
- 📋 Incluir salida completa del error
- 💻 Especificar sistema operativo y versión de Java

---

**© 2025 MoraPack Project - Optimización Logística Inteligente**

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
