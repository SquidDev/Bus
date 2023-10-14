package net.neoforged.bus.test.general;

import java.util.function.Supplier;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.test.ITestHandler;

import static org.junit.jupiter.api.Assertions.*;

public class NonPublicEventHandler implements ITestHandler {
    private static boolean hit = false;

    @Override
    public void test(Supplier<BusBuilder> builder) {
        var bus = builder.get().build();
        assertDoesNotThrow(() -> bus.register(new PUBLIC()));
        testCall(bus, true, "PUBLIC");

        assertDoesNotThrow(() -> bus.register(new PROTECTED()));
        testCall(bus, true, "PROTECTED");
        assertDoesNotThrow(() -> bus.register(new DEFAULT()));
        testCall(bus, true, "DEFAULT");
        assertDoesNotThrow(() -> bus.register(new PRIVATE()));
        testCall(bus, true, "PRIVATE");
    }

    private void testCall(IEventBus bus, boolean expected, String name) {
        hit = false;
        bus.post(new Event());
        assertEquals(expected, hit, name + " did not behave correctly");
    }

    public static class PUBLIC {
        @SubscribeEvent
        public void handler(Event e) {
            hit = true;
        }
    }

    public static class PRIVATE {
        @SubscribeEvent
        private void handler(Event e) {
            hit = true;
        }
    }
    public static class PROTECTED {
        @SubscribeEvent
        protected void handler(Event e) {
            hit = true;
        }
    }
    public static class DEFAULT {
        @SubscribeEvent
        void handler(Event e) {
            hit = true;
        }
    }
}
