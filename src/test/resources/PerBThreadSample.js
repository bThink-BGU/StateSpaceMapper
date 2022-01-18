const bthreads = {}

bthreads['bt1'] = function () {
  while (true) {
    bp.sync({request: [bp.Event('a',9)]})
    bp.sync({request: [bp.Event('a',8)]})
    bp.sync({waitFor: [bp.Event('b')]})
  }
}

bthreads['bt2'] = function () {
  while (true) {
    bp.sync({waitFor: bp.Event('a')})
    bp.sync({request: bp.Event('b')})
  }
}
