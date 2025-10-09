# 🪟 MoraPack para Windows

## 🎯 Sistemas Disponibles

### 🐜 **Sistema ACO (Ant Colony Optimization)**
Algoritmo de Colonia de Hormigas que optimiza **TODAS** las rutas.

### 🧬 **Sistema Genético**
Algoritmo Genético para comparación y validación.

## 📋 **Requisitos**
- ✅ **Java 8 o superior** instalado
- ✅ **Variables de entorno** configuradas (JAVA_HOME, PATH)
- ✅ **Windows 10/11** o superior

## 🚀 **Instrucciones de Uso**

### **Opción 1: Ejecutar Sistema ACO**
```batch
# Abrir Command Prompt (cmd) o PowerShell
cd C:\ruta\a\tu\proyecto\morapack_coloniav2

# Ejecutar script
ejecutar_aco_windows.bat
```

### **Opción 2: Ejecutar Sistema Genético**
```batch
# Desde el mismo directorio
ejecutar_genetico_windows.bat
```

### **Opción 3: Demostración ACO Simplificada**
```batch
# Compilar y ejecutar demo
javac ACOWindows.java
java ACOWindows
```

## 📊 **Resultados Esperados**

### 🐜 **ACO Corregido**
- 📦 **Pedidos procesados**: 211
- 🐜 **Rutas optimizadas por ACO**: 211 (100%)
- ✅ **Éxito**: ~96-100%
- 🔄 **Escalas**: ~28% (similar al genético)

### 🧬 **Genético**
- 📦 **Pedidos procesados**: 211
- ✅ **Éxito**: 100%
- ✈️ **Rutas directas**: 152 (72%)
- 🔄 **Rutas con escalas**: 59 (28%)

## 🔧 **Compilación Manual (si es necesario)**

### **Para ACO:**
```batch
# Limpiar
rmdir /s /q bin
mkdir bin

# Compilar paso a paso
javac -d bin -cp src src\morapack\modelo\*.java
javac -d bin -cp "src;bin" src\morapack\datos\*.java
javac -d bin -cp "src;bin" src\morapack\planificacion\PlanificadorTemporalColoniaV2.java
javac -d bin -cp "src;bin" src\morapack\main\MainColoniaV2Corregido.java

# Ejecutar
java -cp "bin;src" morapack.main.MainColoniaV2Corregido
```

### **Para Genético:**
```batch
cd ..\morapack_genetico

# Compilar (si no existe bin)
javac -d bin -cp src src\morapack\modelo\*.java
javac -d bin -cp "src;bin" src\morapack\datos\*.java
javac -d bin -cp "src;bin" src\morapack\planificacion\*.java
javac -d bin -cp "src;bin" src\morapack\main\*.java

# Ejecutar
java -cp "bin;src" morapack.main.MainMoraPackCorregido
```

## 🐛 **Solución de Problemas**

### **Error: 'javac' no se reconoce**
1. Instalar Java JDK
2. Configurar JAVA_HOME: `C:\Program Files\Java\jdk-XX`
3. Agregar al PATH: `%JAVA_HOME%\bin`

### **Error: no se ha encontrado o cargado la clase**
- Verificar que el classpath incluya tanto `bin` como `src`
- Usar punto y coma `;` como separador en Windows (no dos puntos `:`)

### **Error de compilación**
- Verificar que todos los archivos `.java` estén presentes
- Compilar en orden: modelo → datos → planificación → main

## 📈 **Comparación de Algoritmos**

| Métrica | 🧬 Genético | 🐜 ACO Antes | 🐜 ACO Corregido |
|---------|-------------|-------------|------------------|
| **Optimizaciones** | N/A | 21 ❌ | 211 ✅ |
| **Cobertura** | 100% | 10% ❌ | 100% ✅ |
| **Escalas** | 28% | Sí | Sí ✅ |
| **Éxito** | 100% | 100% | 100% ✅ |

## 🎉 **Objetivo Logrado**
El sistema ACO ahora optimiza **TODAS** las rutas (211), no solo una muestra (21).

¡Ambos algoritmos funcionan correctamente en Windows! 🪟✨
