import networkx as nx
import pandas as pd
import os

gen_dir = "/home/cristian/Escritorio/TFM/pap_metaheuristics"
dir = f"{gen_dir}/previous_work/instances/"
df = pd.DataFrame()
for file in os.listdir(dir):
    if "65_64_1_social_0.in" == file:
        print("hihi")
    path_problem = "{}/{}".format(dir, file)
    f = open(path_problem, "r")
    f.readline()
    f.readline()
    # n is the number of nodes of each instance
    n = int(f.readline())
    edges = int(f.readline())
    G = nx.Graph()
    for _ in range(edges):
        s, e = map(int, f.readline().split())
        G.add_edge(s, e)
    set_leafs = set()
    set_support = set()
    for k, v in dict(G.degree).items():
        if v == 1:
            set_leafs.add(k)
            for neigh in G.neighbors(k):
                set_support.add(neigh)
    df = pd.concat([df, pd.DataFrame([{'name': file, 'leafs': len(set_leafs), 'support': len(set_support)}])], ignore_index = True)
df.to_csv("/home/cristian/Escritorio/TFM/pap_metaheuristics/leafs_support.csv")