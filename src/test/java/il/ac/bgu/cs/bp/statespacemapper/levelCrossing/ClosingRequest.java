package il.ac.bgu.cs.bp.statespacemapper.levelCrossing;

@SuppressWarnings("serial")
public class ClosingRequest extends IEvent {
  public static final String NAME = "CR";

  public ClosingRequest(int i) {
    super(NAME, i);
  }

  public ClosingRequest() {
    super(NAME);
  }
}
