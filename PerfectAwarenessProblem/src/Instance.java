import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.util.Scanner;

public class Instance {
    public String name;
    private int numberNodes;
    private int numberEdges;
    public HashMap<Integer, Integer> nSpreaders = new HashMap<>();
    private HashMap<Integer, Integer> dCentrality = new HashMap<>();
    private int seed;
    private int k;

    HashMap<Integer, ArrayList<Integer>> graph = new HashMap<>();

    public Instance(File file) {
        try {
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
                    if(!nSpreaders.containsKey(edge[0])) nSpreaders.put(edge[0], 0);
                    if(!nSpreaders.containsKey(edge[1])) nSpreaders.put(edge[1], 0);
                    dCentrality.put(edge[0], dCentrality.getOrDefault(edge[0], 0) + 1);
                    dCentrality.put(edge[1], dCentrality.getOrDefault(edge[1], 0) + 1);
                    graph.put(edge[0], edgeListStart);
                    graph.put(edge[1], edgeListEnd);
                }
            }
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

    public double nodeValue(int n) {
        return 0.5*dCentrality.get(n) - nSpreaders.get(n);
    }

    public int getCentrality(int n) { return dCentrality.get(n); }
}
