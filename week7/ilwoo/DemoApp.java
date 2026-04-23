import java.util.ArrayList;
import java.util.List;

public class DemoApp {

    // 일부러 정적 리스트에 쌓아서 GC가 못 치우게 → 힙 덤프에 잘 잡힘
    static final List<byte[]> memory = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("PID: " + ProcessHandle.current().pid());

        // ① 메모리를 1초마다 1MB씩 먹는 스레드 → 힙 덤프에서 byte[] 로 보임
        new Thread(() -> {
            while (true) {
                memory.add(new byte[1024 * 1024]);
                System.out.println("heap used: " + memory.size() + " MB");
                sleep(1000);
            }
        }, "MemoryEater").start();

        // ② CPU를 태우는 스레드 → top -H 에서 혼자 100% 찍음
        new Thread(() -> {
            long x = 0;
            while (true) x++;
        }, "CpuBurner").start();

        // ③ 그냥 대기하는 스레드 → 스레드 덤프에서 WAITING 상태로 보임
        new Thread(() -> {
            synchronized (DemoApp.class) {
                try { DemoApp.class.wait(); } catch (InterruptedException e) {}
            }
        }, "IdleWaiter").start();

        // main은 종료 안 되게 잡아둠
        Thread.currentThread().join();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}