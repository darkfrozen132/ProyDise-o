# 🧬 MORAPACK - Configuración del Sistema

## 🎯 Configuración de Ejecución

### Sistema Operativo Detectado
- **Linux**: ✅ Usar `./ejecutar_genetico_sin_iconos.sh`
- **Windows**: ✅ Usar `ejecutar_genetico_sin_iconos.bat`
- **macOS**: ✅ Usar `./ejecutar_genetico_sin_iconos.sh`

---

## ⚙️ Parámetros del Sistema

### **Algoritmo Genético:**
```yaml
poblacion: 50          # Individuos por generación
generaciones: 100      # Número de iteraciones
tasa_cruce: 0.8       # 80% probabilidad de cruce
tasa_mutacion: 0.1    # 10% probabilidad de mutación
elitismo: true        # Mantener mejores individuos
```

### **Planificación de Rutas:**
```yaml
max_escalas: 2        # Máximo 2 escalas por ruta
tiempo_escala: 60min  # Tiempo mínimo en aeropuerto
ventana_tiempo: 24h   # Ventana de búsqueda
zona_horaria: UTC     # Gestión de tiempo unificada
```

### **Datos de Entrada:**
```yaml
vuelos: "datos/vuelos_simple.csv"
pedidos: "datos/pedidos_02.csv"  
aeropuertos: "datos/aeropuertos_simple.csv"
```

---

## 🎨 Configuración de Salida

### **Formato de Reporte:**
- ✅ **Sin iconos** - Solo texto plano
- ✅ **Tablas alineadas** - Formato profesional
- ✅ **Colores deshabilitados** - Compatible con todos los terminales
- ✅ **Progreso numérico** - Sin barras de progreso

### **Métricas Incluidas:**
- 📊 Tasa de éxito de planificación
- 🚁 Rutas directas vs con escalas  
- 📦 Análisis de consolidación de vuelos
- ⏱️ Tiempo de procesamiento
- 💰 Cálculo de eficiencia operativa

---

## 🔧 Resolución de Problemas Comunes

### **Java no encontrado:**
```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk

# CentOS/RHEL  
sudo yum install java-11-openjdk-devel

# Windows
# Descargar desde: https://adoptium.net/
```

### **Permisos en Linux:**
```bash
chmod +x *.sh
```

### **Compilación fallida:**
```bash
# Limpiar y recompilar
rm -rf bin
mkdir bin
javac -d bin src/morapack/main/DemoGeneticoSinIconos.java
```

### **Archivos CSV no encontrados:**
- Verificar que exista la carpeta `datos/`
- Comprobar nombres exactos de archivos
- La demo funciona sin archivos CSV externos

---

## 📈 Interpretación de Métricas

### **Eficiencia de Consolidación:**
| Rango | Estado | Acción Recomendada |
|-------|--------|-------------------|
| ≥ 40% | 🟢 Excelente | Mantener estrategia actual |
| 25-39% | 🟡 Buena | Optimizar rutas populares |
| 15-24% | 🟠 Regular | Revisar algoritmo de asignación |
| < 15% | 🔴 Baja | Reestructurar planificación |

### **Utilización de Capacidad:**
| Rango | Estado | Descripción |
|-------|--------|-------------|
| ≥ 90% | 🟢 Óptimo | Máximo aprovechamiento |
| 70-89% | 🟡 Bueno | Ligero margen de mejora |
| 50-69% | 🟠 Regular | Capacidad subutilizada |
| < 50% | 🔴 Bajo | Ineficiencia crítica |

---

## 📝 Registro de Cambios

### **v2.1.0** - Sistema Sin Iconos + Consolidación
- ✅ Eliminación completa de iconos/emoticons
- ✅ Reporte detallado de consolidación
- ✅ Métricas de eficiencia operativa
- ✅ Scripts mejorados para múltiples plataformas

### **v2.0.0** - Planificador Mejorado  
- ✅ Gestión UTC integrada
- ✅ Validación en tiempo real
- ✅ Optimización de memoria

### **v1.0.0** - Versión Base
- ✅ Algoritmo genético funcional
- ✅ Carga de datos CSV
- ✅ Planificación básica

---

**🏢 MoraPack Systems - Logística Inteligente 2025**
