importPackage(Packages.events);
const n = 1;
const x = [Approaching(0), Entering(0), Leaving(0), Approaching(1), Entering(1), Leaving(1),Raise(), Lower()];

for (var i = 0; i < n; i++){
    (function(i){
        bp.registerBThread("Railway " + i + " Sensors", function() {
            while (true){
                bp.sync({request: Approaching(i)});
                bp.sync({request: Entering(i)});
                bp.sync({request: Leaving(i)});
            }
        });

        bp.registerBThread("Barrier cannot be raised when there is a train in railway " + i, function() {
            while (true){
                bp.sync({waitFor: Approaching(i)});
                bp.sync({waitFor: Leaving(i), block: Raise()});//
            }
        });

        bp.registerBThread("Trains cannot enter railway " + i + " when the barier is down", function() {
            while (true){
                bp.sync({waitFor: Lower(), block: Entering(i)});
                bp.sync({waitFor: Raise()});
            }
        });

    })(i);
}

bp.registerBThread("Lower the barrier when a train is approaching and then raise it as soon as possible", function() {
    while (true){
        bp.sync({waitFor: Approaching()});
        bp.sync({request: Lower()});
        bp.sync({request: Raise()});
    }
});



//
// { Approaching(0) Lower Entering(0) Leaving(0) Raise }

// importPackage(Packages.events);
// const x = [Approaching(0), Entering(0), Leaving(0), Raise(), Lower()];
//
//
// bp.registerBThread("Trains keep comming to railway " + 0 + " and progress in Approaching, Entring, and Leaving sequence", function() {
//     while (true){
//         bp.sync({request: Approaching(0)});//
//         bp.sync({request: Entering(0)});
//         bp.sync({request: Leaving(0)});
//     }
// });
// bp.registerBThread("The barrier cannot be raised when there is a train in railway " + 0, function() {
//     while (true){
//         bp.sync({waitFor: Approaching(0)});//
//         bp.sync({waitFor: Leaving(0), block: Raise()});
//     }
// });
// bp.registerBThread("Trains cannot enter railway " + 0 + " when the barier is down", function() {
//     while (true){
//         bp.sync({waitFor: Lower(), block: Entering(0)});
//         bp.sync({waitFor: Raise()});//
//     }
// });
//
// bp.registerBThread("Lower the barrier when a train is approaching and then raise it as soon as possible", function() {
//     while (true){
//         bp.sync({waitFor: Approaching()});
//         bp.sync({request: Lower()});
//         bp.sync({request: Raise()});//
//     }
// });