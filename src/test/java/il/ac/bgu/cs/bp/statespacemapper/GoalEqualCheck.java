package il.ac.bgu.cs.bp.statespacemapper;

import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;
import org.svvrl.goal.core.aut.fsa.Equivalence;

import java.nio.file.Files;
import java.nio.file.Path;

public class GoalEqualCheck {
  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 432; i++) {
      if (i % 100 == 0)
        System.out.println("Checking " + i);
      var file0 = Files.readString(Path.of(getName(i, 0)));
      var fsa0 = GoalTool.string2fsa(file0, false);
      for (int j = 0; j < 20; j++) {
        var filej = Files.readString(Path.of(getName(i, j)));
        var fsaj = GoalTool.string2fsa(filej, false);
        var eq = new Equivalence();
        if (!eq.isEquivalent(fsa0, fsaj).isEquivalent()) {
          System.out.println("Automata " + getName(i, j) + " is not equal");
        }
      }
    }
  }

  private static String getName(int i, int j) {
    return "exports/test_" + i + "_" + j + ".gff";
  }
}
