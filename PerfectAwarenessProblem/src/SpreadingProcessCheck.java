import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;


public class SpreadingProcessCheck {

    private Instance instance;

    public SpreadingProcessCheck(Instance i) {
        this.instance = i;
    }

    // TODO sería interesante darle una vuelta a esta forma de estudiar si la solución es solución, efectivamente.
    // Si bien es cierto que haría falta simular el proceso de propagación, ¿sería necesario hacerlo con tantas estructuras de datos complejas? Creo que podríamos
    // sacar un algoritmo basado en estructuras más simples, de tal manera que la complejidad computacional se redujese
    public boolean isSolution(ArrayList<Integer> sol) {
        // We don't need to know the round we become aware/spreaders. If so, we can compute it just adding a null every time we insert every new neighbor in the queue
        Queue<Integer> qSpreaders = new LinkedList<>();
        boolean[] spreadersTau = new boolean[instance.getNumberNodes()];
        boolean[] spreadersTaup = new boolean[instance.getNumberNodes()];
        boolean[] aware = new boolean[instance.getNumberNodes()];
        int[] spreaderCount = new int[instance.getNumberNodes()];  // Para llevar la cuenta de vecinos propagadores

        int awareSize = 0;
        for(Integer i: sol) {
            qSpreaders.add(i);
            spreadersTaup[i] = true;
            aware[i] = true;
            awareSize++;
        }
        boolean[] visited = new boolean[instance.getNumberNodes()];
        while(qSpreaders.size() > 0 && awareSize != instance.getNumberNodes()) {
            spreadersTau = spreadersTaup;
            int node = qSpreaders.poll();
            if(!visited[node]) {
                visited[node] = true;
                for (Integer neigh : instance.graph.get(node)) {
                    // Become aware if your neigbor
                    if(!aware[neigh]) {
                        aware[neigh] = true;
                        awareSize++;
                    }
                    // The number of spreaders around neigh increments, as node is a spreader
                    spreaderCount[neigh]++;
                    if (!spreadersTau[neigh] && spreaderCount[neigh] >= instance.graph.get(neigh).size() * 0.5) {
                        spreadersTaup[neigh] = true;
                        qSpreaders.add(neigh);
                    }
                }
            }
        }
        for(int i = 0; i < aware.length; i++) {
            if(!aware[i]) System.out.println(i);
        }
        return awareSize == instance.getNumberNodes();
    }
}