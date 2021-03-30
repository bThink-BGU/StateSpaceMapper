const N = 2

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

const OMoves = flatten(O)
const XMoves = flatten(X)
const allMoves = OMoves.concat(XMoves);

bp.registerBThread('Moves', function () {
  while (true)
    bp.sync({ request: allMoves })
})

rows.forEach(i => {
  columns.forEach(j => {
    bp.registerBThread('Square" + i + "," + j +" can only be marked once', function () {
      bp.sync({ waitFor: [O[i][j], X[i][j]] })
      bp.sync({ block: [O[i][j], X[i][j]] })
    })
  })
})

bp.registerBThread('X ans O Moves interleave', function () {
  while (true) {
    bp.sync({ waitFor: XMoves, block: OMoves })
    bp.sync({ waitFor: OMoves, block: XMoves })
  }
})

// TODO: Add winning conditions

// bp.registerBThread('Maximum Nine Moves', function () {
//   for (let i = 0; i < 9; i++) {
//     bp.sync({ waitFor: bp.all })
//   }
//   bp.sync({ block: bp.all })
// })
