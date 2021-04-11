const N = 3

//events = Array.from({length: n}, (x, i) => Array.from({length: n}, (x,j) => bp.Event(i+","+j)))
// const arr = Array.from({ length: n * n }, (x, i) => (i / n) + "," + j % n)
// const events = arr.map(n=>bp.Event(n))

const flatten = arr => [].concat.apply([], arr);

rows = Array(N).fill().map((d, i) => i);
columns = Array(N).fill().map((d, i) => i);

O = []; X = [];

rows.forEach(i => {
  O[i] = []; X[i] = []
  columns.forEach(j => {
    // O[i][j] = bp.Event("O(" + i + "," + j + ")");
    // X[i][j] = bp.Event("X(" + i + "," + j + ")");
    O[i][j] = bp.Event(String.fromCharCode( 'a'.charCodeAt(0) + i*N + j));
    X[i][j] = bp.Event(String.fromCharCode( 'A'.charCodeAt(0) + i*N + j));
  })
})

const lines = [ [ { x:0, y:0 }, { x:0, y:1 }, { x:0, y:2 } ],
  [ { x:1, y:0 }, { x:1, y:1 }, { x:1, y:2 } ],
  [ { x:2, y:0 }, { x:2, y:1 }, { x:2, y:2 } ],
  [ { x:0, y:0 }, { x:1, y:0 }, { x:2, y:0 } ],
  [ { x:0, y:1 }, { x:1, y:1 }, { x:2, y:1 } ],
  [ { x:0, y:2 }, { x:1, y:2 }, { x:2, y:2 } ],
  [ { x:0, y:0 }, { x:1, y:1 }, { x:2, y:2 } ],
  [ { x:0, y:2 }, { x:1, y:1 }, { x:2, y:0 } ] ];

lines.map(l=>l.map(c=>O[c.x][c.y])).forEach(l=> {
  bp.registerBThread('win line', function () {
    for(let i=0; i<3; i++)
      bp.sync({waitFor: l })
    if(use_accepting_states) {
      // AcceptingState.Continuing()
      AcceptingState.Stopping()
    }
  })
})

const OMoves = flatten(O)
const XMoves = flatten(X)
const allMoves = OMoves.concat(XMoves);

bp.registerBThread('Moves', function () {
  for(let i=0; i<4; i++)
    bp.sync({ request: oMoves })
})

rows.forEach(i => {
  columns.forEach(j => {
    bp.registerBThread('Square" + i + "," + j +" can only be marked once', function () {
      bp.sync({ waitFor: O[i][j] })
      bp.sync({ block: O[i][j] })
    })
  })
})
/*
bp.registerBThread('X ans O Moves interleave', function () {
  while (true) {
    bp.sync({ waitFor: XMoves, block: OMoves })
    bp.sync({ waitFor: OMoves, block: XMoves })
  }
})*/

// TODO: Add winning conditions
