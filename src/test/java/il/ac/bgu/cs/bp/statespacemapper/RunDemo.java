package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;

public class RunDemo {
  public static void main(String[] args) throws Exception {
    String[] files = args.length == 0 ? new String[]{"TTT.js"} : args;
    var cli = new SpaceMapperCliRunner() {
      @Override
      protected void setExporterProviders(Exporter exporter, String runName, MapperResult res) {
        var vp = exporter.getVertexAttributeProvider();
        exporter.setVertexAttributeProvider(v -> {
            var map = vp.apply(v);
            map.remove("hash");
            map.remove("store");
            map.remove("statements");
            map.remove("bthreads");
            map.remove("start");
            map.remove("accepting");
            return map;
          }
        );

        var ep = exporter.getEdgeAttributeProvider();
        exporter.setEdgeAttributeProvider(e -> {
            var map = ep.apply(e);
            map.remove("Event");
            map.remove("Event_name");
            map.remove("Event_value");
            return map;
          }
        );
      }
    };
    cli.run(files);
  }
}
