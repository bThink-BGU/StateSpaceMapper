package il.ac.bgu.cs.bp.statespacemapper;

import java.util.*;

public class PathGenerationNode<V, E> {
    
    public V systemState;
    public E lastEvent;
    public Object[] nextEvents;
    public List<List<E>> traces;
    public int nextEventsPointer;
    
    public PathGenerationNode(V systemState, E lastEvent, Set<E> nextEvents, List<List<E>> traces)
    {
        this.systemState = systemState;
        this.lastEvent = lastEvent;
        this.nextEvents = nextEvents.toArray();
        this.traces = traces;
        this.nextEventsPointer = 0;
    }

    public E getNextEvent(){
        if (nextEventsPointer >= nextEvents.length){
            return null;
        } else {
            E next = (E)nextEvents[nextEventsPointer];
            this.nextEventsPointer += 1;
            return next;
        }
    }

    public void addRuns(List<List<E>> otherNodeTraces, E lastEvent){
        if(traces == null){
            this.traces = new ArrayList<>();  
        } 
        List<E> curList;
        for (List<E> t : otherNodeTraces){
            curList = new ArrayList<>();
            curList.add(lastEvent);
            curList.addAll(t);
            this.traces.add(curList);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!getClass().isInstance(obj)) {
            return false;
        }
        PathGenerationNode<V, E> other = (PathGenerationNode<V, E>) obj;
        return systemState == other.systemState;
    }

}
