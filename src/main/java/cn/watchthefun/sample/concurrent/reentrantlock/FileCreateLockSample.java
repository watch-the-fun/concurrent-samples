package cn.watchthefun.sample.concurrent.reentrantlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author geyx
 * 文件创建锁样例
 */
public class FileCreateLockSample {

    public static final Logger LOG = LoggerFactory.getLogger("FileCreateLockSample");

    private final static ReentrantLock LOCK = new ReentrantLock();

    public static AtomicBoolean isRunning = new AtomicBoolean(false);
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
    /**
     * 用于模拟文件是否存在
     */
    public static AtomicBoolean isFileExisted = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {
        int count = 10;
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path targetFile = Paths.get(tmpDir, "all.txt");
        LOG.info("要读取的文件路径为 {}", targetFile);
        try {
            // 启动时清理，方便测试多线程处理逻辑是否正确
            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileCreateLockSample main = new FileCreateLockSample();
        // 使用 CountDownLatch 进行并发模拟，实际业务中不需要该逻辑
        CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            executorService.execute(() -> {
                LOG.info("开启线程 {}", Thread.currentThread());
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (isRunning.get() || !Files.exists(targetFile)) {
                    LOG.info("进行文件访问，文件不存在");
                    main.readFile(targetFile);
                }
                try {
                    LOG.info("已存在文件，读取文件操作，文件内容为 {}", new String(Files.readAllBytes(targetFile), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
        }
        countDownLatch.countDown();
    }

    public void readFile(Path targetFile) {
//        if (!isRunning.get()) {
        LOG.info("准备获取锁");
        LOCK.lock();
        try {
            // 准备创建文件
            LOG.info("已获取锁");
            if (Files.exists(targetFile) && !isRunning.get()) {
                LOG.info("拿锁后再次判断，文件存在，跳过生成");
            } else {
                isRunning.set(true);
                LOG.info("开始创建文件!!!!!!!，isRunning = {} ", isRunning.get());
                Files.createFile(targetFile);
                Files.write(targetFile, "you win".getBytes(StandardCharsets.UTF_8));
                // 创建文件过程
//                Thread.sleep(1000);
                LOG.info("文件创建完成，保存位置为 {}", targetFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            LOCK.unlock();
            isRunning.set(false);
        }
//        }
    }
}