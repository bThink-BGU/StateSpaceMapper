function bthread(name, func) {
  bp.registerBThread(name, func)
}

function sync(stmt, sdata) {
  if (sdata)
    return bp.sync(stmt, sdata)
  return bp.sync(stmt)
}

function Event(name, data) {
  if (data) return bp.Event(name, data)
  return bp.Event(name)
}

function Any(name, data) {
  return bp.EventSet('Any(' + name + ',' + JSON.stringify(data) + ')', function (e) {
    if (e.name != name) return false
    if (data) {
      if (e.data === null) return false
      for (key in data)
        if (!(key in e.data)) return false
      if (e.data[key] != data[key]) return false
    }
    return true
  })
}

bthread('Admin adds a course', function () {
  sync({ request: Event('Session.Start', { s: 'admin' }) }) // Event's parameters are its name and data.

  sync({ request: Event('AddCourse.Start', { name: 'course 1' }) })
  sync({ request: Event('AddCourse.Submit', { name: 'course 1' }) })
  sync({ request: Event('EnrollUser.Start', { course: 'course 1', user: 'Terri Teacher', role: 'Teacher' }) })
  sync({ request: Event('EnrollUser.Submit', { course: 'course 1', user: 'Terri Teacher', role: 'Teacher' }) })
  sync({ request: Event('EnrollUser.Start', { course: 'course 1', user: 'Sam Student', role: 'Student' }) })
  sync({ request: Event('EnrollUser.Submit', { course: 'course 1', user: 'Sam Student', role: 'Student' }) })

  sync({ request: Event('Session.End', { s: 'admin' }) })
})

bthread('Teacher adds a quiz with questions', function () {
  let c = sync({ waitFor: Any('EnrollUser.Submit', { role: 'Teacher' }) }).data
  sync({ request: Event('Session.Start', { s: 'teacher' }) })

  sync({ request: Event('AddQuiz.Start', { s: 'teacher', course: c.course, name: 'quiz 1' }) })
  sync({ request: Event('AddQuiz.Submit', { s: 'teacher', course: c.course, name: 'quiz 1' }) })
  sync({ request: Event('AddQuestion.Start', { s: 'teacher', quiz: 'quiz 1', name: 'Question 1' }) })
  sync({ request: Event('AddQuestion.Submit', { s: 'teacher', quiz: 'quiz 1', name: 'Question 1' }) })
  sync({ request: Event('AddQuestion.Start', { s: 'teacher', quiz: 'quiz 1', name: 'Question 2' }) })
  sync({ request: Event('AddQuestion.Submit', { s: 'teacher', quiz: 'quiz 1', name: 'Question 2' }) })

  sync({ request: Event('Session.End', { s: 'teacher' }) })
})

bthread('Student answers false to questions', function () {
  sync({ waitFor: Any('EnrollUser.Submit', { role: 'Student' }) })
  sync({ request: Event('Session.Start', { s: 'student' }) })

  let q = sync({ waitFor: Any('AddQuestion.Submit') }).data
  sync({ request: Event('AnswerQuestion.Start', { quiz: q.quiz, name: q.name, answer: false }) })
  sync({ request: Event('AnswerQuestion.Submit', { quiz: q.quiz, name: q.name, answer: false }) })

  sync({ request: Event('Session.End', { s: 'student' }) })
})

/*
bthread('accepting', function () {
  bp.sync({ waitFor: Any('Session.End') })
  bp.sync({ waitFor: Any('Session.End') })
  bp.sync({ waitFor: Any('Session.End') })
  if (typeof use_accepting_states !== 'undefined') {
    AcceptingState.Continuing()
    // AcceptingState.Stopping()
  }
})

bthread('accepting', function () {
  sync({ waitFor: Any('AddQuestion.Submit'), interrupt: Event('Session.Start', { s: 'student' }) })
  sync({ waitFor: Any('AddQuestion.Submit'), interrupt: Event('Session.Start', { s: 'student' }) })
  sync({ waitFor: bp.All() })
  sync({ waitFor: bp.All() })
  sync({ waitFor: bp.All() })
  if (typeof use_accepting_states !== 'undefined') {
    AcceptingState.Continuing()
    // AcceptingState.Stopping()
  }
})

bthread('accepting', function () {
  sync({ waitFor: Any('AddQuestion.Submit'), interrupt: Event('Session.Start', { s: 'student' }) })
  sync({ waitFor: Any('AddQuestion.Submit'), interrupt: Event('Session.Start', { s: 'student' }) })
  sync({ waitFor: Event('Session.End', { s: 'teacher' }) })
  sync({ waitFor: Event('Session.Start', { s: 'student' }) })

  if (typeof use_accepting_states !== 'undefined') {
    AcceptingState.Continuing()
    // AcceptingState.Stopping()
  }
})*/
