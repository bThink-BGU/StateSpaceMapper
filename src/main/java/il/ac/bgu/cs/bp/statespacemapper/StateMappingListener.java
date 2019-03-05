package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsBProgramVerifier;
import il.ac.bgu.cs.bp.bpjs.analysis.DfsTraversalNode;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspections;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author michael
 */
public class StateMappingListener implements ExecutionTraceInspection, DfsBProgramVerifier.ProgressListener {
    
    private final PrintStream out;
    private final BProgram bprog;

    public StateMappingListener(BProgram aBProgram, PrintStream out) {
        this.out = out;
        bprog = aBProgram;
    }
    
    //////////////////////////////
    // Trace Inspection Methods
    //////////////////////////////
    
    @Override
    public String title() {
        return "StateMapping";
    }
    
    @Override
    public Optional<Violation> inspectTrace(ExecutionTrace et) {
        Optional<Violation> inspection = ExecutionTraceInspections.FAILED_ASSERTIONS.inspectTrace(et);
        
        if ( et.isCyclic() ) {
            out.println( bpssNodeId(et.getLastState()) + " -> " 
                + bpssNodeId(et.getFinalCycle().get(0).getState())
                + "[label=\"" + et.getLastEvent().getName() + "\"]");
        } else {
            out.println(newNode(et.getLastState(), inspection.isEmpty()));
            if ( et.getStateCount() > 1 ) {
                out.println( bpssNodeId(et.getNodes().get(et.getStateCount()-2).getState()) + " -> " 
                    + bpssNodeId(et.getLastState())
                    + "[label=\"" + et.getLastEvent().getName() + "\"]");
            } else {
                out.println( "start -> " + bpssNodeId(et.getLastState()) + " [color=blue]");
            }
        }
        
        // We prune on failed assertions.
        return inspection;
    }
    
    //////////////////////////////
    // Progress Listener Methods
    //////////////////////////////
    @Override
    public void started(DfsBProgramVerifier dbpv) {
        out.println("digraph " + GVUtils.sanitize(bprog.getName()) + " {");
        out.println("label=\"" + bprog.getName() + "\"");
        out.println("start [shape=none fontcolor=blue label=\"start\"]");
    }

    @Override
    public void iterationCount(long iteration, long statesFound, DfsBProgramVerifier dbpv) {
        System.err.println(" - " + iteration + "/" + statesFound);
    }

    @Override
    public void maxTraceLengthHit(List<DfsTraversalNode> list, DfsBProgramVerifier dbpv) {
        System.err.println("Max trace length hit");
    }

    @Override
    public boolean violationFound(Violation vltn, DfsBProgramVerifier dbpv) {
        return true;
    }

    @Override
    public void done(DfsBProgramVerifier dbpv) {
        out.println("}");
    }
    
    private static final String INVALID_NODE_STYLE = " color=red fontcolor=red shape=hexagon ";
    private static final String HOT_NODE_STYLE = " penwidth=2 color=\"#888800\" ";
    
    private String newNode( BProgramSyncSnapshot bpss, boolean isValid ) {
        String bpssNodeTitle = bpssNodeTitle(bpss);
        System.err.println( bpssNodeTitle + ":" );
        for ( BThreadSyncSnapshot btss : bpss.getBThreadSnapshots() ) {
            System.err.println("  " + btss.getName() + "@" + btss.getContinuationProgramState().getProgramCounter());
            System.err.println("   : " + btss.getSyncStatement());
        }
        return bpssNodeId(bpss) + "[label=\"" + bpssNodeTitle + "\""
            + (isValid? (bpss.isHot()?HOT_NODE_STYLE:"") :INVALID_NODE_STYLE) + "]";
    }
    
    private String bpssNodeTitle( BProgramSyncSnapshot bpss ) {
        Optional<BThreadSyncSnapshot> obt = bpss.getBThreadSnapshots().stream().filter( s -> s.getName().equals("stateTitler")).findAny();
//        return obt.map( bt -> bt.getSyncStatement().getData().toString() ).orElse(Integer.toHexString(bpss.hashCode()));
        return obt.map( bt -> bt.getSyncStatement().getData().toString() + "\\n" ).orElse("") + Integer.toHexString(bpss.hashCode());
    }
    
    private String bpssNodeId( BProgramSyncSnapshot bpss ) {
        return "bpss" + Integer.toHexString(bpss.hashCode());
    }
}
