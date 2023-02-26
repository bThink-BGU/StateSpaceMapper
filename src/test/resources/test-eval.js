"111$0$13$4$16$2$func$oneLine$FALSE$FALSE$global$insideWhile$FALSE$let$/* global bp, Packages, EventSets , Set, java*/ // <-- Turn off warnings
importPackage(Packages.il.ac.bgu.cs.bp.statespacemapper)

function Any(name) {
  return bp.EventSet('Any(' + name + ')', function (e) {
    return e.name.equals(name);
  });
}

function globalWhenHelper(d, f) {
  bp.registerBThread('when helper', function () {
    f(d);
  });
}

function login(data) {
  bp.sync({ request: bp.Event('Login', data) });
}

function addToCart(data) {
  bp.sync({ request: bp.Event('AddToCart', data) })
}

function func (e) {
  addToCart({ s: e.s });
}

function when (eventSet, f) {
  while (true) {
    let e = bp.sync({ waitFor: eventSet });
    let data = e.data
    globalWhenHelper(data, f);
  }
}

bp.registerBThread('C1 Login story', function () {
  login({ s: 'C1' })
})

bp.registerBThread('C2 Login story', function () {
  login({ s: 'C2' })
})

bp.registerBThread('Add women jacket story', function () {
  when(Any('Login'), func);
});"