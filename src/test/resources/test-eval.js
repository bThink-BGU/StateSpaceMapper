"0$0$18$5$33$1$func$oneLine$TRUE$TRUE$global$none$FALSE$var$/* global bp, Packages, EventSets , Set, java*/ // <-- Turn off warnings
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
  SpaceMapperCliRunner.removeParent.accept(data);
  bp.sync({ request: bp.Event('Login', data) });
}

function addToCart(data) {
  SpaceMapperCliRunner.removeParent.accept(data);
  bp.sync({ request: bp.Event('AddToCart', data) })
}

function checkOut(data) {
  SpaceMapperCliRunner.removeParent.accept(data);
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
    bp.sync({ request: bp.Event('foo') });
    globalWhenHelper(bp.sync({ waitFor: eventSet }).data, f);

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