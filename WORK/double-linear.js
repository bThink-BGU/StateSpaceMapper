var A = bp.Event("A");
var B = bp.Event("B");
var C = bp.Event("C");
var X = bp.Event("X");
var Y = bp.Event("Y");
var Z = bp.Event("Z");

bp.registerBThread( "ta", function(){
    bp.sync({request:A});
    bp.sync({request:B});
    bp.sync({request:C});
});

bp.registerBThread( "tx", function(){
    bp.sync({request:X});
    bp.sync({request:Y});
    bp.sync({request:Z});
});

bp.registerBThread( "tq", function(){
    bp.sync({waitFor:A});
    bp.sync({waitFor:B});
    bp.sync({waitFor:C});
    bp.sync({waitFor:X});
    bp.ASSERT(false,"ABC->X");
});

bp.registerBThread("stateTitler", function(){
    var soFar="";
    while ( true ) {
        var le = bp.sync({waitFor:[A,B,C,X,Y,Z]}, "c" + soFar);
        soFar = soFar + le.name;
    }
});