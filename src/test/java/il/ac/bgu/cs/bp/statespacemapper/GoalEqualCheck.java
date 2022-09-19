package il.ac.bgu.cs.bp.statespacemapper;

import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;
import org.svvrl.goal.core.aut.fsa.Equivalence;
import org.svvrl.goal.core.aut.fsa.FSA;

import java.nio.file.Files;
import java.nio.file.Path;

public class GoalEqualCheck {
  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 432; i++) {
      if (i % 100 == 0)
        System.out.println("Checking " + i);
      var fsa0 = getFSA(i,0);
      for (int j = 0; j < 20; j++) {
        var fsaj = getFSA(i, j);
        var eq = new Equivalence();
        if (!eq.isEquivalent(fsa0, fsaj).isEquivalent()) {
          System.out.println("Automata " + getName(i, j) + " is not equal");
        }
      }
    }
  }

  public static FSA getFSA(int i, int j) throws Exception {
    var file = Files.readString(Path.of(getName(i, j)));
    return GoalTool.string2fsa(file, false);
  }

  private static String getName(int i, int j) {
    return "exports/test_" + i + "_" + j + ".gff";
  }
}
