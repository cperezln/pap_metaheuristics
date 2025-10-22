import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;
import java.io.File;


public class Instance {
    public String name;
    private int numberNodes;
    private int numberEdges;
    private HashMap<Integer, Integer> degreeMap = new HashMap<>();
    private HashMap<Integer, double[]> centrality = new HashMap<>();
    private int[] state; // 0: ignorant, 1: aware, 2: spreader
    private HashSet<Integer> leafNodes = new HashSet<>();
    private int seed, k;
    HashMap<Integer, ArrayList<Integer>> graph = new HashMap<>();
    private int uniqueNode = 0;

    private ArrayList<int[]> oldpreprocessing(ArrayList<int[]> arr, int numberNodes) {
        HashMap<Integer, ArrayList<Integer>> localGraph = new HashMap<>();
        HashMap<Integer, Integer> localDegreeMap = new HashMap<>();
        int lastNodeLabel = 0;
        int[] mapLabels = new int[numberNodes];
        Arrays.fill(mapLabels, -1);
        for (int i = 0; i < arr.size(); i++) {
            int[] edge = arr.get(i);
            if (edge[0] != edge[1]) {
                ArrayList<Integer> edgeListStart = localGraph.getOrDefault(edge[0], new ArrayList<>());
                ArrayList<Integer> edgeListEnd = localGraph.getOrDefault(edge[1], new ArrayList<>());
                if (!edgeListStart.contains(edge[1])) {
                    edgeListStart.add(edge[1]);
                }
                if (!edgeListEnd.contains(edge[0])) {
                    edgeListEnd.add(edge[0]);
                }
                localDegreeMap.put(edge[0], localDegreeMap.getOrDefault(edge[0], Integer.valueOf(0)) + 1);
                localDegreeMap.put(edge[1], localDegreeMap.getOrDefault(edge[1], Integer.valueOf(0)) + 1);
                localGraph.put(edge[0], edgeListStart);
                localGraph.put(edge[1], edgeListEnd);
            }
        }
        for(int i = 0; i < arr.size(); i++) {
            int[] edge = arr.get(i);
            if (edge[0] != edge[1]) {
                if (localDegreeMap.get(edge[0]) == 2 || localDegreeMap.get(edge[1]) == 2) {
                    if(mapLabels[edge[0]] != -1 && mapLabels[edge[1]] == -1) {
                        mapLabels[edge[1]] = mapLabels[edge[0]];
                    }
                    else if(mapLabels[edge[0]] == -1 && mapLabels[edge[1]] != -1) {
                        mapLabels[edge[0]] = mapLabels[edge[1]];
                    }
                    else if(mapLabels[edge[0]] == -1 && mapLabels[edge[1]] == -1) {
                        mapLabels[edge[0]] = lastNodeLabel;
                        mapLabels[edge[1]] = lastNodeLabel;
                        lastNodeLabel++;
                    }
                    else {
                        int firstLabel = mapLabels[edge[0]];
                        int secondLabel = mapLabels[edge[1]];
                        for(int j = 0; j < mapLabels.length; j++){
                            if(mapLabels[j] == firstLabel || mapLabels[j] == secondLabel) {
                                mapLabels[j] = lastNodeLabel;
                            }
                        }
                        mapLabels[edge[0]] = lastNodeLabel;
                        mapLabels[edge[1]] = lastNodeLabel;
                        lastNodeLabel++;
                    }
                } else {
                    if(mapLabels[edge[0]] == -1) {
                        mapLabels[edge[0]] = lastNodeLabel;
                        lastNodeLabel++;
                    }
                    if(mapLabels[edge[1]] == -1) {
                    mapLabels[edge[1]] = lastNodeLabel;
                    lastNodeLabel++;
                    }
                }
            }
        }
        ArrayList<int[]> newEdges = new ArrayList<>();
        HashMap<Integer, Integer> finalLabels = new HashMap<>();
        int lastAvailableNode = 0;
        for(int i = 0; i < arr.size(); i++) {
            int[] edge = arr.get(i);
            if(mapLabels[edge[0]] != mapLabels[edge[1]]) {
                if(!finalLabels.containsKey(mapLabels[edge[0]])) {
                    finalLabels.put(mapLabels[edge[0]], lastAvailableNode);
                    lastAvailableNode++;
                }
                if(!finalLabels.containsKey(mapLabels[edge[1]])) {
                    finalLabels.put(mapLabels[edge[1]], lastAvailableNode);
                    lastAvailableNode++;
                }
                int[] newEdge = {finalLabels.get(mapLabels[edge[0]]), finalLabels.get(mapLabels[edge[1]])};
                newEdges.add(newEdge);
            }
        }
        HashSet<Integer> finalDegreeSet = new HashSet<>(mapLabels.length);
        for(int i = 0; i < mapLabels.length; i++) {
            finalDegreeSet.add(mapLabels[i]);
        }
        if(finalDegreeSet.size() == 1){
            int elem = finalDegreeSet.iterator().next();
            uniqueNode = elem;
        }
        return newEdges;
    }

