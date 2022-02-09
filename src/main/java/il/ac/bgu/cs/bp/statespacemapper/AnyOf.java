package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.ComposableEventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSets;

import java.util.*;
import java.util.stream.Collectors;

public class AnyOf implements EventSet {
  public final Set<BEvent> events;

  public AnyOf() {
    this.events = new HashSet<>();
  }

  public AnyOf(BEvent ... events) {
    if (events == null) {
      throw new IllegalArgumentException("Cannot instantiate 'AnyOf' with null event set.");
    } else {
      this.events = new HashSet<>();
      this.events.addAll(Arrays.stream(events).collect(Collectors.toList()));
    }
  }

  public AnyOf(EventSet es) {
    if(es instanceof BEvent) {
      this.events = new HashSet<>();
      events.add((BEvent) es);
    } else if(es instanceof Collection) {
      this.events = new HashSet<>();
      events.addAll((Collection<BEvent>) es);
    } else if(es instanceof ComposableEventSet.AnyOf) {
      this.events = new HashSet<>();
      events.addAll(((ComposableEventSet.AnyOf) es).events.stream().map(e->(BEvent)e).collect(Collectors.toList()));
    } else if(es.equals(EventSets.none)){
      this.events = Set.of();
    } else {
      throw new IllegalArgumentException("EventSet is not a BEvent or a Collecion<BEvent>");
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
    } else {
      return other instanceof AnyOf && ((AnyOf) other).events.equals(this.events);
    }
  }
}