import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ATRM {
    public static void main(String[] args) throws InterruptedException {
        // 8 temperature sensors
        int sensors = 8;
        // 24 hours duration
        int duration = 24;
        // keep track of the time of execution
        long startTime, endTime, totalTime;
        // used memory
        long startUsedMem, endUsedMem, memUsed;

        startTime = System.currentTimeMillis();
        startUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        AtomicBoolean isFinished = new AtomicBoolean(false);

        TempModule record = new TempModule(isFinished);

        Thread[] thread = new Thread[sensors];

        for (int i = 0; i < sensors; i++) {
            thread[i] = new Thread(new TemperatureSensor(record, isFinished, duration), "Sensor " + i);
        }

        for (int i = 0; i < sensors; i++) {
            thread[i].start();
        }

        for (int i = 0; i < sensors; i++) {
            thread[i].join();
        }

        endUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        endTime = System.currentTimeMillis();

        totalTime = endTime - startTime;
        memUsed = endUsedMem - startUsedMem;

        System.out.println("========================");
        System.out.println("Exetution time: " + (int) (totalTime) / 100 + "ms");
        System.out.println("Memory usage: " + memUsed);
        System.out.println("========================");
    }
}

class TemperatureSensor implements Runnable {
    AtomicBoolean isFinished;
    private AtomicInteger duration;
    private TempModule record;

    public TemperatureSensor(TempModule record, AtomicBoolean isFinished, int duration) {
        this.record = record;
        this.duration = new AtomicInteger(duration);
        this.isFinished = isFinished;
    }

