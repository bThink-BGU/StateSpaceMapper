/* global bp, Packages, EventSets , Set, java*/ // <-- Turn off warnings
importPackage(Packages.il.ac.bgu.cs.bp.bpjs.model.eventsets)

// importPackage(Packages.il.ac.bgu.cs.bp.bpjs.model.eventsets);

/**
 * Generate an event set based on the filter data.
 * If `filterData` is a String, this is a by-name filter.
 * If `filterData` is a RegExp, this is a by-name filter, but with a regular expression.
 * If `filterData` is an object, we filter by the existing fields of the event's data object.
 *
 * @param {String/Object/RegExp} filterData
 * @returns {EventSet}
 */
function Any(filterData) {
  const filterType = (typeof filterData)
  switch (filterType) {
    case 'string':
      return AnyNamed(filterData)
      break

    case 'object':
      if (filterData instanceof RegExp) {
        return bp.EventSet('AnyNamedRegEx ' + filterData, function (e) {
          return filterData.test(e.name)
        })

      } else {
        let keys = Object.keys(filterData)
        let str = ''
        for (let idx in keys) {
          str = str + ' ' + keys[idx] + ':' + filterData[keys[idx]]
        }
        return bp.EventSet('AnyWithData' + str, function (e) {
          if (!e.data) return false

          for (let key of keys) {
            if (filterData[key] != e.data[key]) {
              return false
            }
          }
          return true

        })
      }
      break
  }
  throw 'Any: Unsupported filterData type: ' + filterType + ' (' + filterData + ')'
}

function when(eventSet, f) {
  while (true) {
    let e = bp.sync({ waitFor: eventSet }).data
    bp.registerBThread('when helper', function () {
      f(e)
    })
  }
}

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

bp.registerBThread('Add women jacket story', function () {
  when(Any('Login'), function (e) {
    addToCart({ s: e.s })
    checkOut({ s: e.s })
  })
})

/*
bp.registerBThread('C2 Login story', function () {
  bp.sync({waitFor:Any("CheckOut")})
  bp.sync({waitFor:Any("CheckOut")})
  if (typeof use_accepting_states !== 'undefined') {
    // AcceptingState.Continuing()
    AcceptingState.Stopping()
  }
})*/
