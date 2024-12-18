import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.util.Scanner;

public class Instance {
    static int numberNodes;
    static int numberEdges;
    static int seed;
    static int k;

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
                ArrayList<Integer> edgeList = graph.getOrDefault(edge[0], new ArrayList<>());
                edgeList.add(edge[1]);
                graph.put(edge[0], edgeList);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
