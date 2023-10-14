package net.neoforged.bus.benchmarks;

import net.neoforged.bus.api.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ManyListenersBenchmark {
    private static final IEventBus BUS = BusBuilder.builder().build();

    public static class TestEvent extends Event {
        int x = 0;
        int y = 0;
    }

    public static class TestCancellableEvent extends Event implements ICancellableEvent {
        int x = 0;
        int y = 0;
    }

    public static class Listeners0 {
        private final int lookingFor;

        public Listeners0(int lookingFor) {
            this.lookingFor = lookingFor;
        }

        @SubscribeEvent
        public void onEvent(TestEvent evt) {
            if (evt.x == lookingFor) {
                evt.y++;
            }
        }

        @SubscribeEvent
        public void onEvent(TestCancellableEvent evt) {
            if (evt.x == lookingFor) {
                evt.y++;
            }
        }
    }

    public static class Listeners1 {
        private final int lookingFor;

        public Listeners1(int lookingFor) {
            this.lookingFor = lookingFor;
        }

        @SubscribeEvent
        public void onEvent(TestEvent evt) {
            if (evt.x == lookingFor) {
                evt.y++;
            }
        }

        @SubscribeEvent
        public void onEvent(TestCancellableEvent evt) {
            if (evt.x == lookingFor) {
                evt.y++;
            }
        }
    }

    public static class Listeners2 {
        private final int lookingFor;

        public Listeners2(int lookingFor) {
            this.lookingFor = lookingFor;
        }

        @SubscribeEvent
        public void onEvent(TestEvent evt) {
            if (evt.x == lookingFor) {
                evt.y++;
            }
        }

        @SubscribeEvent
        public void onEvent(TestCancellableEvent evt) {
            if (evt.x == lookingFor) {
                evt.y++;
            }
        }
    }

    @Setup
    public void setup() {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                BUS.register(switch (j % 3) {
                    case 0 -> new Listeners0(j);
                    case 1 -> new Listeners1(j);
                    case 2 -> new Listeners2(j);
                    default -> throw new ArithmeticException("Impossible!");
                });
            }
        }
    }

    @Benchmark
    public int testHundredListeners() {
        return BUS.post(new TestEvent()).y;
    }

    @Benchmark
    public int testHundredListenersCancellable() {
        return BUS.post(new TestCancellableEvent()).y;
    }
}
