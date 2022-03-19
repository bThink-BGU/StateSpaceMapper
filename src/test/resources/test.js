/* global bp, Packages, EventSets , Set, java*/ // <-- Turn off warnings
importPackage(Packages.il.ac.bgu.cs.bp.statespacemapper);

function Any(name) {
  return bp.EventSet('Any(' + name + ')', function (e) {
    return e.name.equals(name)
  })
}

function whenHelper(d, f) {
  bp.registerBThread('when helper', function () {
    f(d)
  })
}

function when1(eventSet, f) {
  var data = null
  while (true) {
    data = bp.sync({ waitFor: eventSet }).data
    whenHelper(data, f)
    data = null
  }
}

function when2(eventSet, f) {
  let data = null
  while (true) {
    data = bp.sync({ waitFor: eventSet }).data
    whenHelper(data, f)
    data = null
  }
}

function when3(eventSet, f) { // the winner
  var data = null
  while (true) {
    data = bp.sync({ waitFor: eventSet }).data;
    ((data) => bp.registerBThread('when helper', function () {
      f(data)
    }))(data)
    data = null
  }
}

function when4(eventSet, f) {
  const helper = function(d, f) {
    bp.registerBThread('when helper', function () {
      f(d)
    })
  }

  var data = null
  while (true) {
    data = bp.sync({ waitFor: eventSet }).data
    helper(data, f)
    data = null
  }
}

function when5(eventSet, f) {
  const helper = function(d, f) {
    bp.registerBThread('when helper', function () {
      f(d)
    })
  }
  while (true) {
    helper(bp.sync({ waitFor: eventSet }).data, f)
  }
}


const when = when2

function login(data) {
  bp.sync({ request: bp.Event('Login', data) })
}

function addToCart(data) {
  bp.sync({ request: bp.Event('AddToCart', data) })
}

function checkOut(data) {
  bp.sync({ request: bp.Event('CheckOut', data) })
}

bp.registerBThread('C1 Login story', function () {
  login({ s: 'C1' })
})


bp.registerBThread('C2 Login story', function () {
  login({ s: 'C2' })
})

const func = function (e) {
  addToCart({ s: e.s })
  SpaceMapperCliRunner.removeParent.accept(this);
  checkOut({ s: e.s })
  SpaceMapperCliRunner.removeParent.accept(this);
}

bp.registerBThread('Add women jacket story', function () {
  when(Any('Login'), func)
  /*when(Any('Login'), function (e) {
    addToCart({ s: e.s })
    checkOut({ s: e.s })
  })*/
})
/*
bp.registerBThread('C2 Login story', function () {
  bp.sync({waitFor:Any("Login"), interrupt: Any("AddToCart")})
  bp.sync({waitFor:Any("Login"), interrupt: Any("AddToCart")})
  if (typeof use_accepting_states !== 'undefined') {
    // AcceptingState.Continuing()
    AcceptingState.Stopping()
  }
})*/
