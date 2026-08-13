import os
import time
import math
import matplotlib.pyplot as plt

from gurobipy import *
import networkx as nx

#path = "/home/isaac/testPAP/"
path = "/Users/isaaclozano/Documents/GitHub/pap_metaheuristics/"
def compute_spreading(G, n, seed_set):
    s_val = {}
    for v in G.nodes:
        s_val[(v, 0)] = 1 if v in seed_set else 0
    for tau in range(1, n + 1):
        for v in G.nodes:
            if s_val[(v, tau - 1)] == 1:
                s_val[(v, tau)] = 1
            elif sum(s_val[(u, tau - 1)] for u in G.neighbors(v)) >= math.ceil(0.5 * G.degree[v]):
                s_val[(v, tau)] = 1
            else:
                s_val[(v, tau)] = 0
    return s_val


def model_n2(G, initial_seed_set=None):
    init_time = time.time()
    # Defining the model
    model = Model("PAP_v1")
    model.setParam("LogToConsole", 1)
    model.setParam("TimeLimit", 200000)
    model.setParam("Threads", 8)

    # Defining the n² variables
    s = model.addVars(G.nodes, range(n + 1), vtype=GRB.BINARY, name='s')

    # Warm start from provided initial solution
    if initial_seed_set is not None:
        s_start = compute_spreading(G, n, initial_seed_set)
        for (v, tau), val in s_start.items():
            s[v, tau].Start = val

    # Objective function
    model.setObjective(quicksum(s[v, 0] for v in G.nodes), GRB.MINIMIZE)

    # Problem restrictions

    for v in G.nodes:
        for tau in range(1, n + 1):
            model.addConstr(s[v, tau] - s[v, tau - 1] >= 0)
            model.addConstr(
                quicksum(s[u, tau - 1] for u in G.neighbors(v)) - 0.5 * G.degree[v] * (s[v, tau] - s[v, 0]) >= 0)
            # Forced-activation (big-M): if Σ neighbours spreaders ≥ ⌈0.5·deg⌉, node MUST spread.
            # Old (buggy for odd degrees): used 0.5·deg-1 as upper bound → forced leaves always.
            T = math.ceil(0.5 * G.degree[v])
            model.addConstr(quicksum(s[u, tau - 1] for u in G.neighbors(v)) <= (T - 1) + G.degree[v] * s[v, tau])
    for v in G.nodes:
        model.addConstr(s[v, 0] + quicksum(s[u, n - 1] for u in G.neighbors(v)) >= 1)
    model.addConstr(quicksum(s[v, 0] for v in G.nodes) == 8) #test

    try:
        model.optimize()
        computed_seed_set = {v for v in G.nodes if s[v, 0].X == 1}
        computed_solution_value = quicksum(s[v, 0] for v in computed_seed_set).getValue()
        print("---------------------- MODEL |V|² ----------------------")
        print("Problem: ", path_problem)
        print("Execution time: ", time.time() - init_time)
        print("Computed solution seed set: ", computed_seed_set)
        print("Computed solution value: ", computed_solution_value)
        return computed_solution_value
    except GurobiError as e:
        print(f"Gurobi status {model.Status}. Model errored.")
        problematic_instances.append(path_problem)
        # print('Error code ' + str(e.errno) + ': ' + str(e))
        # print("Try with academic license")


JAZZ_INITIAL_SOLUTION = {24, 40, 41, 55, 57, 60, 64, 75, 95, 107, 126, 143, 178}

dir = f"{path}/previous_work/instances/"
#dir = f"{path}/instances/"
problematic_instances = []
for file in os.listdir(dir):
    if 'DS' in file:
        continue
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

    initial_sol = JAZZ_INITIAL_SOLUTION if 'jazz' in file else None
    res = model_n2(G, initial_sol)
    with open(f"{path}/gurobi_sols/{file}", "w") as f:
        f.write(f"{res}")



