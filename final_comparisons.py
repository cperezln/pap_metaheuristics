import os

import pandas as pd
from networkx.algorithms.bipartite.basic import density
def get_freq(data, bins):
    dist_dict = {k: [] for k in bins}
    for ele in data:
        for end_int in dist_dict:
            if ele <= end_int:
                dist_dict[end_int].append(data[ele])
    return dist_dict
if __name__ == "__main__":
    # Carga del archivo Excel y hoja específica
    excel_path = '/home/cristian/Escritorio/TFM/pap_metaheuristics/results_fin.ods'
    dir_results = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutionsFinal/roundsModelSols"

    # Leer el archivo Excel y la hoja Our
    model = True
    alternativa = True
    # Iterar sobre los archivos y actualizar el DataFrame
    if not alternativa:
        df = pd.read_excel(excel_path, sheet_name='MODEL')
        if model:
            for file in os.listdir(dir_results):
                with open(os.path.join(dir_results, file), 'r') as f:
                    lines = f.readlines()
                    name = file.split(".")[0].strip() + '.sol'
                    index = lines[0].find(".")
                    seed_size = float(lines[0][0:index + 2])
                    time = float(lines[0][index + 2:])
                    # Actualizar el DataFrame
                    df.loc[df['Name'] == name, 'Seed Size'] = seed_size
                    df.loc[df['Name'] == name, 'Time'] = time
        else:
            for file in os.listdir(dir_results):
                with open(os.path.join(dir_results, file), 'r') as f:
                    lines = f.readlines()
                    name = lines[0].strip() + '.sol'
                    seed_size = int(lines[1])
                    time = int(lines[3])

                    # Actualizar el DataFrame
                    df.loc[df['Name'] == name, 'Seed Size'] = seed_size
                    df.loc[df['Name'] == name, 'Time'] = time/10**9
        with pd.ExcelWriter(excel_path, mode='a', if_sheet_exists='replace') as writer:
            df.to_excel(writer, sheet_name='MODEL', index=False)

    if alternativa:
        dir_results = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutionsFinal/roundsModelSols"
        model_dict = {k: dict() for k in range(10, 105, 5)}
        for file in os.listdir(dir_results):
            with open(os.path.join(dir_results, file), 'r') as f:
                lines = f.readlines()
                number_nodes = int(file.split("_")[0])
                number_edges = int(file.split("_")[1])
                density = 2 * number_edges / (number_nodes * (number_nodes - 1))
                name = file.split(".")[0].strip() + '.sol'
                index = lines[0].find(".")
                seed_size = float(lines[0][0:index + 2])
                time = float(lines[0][index + 2:])
                if time > 1800:
                    model_dict[number_nodes][density] = -1
                    continue
                model_dict[number_nodes][density] = seed_size
        df = pd.read_excel(excel_path, sheet_name='sin_tiempo', engine="odf")
        nt_best_our = {k: v for k, v in zip(list(df['Name']), list(df['Bour']))}
        nt_time_our = {k: v for k, v in zip(list(df['Name']), list(df['Tour']))}
        nt_final_dict_s = {}
        nt_final_dict_t = {}
        density_list = []
        best_them = {k: v for k, v in zip(list(df['Name']), list(df['Bprev']))}
        bests_ct = {}
        eq_ct = {}
        bests_st = {}
        eq_st = {}
        for k, v in nt_best_our.items():
            spl = k.split("_")

            number_nodes = int(spl[0])
            number_edges = int(spl[1])
            if number_nodes not in eq_st:
                eq_st[number_nodes] = 0
            if number_nodes not in bests_st:
                bests_st[number_nodes] = 0
            density = 2 * number_edges / (number_nodes * (number_nodes - 1))
            density_list.append(density)
            if number_nodes not in nt_final_dict_s:
                nt_final_dict_s[number_nodes] = []
            nt_final_dict_s[number_nodes].append(v)
            if number_nodes not in nt_final_dict_t:
                nt_final_dict_t[number_nodes] = []
            nt_final_dict_t[number_nodes].append(nt_time_our[k])
            if v < best_them[k]:
                bests_st[number_nodes] += 1
            if v == best_them[k]:
                eq_st[number_nodes] += 1
        df = pd.read_excel(excel_path, sheet_name='con_tiempo', engine="odf")
        wt_best_our = {k: v for k, v in zip(list(df['Name']), list(df['Bour']))}
        wt_time_our = {k: min(v, 300) for k, v in zip(list(df['Name']), list(df['Tour']))}
        wt_final_dict_s = {}
        wt_final_dict_t = {}

        for k, v in wt_best_our.items():
            spl = k.split("_")
            number_nodes = int(spl[0])
            number_edges = int(spl[1])
            if number_nodes not in eq_ct:
                eq_ct[number_nodes] = 0
            if number_nodes not in bests_ct:
                bests_ct[number_nodes] = 0
            density = 2 * number_edges / (number_nodes * (number_nodes - 1))
            if number_nodes not in wt_final_dict_s:
                wt_final_dict_s[number_nodes] = []
            wt_final_dict_s[number_nodes].append(v)
            if number_nodes not in wt_final_dict_t:
                wt_final_dict_t[number_nodes] = []
            wt_final_dict_t[number_nodes].append(wt_time_our[k])
            if v < best_them[k]:
                bests_ct[number_nodes] += 1
            if v == best_them[k]:
                eq_ct[number_nodes] += 1


        best_them_final = {}

        for k, v in best_them.items():
            spl = k.split("_")
            number_nodes = int(spl[0])
            number_edges = int(spl[1])
            density = 2 * number_edges / (number_nodes * (number_nodes - 1))
            if number_nodes not in best_them_final:
                best_them_final[number_nodes] = []
            best_them_final[number_nodes].append(v)
        import numpy as np
        keys = sorted(best_them_final.keys())
        best_them_final = {k: np.mean(best_them_final[k]) for k in keys}
        wt_final_dict_t = {k: np.mean(wt_final_dict_t[k]) for k in keys}
        wt_final_dict_s = {k: np.mean(wt_final_dict_s[k]) for k in keys}
        nt_final_dict_t = {k: np.mean(nt_final_dict_t[k]) for k in keys}
        nt_final_dict_s = {k: np.mean(nt_final_dict_s[k]) for k in keys}
        best_ct_final = {k: bests_ct[k] for k in keys}
        best_st_final = {k: bests_st[k] for k in keys}
        eq_ct_final = {k: eq_ct[k] for k in keys}
        eq_st_final = {k: eq_st[k] for k in keys}

        df = pd.DataFrame({'Them': best_them_final.values(), 'Our (s)': wt_final_dict_s.values(), 'Our (t)': wt_final_dict_t.values(),
                           'Ourn (s)': nt_final_dict_s.values(), 'Ourn (t)': nt_final_dict_t.values(), 'Bests': best_ct_final.values(),
                           'Eq': eq_ct_final.values(), 'Bestsn': best_st_final.values(), 'Eqn': eq_st_final.values()})
        df['keys'] = keys
        df = df.set_index('keys')
        df['desv_ct'] = 100*(df['Our (s)'] -  np.minimum(df['Our (s)'], df['Ourn (s)'], df['Them']) )  /  np.minimum(df['Our (s)'], df['Ourn (s)'], df['Them'])
        df['desv_st'] = 100 * ( df['Ourn (s)'] -  np.minimum(df['Our (s)'], df['Ourn (s)'], df['Them'])) /  np.minimum(df['Our (s)'], df['Ourn (s)'], df['Them'])
        df['desv_them'] = 100 * (df['Them'] - np.minimum(df['Our (s)'], df['Ourn (s)'], df['Them'])) /  np.minimum(df['Our (s)'], df['Ourn (s)'], df['Them'])
        print(df[['Them', 'Our (s)', 'Ourn (s)', 'Our (t)', 'Ourn (t)', 'desv_them', 'desv_ct', 'desv_st', 'Bests', 'Eq', 'Bestsn', 'Eqn']].round(decimals = 2).to_latex())
    # Guardar los cambios en el archivo Excel

