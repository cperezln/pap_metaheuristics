import os
import time

from gurobipy import *
import networkx as nx

model = Model("newPap")
model.setParam("LogToConsole", 0)
model.setParam("TimeLimit", 60)
G = nx.Graph()
edge = [(1, 3), (1, 6), (1, 4), (3, 4), (3, 6), (3, 5), (5, 4), (6, 2), (4, 2)]
for e in edge:
    G.add_edge(e[0], e[1])
    G.add_edge(e[1], e[0])

number_nodes = G.number_of_nodes()
x = model.addVars(G.nodes, vtype = GRB.BINARY, name='x')
s = model.addVars(G.nodes, vtype = GRB.INTEGER, name = 's')
p = model.addVars(G.nodes, G.nodes, vtype = GRB.BINARY, name = 'p')

# Modelo original
model.setObjective(quicksum(x[i] for i in G.nodes), GRB.MINIMIZE)

for node in G.nodes:
    model.addConstr(s[node] >= 1)
    for neigh in G.neighbors(node):
        model.addConstr(s[node] <= x[neigh] + 1000*(1-x[neigh]))
        for double_neigh in G.neighbors(neigh):
            model.addConstr(s[node] >= s[double_neigh] + 1 - 1000*(1 - p[(neigh, node)]))
    model.addConstr(s[node] <= 1000*(quicksum(x[neigh] for neigh in G.neighbors(node)) + x[node] + quicksum(p[(neigh, node)] for neigh in G.neighbors(node))))
    model.addConstr(quicksum(p[(node, neigh)] for neigh in G.neighbors(node)) <= 1)
    model.addConstr(quicksum(p[(neigh, node)] for neigh in G.neighbors(node)) <= 1)
    for i in G.nodes:
        for j in G.nodes:
            if i != j:
                model.addConstr(p[(i, j)] + p[(j, i)] <= 1)
    model.addConstr(s[node] <= len(G.nodes))
    for neigh in G.neighbors(node):
        model.addConstr(1000*(1 - p[(neigh, node)]) + quicksum(p[(double_neigh, neigh)] for double_neigh in G.neighbors(neigh)) - G.degree[neigh]*0.5 >= 0)

print("wuola")
model.optimize()
print("wadios")