import networkx as nx
import os
gen_dir = "/home/cristian/Escritorio/TFM/pap_metaheuristics"
dir = f"{gen_dir}/previous_work/instances/"
for file in os.listdir(dir):
    if file not in os.listdir(f"{gen_dir}/centralities/eigenvector"):
        path_problem = "{}/{}".format(dir, file)
        f = open(path_problem, "r")
        f.readline(); f.readline()
        # n is the number of nodes of each instance
        n = int(f.readline())
        edges = int(f.readline())
        G = nx.Graph()
        for _ in range(edges):
            s, e = map(int, f.readline().split())
            G.add_edge(s, e)

        # kc = nx.centrality.katz_centrality(G);
        ec = nx.centrality.eigenvector_centrality(G, max_iter=500);
        dc = nx.centrality.degree_centrality(G)
        bc = nx.centrality.betweenness_centrality(G);
        with open(f"{gen_dir}/centralities/betweeness/{file}", "w") as f:
            for k, v in bc.items():
                f.write(f"{k} {v}\n")
        with open(f"{gen_dir}/centralities/eigenvector/{file}", "w") as f:
            for k, v in ec.items():
                f.write(f"{k} {v}\n")
        with open(f"{gen_dir}/centralities/degree/{file}", "w") as f:
            for k, v in dc.items():
                f.write(f"{k} {v}\n")