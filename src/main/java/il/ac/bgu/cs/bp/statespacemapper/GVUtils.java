/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.ac.bgu.cs.bp.statespacemapper;

/**
 *
 * @author michael
 */
public class GVUtils {
    
    public static String sanitize( String in ) {
        return in.replaceAll("[. -]", "_");
    }
}

