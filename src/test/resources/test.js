
bp.registerBThread('try', function() {
  for(let i=0; i < 8; i++)
    bp.sync({request: bp.Event(String("A"))}, 10)
})
