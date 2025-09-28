# ===================================================
# Makefile para MoraPack Colonia v2
# ===================================================

# Variables
SRCDIR = src
BINDIR = bin
MAINCLASS = morapack.main.Main

# Directorios de código fuente
CORE_SRC = $(SRCDIR)/morapack/core
COLONIA_SRC = $(SRCDIR)/morapack/colonia
MAIN_SRC = $(SRCDIR)/morapack/main

# Archivos Java
CORE_FILES = $(CORE_SRC)/solucion/Solucion.java \
             $(CORE_SRC)/problema/Problema.java \
             $(CORE_SRC)/problema/ProblemaTSP.java

COLONIA_FILES = $(COLONIA_SRC)/componentes/Hormiga.java \
                $(COLONIA_SRC)/componentes/Feromona.java \
                $(COLONIA_SRC)/componentes/Heuristica.java \
                $(COLONIA_SRC)/algoritmo/AlgoritmoColoniaHormigas.java

MAIN_FILES = $(MAIN_SRC)/Main.java

ALL_JAVA_FILES = $(CORE_FILES) $(COLONIA_FILES) $(MAIN_FILES)

# Targets
.PHONY: all compile run clean help test

# Target por defecto
all: compile

# Compilar todo el proyecto
compile: $(BINDIR)
	@echo "[INFO] Compilando MoraPack Colonia v2..."
	@echo "[INFO] Compilando clases core..."
	javac -d $(BINDIR) $(CORE_FILES)
	@echo "[INFO] Compilando componentes de colonia..."
	javac -cp $(BINDIR) -d $(BINDIR) $(COLONIA_FILES)
	@echo "[INFO] Compilando clase Main..."
	javac -cp $(BINDIR) -d $(BINDIR) $(MAIN_FILES)
	@echo "[SUCCESS] Compilacion completada!"

# Crear directorio bin
$(BINDIR):
	@echo "[INFO] Creando directorio $(BINDIR)..."
	mkdir -p $(BINDIR)

# Ejecutar el programa
run: compile
	@echo "[INFO] Ejecutando MoraPack Colonia v2..."
	java -cp $(BINDIR) $(MAINCLASS)

# Limpiar archivos compilados
clean:
	@echo "[INFO] Limpiando archivos compilados..."
	rm -rf $(BINDIR)
	@echo "[INFO] Limpieza completada"

# Compilación rápida (solo archivos modificados)
quick:
	@echo "[INFO] Compilacion rapida..."
	javac -cp $(BINDIR) -d $(BINDIR) $(ALL_JAVA_FILES)

# Verificar que Java esté instalado
check-java:
	@echo "[INFO] Verificando instalacion de Java..."
	@java -version
	@javac -version

# Compilar con información de debug
debug: $(BINDIR)
	@echo "[INFO] Compilando con informacion de debug..."
	javac -g -cp $(BINDIR) -d $(BINDIR) $(ALL_JAVA_FILES)

# Mostrar estructura del proyecto
tree:
	@echo "[INFO] Estructura del proyecto:"
	@find $(SRCDIR) -name "*.java" | sort

# Contar líneas de código
lines:
	@echo "[INFO] Contando lineas de codigo..."
	@find $(SRCDIR) -name "*.java" -exec wc -l {} + | tail -1

# Ejecutar con parámetros específicos
run-simple:
	@echo "[INFO] Ejecutando ejemplo simple..."
	java -cp $(BINDIR) -Djava.awt.headless=true $(MAINCLASS)

# Target de ayuda
help:
	@echo "Makefile para MoraPack Colonia v2"
	@echo ""
	@echo "Targets disponibles:"
	@echo "  all         - Compilar todo el proyecto (default)"
	@echo "  compile     - Compilar todo el proyecto"
	@echo "  run         - Compilar y ejecutar el programa"
	@echo "  clean       - Limpiar archivos compilados"
	@echo "  quick       - Compilacion rapida"
	@echo "  debug       - Compilar con informacion de debug"
	@echo "  check-java  - Verificar instalacion de Java"
	@echo "  tree        - Mostrar estructura del proyecto"
	@echo "  lines       - Contar lineas de codigo"
	@echo "  run-simple  - Ejecutar en modo headless"
	@echo "  help        - Mostrar esta ayuda"
	@echo ""
	@echo "Ejemplos:"
	@echo "  make                  # Compilar"
	@echo "  make run              # Compilar y ejecutar"
	@echo "  make clean compile    # Limpiar y compilar"