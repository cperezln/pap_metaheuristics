import os
import pandas as pd

# Leer el archivo instances.txt para obtener el orden
with open('instances.txt', 'r') as f:
    instance_list = [line.strip().replace('.sol', '') for line in f.readlines()]

# Directorio con los archivos
directory = "froundsModelSols1h"

# Listas para almacenar los datos
instances = []
objectives = []
times = []

# Recorrer las instancias en el orden especificado
for instance_name in instance_list:
    # El archivo correspondiente tiene extensión .in
    filepath = os.path.join(directory, instance_name + '.in')

    instances.append(instance_name)

    # Verificar si el archivo existe
    if os.path.isfile(filepath):
        # Leer las primeras dos líneas del archivo
        with open(filepath, 'r') as f:
            lines = f.readlines()
            if len(lines) >= 2:
                objectives.append(lines[0].strip())
                times.append(lines[1].strip())
            else:
                objectives.append('')
                times.append('')
    else:
        # Si el archivo no existe, dejar vacío
        objectives.append('')
        times.append('')

# Crear DataFrame
df = pd.DataFrame({
    'Instance': instances,
    'Objective': objectives,
    'Time': times
})

# Guardar a Excel (requiere openpyxl)
try:
    output_file = "frounds_results.xlsx"
    df.to_excel(output_file, index=False)
    print(f"Archivo {output_file} generado con {len(df)} instancias")
except ImportError:
    # Si no está openpyxl, guardar como CSV
    output_file = "frounds_results.csv"
    df.to_csv(output_file, index=False)
    print(f"Archivo {output_file} generado con {len(df)} instancias (CSV)")
