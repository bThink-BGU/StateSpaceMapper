function g(data) {
  java.lang.System.out.println("data is " + data)
}
function f() {
  while(true) {
    let x =capture();
    g(x)
  }
}

f();