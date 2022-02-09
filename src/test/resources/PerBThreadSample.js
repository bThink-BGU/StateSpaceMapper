const bthreads = {}

bthreads['bt1'] = function () {
  while (true) {
    bp.sync({request: [bp.Event('a',9),bp.Event('a',10)], interrupt:bp.Event('c')})
    bp.sync({request: [bp.Event('a',8)]})
    bp.sync({waitFor: [bp.Event('b'),bp.Event('a',9)]})
  }
}

bthreads['bt2'] = function () {
  while (true) {
    bp.sync({waitFor: bp.Event('a')})
    bp.sync({request: bp.Event('b')})
  }
}
