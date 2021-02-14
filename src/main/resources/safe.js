const MAX_NUMBER = 4
const INCORRECT = bp.Event("INCORRECT\nCODE")
const CORRECT = bp.Event("CORRECT\nCODE")
let arr = []
for(let i=0; i<=MAX_NUMBER; i++) {
  arr.push(i.toString())
}
const events = arr.map(n=>bp.Event(n))
const code = [2].map(n=>n.toString())

bp.registerBThread('try', function() {
  // for(let i=0; i < code.length; i++)
  while(true)
    bp.sync({request:events},10)
})

bp.registerBThread('correct code', function() {
  for(let i=0; i < code.length; i++) {
    if (code[i] != bp.sync({waitFor: bp.all}).name) {
      return
    }
  }
  bp.sync({request: CORRECT, block: CORRECT.negate()})
  bp.sync({block: bp.all})
})
