const MAX_NUMBER = 4
const INCORRECT = bp.Event("INCORRECT CODE")
const CORRECT = bp.Event("CORRECT CODE")
let arr = []
for(let i=0; i<=MAX_NUMBER; i++) {
  arr.push(i.toString())
}
const events = arr.map(n=>bp.Event(n))
const code = [2,4,2,2].map(n=>n.toString())

bp.registerBThread('try', function() {
  for(let i=0; i < 100; i++) {
    bp.sync({request:events})
  }
})

bp.registerBThread('correct code', function() {
  let correct = true
  for(let i=0; i < code.length; i++) {
    if (code[i] != bp.sync({waitFor: bp.all}).name) {
      correct = false
      break
    }
  }
  if(correct)
    bp.sync({request: CORRECT, block: CORRECT.negate()})
  else
    bp.sync({request: INCORRECT, block: INCORRECT.negate()})
  bp.sync({block:bp.all})
})
