import pandas as pd
import os

if __name__ == "__main__":
    # Carga del archivo Excel y hoja específica
    excel_path = '/home/cristian/Escritorio/TFM/pap_metaheuristics/results.xlsx'
    dir_results = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutionsv7/grasp_solutions"

    # Leer el archivo Excel y la hoja Our
    df = pd.read_excel(excel_path, sheet_name='GRASPV7')

    # Iterar sobre los archivos y actualizar el DataFrame
    for file in os.listdir(dir_results):
        with open(os.path.join(dir_results, file), 'r') as f:
            lines = f.readlines()
            name = lines[0].strip() + '.sol'
            seed_size = int(lines[1])
            time = int(lines[3])

            # Actualizar el DataFrame
            df.loc[df['Name'] == name, 'Seed Size'] = seed_size
            df.loc[df['Name'] == name, 'Time'] = time/10**9


    # Guardar los cambios en el archivo Excel
    with pd.ExcelWriter(excel_path, mode='a', if_sheet_exists='replace') as writer:
        df.to_excel(writer, sheet_name='GRASPV7', index=False)
