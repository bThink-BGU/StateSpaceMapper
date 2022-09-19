package il.ac.bgu.cs.bp.statespacemapper;

public class CheckGraphEquality {
  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 432; i++) {
      var graph0 = Bug2Detector.getResourceFileAsString(getName(i,0));
      var goal0 = GoalTool.string2fsa(graph0,false);
      for (int j = 1; j < 20; j++) {
        var graphJ = Bug2Detector.getResourceFileAsString(getName(i,j));
        var goalJ = GoalTool.string2fsa(graphJ,false);
      }
    }
  }
  private static String getName(int i, int j){
    return "exports/test_"+i+"_"+j;
  }
}
