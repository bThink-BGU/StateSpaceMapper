package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnyOf implements EventSet {
  public final Set<BEvent> events;

  public AnyOf() {
    this.events = new HashSet<>();
  }

  public AnyOf(BEvent event) {
    this(new BEvent[]{event});
  }

  public AnyOf(BEvent[] events) {
    if (events == null) {
      throw new IllegalArgumentException("Cannot instantiate 'AnyOf' with null event set.");
    } else {
      this.events = new HashSet<>();
      this.events.addAll(Arrays.stream(events).collect(Collectors.toList()));
    }
  }

  public boolean contains(BEvent event) {
    return this.events.stream().anyMatch((es) -> es.contains(event));
  }


  public String toString() {
    if(events.isEmpty()) return "";
    return this.events.stream().map((es) -> es.name).sorted().collect(Collectors.joining(","));
  }

  public int hashCode() {
    return 17 * this.events.hashCode();
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (this == null) {
      return false;
    } else {
      return other instanceof AnyOf ? ((AnyOf) other).events.equals(this.events) : false;
    }
  }
}
