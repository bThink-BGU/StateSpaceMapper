package il.ac.bgu.cs.bp.statespacemapper.jgrapht;

import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;

import java.util.Objects;

public class MapperVertex {
  private static final long serialVersionUID = 2565436616076859325L;
  public final BProgramSyncSnapshot bpss;

  public MapperVertex(BProgramSyncSnapshot bpss) {
    this.bpss = bpss;
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
