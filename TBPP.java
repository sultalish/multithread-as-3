
// Alisher Sultangazin
// Problem 1: The Birthday Presents Party (50 points)

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TBPP {
    public static void main(String[] args) throws InterruptedException {
        int servants = 4;
        int presents = 500000;
        // keep track of the time of execution
        long startTime, endTime, totalTime;
        // used memory
        long startUsedMem, endUsedMem, memUsed;
        AtomicBoolean isFinished = new AtomicBoolean(false);

        List<Integer> gifts;
        int[] stream = IntStream.rangeClosed(1, presents).toArray();
        gifts = Arrays.stream(stream).boxed().collect(Collectors.toList());
        Collections.shuffle(gifts);

        startTime = System.currentTimeMillis();
        startUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        GiftsList gifts_list = new GiftsList(isFinished);
        GiftsQueue gifts_queue = new GiftsQueue(presents, gifts);
        gifts_queue.addAll();

        Thread[] thread = new Thread[servants];

        for (int i = 0; i < servants; i++) {
            thread[i] = new Thread(new Servant(gifts_list, gifts_queue, isFinished, presents), "Servant " + i);
        }

        for (int i = 0; i < servants; i++) {
            thread[i].start();
        }

        for (int i = 0; i < servants; i++) {
            thread[i].join();
        }

        if (isFinished.get() == true) {
            System.out.println("\nEND!!!!!!!!");
        }

        endUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        endTime = System.currentTimeMillis();

        totalTime = endTime - startTime;
        memUsed = endUsedMem - startUsedMem;

        System.out.println("\n___________________________________________");
        System.out.println("Exetution time: " + (int) (totalTime) / 1000 + "s");
        System.out.println("Memory usage: " + memUsed);
        System.out.println("___________________________________________");
    }
}

class Servant implements Runnable {

    AtomicBoolean finished;
    private AtomicInteger guestNum;
    private GiftsList gifts_list;
    private GiftsQueue gifts_queue;

    public Servant(GiftsList gifts_list, GiftsQueue gifts_queue, AtomicBoolean finished, int guestNum) {
        this.gifts_list = gifts_list;
        this.gifts_queue = gifts_queue;
        this.guestNum = new AtomicInteger(guestNum);
        this.finished = finished;
    }

    public void run() {
        while (!finished.get()) {
            try {
                gifts_list.operation(gifts_list, gifts_queue, guestNum);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Gift {

    int tag;
    Gift next;

    public Gift(int tag) {
        this.tag = tag;
        this.next = null;
    }
}

class GiftsList {

    int num = 0;
    Gift head;
    Random rand = new Random();
    Lock lock = new ReentrantLock();
    AtomicInteger action = new AtomicInteger();
    public AtomicInteger counter = new AtomicInteger(0);
    public AtomicInteger size = new AtomicInteger(0);
    public AtomicBoolean finished = new AtomicBoolean(false);
    String servant = new String(Thread.currentThread().getName());

    public GiftsList(AtomicBoolean finished) {
        head = new Gift(Integer.MIN_VALUE);
        head.next = new Gift(Integer.MAX_VALUE);
        this.finished = finished;
    }

    public void add(int item) {
        servant = new String(Thread.currentThread().getName());

        lock.lock();
        try {
            if (isEmpty()) {
                Gift newGift = new Gift(item);
                head = newGift;
                head.next = null;
                System.out.printf("%s - Tag%d: Present is added.\n", servant, item);
                size.incrementAndGet();
            } else {
                Gift pred = head;

                while (pred.next != null && pred.next.tag < item) {
                    pred = pred.next;
                }

                Gift newGift = new Gift(item);
                newGift.next = pred.next;
                pred.next = newGift;

                size.incrementAndGet();
                System.out.printf("%s - Tag %d: Present is added.\n", servant, item);
            }

        } finally {
            lock.unlock();
        }
    }

    public void remove() {
        servant = new String(Thread.currentThread().getName());

        lock.lock();

        try {
            if (!isEmpty()) {

                Gift pred = head;
                if (pred.tag != Integer.MIN_VALUE && pred.tag != Integer.MAX_VALUE)
                    System.out.printf("%s - THANK YOU FOR YOUR PRESENT %d.\n", servant, pred.tag);
                if (pred.next != null) {
                    head = pred.next;
                } else {
                    head = null;
                }

                counter.getAndIncrement();
            }
        } finally {
            lock.unlock();
        }

    }

    public void contains(int item) {
        lock.lock();

        try {
            servant = new String(Thread.currentThread().getName());

            if (isEmpty()) {
                System.out.printf("%s - Gift %d is not presented in the list.\n", servant, item);
            } else {
                Gift pred = head;

                if (pred.tag == item)
                    System.out.printf("%s - Gift %d is presented in the list.\n", servant, item);
                else {
                    while (pred.next != null && pred.next.tag != item) {
                        pred = pred.next;
                    }

                    if (pred.tag == item)
                        System.out.printf("%s - Gift %d is presented in the list.\n", servant, item);
                    else
                        System.out.printf("%s - Gift %d is not presented in the list.\n", servant, item);
                }
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

    public void operation(GiftsList list, GiftsQueue giftBag, AtomicInteger guest) throws InterruptedException {

        int giftTag;

        action.set(rand.nextInt(10));

        if (!giftBag.isEmpty() && action.get() < 4) {
            giftTag = giftBag.deq();
            list.add(giftTag);
        } else if (!list.isEmpty() && action.get() < 8) {
            list.remove();
        } else {
            if (!list.isEmpty() && action.get() < 10) {
                list.contains(rand.nextInt(500000));
            }
        }

        if (counter.get() == guest.get())
            finished.set(true);
    }
}

class GiftsQueue {
    int giftNum;
    Lock lock;
    List<Integer> giftBag;
    Queue<Integer> gift_queue;

    public GiftsQueue(int giftNum, List<Integer> giftBag) {
        this.giftNum = giftNum;
        this.giftBag = giftBag;
        gift_queue = new LinkedList<Integer>();
        lock = new ReentrantLock();
    }

    // from list to lock based queue
    public void addAll() {
        for (int i = 0; i < giftNum; i++) {
            int e = (int) giftBag.remove(0);
            gift_queue.add(e);
        }
    }

    // enq() operation
    public void enq(int item) {
        lock.lock();

        try {
            if (!gift_queue.contains(item))
                gift_queue.add(item);

        } finally {
            lock.unlock();
        }
    }

    // deq() operation
    public int deq() throws InterruptedException {
        lock.lock();

        try {
            return gift_queue.poll();
        } finally {
            lock.unlock();
        }
    }

    // size() operation
    public int size() {
        return gift_queue.size();
    }

    // isEmpty() operation
    public boolean isEmpty() {
        return (gift_queue.size() == 0) ? true : false;
    }

    // contains() operation
    public Boolean contains(int item) {
        lock.lock();

        try {
            return gift_queue.contains(item);

        } finally {
            lock.unlock();
        }
    }

    public String display() {
        return gift_queue.toString();
    }
}