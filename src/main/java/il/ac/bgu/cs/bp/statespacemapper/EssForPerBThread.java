package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.AbstractEventSelectionStrategy;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSets;
import org.mozilla.javascript.Context;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toList;

public class EssForPerBThread extends AbstractEventSelectionStrategy {
  @Override
  public Set<BEvent> selectableEvents(BProgramSyncSnapshot bpss) {
    warnOnHints(bpss);
    Set<SyncStatement> statements = bpss.getStatements();
    List<BEvent> externalEvents = bpss.getExternalEvents();
    if (statements.isEmpty()) {
      // Corner case, not sure this is even possible.
      return externalEvents.isEmpty() ? emptySet() : singleton(externalEvents.get(0));
    }

    var blocked = statements.stream()
        .filter(Objects::nonNull)
        .map(SyncStatement::getBlock)
        .filter(r -> r != EventSets.none)
        .flatMap(es -> new AnyOf(es).events.stream())
        .collect(toSet());

    var requested = statements.stream()
        .filter(Objects::nonNull)
        .flatMap(stmt -> stmt.getRequest().stream())
        .map(e->(BEvent)e)
        .collect(toSet());

    var waitFor = statements.stream()
        .filter(Objects::nonNull)
        .map(SyncStatement::getWaitFor)
        .filter(r -> r != EventSets.none)
        .flatMap(es -> new AnyOf(es).events.stream())
        .collect(toSet());

    requested.addAll(waitFor);

    // Let's see what internal events are requested and not blocked (if any).
    try {
      BPjs.enterRhinoContext();
      Set<BEvent> requestedAndNotBlocked = requested.stream()
          .filter(req -> !blocked.contains(req))
          .collect(toSet());

      return requestedAndNotBlocked.isEmpty() ?
          externalEvents.stream().filter(e -> !blocked.contains(e)) // No internal events requested, defer to externals.
              .findFirst().map(e -> singleton(e)).orElse(emptySet())
          : requestedAndNotBlocked;
    } finally {
      Context.exit();
    }
  }
}