package il.ac.bgu.cs.bp.statespacemapper.jgrapht;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import org.jgrapht.graph.DefaultEdge;

public class MapperEdge extends DefaultEdge {
  private static final long serialVersionUID = -7176704143413508648L;
  public final BEvent event;

  public MapperEdge(BEvent event) {
    this.event = event;
  }

  public BEvent getEvent() {
    return event;
  }

  @Override
  public String toString() {
    return "(" + getSource() + " : " + getTarget() + " : " + event + ")";
  }
}
