package net.llvg.eventlib.impl.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.impl.Util;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

@UtilityClass
public final class TopoSorter {
    @CanIgnoreReturnValue
    public <N extends Node<N>> boolean sort(
      final @UnmodifiableView Iterable<? extends N> nodes,
      final Collector<? super N> collector,
      final Comparator<? super N> comparator
    ) {
        Util.argNotNull(nodes, "nodes");
        Util.argNotNull(collector, "collector");
        Util.argNotNull(comparator, "comparator");
        
        val sccSet = Tarjan.compute(nodes);
        var cycleless = true;
        
        val que = new PriorityQueue<Scc<N>>(Comparator.comparing(it -> it.nodes.get(0), comparator));
        
        for (val scc : sccSet) {
            if (scc.nodes.size() > 1) cycleless = false;
            scc.nodes.sort(comparator);
            
            if (scc.inBound == 0) que.add(scc);
        }
        
        collector.prepare();
        var processed = 0;
        
        for (Scc<N> curr; (curr = que.poll()) != null; ) {
            collector.add(Collections.unmodifiableList(curr.nodes));
            ++processed;
            
            for (val next : curr.successors) {
                if (0 == --next.inBound) que.add(next);
            }
        }
        
        Util.check(
          processed == sccSet.size(),
          "Failed to process all nodes ({} of {} done). " +
          Node.CONCURRENT_MODIFICATION_MSG,
          processed, sccSet.size()
        );
        
        return cycleless;
    }
    
    public interface Collector<N extends Node<N>> {
        default void prepare() { }
        
        void add(final @Unmodifiable List<N> nodes);
    }
    
}