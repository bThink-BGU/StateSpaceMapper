import il.ac.bgu.cs.bp.statespacemapper.StateSpaceMapper;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SpaceMapperRunner {
    public static void main(String[] args) throws Exception {
        System.out.println("// start");
        if ( args.length == 0 ) {
            System.err.println("Missing input files");
            System.exit(1);
        }

        Files.createDirectories(Paths.get("graphs"));
        StateSpaceMapper mpr = new StateSpaceMapper(args[0], true);
        mpr.mapSpace();
        System.out.println("// done");
    }
}