    static class UnionFind {
        int[] parent, rank;
        UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }
        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);
            return parent[x];
        }
        void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if (rank[ra] < rank[rb]) parent[ra] = rb;
            else if (rank[ra] > rank[rb]) parent[rb] = ra;
            else { parent[rb] = ra; rank[ra]++; }
        }
    }

    /**
     * Performs preprocessing by merging adjacent nodes u,v if
     * ceil(0.5 * degree(u)) = ceil(0.5 * degree(v)) = 1
     * (i.e., degree(u) <= 2 and degree(v) <= 2).
     *
     * @param edges list of edges (each int[]{u,v}) with arbitrary integer node labels
     * @return reduced edge list (nodes relabeled 0..k-1)
     */
    public static ArrayList<int[]> preprocessing(ArrayList<int[]> edges) {
        if (edges.isEmpty()) return new ArrayList<>();

        // --- Step A: map arbitrary node labels to compact indices ---
        HashMap<Integer, Integer> labelToIndex = new HashMap<>();
        ArrayList<Integer> indexToLabel = new ArrayList<>();
        for (int[] e : edges) {
            int u = e[0], v = e[1];
            if (!labelToIndex.containsKey(u)) {
                labelToIndex.put(u, indexToLabel.size());
                indexToLabel.add(u);
            }
            if (!labelToIndex.containsKey(v)) {
                labelToIndex.put(v, indexToLabel.size());
                indexToLabel.add(v);
            }
        }

        int n = indexToLabel.size();
        int[] degree = new int[n];

        // --- Step B: compute node degrees ---
        for (int[] e : edges) {
            int iu = labelToIndex.get(e[0]);
            int iv = labelToIndex.get(e[1]);
            if (iu == iv) continue;  // ignore self-loops
            degree[iu]++;
            degree[iv]++;
        }

        // --- Step C: Union nodes u,v if both satisfy ceil(0.5*deg) = 1 ---
        UnionFind uf = new UnionFind(n);
        for (int[] e : edges) {
            int iu = labelToIndex.get(e[0]);
            int iv = labelToIndex.get(e[1]);
            if (iu == iv) continue;
            // Merge if both degrees <= 2
            if (degree[iu] <= 2 && degree[iv] <= 2) {
                uf.union(iu, iv);
            }
        }

        // --- Step D: Compact representatives ---
        HashMap<Integer, Integer> repToNew = new HashMap<>();
        int nextId = 0;
        for (int i = 0; i < n; i++) {
            int rep = uf.find(i);
            if (!repToNew.containsKey(rep)) {
                repToNew.put(rep, nextId++);
            }
        }

        // --- Step E: Build reduced edge list ---
        HashSet<Long> seen = new HashSet<>();
        ArrayList<int[]> newEdges = new ArrayList<>();

        for (int[] e : edges) {
            int iu = labelToIndex.get(e[0]);
            int iv = labelToIndex.get(e[1]);
            int a = repToNew.get(uf.find(iu));
            int b = repToNew.get(uf.find(iv));
            if (a == b) continue;  // self-loop after merge
            // deduplicate unordered pairs (a,b)
            int min = Math.min(a, b), max = Math.max(a, b);
            long key = (((long)min) << 32) | (max & 0xffffffffL);
            if (seen.add(key)) {
                newEdges.add(new int[]{min, max});
            }
        }

        return newEdges;
    }

    public Instance(File file, String c) {
        try {
            Scanner reader = new Scanner(file);
            seed = Integer.parseInt(reader.nextLine());
            k = Integer.parseInt(reader.nextLine());
            int givenNumberNodes = Integer.parseInt(reader.nextLine());
            int givenNumberEdges = Integer.parseInt(reader.nextLine());
            name = file.getName();
            ArrayList<int[]> edgeList = new ArrayList<>();
            // Preprocesado para estudiar los nodos que pueden colapsarse, siguiendo la filosofía del estado del arte
            for(int i = 0; i < givenNumberEdges; i++) {
                String[] sEdge = reader.nextLine().split(" ");
                int[] edge = {Integer.parseInt(sEdge[0]), Integer.parseInt(sEdge[1])};
                edgeList.add(edge);
            }
            //numberNodes = givenNumberNodes;
            ArrayList<int[]> newEdges = preprocessing(edgeList);
            //numberEdges = newEdges.size();
            for(int[] edge: newEdges) {
                if(edge[0] != edge[1]) {
                    ArrayList<Integer> edgeListStart = graph.getOrDefault(edge[0], new ArrayList<>());
                    ArrayList<Integer> edgeListEnd = graph.getOrDefault(edge[1], new ArrayList<>());
                    if (!edgeListStart.contains(edge[1])) {
                        edgeListStart.add(edge[1]);
                    }
                    if (!edgeListEnd.contains(edge[0])) {
                        edgeListEnd.add(edge[0]);
                    }
                    degreeMap.put(edge[0], degreeMap.getOrDefault(edge[0], Integer.valueOf(0)) + 1);
                    degreeMap.put(edge[1], degreeMap.getOrDefault(edge[1], Integer.valueOf(0)) + 1);
                    graph.put(edge[0], edgeListStart);
                    graph.put(edge[1], edgeListEnd);
                }
            }
            numberNodes = graph.size();
            state = new int[numberNodes];
            // Arrays.fill(state, 0); // Not needed, default is 0
            this.setCentrality(c);
            this.setLeafNodes();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNumberNodes() {
        return numberNodes;
    }

    public ArrayList<Integer> getNodes() {
        ArrayList<Integer> nodes = new ArrayList<Integer>(this.graph.keySet());
        Collections.sort(nodes);  // Ordenar para garantizar determinismo
        return nodes;
    }

    public void setLeafNodes() {
        for(Map.Entry<Integer, Integer> entry: degreeMap.entrySet()) {
            if(entry.getValue() == 1) this.leafNodes.add(entry.getKey());
        }
    }
    public double[] getCentrality(int n) { return centrality.get(n); }

    public int getGreatestDegree() {
        int maxDeg = 0;
        for(int node = 0; node < numberNodes; node++) if(maxDeg < degreeMap.get(node)) maxDeg = degreeMap.get(node);
        return maxDeg;
    }

    public void setCentrality(String file) {
        String inPath = System.getProperty("user.dir") + "/centralities/";
        HashMap<Integer, Double> betMap = new HashMap<>();
        HashMap<Integer, Double> degMap = new HashMap<>();
        HashMap<Integer, Double> eigMap = new HashMap<>();

        // Try to load centrality files, if they don't exist, use default values
        try {
            Scanner reader2 = new Scanner(new File(inPath + "betweeness/" + file));
            while (reader2.hasNext()) {
                String[] line = reader2.nextLine().split(" ");
                betMap.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
            }
            reader2.close();

            reader2 = new Scanner(new File(inPath + "degree/" + file));
            while (reader2.hasNext()) {
                String[] line = reader2.nextLine().split(" ");
                degMap.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
            }
            reader2.close();

            reader2 = new Scanner(new File(inPath + "eigenvector/" + file));
            while (reader2.hasNext()) {
                String[] line = reader2.nextLine().split(" ");
                eigMap.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
            }
            reader2.close();

            for(Map.Entry<Integer, Double> i: degMap.entrySet()) {
                double betCet = betMap.get(i.getKey());
                double eigCet = eigMap.get(i.getKey());
                double degCet = i.getValue();
                double[] arrCet = {betCet, degCet, eigCet};
                this.centrality.put(i.getKey(), arrCet);
            }
        } catch (FileNotFoundException e) {
            // If centrality files don't exist, use default values (0.5 for all)
            System.out.println("Warning: Centrality files not found. Using default values.");
            for(int i = 0; i < this.numberNodes; i++) {
                double[] arrCet = {0.5, 0.5, 0.5}; // Default centrality values
                this.centrality.put(i, arrCet);
            }
        }
    }

    public void setState(int node, int newState) {
        switch (newState) {
            case 0:
                this.state[node] = 0;
                break;
            case 1:
                if (this.state[node] <= 1) {
                    this.state[node] = 1;
                    break;
                }
            case 2:
                if(this.state[node] <= 2) {
                    this.state[node] = 2;
                    break;
                }
        }
    }

    public void resetState(Solution sol) {
        for(int node = 0; node < numberNodes; node++) {
         if (sol.isIn(node)) { this.state[node] = 2; }
         else { this.state[node] = 0; }
        }
    }

    public int getNodeState(int node) { return this.state[node]; }

    public boolean isLeaf(int node) { return this.leafNodes.contains(node);}

    public int getUniqueNode() {
        return uniqueNode;
    }

    public int getNumberEdges() {
        return numberEdges;
    }
}
