import java.math.BigInteger;


public class SpreadingProcess {

    private Instance instance;

    public SpreadingProcess(Instance i) {
        this.instance = i;
    }

    // TODO sería interesante darle una vuelta a esta forma de estudiar si la solución es solución, efectivamente.
    // Si bien es cierto que haría falta simular el proceso de propagación, ¿sería necesario hacerlo con tantas estructuras de datos complejas? Creo que podríamos
    // sacar un algoritmo basado en estructuras más simples, de tal manera que la complejidad computacional se redujese
    public boolean isSolution(Solution sol) {
        // We don't need to know the round we become aware/spreaders. If so, we can compute it just adding a null every time we insert every new neighbor in the queue
        int[] spreaderCount = new int[instance.getNumberNodes()];  // Para llevar la cuenta de vecinos propagadores
        int[] awareCount = new int[instance.getNumberNodes()];  // Para llevar la cuenta de vecinos propagadores

        BigInteger spreadersTaubw = sol.getBitwiseRepresentation();
        BigInteger qSpreaders = sol.getBitwiseRepresentation();
        int awareSize = sol.solutionValue();
        BigInteger aware = sol.getBitwiseRepresentation();
        boolean[] visited = new boolean[instance.getNumberNodes()];
        BigInteger spreadersTaupbw = spreadersTaubw;
        while(!qSpreaders.equals(BigInteger.ZERO) && awareSize != instance.getNumberNodes()) {
            spreadersTaubw = spreadersTaupbw;
            int node = qSpreaders.getLowestSetBit();
            qSpreaders = qSpreaders.clearBit(node);
            if(!visited[node]) {
                visited[node] = true;
                for (Integer neigh : instance.graph.get(node)) {
                    // Become aware if your neigbor
                    if(!aware.testBit(neigh)) {
                        aware = aware.setBit(neigh);
                        awareSize++;
                        awareCount[node]++;
                    }
                    // The number of spreaders around neigh increments, as node is a spreader
                    spreaderCount[neigh]++;
                    awareCount[neigh]++;
                    if (!spreadersTaubw.testBit(neigh) && spreaderCount[neigh] >= instance.graph.get(neigh).size() * 0.5) {
                        spreadersTaupbw = spreadersTaupbw.xor(BigInteger.ONE.shiftLeft(neigh));
                        qSpreaders = qSpreaders.setBit(neigh);
                    }
                }
            }
        }
        sol.setAware(awareSize);
        sol.setAwareCount(awareCount);
        sol.setSpreaderCount(spreaderCount);
        return awareSize == instance.getNumberNodes();
    }

    public static boolean checkEqualSets(boolean[] s, boolean[] sp) {
        boolean equals = true;
        for(int i = 0; i < s.length; i++) equals = equals && (s[i] == sp[i]);
        return equals;
    }
}
