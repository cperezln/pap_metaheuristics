"""
Centrality-based heuristics for the Perfect Awareness Problem, following
Gautam, Kare & Bhavani, "Centrality Measures Based Heuristics for Perfect
Awareness Problem in Social Networks" (MIWAI 2023, LNAI 14078, pp. 91-100),
https://doi.org/10.1007/978-3-031-36402-0_8

Implements:
  - Algorithm 1: GetPotSeedSet (centrality-ranked greedy construction)
  - Algorithm 2: Incremental diffusion (here: recomputed from scratch each
    step instead of incrementally maintained -- equivalent output, simpler,
    fine for graphs this size)
  - Algorithm 3: Pruning
using the five centrality measures from the paper: degree, eigenvector,
closeness, betweenness, page rank.
"""
import csv
import math
import os
import signal
import sys
import time

import networkx as nx

DEFAULT_PER_ALGO_TIMEOUT_SECS = int(os.environ.get("PER_ALGO_TIMEOUT_SECS", 2 * 3600))
CENTRALITY_NAMES = ["DC", "PR", "EC", "CC", "BC"]  # cheap/likely-to-finish first

path = os.path.dirname(os.path.abspath(__file__))


def load_instance(instance_path):
    with open(instance_path) as f:
        f.readline(); f.readline()
        n = int(f.readline())
        edges = int(f.readline())
        G = nx.Graph()
        G.add_nodes_from(range(n))
        for _ in range(edges):
            a, b = map(int, f.readline().split())
            G.add_edge(a, b)
    return G


def default_threshold(G):
    return {v: math.ceil(G.degree[v] / 2) for v in G.nodes}


def diffusion(G, seed_set, threshold):
    """Two-tier PAP diffusion: a node becomes AWARE as soon as any neighbor
    is a spreader; it becomes a SPREADER itself once >= threshold[v] of its
    neighbors are spreaders. Returns (aware_set, spreader_set)."""
    is_spreader = {v: False for v in G.nodes}
    spreader_count = {v: 0 for v in G.nodes}
    aware = set(seed_set)
    for v in seed_set:
        is_spreader[v] = True
    frontier = list(seed_set)
    while frontier:
        next_frontier = []
        for node in frontier:
            for neigh in G.neighbors(node):
                aware.add(neigh)
                spreader_count[neigh] += 1
                if not is_spreader[neigh] and spreader_count[neigh] >= threshold[neigh]:
                    is_spreader[neigh] = True
                    next_frontier.append(neigh)
        frontier = next_frontier
    spreaders = {v for v in G.nodes if is_spreader[v]}
    return aware, spreaders


def get_potential_seed_set(G, centrality, threshold):
    """Algorithm 1: greedily add vertices in decreasing order of centrality,
    keeping only those that strictly increase the aware set."""
    n = G.number_of_nodes()
    K = sorted(G.nodes, key=lambda v: centrality[v], reverse=True)
    S_hat = []
    A, Sp = diffusion(G, S_hat, threshold)
    for u in K:
        if u in Sp:
            # already a spreader as a side effect of the current S_hat
            continue
        A2, Sp2 = diffusion(G, S_hat + [u], threshold)
        if len(A2) > len(A):
            S_hat.append(u)
            A, Sp = A2, Sp2
        if len(A) == n:
            break
    return S_hat


def prune(G, S_hat, threshold):
    """Algorithm 3: try removing each seed (in decreasing order of how many
    of its neighbors are spreaders) if the set stays a perfect awareness set."""
    n = G.number_of_nodes()
    _, Sp_full = diffusion(G, S_hat, threshold)
    m = lambda u: sum(1 for w in G.neighbors(u) if w in Sp_full)
    K = sorted(S_hat, key=m, reverse=True)
    S = list(S_hat)
    for u in K:
        candidate = [x for x in S if x != u]
        A, _ = diffusion(G, candidate, threshold)
        if len(A) == n:
            S = candidate
    return S


