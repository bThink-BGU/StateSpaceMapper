package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;
import org.jgrapht.nio.DefaultAttribute;

public class RunDemo {
  public static void main(String[] args) throws Exception {
    String[] files = args.length == 0 ? new String[]{"vault.js"} : args;
    BProgram bprog = new ResourceBProgram(files);
    var runName = bprog.getName();

    System.out.println("// start");

    // You can use a different EventSelectionStrategy, for example:
    var ess = new PrioritizedBSyncEventSelectionStrategy();
    bprog.setEventSelectionStrategy(ess);

    var mapper = new StateSpaceMapper(bprog, runName);

    //Example for adding attributes
    mapper.setAttributeProviderSetter(exporter -> {
      var graphProvider = exporter.getGraphAttributeProvider();
      exporter.setGraphAttributeProvider(() -> {
        var map = graphProvider.get();
        map.put("Attribute_name", DefaultAttribute.createAttribute("Attribute value"));
        return map;
      });
    });
    mapper.mapSpace();
    mapper.exportSpace();

//    WARNING: May take extremely long time and may generate extremely large files
//    mapper.writeCompressedPaths();

    System.out.println("// done");
  }
}
