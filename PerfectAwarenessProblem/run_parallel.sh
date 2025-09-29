#!/bin/bash

# Script para ejecutar PerfectAwarenessProblem.jar en paralelo para todas las instancias
# Uso: ./run_parallel.sh [N_PROCESOS_PARALELOS] [ALGORITHM]
# ALGORITHM puede ser: GRASP (default) o ILS

# Número de procesos paralelos (por defecto 4)
N_PARALLEL=${1:-4}

# Algoritmo a ejecutar (por defecto GRASP)
ALGORITHM=${2:-ILS}

# Directorio de instancias (solo los archivos específicos)
INSTANCES_DIR="/home/isaac/pap/previous_work/instances"

# Archivo JAR
JAR_FILE="/home/isaac/pap/PerfectAwarenessProblem.jar" #/home/isaac/pap  "./out/artifacts/PerfectAwarenessProblem_jar/PerfectAwarenessProblem.jar"

# Verificar que existe el directorio de instancias
if [ ! -d "$INSTANCES_DIR" ]; then
    echo "Error: El directorio $INSTANCES_DIR no existe"
    exit 1
fi

# Verificar que existe el archivo JAR
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: El archivo JAR $JAR_FILE no existe"
    exit 1
fi

echo "Ejecutando PerfectAwarenessProblem.jar en paralelo con $N_PARALLEL procesos"
echo "Algoritmo: $ALGORITHM"
echo "Directorio de instancias: $INSTANCES_DIR"
echo "Archivo JAR: $JAR_FILE"

# Listar instancias que se procesarán
echo "Instancias a procesar:"
find "$INSTANCES_DIR" -name "*.in" -type f | sort
echo "----------------------------------------"

# Función para procesar una instancia
process_instance() {
    local instance_file="$1"
    local instance_name=$(basename "$instance_file")

    echo "Procesando: $instance_name"

    # Crear directorios de solución esperados
    mkdir -p "solutions/grasp" "solutions/ils"

    # Ejecutar java con la instancia específica como parámetro
    # Nuevo formato: java -jar JAR_FILE <instance_file> <config_id> <algorithm>
    java -jar "$JAR_FILE" "$instance_file" "1" "$ALGORITHM" 2>&1 | while IFS= read -r line; do
        echo "[$instance_name] $line"
    done

    echo "Completado: $instance_name"

    # Buscar archivos de solución generados en las carpetas solutions
    echo "[$instance_name] Buscando archivos de solución generados..."
    find solutions/ -name "*${instance_name%.in}*" -type f 2>/dev/null | while read sol_file; do
        echo "[$instance_name] ✓ Solución encontrada: $sol_file"
    done

    # También verificar en el directorio actual por si acaso
    find . -maxdepth 1 -name "*${instance_name%.in}*.txt" -type f 2>/dev/null | while read sol_file; do
        echo "[$instance_name] ✓ Solución encontrada: $sol_file"
    done
}

# Exportar las variables para que estén disponibles en los subprocesos
export -f process_instance
export JAR_FILE ALGORITHM

# Solo procesar archivos .in específicos en el directorio instances
find "$INSTANCES_DIR" -name "*.in" -type f | sort | xargs -P "$N_PARALLEL" -I {} bash -c 'process_instance "{}"'

echo "----------------------------------------"
echo "Todas las instancias han sido procesadas"