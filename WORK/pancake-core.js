/* global bp */

var ADD_DRY = bp.Event("ADD_DRY");
var ADD_WET = bp.Event("ADD_WET");

var DOSE_COUNT = 5;

bp.registerBThread("Dry", function(){
    for ( var i=0; i<DOSE_COUNT; i++ ){
        bp.sync({request:ADD_DRY});
}});

bp.registerBThread("Wet", function(){
    for ( var i=0; i<DOSE_COUNT; i++ ){
        bp.sync({request:ADD_WET});
}});

// Can't stay at a location where d > w.
bp.registerBThread("enhot", function(){
    var d = 0;
    var w = 0;
    while( true ) {
        var le = bp.hot(d>w).sync({waitFor:[ADD_DRY, ADD_WET]});
        if ( le.equals(ADD_DRY) ) d = d+1;
        if ( le.equals(ADD_WET) ) w = w+1;
    }
});

bp.registerBThread("not-extreme", function(){
    var d = 0;
    var w = 0;
    while( true ) {
        var le = bp.sync({waitFor:[ADD_DRY, ADD_WET]});
        if ( le.equals(ADD_DRY) ) d = d+1;
        if ( le.equals(ADD_WET) ) w = w+1;
        bp.ASSERT( Math.abs(d-w)<4, "d/w difference too extreme" );
    }
});

bp.registerBThread("stateTitler", function(){
    var d = 0;
    var w = 0;
    while( true ) {
        var le = bp.sync({waitFor:[ADD_DRY, ADD_WET]}, d+":"+w);
        if ( le.equals(ADD_DRY) ) d = d+1;
        if ( le.equals(ADD_WET) ) w = w+1;
    }
});