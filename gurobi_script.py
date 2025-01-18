import os
import time

from gurobipy import *
import networkx as nx

dir = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/"
problematic_instances = []
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


    init_time = time.time()
    # Defining the model
    model = Model("PAP")
    model.setParam("LogToConsole", 0)
    model.setParam("TimeLimit", 60)

    # Defining the n² variables
    s = model.addVars(G.nodes, range(n + 1), vtype = GRB.BINARY, name = 's')

    # Objective function
    model.setObjective(quicksum(s[v, 0] for v in G.nodes), GRB.MINIMIZE)

    # Problem restrictions
    for v in G.nodes:
        for tau in range(1, n + 1):
            model.addConstr(quicksum(s[u, tau-1] for u in G.neighbors(v)) - 0.5*G.degree[v]*(s[v, tau] - s[v, 0]) >= 0)
    for v in G.nodes:
        model.addConstr(s[v, 0] + quicksum(s[u, n-1] for u in G.neighbors(v)) >= 1)

    try:
        model.optimize()
        computed_seed_set = {v for v in G.nodes if s[v, 0].X == 1}
        computed_solution_value = quicksum(s[v, 0] for v in computed_seed_set).getValue()

        print("Problem: ", path_problem)
        print("Execution time: ", time.time() - init_time)
        print("Computed solution seed set: ", computed_seed_set)
        print("Computed solution value: ", computed_solution_value)
    except GurobiError as e:
        problematic_instances.append(path_problem)
        #print('Error code ' + str(e.errno) + ': ' + str(e))
        #print("Try with academic license")

print("Problematic instances: ", problematic_instances)