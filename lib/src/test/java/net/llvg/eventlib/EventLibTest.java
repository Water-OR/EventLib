package net.llvg.eventlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import lombok.Value;
import lombok.val;
import net.llvg.eventlib.api.bus.EventBus;
import net.llvg.eventlib.api.bus.EventListener;
import net.llvg.eventlib.api.bus.ForwardingEventBus;
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
    void testRegistrationProperties() {
        val bus = EventBus.create("default");
        
        val phase = "some_phase";
        final EventListener<TestEvent> listener = e -> { };
        
        val reg = bus.register(TestEvent.class, phase, listener);
        
        Assertions.assertEquals(phase, reg.getPhase(), "[reg.getPhase()] mismatch.");
        Assertions.assertEquals(listener, reg.getListener(), "[reg.getListener()] mismatch.");
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
    
    enum EnumPhases {
        ANOTHER, DEFAULT
    }
    
    @Test
    void testEnumPhase() {
        val bus = EventBus.create(PhaseManager.builderEnum(EnumPhases.DEFAULT));
        
        val list = new ArrayList<Integer>();
        
        bus.register(TestEvent.class, EnumPhases.DEFAULT, e -> list.add(1));
        bus.register(TestEvent.class, EnumPhases.ANOTHER, e -> list.add(0));
        
        bus.post(new TestEvent());
        Assertions.assertEquals(
          Arrays.asList(0, 1),
          list,
          "Visit order mismatch."
        );
        
        bus.getPhases().link(EnumPhases.DEFAULT, EnumPhases.ANOTHER);
        
        list.clear();
        
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
    
    @Test
    void testForwardingBus() {
        val bus = new ForwardingEventBus<String>() {
            final EventBus<String> delegate = EventBus.create("default");
            
            @Override
            protected EventBus<String> delegate() {
                return delegate;
            }
        };
        
        val visit = new int[]{ 0 };
        
        bus.register(TestEvent.class, e -> ++visit[0]);
        
        bus.post(new TestEvent());
        Assertions.assertEquals(1, visit[0], "Visit count mismatch.");
    }
    
    @Test
    void testSnapshot() {
        val bus = EventBus.create("default");
        
        val reg = bus.register(TestEvent.class, e -> { });
        
        val snapshot1 = bus.getSnapshot(TestEvent.class);
        Assertions.assertEquals(1, snapshot1.size(), "Snapshot 1 size mismatch.");
        Assertions.assertEquals(snapshot1.get(0), reg);
        Assertions.assertThrows(
          UnsupportedOperationException.class,
          snapshot1::clear,
          "SnapshotList should be immutable"
        );
        
        bus.register(TestEvent.class, e -> { });
        
        val snapshot2 = bus.getSnapshot(TestEvent.class);
        Assertions.assertEquals(1, snapshot1.size(), "Snapshot 1 size mismatch. (should not be modified)");
        Assertions.assertEquals(2, snapshot2.size(), "Snapshot 2 size mismatch.");
    }
}
