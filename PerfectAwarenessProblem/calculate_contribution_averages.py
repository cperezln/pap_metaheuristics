#!/usr/bin/env python3
"""
Script para calcular los promedios de los valores de contribución desde los archivos
en el directorio time_to_best-28/

Los tres números representan:
1. Calidad después del constructivo
2. Calidad después de la búsqueda local
3. Calidad después del ILS
"""

import os
import glob

def calculate_averages(directory_path):
    """
    Calcula los promedios de los tres números en todos los archivos contribution_*.txt

    Args:
        directory_path (str): Ruta al directorio que contiene los archivos

    Returns:
        tuple: (promedio_constructivo, promedio_busqueda_local, promedio_ils, num_archivos)
    """
    # Buscar todos los archivos que empiecen con "contribution_" y terminen con ".txt"
    pattern = os.path.join(directory_path, "contribution_*.txt")
    files = glob.glob(pattern)

    if not files:
        print(f"No se encontraron archivos en {directory_path}")
        return None, None, None, 0

    # Listas para almacenar los valores
    constructivo_values = []
    busqueda_local_values = []
    ils_values = []

    files_processed = 0
    files_with_errors = 0

    for file_path in files:
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()

                # Buscar la línea con los datos (segunda línea normalmente)
                data_line = None
                for line in lines:
                    line = line.strip()
                    if line and not line.startswith('Method') and '\t' in line:
                        data_line = line
                        break

                if data_line:
                    # Dividir por tabuladores
                    parts = data_line.split('\t')
                    if len(parts) >= 3:
                        try:
                            # Convertir a números
                            constructivo = float(parts[0])
                            busqueda_local = float(parts[1])
                            ils = float(parts[2])

                            constructivo_values.append(constructivo)
                            busqueda_local_values.append(busqueda_local)
                            ils_values.append(ils)

                            files_processed += 1

                        except ValueError as e:
                            print(f"Error convirtiendo valores en {os.path.basename(file_path)}: {e}")
                            files_with_errors += 1
                    else:
                        print(f"Formato incorrecto en {os.path.basename(file_path)}: {data_line}")
                        files_with_errors += 1
                else:
                    print(f"No se encontraron datos válidos en {os.path.basename(file_path)}")
                    files_with_errors += 1

        except Exception as e:
            print(f"Error leyendo {os.path.basename(file_path)}: {e}")
            files_with_errors += 1

    # Calcular promedios
    if constructivo_values:
        avg_constructivo = sum(constructivo_values) / len(constructivo_values)
        avg_busqueda_local = sum(busqueda_local_values) / len(busqueda_local_values)
        avg_ils = sum(ils_values) / len(ils_values)

        return avg_constructivo, avg_busqueda_local, avg_ils, files_processed, files_with_errors
    else:
        return None, None, None, files_processed, files_with_errors

def main():
    # Directorio que contiene los archivos de contribución
    directory = "time_to_best-28"

    if not os.path.exists(directory):
        print(f"Error: El directorio '{directory}' no existe")
        return

    print("Calculando promedios de contribución...")
    print("=" * 50)

    # Calcular promedios
    avg_constructivo, avg_busqueda_local, avg_ils, files_processed, files_with_errors = calculate_averages(directory)

    if avg_constructivo is not None:
        print(f"Archivos procesados exitosamente: {files_processed}")
        if files_with_errors > 0:
            print(f"Archivos con errores: {files_with_errors}")
        print()

        print("RESULTADOS:")
        print("-" * 30)
        print(f"Promedio después del constructivo:     {avg_constructivo:.6f}")
        print(f"Promedio después de búsqueda local:    {avg_busqueda_local:.6f}")
        print(f"Promedio después del ILS:              {avg_ils:.6f}")
        print()

        # Calcular mejoras
        mejora_busqueda_local = avg_constructivo - avg_busqueda_local
        mejora_ils = avg_busqueda_local - avg_ils
        mejora_total = avg_constructivo - avg_ils

        print("MEJORAS:")
        print("-" * 30)
        print(f"Mejora con búsqueda local:   {mejora_busqueda_local:.6f} ({(mejora_busqueda_local/avg_constructivo*100):.2f}%)")
        print(f"Mejora con ILS:              {mejora_ils:.6f} ({(mejora_ils/avg_busqueda_local*100):.2f}%)")
        print(f"Mejora total:                {mejora_total:.6f} ({(mejora_total/avg_constructivo*100):.2f}%)")

    else:
        print("No se pudieron calcular los promedios")
        if files_with_errors > 0:
            print(f"Archivos con errores: {files_with_errors}")

if __name__ == "__main__":
    main()