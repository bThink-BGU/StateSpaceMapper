package il.ac.bgu.cs.bp.statespacemapper;

public class RunPerBThreadDemo {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      PerBThreadMain.main(new String[]{"PerBThreadSample.js"});
    } else {
      PerBThreadMain.main(args);
    }
  }
}