    public void run() {
        while (!isFinished.get()) {
            try {
                record.start(record, duration);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

class temperatureRead {

    int temperature;
    int start;
    int end;
    temperatureRead next;

    public temperatureRead(int temperature, int start, int end) {
        this.temperature = temperature;
        this.start = start;
        this.end = end;
        this.next = null;
    }
}

class TempModule {
    temperatureRead head;
    Random rand = new Random();
    Lock lock = new ReentrantLock();
    AtomicInteger temperature = new AtomicInteger();
    AtomicInteger start = new AtomicInteger(-1);
    AtomicInteger end = new AtomicInteger(0);
    public AtomicInteger hour = new AtomicInteger(0);
    public AtomicInteger timeCounter = new AtomicInteger(0);
    public AtomicBoolean isFinished = new AtomicBoolean(false);

    public TempModule(AtomicBoolean isFinished) {
        head = new temperatureRead(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        head.next = new temperatureRead(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.isFinished = isFinished;
    }

    public void add(int temperature, int start, int end) {

        lock.lock();

        try {
            if (isEmpty()) {
                temperatureRead newNode = new temperatureRead(temperature, start, end);
                head = newNode;
                head.next = null;
                timeCounter.incrementAndGet();
            } else {
                temperatureRead pred = head;

                while (pred.next != null && pred.next.start < start) {
                    pred = pred.next;
                }

                temperatureRead newNode = new temperatureRead(temperature, start, end);
                newNode.next = pred.next;
                pred.next = newNode;
                timeCounter.incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {

        lock.lock();

        try {
            return (head == null) ? true : false;

        } finally {
            lock.unlock();
        }

    }

    public void showRecords(int s_interval, int e_interval) {

        lock.lock();

        try {
            if (isEmpty()) {
                System.out.printf("There's no records for the given intervals.\n");
            } else {
                temperatureRead pred = head;

                while (pred.next != null && pred.next.start >= s_interval && pred.next.start <= e_interval) {

                    if (pred.start != Integer.MIN_VALUE && pred.start != Integer.MAX_VALUE) {
                        if (hour.get() == 0) {
                            System.out.printf("Intervals: %d - %d:\tTemperature: %dF\n", pred.start, pred.end,
                                    pred.temperature);
                        } else {
                            System.out.printf("Intervals: %d - %d:\tTemperature: %dF\n", pred.start % 60, pred.end % 60,
                                    pred.temperature);
                        }
                    }
                    pred = pred.next;
                }

            }
        } finally

        {
            lock.unlock();
        }

    }

    public void lowest() {

        lock.lock();

        try {
            if (!isEmpty()) {

                ArrayList<Integer> lowest = new ArrayList<Integer>();

                temperatureRead pred = head;

                while (pred.next != null) {

                    if (pred.temperature != Integer.MIN_VALUE && pred.temperature != Integer.MAX_VALUE) {
                        lowest.add(pred.temperature);
                    }

                    pred = pred.next;
                }

                Collections.sort(lowest);

                int i = 0;
                int counter = 1;
                for (int temperature : lowest) {
                    if (i < 5) {
                        System.out.printf("%d. Lowest Temperature: %dF\n", counter++, temperature);
                    }
                    i++;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void highest() {

        lock.lock();

        try {
            if (!isEmpty()) {

                ArrayList<Integer> highest = new ArrayList<Integer>();

                temperatureRead pred = head;

                while (pred.next != null) {

                    if (pred.temperature != Integer.MIN_VALUE && pred.temperature != Integer.MAX_VALUE) {
                        highest.add(pred.temperature);
                    }

                    pred = pred.next;
                }

                Collections.sort(highest);

                int i = 0;
                int counter = 1;
                for (int temperature : highest) {
                    if (i >= (highest.size() - 5) && i < highest.size()) {
                        System.out.printf("%d. Highest Temperature: %dF\n", counter++, temperature);
                    }
                    i++;
                }
            }
        } finally {
            lock.unlock();
        }

    }

    public void tempDifference() {
        lock.lock();

        try {
            if (!isEmpty()) {
                int i = 0, j = 0;
                int start = 0, end = 0, diff;
                temperatureRead pred = head;
                temperatureRead curr = head.next;
                int[][] differences = new int[50][3];

                while (curr != null && j < 50) {

                    if (pred.temperature != Integer.MIN_VALUE && curr.temperature != Integer.MAX_VALUE) {
                        start = pred.temperature;

                        while (curr.next != null && i < 9) {
                            curr = curr.next;
                            i++;
                        }

                        end = curr.temperature;

                        diff = start - end;

                        differences[j][0] = diff;
                        differences[j][1] = pred.start;
                        differences[j][2] = curr.start;
                        j++;

                    }
                    pred = pred.next;
                    curr = curr.next;

                }

                i = 0;

                int max = differences[0][i];

                for (j = 1; j < 50; j++) {
                    if (differences[j][0] > max) {
                        max = differences[j][0];
                        start = differences[j][1];
                        end = differences[j][2];
                    }
                }
                if (hour.get() == 0) {
                    System.out.printf("Start Time: %d\nEnd Time: %d\nDifference: %dF\n", start, end, max);
                } else {
                    System.out.printf("Start Time: %d\nEnd Time: %d\nDifference: %dF\n", start % 60,
                            end % 60, max);
                }

            }
        } finally {
            lock.unlock();
        }

    }

    public void start(TempModule record, AtomicInteger duration) throws InterruptedException {

        lock.lock();

        try {
            temperature.set(rand.nextInt(171) - 100);
            start.getAndIncrement();
            end.getAndIncrement();

            record.add(temperature.get(), start.get(), end.get());

            if (timeCounter.get() % 60 == 0) {

                System.out.println("\n\n======================================================");
                System.out.printf("Atmospheric Temperature Module on hour %d\n", hour.get());
                System.out.println("======================================================");
                System.out.printf(" Records:\n");

                if (hour.get() == 0)
                    record.showRecords(0, 60);
                else
                    record.showRecords(60 * hour.get(), 120 * hour.get());

                System.out.println("______________________________________________________");
                System.out.println("Lowest Temperature Record: ");
                record.lowest();
                System.out.println("______________________________________________________");

                System.out.println("Highest Temperature Record: ");
                record.highest();
                System.out.println("______________________________________________________");

                System.out.println("Largest temperature difference:");
                record.tempDifference();

                head = null;
                hour.getAndIncrement();
            }

            if (hour.get() == duration.get())
                isFinished.set(true);
        } finally {
            lock.unlock();
        }
    }
}