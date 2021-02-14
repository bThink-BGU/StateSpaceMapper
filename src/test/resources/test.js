const MAX_NUMBER = 0
let arr = []
for(let i=0; i<=MAX_NUMBER; i++) {
  arr.push(i.toString())
}
const events = arr.map(n=>bp.Event(n))

bp.registerBThread('try', function() {
  // for(let i=0; i < code.length; i++)
  while(true) {
    bp.sync({request: events}, 10)
    bp.sync({request: events}, 10)
  }
})
