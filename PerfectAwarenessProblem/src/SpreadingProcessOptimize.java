import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;


public class SpreadingProcessOptimize {

    private Instance instance;
    private Instant startTime;

    // Reusable arrays to avoid allocation overhead
    private int[] spreaderCount;
    private int[] awareCount;
    private boolean[] visited;
    private boolean[] isSpreaderTaubw;
    private boolean[] isSpreaderTaupbw;
    private boolean[] isAware;
    private int[] qSpreaders;


    public SpreadingProcessOptimize(Instance i, Instant startTime) {
        this.instance = i;
        this.startTime = startTime;
        initializeArrays(i.getNumberNodes());
    }

    private void initializeArrays(int maxSize) {
        spreaderCount = new int[maxSize];
        awareCount = new int[maxSize];
        visited = new boolean[maxSize];
        isSpreaderTaubw = new boolean[maxSize];
        isSpreaderTaupbw = new boolean[maxSize];
        isAware = new boolean[maxSize];
        qSpreaders = new int[maxSize];
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public boolean isSolution(Solution sol) {
        int n = instance.getNumberNodes();

        clean_data_structures(n);
        int qSize = 0;

        /*---------------------------------------------------*/
        int awareSize = 0;
        BitSet initialSeeds = sol.getBitwiseRepresentation();
        for (int index = initialSeeds.nextSetBit(0); index >= 0; index = initialSeeds.nextSetBit(index + 1)) {
            isSpreaderTaubw[index] = true;
            isSpreaderTaupbw[index] = true;
            isAware[index] = true;
            awareSize++;
            qSpreaders[qSize++] = index;
        }
        awareCount = spreaderCount;
        /*---------------------------------------------------*/
        int qHead = 0;
        while(qHead < qSize && awareSize != n) {
            int node = qSpreaders[qHead++];
            if(!visited[node]) {
                visited[node] = true;
                for (int neigh : instance.graph.get(node)) {
                    // Become aware if your neighbor
                    if(!isAware[neigh]) {
                        isAware[neigh] = true;
                        awareSize++;
                        this.instance.setState(neigh, 1);
                        if (awareSize==n) break;
                    }
                    awareCount[node]++;
                    // The number of spreaders around neigh increments, as node is a spreader
                    spreaderCount[neigh]++;
                    if (!isSpreaderTaubw[neigh] && spreaderCount[neigh] >= instance.graph.get(neigh).size() * 0.5) {
                        isSpreaderTaupbw[neigh] = true;
                        isSpreaderTaubw[neigh] = true;
                        qSpreaders[qSize++] = neigh;
                        this.instance.setState(neigh, 2);
                    }
                }
            }
        }

        sol.setAware(awareSize);
        // Live view into the reusable buffer (no per-call copy - isSolution() runs too often
        // for that to be affordable on large instances). Valid only until the next isSolution()
        // call, which is fine: every current caller reads it right after evaluating this same sol.
        sol.setAwareCount(awareCount);

        return awareSize == n;
    }

    private void clean_data_structures(int n) {
        for (int i = 0; i < n; i++) {
            spreaderCount[i] = 0;
            awareCount[i] = 0;
            visited[i] = false;
            isSpreaderTaubw[i] = false;
            isSpreaderTaupbw[i] = false;
            isAware[i] = false;
        }
    }
}
