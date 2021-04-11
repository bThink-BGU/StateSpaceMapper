package il.ac.bgu.cs.bp.statespacemapper;

import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.aut.fsa.Equivalence;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.CodecException;
import org.svvrl.goal.core.io.GFFCodec;
import org.svvrl.goal.core.logic.ParseException;
import org.svvrl.goal.core.logic.re.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
    String result = regex
        .replace('$', 'e')
        .replaceAll("\\(\\)", "E")
        .replaceAll("\\+", " | ");
    while (true) {
      String s = result
          .replaceAll("([a-zA-Z)*])([a-zA-Z(])", "$1 $2");
      if (s.equals(result)) break;
      result = s;
    }
    return result;
  }

  /**
   * Generates a {@link FSA} from the output of {@link il.ac.bgu.cs.bp.statespacemapper.writers.TraceResultGoalWriter}
   * and tries to simplify its transitions.
   *
   * @param automaton a string of the automaton to generate.
   * @return the generated automaton.
   */
  public static FSA string2fsa(String automaton) throws CodecException, IOException {
    return string2fsa(automaton, true);
  }

  public static FSA string2fsa(String automaton, boolean simplify) throws CodecException, IOException {
    try (var stream = new ByteArrayInputStream(automaton.getBytes(StandardCharsets.UTF_8))) {
      var codec = new GFFCodec();
      var decode = (FSA) codec.decode(stream);
      if (simplify)
        decode.simplifyTransitions();
      return decode;
    }
  }

  public static String fsa2string(FSA automaton) throws IOException, CodecException {
    try (var baos = new ByteArrayOutputStream();
         var strOut = new PrintStream(baos, false, StandardCharsets.UTF_8)) {
      fsa2string(automaton, strOut);
      return baos.toString();
    }
  }

  public static void fsa2string(FSA automaton, OutputStream out) throws CodecException {
    var codec = new GFFCodec();
    codec.encode(automaton, out);
  }

  public static RegularExpression simplifyGoalRegex(RegularExpression regex) throws UnsupportedException {
    return new RESimplifier().rewrite(regex);
//    return (RegularExpression) LogicSimplifier.simplify(regex);
  }

  public static RegularExpression string2regex(String regex) throws ParseException {
    return new REParser().parse(regex);
  }

  public static String regex2string(RegularExpression regex) throws ParseException {
    return regex.toString();
  }

  public static FSA re2fsa(String regex) throws UnsupportedException, ParseException {
    return re2fsa(string2regex(regex));
  }

  public static FSA re2fsa(RegularExpression regex) throws UnsupportedException {
    var aut = new RETranslator().translate(regex);
    aut.simplifyTransitions();
    return aut;
  }

  public static RegularExpression fsa2re(FSA fsa) throws UnsupportedException {
    return fsa2re(fsa, true);
  }

  public static RegularExpression fsa2re(FSA fsa, boolean simplifyRegex) throws UnsupportedException {
    REExtractor extractor = new REExtractor(fsa);
    var re = extractor.getRegularExpression();
    return simplifyGoalRegex(re);
  }

  public static Equivalence.Result compareAutomata(FSA automaton1, FSA automaton2) {
    return new Equivalence().isEquivalent(automaton1, automaton2);
  }
}
