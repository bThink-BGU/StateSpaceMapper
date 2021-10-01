bp.registerBThread('Idle', function () {
  while (true) {
    bp.sync({request: [bp.Event('elevator_call_different_floor'), bp.Event('elevator_call_same_floor')]})
    bp.sync({waitFor: bp.Event('timeout')});
  }
})

bp.registerBThread('Move_to_floor', function () {
  while (true) {
    bp.sync({waitFor: [bp.Event('elevator_call_different_floor')]});
    bp.sync({request: [bp.Event('floor_reached')]})
  }
})

bp.registerBThread('Open_door-1', function () {
  while (true) {
    bp.sync({waitFor: [bp.Event('floor_reached'), bp.Event('elevator_call_same_floor')]})
    bp.sync({request: [bp.Event('destinatination_select'), bp.Event('timeout')]})
  }
})

bp.registerBThread('Close_door', function () {
  while (true) {
    bp.sync({waitFor: [bp.Event('destinatination_select')]});
    bp.sync({request: [bp.Event('floor_reached')]})
  }
})
