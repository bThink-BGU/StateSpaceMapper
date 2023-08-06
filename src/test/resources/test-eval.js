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

function checkOut(data) {
  bp.sync({ request: bp.Event('CheckOut', data) })
}

const func = function(e) {
  addToCart({ s: e.s });
};

const when = function (eventSet, f) {
  const innerWhenHelper = function(d) {
    bp.registerBThread('when helper', function () {
      f(d);
    });
  };
  while (true) {
    let data = bp.sync({ waitFor: eventSet }).data;
    globalWhenHelper(data, f);

  }
};

bp.registerBThread('C1 Login story', function () {
  login({ s: 'C1' })
})

bp.registerBThread('C2 Login story', function () {
  login({ s: 'C2' })
})

bp.registerBThread('Add women jacket story', function () {
  when(Any('Login'), func);
});"