package net.llvg.eventlib.impl.graph;

import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.UnmodifiableView;

@RequiredArgsConstructor
public abstract class Node<N extends Node<N>> {
    static final String CONCURRENT_MODIFICATION_MSG =
      "This usually implies the graph structure changed during sorting. " +
      "(e.g. concurrent modification or non-deterministic successors)";
    
    protected final Set<N> successors;
    
    public final @UnmodifiableView Set<N> getSuccessors() {
        return Collections.unmodifiableSet(successors);
    }
    
    public boolean linkTo(final N other) {
        return successors.add(other);
    }
}
