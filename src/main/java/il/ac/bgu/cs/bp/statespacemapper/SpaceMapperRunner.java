package il.ac.bgu.cs.bp.statespacemapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author michael
 */
public class SpaceMapperRunner {
    
    
    public static void main(String[] args) throws Exception {
        if ( args.length == 0 ) {
            System.err.println("Missing input files");
            System.exit(1);
        }
        
        List<Path> inputPaths = new ArrayList<>(args.length);
        for ( String arg : args ) {
            Path fn = Paths.get(arg);
            if ( Files.exists(fn) ) {
                inputPaths.add(fn);
            } else {
                System.err.printf("Input file '%s' does not exist (absolute path: '%s')\n", arg, fn.toAbsolutePath().toString());
                System.exit(2);
            }
        }
        StateSpaceMapper mpr = new StateSpaceMapper();
        inputPaths.stream().forEach( mpr::addFile );
        
        mpr.mapSpace();
    }
}
