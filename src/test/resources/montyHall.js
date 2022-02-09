// importPackage(Packages.il.ac.bgu.cs.bp.statespacemapper);

const bthreads = {}
const h = [bp.Event('h1'),bp.Event('h2'),bp.Event('h3')]
const g = [bp.Event('g1'),bp.Event('g2'),bp.Event('g3')]
const o = [bp.Event('o1'),bp.Event('o2'),bp.Event('o3')]

bthreads['bt1'] = function () {
    bp.sync({request: h})
    bp.sync({request: g})
    bp.sync({request: o})
}

bthreads['bt2'] = function () {
    let e_h = String(bp.sync({waitFor: h}).name)
    bp.sync({block: bp.Event('o'+e_h[1])})
}


bthreads['bt3'] = function () {
    let e_g = String(bp.sync({waitFor: g}).name)
    bp.sync({block: bp.Event('o'+e_g[1])})
}

//
// for (btName in bthreads){
// bp.registerBThread(btName, bthreads[btName])
// }