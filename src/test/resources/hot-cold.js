const A = bp.Event("a")
const B = bp.Event("b")

bp.registerBThread("Do-A", function() {
  bp.sync({ request: A })
  bp.sync({ request: A })
  bp.sync({ request: A })
})

bp.registerBThread("Do-B", function() {
  bp.sync({ request: B })
  bp.sync({ request: B })
  bp.sync({ request: B })
})

/*
bp.registerBThread("Interleave", function() {
  while(true) {
    bp.sync({waitFor: A, block: B})
    bp.sync({waitFor: B, block: A})
  }
})*/
