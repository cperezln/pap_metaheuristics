import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.util.Scanner;

public class Instance {
    public String name;
    private int numberNodes;
    private int numberEdges;
    private HashMap<Integer, Float> centrality = new HashMap<>();
    private HashMap<Integer, Integer> eCentrality = new HashMap<>();
    private HashMap<Integer, Integer> bCentrality = new HashMap<>();
    private int seed;
    private int k;

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
                    centrality.put(edge[0], centrality.getOrDefault(edge[0], Float.valueOf(0)) + 1);
                    centrality.put(edge[1], centrality.getOrDefault(edge[1], Float.valueOf(0)) + 1);
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
        return 0.5*centrality.get(n);
    }

    public float getCentrality(int n) { return centrality.get(n); }

    public void setCentrality(String file) throws FileNotFoundException {
        String inPath = "/home/cristian/Escritorio/TFM/pap_metaheuristics/centralities/";
        HashMap<Integer, Float> bw = new HashMap<>();
        Scanner reader2 = new Scanner(new File(inPath + "betweeness/" + file));
        while (reader2.hasNext()) {
            String[] line = reader2.nextLine().split(" ");
            this.centrality.put(Integer.parseInt(line[0]), Float.parseFloat(line[1]));
        }
    }


}
