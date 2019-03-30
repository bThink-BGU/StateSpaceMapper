var A = bp.Event("A");
var B = bp.Event("B");
var C = bp.Event("C");

bp.registerBThread( "t", function(){
    while( true ) {
        bp.sync({request:[A,B,C]});
    }
});