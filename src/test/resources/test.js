/* global bp, Packages, EventSets , Set, java*/ // <-- Turn off warnings
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
%%remove-parent%%
  bp.sync({ request: bp.Event('Login', data) });
}

function addToCart(data) {
%%remove-parent%%
  bp.sync({ request: bp.Event('AddToCart', data) })
}

function checkOut(data) {
%%remove-parent%%
  bp.sync({ request: bp.Event('CheckOut', data) })
}

const func = function(e) {
%%bt-func-body%%};

const when = function (eventSet, f) {
  const innerWhenHelper = function(d) {
    bp.registerBThread('when helper', function () {
      f(d);
    });
  };
%%declare-data%%
  while (true) {
%%sync-foo%%
%%sync-data%%
%%helper%%
%%reset-data%%
  }
};

bp.registerBThread('C1 Login story', function () {
  login({ s: 'C1' })
})

bp.registerBThread('C2 Login story', function () {
  login({ s: 'C2' })
})

bp.registerBThread('Add women jacket story', function () {
  when(Any('Login'), %%bt-func-name%%);
});
