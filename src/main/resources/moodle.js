function Any(name, data) {
  return bp.EventSet('Any(' + name + ')', function (e) {
    if(e.name != name) return false;
    if(data) {
      if(e.data === null) return false;
      let d = true;
      for(key in data)
        if(!(key in e.data)) return false;
        if(e.data[key] != data[key]) return false;
    }
    return true;
  })
}

bp.registerBThread('Admin adds a course', function () {
  bp.sync({ request: bp.Event('Session.Start', {s: 'admin'}) })

  bp.sync({ request: bp.Event('AddCourse.Begin', {name: 'course 1'}) })
  bp.sync({ request: bp.Event('AddCourse.Submit', {name: 'course 1'}) })
  bp.sync({ request: bp.Event('EnrollUser.Begin',
      {course: 'course 1', user: 'Terri Teacher', role: 'Teacher'}) })
  bp.sync({ request: bp.Event('EnrollUser.Submit',
      {course: 'course 1', user: 'Terri Teacher', role: 'Teacher'}) })
  bp.sync({ request: bp.Event('EnrollUser.Begin',
      {course: 'course 1', user: 'Sam Student', role: 'Student'}) })
  bp.sync({ request: bp.Event('EnrollUser.Submit',
      {course: 'course 1', user: 'Sam Student', role: 'Student'}) })

  bp.sync({ request: bp.Event('Session.End', {s: 'admin'}) })
})

bp.registerBThread('Teacher adds a quiz with questions', function () {
  let c = bp.sync({ waitFor: Any('EnrollUser.Submit',
      {role: 'Teacher'}) }).data
  bp.sync({ request: bp.Event('Session.Start', {s: 'teacher'}) })

  bp.sync({ request: bp.Event('AddQuiz.Start',
      {s: 'teacher', course: c.course, name: 'quiz 1'}) })
  bp.sync({ request: bp.Event('AddQuestion.Start',
      {s: 'teacher', quiz: 'quiz 1', name: 'Question 1'}) })
  bp.sync({ request: bp.Event('AddQuestion.Submit',
      {s: 'teacher', quiz: 'quiz 1', name: 'Question 1'}) })
  bp.sync({ request: bp.Event('AddQuestion.Start',
      {s: 'teacher', quiz: 'quiz 1', name: 'Question 2'}) })
  bp.sync({ request: bp.Event('AddQuestion.Submit',
      {s: 'teacher', quiz: 'quiz 1', name: 'Question 2'}) })
  bp.sync({ request: bp.Event('AddQuiz.Submit',
      {s: 'teacher', course: c.course, name: 'quiz 1'}) })

  bp.sync({ request: bp.Event('Session.End', {s: 'teacher'}) })
})

bp.registerBThread('Student answers false to questions', function () {
  bp.sync({ waitFor: Any('EnrollUser.Submit', {role: 'Student'}) }).data
  bp.sync({ request: bp.Event('Session.Start', {s: 'student'}) })

  let q = bp.sync({ waitFor: Any('AddQuestion.Submit') }).data
  bp.sync({ request: bp.Event('AnswerQuestion.Start',
      {quiz: q.quiz, name: q.name, answer: 'False' }) })
  bp.sync({ request: bp.Event('AnswerQuestion.Submit',
      {quiz: q.quiz, name: q.name, answer: 'False' }) })

  bp.sync({ request: bp.Event('Session.End', {s: 'student'}) })
})