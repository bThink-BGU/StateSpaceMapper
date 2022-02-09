package il.ac.bgu.cs.bp.statespacemapper.jgrapht;

import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;

import java.util.Objects;
import java.util.Optional;

public class MapperVertex {
  public final BProgramSyncSnapshot bpss;
  public final boolean startVertex;
  public final boolean accepting;

  public MapperVertex(BProgramSyncSnapshot bpss) {
    this(bpss, false, false);
  }

  public MapperVertex(BProgramSyncSnapshot bpss, boolean startVertex, boolean accepting) {
    this.bpss = bpss;
    this.startVertex = startVertex;
    this.accepting = accepting;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MapperVertex that = (MapperVertex) o;
    return Objects.equals(bpss, that.bpss);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(bpss);
  }
}
