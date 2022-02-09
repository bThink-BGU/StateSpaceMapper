package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.ComposableEventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSets;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Utils {
  public static Collection<BEvent> eventSetToList(EventSet es) {
    if(es instanceof BEvent) {
      return List.of((BEvent) es);
    } else if(es instanceof Collection) {
      return ((Collection<?>) es).stream().map(e->(BEvent)e).collect(toList());
    } else if(es instanceof ComposableEventSet.AnyOf) {
      return ((ComposableEventSet.AnyOf) es).events.stream().map(e->(BEvent)e).collect(Collectors.toList());
    } else if(es.equals(EventSets.none)){
      return Set.of();
    } else {
      throw new IllegalArgumentException("EventSet is not a BEvent or a Collection<BEvent>");
    }
  }
}
