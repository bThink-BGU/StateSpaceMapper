package il.ac.bgu.cs.bp.statespacemapper;

import java.io.InputStream;
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
        System.out.println("// start");
        if ( args.length == 0 ) {
            System.err.println("Missing input files");
            System.exit(1);
        }

        StateSpaceMapper mpr = new StateSpaceMapper(args[0]);
        mpr.mapSpace();
        System.out.println("// done");
    }
}
