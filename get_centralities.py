import networkx as nx
import os

dir = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/"
for file in os.listdir(dir):
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