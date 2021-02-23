const MAX_NUMBER = 4
const CORRECT = bp.Event("CORRECT\nCODE")
let arr = []
for(let i=0; i<=MAX_NUMBER; i++) {
  arr.push(i.toString())
}
const events = arr.map(n=>bp.Event(n))
const code = [0,1,2,3,4].map(n=>n.toString())

bp.registerBThread('try', function() {
  while(true)
    bp.sync({request:events},10)
})

bp.registerBThread('correct code', function() {
  for(let i=0; i < code.length; i++) {
    if (code[i] != bp.sync({waitFor: bp.all}).name) {
      bp.ASSERT(false, "wrong code")
    }
  }
  bp.sync({request: CORRECT, block: CORRECT.negate()})
  bp.sync({block: bp.all})
})
