package net.llvg.eventlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import lombok.Value;
import lombok.val;
import net.llvg.eventlib.api.bus.EventBus;
import net.llvg.eventlib.api.bus.EventListener;
import net.llvg.eventlib.api.bus.EventTopic;
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
    
    @Test
    void testPostAndCatch() {
        val bus = EventBus.create("default");
        val counter = new int[]{ 0 };
        
        bus.getPhases().link("earlier", "default");
        bus.getPhases().link("default", "later");
        
        bus.register(TestEvent.class, "earlier", e -> ++counter[0]);
        val reg = bus.register(
          TestEvent.class,
          e -> {
              throw new RuntimeException();
          }
        );
        bus.register(TestEvent.class, "later", e -> ++counter[0]);
        
        val error = bus.postAndCatch(new TestEvent());
        Assertions.assertNotNull(error, "[bus.postAndCatch(new TestEvent())] should return an EventError on failure.");
        Assertions.assertEquals(1, error.getIndex(), "[error.getIndex()] mismatch.");
        Assertions.assertEquals(reg, error.getRegistration(), "[error.getRegistration()] mismatch.");
        Assertions.assertEquals(1, counter[0], "Visit count mismatch (counter should not increase after exception).");
        
        reg.unregister();
        val noError = bus.postAndCatch(new TestEvent());
        Assertions.assertNull(noError, "[bus.postAndCatch(new TestEvent())] should return null on success.");
    }
    
    @Test
    void testSimpleTopic() {
        val bus = EventBus.create("default");
        
        val topic0 = EventTopic.<TestEvent>of();
        val topicIsolated = EventTopic.<TestEvent>of();
        
        Assertions.assertNotEquals(topic0, topicIsolated, "Empty topics must not be equal. (object identity required)");
        
        val topic1 = EventTopic.<TestChildEvent>of(topic0);
        
        val visit = new int[]{ 0, 0, 0 };
        
        bus.register(topic0, $ -> ++visit[0]);
        bus.register(topic1, $ -> ++visit[1]);
        bus.register(topicIsolated, $ -> ++visit[2]);
        
        bus.post(topic0, new TestChildEvent());
        
        Assertions.assertEquals(1, visit[0], "Visit count mismatch.");
        Assertions.assertEquals(0, visit[1], "Visit count mismatch. (child topic listener should not be triggered)");
        Assertions.assertEquals(0, visit[2], "Visit count mismatch. (isolated topic listener should not be triggered)");
        
        bus.post(topic1, new TestChildEvent());
        
        Assertions.assertEquals(2, visit[0], "Visit count mismatch. (parent topic listeners should be triggered)");
        Assertions.assertEquals(1, visit[1], "Visit count mismatch.");
        Assertions.assertEquals(0, visit[2], "Visit count mismatch. (isolated topic listener should not be triggered)");
    }
    
    @SuppressWarnings ("DataFlowIssue")
    @Test
    void testVarargTopic() {
        val bus = EventBus.create("default");
        val topic1 = EventTopic.<TestEvent>of();
        val topic2 = EventTopic.<TestEvent>of();
        
        Assertions.assertThrows(NullPointerException.class,
          () -> EventTopic.of(topic1, null)
        );
        Assertions.assertThrows(NullPointerException.class,
          () -> EventTopic.of((EventTopic<Object>) null, topic1)
        );
        
        val topic3 = EventTopic.<TestChildEvent>of(topic1, topic2, topic1);
        {
            val supertopics = new ArrayList<>();
            for (val it : topic3.getSupertopics()) supertopics.add(it);
            
            Assertions.assertEquals(2, supertopics.size(), "Supertopics should be deduplicated.");
            Assertions.assertEquals(Arrays.asList(topic1, topic2), supertopics, "Supertopics should preserve insertion order.");
        }
        
        val visit = new int[]{ 0, 0 };
        
        bus.register(topic1, $ -> ++visit[0]);
        bus.register(topic2, $ -> ++visit[1]);
        
        bus.post(topic3, new TestChildEvent());
        
        Assertions.assertEquals(1, visit[0], "Parent A listener visit count mismatch.");
        Assertions.assertEquals(1, visit[1], "Parent B listener visit count mismatch.");
    }
    
    @SuppressWarnings ("NullableProblems")
    @Test
    void testIterableTopic() {
        val bus = EventBus.create("default");
        val topic1 = EventTopic.<TestEvent>of();
        val topic2 = EventTopic.<TestEvent>of();
        
        Assertions.assertThrows(NullPointerException.class, () -> EventTopic.of(Arrays.asList(null, topic1)));
        Assertions.assertThrows(NullPointerException.class, () -> EventTopic.of(Arrays.asList(topic1, null)));
        
        val topic3 = EventTopic.<TestChildEvent>of(Arrays.asList(topic1, topic2, topic1));
        {
            val supertopics = new ArrayList<>();
            for (val it : topic3.getSupertopics()) supertopics.add(it);
            
            Assertions.assertEquals(2, supertopics.size(), "Supertopics should be deduplicated.");
            Assertions.assertEquals(Arrays.asList(topic1, topic2), supertopics, "Supertopics should preserve insertion order.");
        }
        
        val visit = new int[]{ 0, 0 };
        
        bus.register(topic1, $ -> ++visit[0]);
        bus.register(topic2, $ -> ++visit[1]);
        
        bus.post(topic3, new TestChildEvent());
        
        Assertions.assertEquals(1, visit[0], "Parent A listener visit count mismatch.");
        Assertions.assertEquals(1, visit[1], "Parent B listener visit count mismatch.");
    }
    
    @Test
    void testDumpTree() {
        val topic4 = EventTopic.of("T4");
        val topic3 = EventTopic.of("T3", topic4);
        val topic2 = EventTopic.of("T2");
        val topic1 = EventTopic.of("T1", topic2);
        val topic0 = EventTopic.of("T0", topic1, topic3);
        
        val exists = Arrays.asList("T0", "T1", "T2", "T3", "T4");
        
        val dumped = new String[]{
          Assertions.assertDoesNotThrow(() -> topic0.dumpTree()),
          Assertions.assertDoesNotThrow(() -> topic0.dumpTreeAscii())
        };
        
        Assertions.assertNotNull(dumped[0]);
        Assertions.assertNotNull(dumped[1]);
        
        for (val it : exists) {
            Assertions.assertTrue(dumped[0].contains(it), "Dumped tree should contains '" + it + "'");
            Assertions.assertTrue(dumped[1].contains(it), "Dumped tree ASCII should contains '" + it + "'");
        }
        
        System.out.println("!! Dumped tree: (this is not an error)");
        System.out.print(dumped[0]);
        System.out.println("!! Dumped tree ASCII: (this is not an error)");
        System.out.print(dumped[1]);
    }
}
