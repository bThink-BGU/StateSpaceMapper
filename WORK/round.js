// *           +->B1>--+
// *           |       |
// * ->1-->A>--2       3-->C>----+
// *   |       |       |         |
// *   |       +->B2>--+         |
// *   +-------------------------+       




bp.registerBThread("round", function(){
    while( true ) {
        bp.sync({request:bp.Event("A")});
        bp.sync({waitFor:[bp.Event("B1"), bp.Event("B2")]});
        bp.sync({request:bp.Event("C")});
    }
});

bp.registerBThread("round-s1", function(){
    while( true ) {
        bp.sync({waitFor:bp.Event("A")});
        bp.sync({request:bp.Event("B1"), waitFor:bp.Event("B2")});
    }
});

bp.registerBThread("round-s2", function(){
    while( true ) {
        bp.sync({waitFor:bp.Event("A")});
        bp.sync({request:bp.Event("B2"), waitFor:bp.Event("B1")});
    }
});

