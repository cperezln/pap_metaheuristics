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
    private HashMap<Integer, Integer> state = new HashMap<>(); // 0: ignorant, 1: aware, 2: spreader
    private HashSet<Integer> leafNodes = new HashSet<>();
    private int seed, k;
    HashMap<Integer, ArrayList<Integer>> graph = new HashMap<>();

    private static ArrayList<int[]> preprocessing(ArrayList<int[]> arr, int numberNodes) {
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

        return newEdges;
    }

    public Instance(File file, String c) {
        try {
            this.setCentrality(c);
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
            ArrayList<int[]> newEdges = preprocessing(edgeList, givenNumberNodes);
            numberEdges = newEdges.size();
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
                    state.put(edge[0], 0);
                    state.put(edge[1], 0);
                }
            }
            numberNodes = graph.size();
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
        return new ArrayList<Integer>(this.graph.keySet());
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

    public void setCentrality(String file) throws FileNotFoundException {
        String inPath = "/home/cristian/Escritorio/TFM/pap_metaheuristics//centralities/";
        HashMap<Integer, Double> bw = new HashMap<>();
        Scanner reader2 = new Scanner(new File(inPath + "betweeness/" + file));
        HashMap<Integer, Double> betMap = new HashMap<>();
        HashMap<Integer, Double> degMap = new HashMap<>();
        HashMap<Integer, Double> eigMap = new HashMap<>();
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            betMap.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
        }
        reader2 = new Scanner(new File(inPath + "degree/" + file));
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            degMap.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
        }
        reader2 = new Scanner(new File(inPath + "eigenvector/" + file));
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            eigMap.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
        }
        for(Map.Entry<Integer, Double> i: degMap.entrySet()) {
            double betCet = betMap.get(i.getKey());
            double eigCet = eigMap.get(i.getKey());
            double degCet = i.getValue();
            double[] arrCet = {betCet, degCet, eigCet};
            this.centrality.put(i.getKey(), arrCet);
        }
    }

    public void setState(int node, int state) {
        switch (state) {
            case 0:
                this.state.put(node, 0);
                break;
            case 1:
                if (this.state.get(node) <= 1) {
                    this.state.put(node, 1);
                    break;
                }
            case 2:
                if(this.state.get(node) <= 2) {
                    this.state.put(node, 2);
                    break;
                }
        }
    }

    public void resetState(Solution sol) {
        this.state = new HashMap<>();
        for(int node = 0; node < numberNodes; node++) {
         if (sol.isIn(node)) { this.state.put(node, 2); }
         else { this.state.put(node, 0); }
        }
    }

    public int getNodeState(int node) { return this.state.get(node); }

    public boolean isLeaf(int node) { return this.leafNodes.contains(node);}


}
