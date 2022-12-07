package wethinkcode.model;

import java.util.Objects;

/**
 * A Municipality represents any town, neighbourhood, populated area or settled place in the place-names
 * data.
 * <p>
 * We assume that there is only one town with a given getName in each Province. (<em>In reality this
 * is simply not true</em> and we'd have to invent a more sophisticated model to deal with that. But
 * then we'd also need better data than we have access to... Since our mission is to explore
 * Distributed Systems and integration, our assumption is Good Enough.)
 */
public record Place(String name, String municipality) {
    public Place{
        Objects.requireNonNull(name);
        Objects.requireNonNull(municipality);
    }
}

