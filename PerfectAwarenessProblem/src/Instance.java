import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.util.Scanner;

public class Instance {
    private int numberNodes;
    private int numberEdges;
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
}
