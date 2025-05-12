import os
import time
from itertools import combinations

import gurobipy as gp
from gurobipy import *
import networkx as nx


path = "/home/cristian/Escritorio/TFM/pap_metaheuristics/"
def model_n2(G):
    # Callback - use lazy constraints to eliminate sub-tours
    list_nodes = list(G.nodes)
    def subtourelim(model, where):
        if where == GRB.Callback.MIPSOL:
            # make a list of edges selected in the solution
            vals = model.cbGetSolution(model._edges)
            selected = gp.tuplelist((i, j) for i, j in model._edges.keys()
                                    if vals[i, j] > 0.5)
            # find the shortest cycle in the selected edge list
            tour = subtour(selected)
            if len(tour) == 1:
                return
            if len(tour) < len(list_nodes):
                # add subtour elimination constr. for every pair of cities in subtour
                model.cbLazy(gp.quicksum(model._edges[(i, j)] for i, j in combinations(tour, 2))
                             <= len(tour) - 1)

    # Given a tuplelist of edges, find the shortest subtour

    def subtour(edges):
        unvisited = list_nodes[:]
        cycle = list_nodes[:]  # Dummy - guaranteed to be replaced
        while unvisited:  # true if list is non-empty
            thiscycle = []
            neighbors = unvisited
            while neighbors:
                current = neighbors[0]
                thiscycle.append(current)
                unvisited.remove(current)
                neighbors = [j for i, j in edges.select(current, '*')
                             if j in unvisited]
            if len(thiscycle) <= len(cycle):
                cycle = thiscycle  # New shortest subtour
        return cycle

    init_time = time.time()
    # Defining the model
    model = Model("PAP_v1")
    model.setParam("LogToConsole", 0)
    model.setParam("TimeLimit", 1800)

    # Defining the n + e variables
    D = nx.DiGraph(G)
    s = model.addVars(D.nodes, vtype=GRB.BINARY, name='s')
    a = model.addVars(D.edges, vtype=GRB.BINARY, name='a')
    edges = a
    model._edges= edges
    model._vars = s
    # Objective function
    model.setObjective(quicksum(s[v] for v in G.nodes), GRB.MINIMIZE)

    # Problem restrictions

    # Added by my (AUXILIAR)
    model.addConstr(quicksum(s[v] for v in G.nodes) >= 1 )
    for v in G.nodes:
        model.addConstr(quicksum(a[(e, v)] for e in D.predecessors(v)) - D.in_degree[v]*(1-s[v]) <= 0)
        model.addConstr(s[v] + quicksum(a[(e, v)] for e in D.predecessors(v)) >= 1)
        for oe in D.successors(v):
            model.addConstr(quicksum(a[(e, v)] for e in D.predecessors(v)) - D.degree[v]*0.5*(a[(v, oe)] - s[v]) >= 0)
    try:
        model.Params.lazyConstraints = 1
        model.optimize(subtourelim)
        computed_seed_set = {v for v in G.nodes if s[v].X == 1}
        computed_solution_value = quicksum(s[v] for v in computed_seed_set).getValue()
        print("---------------------- MODEL |V| + 2|E| ----------------------")
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



dir = f"{path}/previous_work/instances/"
problematic_instances = []
for file in sorted(os.listdir(dir), key = lambda x: int(x.split("_")[0])):
    #if file in os.listdir(f"{path}/solutionsFinal/arcsModelSols/") or file in problematic_instances:
     #   continue

    print(file)
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
    initTime = time.time()
    res = model_n2(G)
    with open(f"{path}/solutionsFinal/arcsModelSols/{file}", "w") as f:
        f.write(f"{res}")
        f.write(f"{time.time() - initTime}")



