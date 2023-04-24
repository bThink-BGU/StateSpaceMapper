package il.ac.bgu.cs.bp.statespacemapper;

public class RunDemo {
  public static void main(String[] args) throws Exception {
    String[] files = args.length == 0 ? new String[]{"vault.js"} : args;
    var cli = new SpaceMapperCliRunner();
    cli.run(files);
  }
}
