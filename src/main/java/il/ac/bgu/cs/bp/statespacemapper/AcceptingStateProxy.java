package il.ac.bgu.cs.bp.statespacemapper;

import java.io.Serializable;

public class AcceptingStateProxy implements Serializable {
  public static void Stopping() {
    throw new AcceptingState.Stopping();
  }

  public static void Stopping(String message) {
    throw new AcceptingState.Stopping(message);
  }

  public static void Continuing() {
    throw new AcceptingState.Continuing();
  }

  public static void Continuing(String message) {
    throw new AcceptingState.Continuing(message);
  }

  @Override
  public int hashCode() {
    return 13;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof AcceptingStateProxy;
  }
}
