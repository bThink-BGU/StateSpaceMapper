package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.CodecException;
import org.svvrl.goal.core.logic.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class GoalToolTest {

    @Test
    void goal() throws ParseException, UnsupportedException {
        assertEquals("a+", GoalTool.simplifyGoalRegex("a a*"));
        assertEquals("a+", GoalTool.simplifyGoalRegex("a* a"));

        var result = GoalTool.compareAutomata(GoalTool.re2fsa("a* a"), GoalTool.re2fsa("a a*"));
        assertTrue(result.isEquivalent());

        assertEquals("(a a* a) g (a b) | b e E", GoalTool.noam2goalRegexFormat("(aa*a)g(ab)+b$()"), "Original string was: (aa*a)+b$()");
    }

    @Deprecated
    void testAutomatonLoading() throws IOException, CodecException {
        String vault = readFileAsString(getFileFromResourceAsStream("vault.gff"));
        FSA automaton = GoalTool.string2fsa(vault);
        System.out.println("File automaton: " + vault);
        System.out.println("Parsed automaton: " + automaton.toString());
    }

    private InputStream getFileFromResourceAsStream(String fileName) {
        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    private String readFileAsString(InputStream inputStream) {
        StringBuilder result = new StringBuilder();
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                result.append(scanner.nextLine()).append("\n");
            }
        }
        return result.toString();
    }
}