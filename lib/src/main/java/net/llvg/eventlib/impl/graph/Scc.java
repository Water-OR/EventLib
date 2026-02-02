package net.llvg.eventlib.impl.graph;

import java.util.ArrayList;
import java.util.HashSet;
import lombok.ToString;

@ToString
final class Scc<N extends Node<N>> {
    final ArrayList<N> nodes = new ArrayList<>();
    final HashSet<Scc<N>> successors = new HashSet<>();
    
    int inBound = 0;
}
