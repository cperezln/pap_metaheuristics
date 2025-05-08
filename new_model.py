import os
import time
from math import ceil

import matplotlib.pyplot as plt
from gurobipy import *
import networkx as nx

G = nx.Graph()
edge = [(0, 1), (0, 2), (0, 3), (0, 5), (0, 9), (1, 4), (2, 6), (4, 7), (7, 8)]#, (0,0), (1,1), (2,2), (3,3), (4,4), (5,5), (6,6), (9, 9), (7,7), (8,8)]
for e in edge:
    G.add_edge(e[0], e[1])
    G.add_edge(e[1], e[0])
nx.draw(G, with_labels=True)
plt.savefig("Graph.png", format="PNG")
number_nodes = G.number_of_nodes()
# New models
def model_1():
    model_last = Model("last")
    model_last.setParam("LogToConsole", 0)
    model_last.setParam("TimeLimit", 60)
    s = model_last.addVars(G.nodes, vtype=GRB.BINARY, name = 's')
    y = model_last.addVars(G.nodes, vtype = GRB.BINARY, name='y')
    double_directed_edge =  set()
    for e in edge:
        double_directed_edge.add(e)
        double_directed_edge.add((e[1], e[0]))
    p = model_last.addVars(double_directed_edge, vtype = GRB.BINARY, name = 'p')

    # Modelo original
    model_last.setObjective(quicksum(s[i] for i in G.nodes), GRB.MINIMIZE)
    model_last.addConstr(quicksum(s[i] for i in G.nodes) >= 1)

    big_M = 2*len(G.nodes)#max([(G.degree[i]) for i in G.nodes])
    for (i, j) in double_directed_edge:
        model_last.addConstr(p[i, j] <= y[i])
        model_last.addConstr(p[i, j] + p[j, i] <= 1)
    model_last.addConstr(quicksum(p[i,j] for (i,j) in double_directed_edge) >= 1)
    for i in G.nodes:
        #model_last.addConstr(quicksum(p[i, j] for j in G.nodes if (i, j) in double_directed_edge) >= s[i]*(G.degree[i]))
        #model_last.addConstr(quicksum(p[j, i] for j in G.nodes if (j, i) in double_directed_edge) - big_M*(1-y[i]) >= (G.degree[i])/2)
        #model_last.addConstr(quicksum(p[i, j] for j in G.nodes if (i, j) in double_directed_edge) == (G.degree[i])*y[i])
        # Restricciones de test
        model_last.addConstr(quicksum(y[j] for j in G.neighbors(i)) - (G.degree[i]/2) * (1 - y[i]) <= (G.degree[i])*y[i])
        model_last.addConstr(y[i] >= s[i])
        #model_last.addConstr(quicksum(y[j] for j in G.neighbors(i)) >= G.degree[i]*y[i])
        model_last.addConstr(quicksum(y[j] for j in G.neighbors(i)) >= (G.degree[i]/2) * (y[i] - s[i]),
                        name=f"propaga_si_no_semilla_{i}")

    model_last.optimize()
    if model_last.Status != 2:
        model_last.computeIIS()
        model_last.write("model.ilp")
    computed_seed_set = {v for v in G.nodes if s[v].X == 1}
    computed_solution_value = quicksum(s[v] for v in computed_seed_set).getValue()
    print(computed_solution_value)



model_1()
