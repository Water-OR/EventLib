package net.llvg.eventlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import lombok.Value;
import lombok.val;
import net.llvg.eventlib.api.bus.EventBus;
import net.llvg.eventlib.api.phase.PhaseManager;
import net.llvg.eventlib.impl.Util;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@NullMarked
@TestInstance (TestInstance.Lifecycle.PER_METHOD)
public final class EventLibTest {
    static class TestEvent { }
    
    static class TestChildEvent
      extends TestEvent { }
    
    @Test
    void testBasicFunctionality() {
        val bus = EventBus.create("default");
        val visit = new int[]{ 0 };
        
        bus.register(TestEvent.class, e -> ++visit[0]);
        
        bus.post(new TestEvent());
        Assertions.assertEquals(1, visit[0], "Visit count mismatch.");
    }
    
    @Test
    void testPhaseOrdering() {
        val bus = EventBus.create(0);
        
        bus.getPhases().link(1, 0);
        bus.getPhases().link(2, 1);
        
        val list = new ArrayList<Integer>();
        
        bus.register(TestEvent.class, 0, e -> list.add(0));
        bus.register(TestEvent.class, 1, e -> list.add(1));
        bus.register(TestEvent.class, 2, e -> list.add(2));
        
        bus.post(new TestEvent());
        Assertions.assertEquals(
          Arrays.asList(2, 1, 0),
          list,
          "Visit order mismatch."
        );
        
        list.clear();
        
        bus.getPhases().link(0, 2);
        
        bus.post(new TestEvent());
        Assertions.assertEquals(
          Arrays.asList(0, 1, 2),
          list,
          "Visit order mismatch."
        );
    }
    
    @Test
    void testCustomPhase() {
        @Value
        class Wrapper {
            int value;
        }
        
        val phase1 = new Wrapper(1);
        val phase0 = new Wrapper(0);
        
        val bus = EventBus.create(PhaseManager.builder(phase0)
          .comparator(Comparator.comparing(Wrapper::getValue))
        );
        
        bus.getPhases().link(phase1, phase0);
        
        val list = new ArrayList<Integer>();
        
        bus.register(TestEvent.class, phase0, e -> list.add(0));
        bus.register(TestEvent.class, phase1, e -> list.add(1));
        
        bus.post(new TestEvent());
        Assertions.assertEquals(
          Arrays.asList(1, 0),
          list,
          "Visit order mismatch."
        );
    }
    
    @Test
    void testInheritance() {
        val bus = EventBus.create("default");
        
        val list = new ArrayList<String>();
        
        bus.register(TestEvent.class, e -> list.add("TestEvent"));
        bus.register(TestChildEvent.class, e -> list.add("TestChildEvent"));
        
        bus.post(new TestEvent());
        Assertions.assertEquals(1, list.size(), "Visit count mismatch.");
        Assertions.assertTrue(
          list.contains("TestEvent"),
          () -> Util.format("[list.contains(\"TestEvent\")] result mismatch.", list)
        );
        Assertions.assertFalse(
          list.contains("TestChildEvent"),
          () -> Util.format("'[list.contains(\"TestChildEvent\")] result mismatch.", list)
        );
        
        list.clear();
        
        bus.post(new TestChildEvent());
        Assertions.assertEquals(2, list.size(), "Visit count mismatch.");
        Assertions.assertTrue(
          list.contains("TestEvent"),
          () -> Util.format("[list.contains(\"TestEvent\")] result mismatch.", list)
        );
        Assertions.assertTrue(
          list.contains("TestChildEvent"),
          () -> Util.format("[list.contains(\"TestChildEvent\")] result mismatch.", list)
        );
    }
    
    @Test
    void testRegistration() {
        val bus = EventBus.create("default");
        val counter = new int[]{ 0 };
        
        val reg = bus.register(TestEvent.class, e -> ++counter[0]);
        
        bus.post(new TestEvent());
        Assertions.assertEquals(1, counter[0], "Visit count mismatch.");
        
        reg.setActive(false);
        Assertions.assertFalse(reg.isActive(), "[reg.isActive()] result mismatch.");
        
        bus.post(new TestEvent());
        Assertions.assertEquals(1, counter[0], "Visit count mismatch (counter should not increase when registration paused).");
        
        reg.setActive(true);
        Assertions.assertTrue(reg.isActive(), "[reg.isActive()] result mismatch.");
        
        bus.post(new TestEvent());
        Assertions.assertEquals(2, counter[0], "Visit count mismatch.");
        
        reg.unregister();
        Assertions.assertFalse(reg.isRegistered(), "[reg.isRegistered()] result mismatch.");
        
        bus.post(new TestEvent());
        Assertions.assertEquals(2, counter[0], "Visit count mismatch (counter should not increase after registration deprecated).");
    }
    
    @Test
    void testAutoClosing() {
        val bus = EventBus.create("default");
        val counter = new int[]{ 0 };
        
        try (val ignored = bus.register(TestEvent.class, e -> ++counter[0]).asResource()) {
            bus.post(new TestEvent());
            Assertions.assertEquals(1, counter[0], "Visit count mismatch.");
        }
        
        Assertions.assertEquals(1, counter[0], "Visit count mismatch. (counter should not increase after registration auto-closed)");
    }
}
