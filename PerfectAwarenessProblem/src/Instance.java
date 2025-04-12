import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;
import java.io.File;

public class Instance {
    static float betFact;
    static float degFact;
    static float eigFact;
    public String name;
    private int numberNodes;
    private int numberEdges;
    private HashMap<Integer, Integer> degreeMap = new HashMap<>();
    private HashMap<Integer, Float> centrality = new HashMap<>();
    private HashMap<Integer, Integer> state = new HashMap<>(); // 0: ignorant, 1: aware, 2: spreader
    HashSet<Integer> leafNodes = new HashSet<>();
    private int seed, k;
    HashMap<Integer, ArrayList<Integer>> graph = new HashMap<>();

    public Instance(File file, String c) {
        try {
            this.setCentrality(c);
            Scanner reader = new Scanner(file);
            seed = Integer.parseInt(reader.nextLine());
            k = Integer.parseInt(reader.nextLine());
            numberNodes = Integer.parseInt(reader.nextLine());
            numberEdges = Integer.parseInt(reader.nextLine());
            name = file.getName();
            for(int i = 0; i < numberEdges; i++) {
                String[] sEdge = reader.nextLine().split(" ");
                int[] edge = {Integer.parseInt(sEdge[0]), Integer.parseInt(sEdge[1])};
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
                    centrality.put(edge[0], centrality.getOrDefault(edge[0], Float.valueOf(0)) + 1);
                    centrality.put(edge[1], centrality.getOrDefault(edge[1], Float.valueOf(0)) + 1);
                    graph.put(edge[0], edgeListStart);
                    graph.put(edge[1], edgeListEnd);
                    state.put(edge[0], 0);
                    state.put(edge[1], 0);
                }
            }
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
    public float getCentrality(int n) { return centrality.get(n); }

    public int getGreatestDegree() {
        int maxDeg = 0;
        for(int node = 0; node < numberNodes; node++) if(maxDeg < degreeMap.get(node)) maxDeg = degreeMap.get(node);
        return maxDeg;
    }
    public void setCentrality(String file) throws FileNotFoundException {
        String inPath = "/home/cristian/Escritorio/TFM/to_send/centralities/";
        HashMap<Integer, Float> bw = new HashMap<>();
        Scanner reader2 = new Scanner(new File(inPath + "betweeness/" + file));
        HashMap<Integer, Float> betMap = new HashMap<>();
        HashMap<Integer, Float> degMap = new HashMap<>();
        HashMap<Integer, Float> eigMap = new HashMap<>();
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            betMap.put(Integer.parseInt(line[0]), Float.parseFloat(line[1]));
        }
        reader2 = new Scanner(new File(inPath + "degree/" + file));
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            degMap.put(Integer.parseInt(line[0]), Float.parseFloat(line[1]));
        }
        reader2 = new Scanner(new File(inPath + "eigenvector/" + file));
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            eigMap.put(Integer.parseInt(line[0]), Float.parseFloat(line[1]));
        }
        for(Map.Entry<Integer, Float> i: degMap.entrySet()) {
            float numer = i.getValue()*degFact + betMap.get(i.getKey())*betFact + eigMap.get(i.getKey())*eigFact;
            float denom = degFact + betFact + eigFact;
            this.centrality.put(i.getKey(), (numer)/(denom));
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


}
