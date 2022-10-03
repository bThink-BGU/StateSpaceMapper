package il.ac.bgu.cs.bp.statespacemapper;

public class RunDemo {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      SpaceMapperCliRunner.main(new String[]{"vault.js"});
    } else {
      SpaceMapperCliRunner.main(args);
    }
  }
}
