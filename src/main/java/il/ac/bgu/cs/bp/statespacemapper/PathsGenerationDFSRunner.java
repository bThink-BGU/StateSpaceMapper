package il.ac.bgu.cs.bp.statespacemapper;

import java.util.stream.Collectors;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.*;
import il.ac.bgu.cs.bp.bpjs.model.*;
import java.util.List;

public class PathsGenerationDFSRunner {
    public static void main(String[] args) throws Exception {
        BProgram bprog1 = new ResourceBProgram("lc_bp_v1.js");
        StateSpaceMapper mpr = new StateSpaceMapper();
        GenerateAllTracesInspection.MapperResult res = mpr.mapSpace(bprog1);
        PathsGenerationDFS<MapperVertex, MapperEdge> dfs = new PathsGenerationDFS<MapperVertex, MapperEdge>(res.graph);
        List<List<MapperEdge>> traces = dfs.getAllPaths(res.startNode, res.graph.vertexSet(), true, Integer.MAX_VALUE);
        printTraces(traces);
    }

    public static void printTraces(List<List<MapperEdge>> traces){
        List<List<String>> a = traces.stream()
        .map(l -> l.stream()
        .map(e -> ((BEvent)e.event).name)
        .collect(Collectors.toList()))
        .collect(Collectors.toList());
        System.out.println(a);
      }
}
