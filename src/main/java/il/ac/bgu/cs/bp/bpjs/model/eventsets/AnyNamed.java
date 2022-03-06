/*
 * (c) 2021 Testory Technologies
 */
package il.ac.bgu.cs.bp.bpjs.model.eventsets;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;

import java.util.Objects;

/**
 * An event set that contains all events with a given name.
 *
 * @author michael
 */
public class AnyNamed implements EventSet {

    private final String name;

    public AnyNamed(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean contains(BEvent event) {
        return name.equals(event.getName());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AnyNamed other = (AnyNamed) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return "AnyNamed{" + "name=" + name + '}';
    }

}
