importPackage(Packages.il.ac.bgu.cs.bp.statespacemapper);
const empty = AnyOf()

function createGraphPerBThread(btArr) {
  bp.log.info("btarr {0}",btArr)
  bp.log.info("current {0}",currentBT)
  bp.registerBThread(btArr[currentBT].name, btArr[currentBT].func)
}

function sync(stmt) {
  if(!stmt.waitFor) stmt.waitFor = empty
  if(!stmt.block) stmt.block = empty
  bp.sync(stmt)
}

createGraphPerBThread([
  {
    name: 'bt2', func: function () {
      // for(let i=0; i < code.length; i++)
      while (true) {
        sync({request: bp.Event('a')})
        sync({waitFor: AnyOf(bp.Event('b'))})
      }
    }
  },
  {
    name: 'bt1', func: function () {
      // for(let i=0; i < code.length; i++)
      while (true) {
        sync({waitFor: AnyOf(bp.Event('a'))})
        sync({request: bp.Event('b')})
      }
    }
  }
])
