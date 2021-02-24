package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.execution.tasks.FailedAssertionException;

public abstract class AcceptingState extends FailedAssertionException {
  protected AcceptingState(String message) {
    super(message);
  }

  public static class Stopping extends FailedAssertionException {
    public Stopping() {
      super("StoppingAcceptingState");
    }

    public Stopping(String message) {
      super("StoppingAcceptingState: " + message);
    }
  }

  public static class Continuing extends FailedAssertionException {
    public Continuing() {
      super("ContinuingAcceptingState");
    }

    public Continuing(String message) {
      super("ContinuingAcceptingState: " + message);
    }
  }
}
