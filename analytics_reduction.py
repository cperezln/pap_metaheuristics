import math

if __name__ == "__main__":
    import sys
    import os
    import matplotlib.pyplot as plt
    import pandas as pd

    import numpy as np
    sum = 0
    path_dir = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutionsFinal/instanceAnalysisOfSizes"
    density_dict = {i: {'node': [], 'edge': [], 'leaf': []} for i in np.linspace(0, 1, 20)}
    for file in os.listdir(path_dir):
        number_nodes = int(file.split("_")[0])
        number_edges = int(file.split("_")[1])
        with open(f"{path_dir}/{file}", "r") as f:
            lines = f.readlines()
            node_reduction = 1 - float(lines[1])
            edge_reduction = 1 - float(lines[2])
            if math.isnan(edge_reduction):
                sum += 1
            leaf_reduction = float(lines[3])
            if math.isnan(leaf_reduction):
                leaf_reduction = 0
            density = 2*number_edges/(number_nodes*(number_nodes-1))
            densities = list(density_dict.keys())
            find_closest = lambda x: min(densities, key=lambda y: abs(x-y))
            key = find_closest(density)
            density_dict[key]['node'].append(node_reduction)
            density_dict[key]['edge'].append(edge_reduction)
            density_dict[key]['leaf'].append(leaf_reduction)

    print("number nans", sum)
    final_dict = dict()
    for k, v in density_dict.items():
        final_dict[k] = {'node': {'mean': np.mean(v['node']), 'std': np.std(v['node'])},
                         'edge': {'mean': np.mean(v['edge']), 'std': np.std(v['edge'])},
                         'leaf': {'mean': np.mean(v['leaf']), 'std': np.std(v['leaf'])}}
    x = list(final_dict.keys())[:12]  # Las keys para el eje x
    # Extraer los valores medios y desviaciones estándar para cada tipo
    node_means = [final_dict[data]['node']['mean'] for data in x]
    node_stds = [final_dict[data]['node']['std'] for data in x]

    edge_means = [final_dict[data]['edge']['mean'] for data in x]
    edge_stds = [final_dict[data]['edge']['std'] for data in x]

    leaf_means = [final_dict[data]['leaf']['mean'] for data in x]
    leaf_stds = [final_dict[data]['leaf']['std'] for data in x]

    # Crear la figura y los ejes
    plt.figure(figsize=(10, 6))

    # Plotear cada línea con su área sombreada
    plt.plot(x, node_means, label='|V|', color='blue', linewidth=2)
    plt.fill_between(x,
                     np.array(node_means) - np.array(node_stds),
                     np.array(node_means) + np.array(node_stds),
                     color='blue', alpha=0.1)

    plt.plot(x, edge_means, label='|E|', color='red', linewidth=2)
    plt.fill_between(x,
                     np.array(edge_means) - np.array(edge_stds),
                     np.array(edge_means) + np.array(edge_stds),
                     color='red', alpha=0.1)

    plt.plot(x, leaf_means, label='Nodos hoja', color='green', linewidth=2)
    plt.fill_between(x,
                     np.array(leaf_means) - np.array(leaf_stds),
                     np.array(leaf_means) + np.array(leaf_stds),
                     color='green', alpha=0.1)

    # Personalizar la gráfica
    plt.xlabel('Tamaño del Grafo')
    plt.ylabel('%')
    plt.title('Comparación de Métricas con Desviación Estándar')
    plt.legend()
    plt.grid(True, alpha=0.3)

    # Mostrar la gráfica
    plt.tight_layout()
    plt.savefig("analytics_reduction.svg")
