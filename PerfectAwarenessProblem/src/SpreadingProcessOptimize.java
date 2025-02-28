import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;


public class SpreadingProcessOptimize {

    private Instance instance;

    public SpreadingProcessOptimize(Instance i) {
        this.instance = i;
    }

    // TODO sería interesante darle una vuelta a esta forma de estudiar si la solución es solución, efectivamente.

    public boolean isSolution(Solution sol) {
        int[] spreaderCount = new int[instance.getNumberNodes()];  // Para llevar la cuenta de vecinos propagadores
        int[] awareCount = new int[instance.getNumberNodes()];  // Para llevar la cuenta de vecinos propagadores
        // Auxiliar: para no contar spreaders de más
        /*---------------------------------------------------*/
        BigInteger[] checkSpreaders = new BigInteger[instance.getNumberNodes()];
        BigInteger nextPossible = sol.getBitwiseRepresentation();
        int index = nextPossible.getLowestSetBit();
        if (index != -1) {
            do {
                for(int neigh: instance.graph.get(index)) {
                    BigInteger actual = checkSpreaders[neigh];
                    if(actual == null) actual = BigInteger.ZERO;
                    actual = actual.setBit(index);
                    checkSpreaders[neigh] = actual;
                    spreaderCount[neigh]++;
                }
                nextPossible = nextPossible.xor(BigInteger.ONE.shiftLeft(index));
                index = nextPossible.getLowestSetBit();
            } while (index != -1);
        }
        /*---------------------------------------------------*/
        BigInteger spreadersTaubw = sol.getBitwiseRepresentation();
        BigInteger qSpreaders = sol.getBitwiseRepresentation();
        int awareSize = sol.getAware();
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
                        this.instance.setState(neigh, 1);
                    }
                    // The number of spreaders around neigh increments, as node is a spreader
                    if(checkSpreaders[neigh] == null) checkSpreaders[neigh] = BigInteger.ZERO;
                    if(!checkSpreaders[neigh].testBit(node)) {
                        checkSpreaders[neigh] = checkSpreaders[neigh].setBit(node);
                        spreaderCount[neigh]++;
                    }
                    if (!spreadersTaubw.testBit(neigh) && spreaderCount[neigh] >= instance.graph.get(neigh).size() * 0.5) {
                        spreadersTaupbw = spreadersTaupbw.setBit(neigh);
                        qSpreaders = qSpreaders.setBit(neigh);
                        this.instance.setState(neigh, 2);
                    }
                }
            }
        }
        sol.setAware(awareSize);
        return awareSize == instance.getNumberNodes();
    }

    public static boolean checkEqualSets(boolean[] s, boolean[] sp) {
        boolean equals = true;
        for(int i = 0; i < s.length; i++) equals = equals && (s[i] == sp[i]);
        return equals;
    }
}
