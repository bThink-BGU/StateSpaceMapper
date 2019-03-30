var A = bp.Event("A");
var B = bp.Event("B");
var C = bp.Event("C");

bp.registerBThread( "t", function(){
    
    bp.sync({request:A});
    bp.sync({request:B});
    bp.sync({request:C});
    
});