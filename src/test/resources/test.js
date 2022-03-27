/* global bp, Packages, EventSets , Set, java*/ // <-- Turn off warnings
importPackage(Packages.il.ac.bgu.cs.bp.statespacemapper)

function Any(name) {
  return bp.EventSet('Any(' + name + ')', function (e) {
    return e.name.equals(name)
  })
}

function globalWhenHelper(d, f) {
  bp.registerBThread('when helper', function () {
    f(d)
  })
}

const btFuncBody = [
  '\taddToCart({ s: e.s });\n' +
  '\tcheckOut({ s: e.s });\n',

  '\taddToCart({ s: e.s });\n' +
  '\tSpaceMapperCliRunner.removeParent.accept(this);\n' +
  '\tcheckOut({ s: e.s });\n',

  '\taddToCart({ s: e.s });\n',

  '\taddToCart({ s: e.s });\n' +
  '\tSpaceMapperCliRunner.removeParent.accept(this);\n'
]

const btFuncOptions = [
  'func',
  'function(e) {\n' +
  '\t%%btFuncBody%%' +
  '\t}'
]

const btTemplate = '' +
  'bp.registerBThread(\'Add women jacket story\', function () {\n' +
  '  when(Any(\'Login\'), %%btFuncOptions%%);\n' +
  '});\n\n'

const whenTemplate = '' +
  'const when = function (eventSet, f) {\n' +
  '%%before-while%%' +
  '  while (true) {\n' +
  '%%before-sync%%' +
  '%%sync%%' +
  '%%after-sync%%' +
  '%%helper-call%%' +
  '%%before-end-while%%' +
  '  }' +
  '  %%after-while%%\n' +
  '};\n\n'

const helperCallOptions = [
  'globalWhenHelper(%%data-variable%%, f);\n',

  'bp.registerBThread(\'when helper\', function () {\n' +
  '    f(%%data-variable%%);\n' +
  '  });\n',

  '((d) => bp.registerBThread(\'when helper\', function () {\n' +
  ' f(d);\n' +
  '}))(%%data-variable%%);\n'
]

const dataTypes = ['var', 'let']

const whenOptions = [
  {
    '%%before-while%%': '%%dataTypes%% data = null;\n',
    '%%before-sync%%': 'data = ',
    '%%sync%%': 'bp.sync({ waitFor: eventSet }).data',
    '%%after-sync%%': ';\n',
    '%%before-end-while%%': 'data = null;\n',
    '%%data-variable%%': 'data'
  },

  {
    '%%before-while%%': '',
    '%%before-sync%%': '%%dataTypes%% data = ',
    '%%sync%%': 'bp.sync({ waitFor: eventSet }).data',
    '%%after-sync%%': ';\n',
    '%%before-end-while%%': 'data = null;\n',
    '%%data-variable%%': 'data'
  },

  {
    '%%before-while%%': '',
    '%%before-sync%%': '',
    '%%sync%%': '',
    '%%after-sync%%': '',
    '%%before-end-while%%': '',
    '%%data-variable%%': 'bp.sync({ waitFor: eventSet }).data'
  }

  /*{
    '%%before-while%%': 'const helper = function (d, f) {\n' +
      '    bp.registerBThread(\'when helper\', function () {\n' +
      '      f(d);\n' +
      '    });\n' +
      '  };',
    '%%before-sync%%': '%%data-type%% data = ',
    '%%after-sync%%': ';\n',
    '%%before-end-while%%': ''
  },

  {
    '%%before-while%%': '',
    '%%before-sync%%': '',
    '%%after-sync%%': '',
    '%%helper-call%%': '',
    '%%before-end-while%%': ''
  }*/
]

const template = '' +
  'const func = function(e) {\n' +
  '  %%btFuncBody%%' +
  '};\n\n' +
  whenTemplate +
  btTemplate


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


/*bp.registerBThread('Add women jacket story', function () {
  when(Any('Login'), func)
  /!*when(Any('Login'), function (e) {
    addToCart({ s: e.s })
    checkOut({ s: e.s })
  })*!/
})*/
/*
bp.registerBThread('C2 Login story', function () {
  bp.sync({waitFor:Any("Login"), interrupt: Any("AddToCart")})
  bp.sync({waitFor:Any("Login"), interrupt: Any("AddToCart")})
  if (typeof use_accepting_states !== 'undefined') {
    // AcceptingState.Continuing()
    AcceptingState.Stopping()
  }
})*/
