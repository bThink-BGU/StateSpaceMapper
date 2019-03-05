package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsBProgramVerifier;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.StringBProgram;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author michael
 */
public class StateSpaceMapper {
    
    private final List<Path> fileNames = new ArrayList<>();
    
    public void addFile( Path p ) {
        fileNames.add(p);
    }
    
    public void mapSpace() throws IOException, Exception {
        BProgram bprog = createBProgram();
        DfsBProgramVerifier vfr = new DfsBProgramVerifier();
        
        PrintStream out = System.out;
        StateMappingListener stateMappingInspection = new StateMappingListener(bprog, out);
        
        vfr.addInspection(stateMappingInspection);
        vfr.setProgressListener(stateMappingInspection);
        
        vfr.verify(bprog);
    }

    private BProgram createBProgram() throws IOException {
        BProgram retVal = new StringBProgram(fileNames.get(0).getFileName().toString(), "");
        for ( Path fn:fileNames ) {
            retVal.appendSource(
                new String(Files.readAllBytes(fn), StandardCharsets.UTF_8)
            );
        }
        return retVal;
    }
    
    
    
}
