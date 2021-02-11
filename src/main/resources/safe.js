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
  if(code[0] != bp.sync({waitFor: bp.all }).name) {
    bp.sync({request: INCORRECT, block: INCORRECT.negate()})
    return
  }
  if(code[1] != bp.sync({waitFor: bp.all }).name){
    bp.sync({request: INCORRECT, block: INCORRECT.negate()})
    return
  }
  if(code[2] != bp.sync({waitFor: bp.all }).name) {
    bp.sync({request: INCORRECT, block: INCORRECT.negate()})
    return
  }
  if(code[3] != bp.sync({waitFor: bp.all }).name) {
    bp.sync({request: INCORRECT, block: INCORRECT.negate()})
    return
  }
  bp.sync({request: CORRECT, block: CORRECT.negate()})
  bp.sync({block:bp.all})
})

bp.registerBThread('incorrect code', function() {
  bp.sync({waitFor: INCORRECT})
  bp.sync({block:bp.all})
})