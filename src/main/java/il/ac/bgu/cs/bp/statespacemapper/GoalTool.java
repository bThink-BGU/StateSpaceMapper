package il.ac.bgu.cs.bp.statespacemapper;

import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.aut.fsa.Equivalence;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.CodecException;
import org.svvrl.goal.core.io.FSACodec;
import org.svvrl.goal.core.logic.ParseException;
import org.svvrl.goal.core.logic.re.REParser;
import org.svvrl.goal.core.logic.re.RESimplifier;
import org.svvrl.goal.core.logic.re.RETranslator;
import org.svvrl.goal.core.logic.re.RegularExpression;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@SuppressWarnings("unused")
public class GoalTool {
  /**
   * Simplifies GOAL's regular expression.<br/>
   * The regular expression, represented as a {@link String}, must follow this format:<br/>
   * Operators:
   * Empty: E
   * Epsilon: e
   * Alternation: a | b
   * Concatenation: a b
   * Kleene Star: a*
   * Kleene Cross: a+
   * Zero or One: a?
   *
   * @param regex
   * @return
   */
  public static String simplifyGoalRegex(String regex) throws ParseException, UnsupportedException {
    return simplifyGoalRegex(string2regex(regex)).toString();
  }

  public static String noam2goalRegexFormat(String regex) {
    return regex.replace('$', 'e').replace('+', '|').replaceAll("\\(\\)", "E");
  }

  /**
   * Generates a {@link FSA} from the output of {@link il.ac.bgu.cs.bp.statespacemapper.writers.TraceResultGoalWriter}.
   *
   * @param automaton
   * @return
   */
  public static FSA string2automaton(String automaton) throws CodecException, IOException {
    try (var stream = new ByteArrayInputStream(automaton.getBytes())) {
      return (FSA) new FSACodec().decode(stream);
    }
  }

  public static RegularExpression simplifyGoalRegex(RegularExpression regex) throws UnsupportedException {
    return new RESimplifier().rewrite(regex);
//    return (RegularExpression) LogicSimplifier.simplify(regex);
  }

  public static RegularExpression string2regex(String regex) throws ParseException {
    return new REParser().parse(regex);
  }

  public static RegularExpression regex2string(String regex) throws ParseException {
    return new REParser().parse(regex);
  }

  public static FSA re2fsa(String regex) throws UnsupportedException, ParseException {
    return re2fsa(string2regex(regex));
  }

  public static FSA re2fsa(RegularExpression regex) throws UnsupportedException {
    return new RETranslator().translate(regex);
  }

  public static Equivalence.Result compareAutomata(FSA automaton1, FSA automaton2) {
    return new Equivalence().isEquivalent(automaton1, automaton2);
  }
}
