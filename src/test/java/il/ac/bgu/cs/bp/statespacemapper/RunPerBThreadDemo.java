package il.ac.bgu.cs.bp.statespacemapper;

public class RunPerBThreadDemo {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      PerBTSpaceMapperRunner.main(new String[]{"PerBThreadSample.js"});
    } else {
      PerBTSpaceMapperRunner.main(args);
    }
  }
}
