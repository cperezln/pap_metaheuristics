#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.
#This program is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU General Public License for more details.
#You should have received a copy of the GNU General Public License
#along with this program.  If not, see <https://www.gnu.org/licenses/>5.

#File: BA_RSP_Graph_Generator.py
#Version: 1.0
#Description: Generator of graphs with characteristics of social networks based on the Barábasi-Albert model.
#Author: Felipe de C. Pereira
#University of Campinas
#Copyright© 2020

import networkx as nx
import numpy as np
import math
import random
from sympy.solvers import solve
from sympy import Symbol
from sympy import N

# Return a random subset of seq with m elements
def _random_subset(seq, m):
    sources = set()
    while len(sources) < m:
        x = random.choice(seq)
        sources.add(x)
    return sources

# Add n_excedent_edges in a graph g with n_vertices
def add_excedent_edges(g, n_vertices, n_excedent_edges, seed):

	random.seed(seed) # Start the random number generator with a seed
	vertices = list(range(n_vertices)) # Get a list of vertex ids
	sources = _random_subset(vertices, n_excedent_edges) # Get m random vertex ids

	# For each vertex u in sources do
	for source in sources:
		targets = [] # List of vertices to be connected to u
		for vertex in range(n_vertices):  # For each vertex v
			if not vertex in g.neighbors(source): # If v is not adjacent to u
				targets.extend([vertex]*(g.degree[vertex])) # Add v to the list targets d_v times where d_v = degree of v

		chosen_target = random.choice(targets) # Chose a random v in targets
		g.add_edge(source, chosen_target) # Add the edge {u, v}

# Get input args
inputArgs = input()
inputArgs = inputArgs.split()

n_vertices = int(inputArgs[0]) # Number of vertices
n_edges = int(inputArgs[1]) # Number of edges

# Seed to be used in the random number generator
seed = 0
if(len(inputArgs) > 2):
	seed = int(inputArgs[2]) # Get the seed from input
else:
	seed = np.random.randint(1, 1000000000) # Generate the seed

# Calculate m_0 of Barabasi-Albert algorithm
x = Symbol('x')
result = solve(n_vertices*x - n_edges - x**2) # Solve the equation x^2 - n_vertices x + n_edges = 0
m_0 = math.floor(min(float(N(r)) for r in result)) # Select m_0 as the smallest root

# Generate graph by Barabasi-Albert algorithm 
g = nx.barabasi_albert_graph(n_vertices, m_0, seed)

# Calculate the number of excedent edges, that is, the edges that will be added in order to have n_edges in g
n_excedent_edges = n_edges - g.number_of_edges()

# Add excedent edges in g
add_excedent_edges(g, n_vertices, n_excedent_edges, seed)

# Get list of edges in g
list_of_edges = '\n'.join(str(u) + ' ' + str(v) for u, v in g.edges())

# Save output file
output_file_name = str(n_vertices) + '_' + str(g.number_of_edges()) + '_' + str(m_0) + '_social' + '.in'
output_string = [seed, m_0, n_vertices, g.number_of_edges()]
output_string = [str(val) + '\n' for val in output_string]
output_string.append(list_of_edges)
with open(output_file_name, mode='w') as f:
	f.writelines(output_string)