def compute_one_centrality(G, cname):
    if cname == "DC":
        return dict(G.degree())
    if cname == "PR":
        return nx.pagerank(G)
    if cname == "EC":
        return nx.eigenvector_centrality(G, max_iter=1000)
    if cname == "CC":
        return nx.closeness_centrality(G)
    if cname == "BC":
        return nx.betweenness_centrality(G)
    raise ValueError(cname)


class _AlgoTimeout(Exception):
    pass


def _alarm_handler(signum, frame):
    raise _AlgoTimeout()


def open_csv(name, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    csv_path = os.path.join(out_dir, f"{name}.csv")
    f = open(csv_path, "w", newline="")
    writer = csv.writer(f)
    writer.writerow(["instance", "algorithm", "objective", "feasible", "time_s", "status"])
    f.flush()
    return f, writer, csv_path


def append_csv_row(f, writer, name, cname, objective, feasible, elapsed, status):
    writer.writerow([name, cname, objective if objective is not None else "", feasible, f"{elapsed:.6f}", status])
    f.flush()


def run_all_heuristics(G, name, out_dir, per_algo_timeout=DEFAULT_PER_ALGO_TIMEOUT_SECS):
    """Runs each of the 5 centrality-based heuristics independently, each
    capped at `per_algo_timeout` seconds (covers both computing the
    centrality and building/pruning the seed set). Prints and appends to the
    CSV immediately after each algorithm finishes, times out, or errors, so
    results for the algorithms that DO finish are never lost even if a later
    one hangs/gets killed."""
    threshold = default_threshold(G)
    n = G.number_of_nodes()
    print(f"\n=== {name} (n={n}, e={G.number_of_edges()}) ===", flush=True)

    f, writer, csv_path = open_csv(name, out_dir)
    results = {}
    old_handler = signal.signal(signal.SIGALRM, _alarm_handler)
    try:
        for cname in CENTRALITY_NAMES:
            t0 = time.time()
            signal.alarm(per_algo_timeout)
            try:
                centrality = compute_one_centrality(G, cname)
                S_hat = get_potential_seed_set(G, centrality, threshold)
                S = prune(G, S_hat, threshold)
                signal.alarm(0)
                elapsed = time.time() - t0
                A, _ = diffusion(G, S, threshold)
                feasible = len(A) == n
                results[cname] = (len(S), feasible, elapsed, sorted(S))
                status = "OK" if feasible else "INFEASIBLE"
                print(f"  {cname}: seed size={len(S):4d}  feasible={status}  time={elapsed:.3f}s  seed={S}", flush=True)
                append_csv_row(f, writer, name, cname, len(S), feasible, elapsed, status)
            except _AlgoTimeout:
                elapsed = time.time() - t0
                print(f"  {cname}: TIMEOUT after {elapsed:.1f}s (limit {per_algo_timeout}s) -- skipped", flush=True)
                append_csv_row(f, writer, name, cname, None, False, elapsed, "TIMEOUT")
            except Exception as e:
                signal.alarm(0)
                elapsed = time.time() - t0
                print(f"  {cname}: ERROR after {elapsed:.1f}s: {e!r}", flush=True)
                append_csv_row(f, writer, name, cname, None, False, elapsed, f"ERROR: {e}")
    finally:
        signal.alarm(0)
        signal.signal(signal.SIGALRM, old_handler)
        f.close()

    print(f"  [csv written to {csv_path}]", flush=True)
    return results


if __name__ == "__main__":
    out_dir = f"{path}/results_centrality"

    if len(sys.argv) > 1:
        for instance_path in sys.argv[1:]:
            name = os.path.splitext(os.path.basename(instance_path))[0]
            G = load_instance(instance_path)
            run_all_heuristics(G, name, out_dir)
    else:
        karate = load_instance(f"{path}/previous_work/instances-nuevas/karate.in")
        run_all_heuristics(karate, "karate", out_dir)

        jazz = load_instance(f"{path}/previous_work/instances-nuevas/jazz.in")
        run_all_heuristics(jazz, "jazz", out_dir)
