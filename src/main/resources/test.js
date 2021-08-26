importPackage(Packages.il.ac.bgu.cs.bp.statespacemapper);
const empty = AnyOf()


function sync(stmt, data) {
  if (!stmt.waitFor) stmt.waitFor = empty
  if (!stmt.block) stmt.block = empty
  if(data)
    return bp.sync(stmt, data)
  else
    return bp.sync(stmt)
}

const bthreads = {}

bthreads['bt1'] = function () {
  while (true) {
    sync({request: [bp.Event('a',9)]})
    sync({request: [bp.Event('a',8)]})
    sync({waitFor: AnyOf([bp.Event('b')])})
  }
}

bthreads['bt2'] = function () {
  while (true) {
    sync({waitFor: AnyOf(bp.Event('a'))})
    sync({request: bp.Event('b')})
  }
}

bp.registerBThread(btName, bthreads[btName])