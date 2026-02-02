package net.llvg.eventlib.impl.graph;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.impl.Util;

@NoArgsConstructor (access = AccessLevel.PRIVATE)
@FieldDefaults (level = AccessLevel.PRIVATE)
final class Tarjan<N extends Node<N>> {
    final HashMap<N, Integer> node2dfn = new HashMap<>();
    int dfn = 0;
    final ArrayDeque<N> stack = new ArrayDeque<>();
    final HashSet<N> stacked = new HashSet<>();
    
    final HashMap<N, Scc<N>> node2scc = new HashMap<>();
    
    public static <N extends Node<N>> HashSet<Scc<N>> compute(final Iterable<? extends N> nodes) {
        val algo = new Tarjan<N>();
        algo.search(nodes);
        algo.link();
        
        return new HashSet<>(algo.node2scc.values());
    }
    
    private void search(final Iterable<? extends N> nodes) {
        for (val node : nodes) {
            if (!node2dfn.containsKey(node)) dfs(node);
        }
    }
    
    private void link() {
        for (val entry : node2scc.entrySet()) {
            val scc = entry.getValue();
            
            for (val next : entry.getKey().getSuccessors()) {
                val it = node2scc.get(next);
                
                Util.check(
                  it != null,
                  "Failed to find scc of node {}. " +
                  Node.CONCURRENT_MODIFICATION_MSG,
                  next
                );
                
                if (it != scc && scc.successors.add(it)) ++it.inBound;
            }
        }
    }
    
    private int dfs(final N curr) {
        val ci = ++dfn;
        node2dfn.put(curr, ci);
        
        stack.push(curr);
        stacked.add(curr);
        
        var low = ci;
        
        for (val next : curr.getSuccessors()) {
            val ni = node2dfn.get(next);
            
            if (ni == null) {
                low = Math.min(dfs(next), low);
            } else if (stacked.contains(next)) {
                low = Math.min(ni, low);
            }
        }
        
        if (ci == low) {
            val scc = new Scc<N>();
            
            N it;
            do {
                it = stack.pop();
                scc.nodes.add(it);
                node2scc.put(it, scc);
                stacked.remove(it);
            } while (it != curr);
        }
        
        return low;
    }
}
