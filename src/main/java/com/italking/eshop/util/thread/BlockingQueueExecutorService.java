package com.italking.eshop.util.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 
 * 进入到同一个队列中的 Callable 会在一个线程的中按顺序执行 
 * @author gang
 *
 */
public class BlockingQueueExecutorService {

	private static int SIZE = 5;
	private ExecutorService threadPool;
	private int size = SIZE;
	private List<ArrayBlockingQueue<Task>> queues = new ArrayList<ArrayBlockingQueue<Task>>();
	private Router router = new DefaultRouter();
	 
	public BlockingQueueExecutorService() {
		this(SIZE, null);
	}
	
	public BlockingQueueExecutorService(int size) {
		this(size, null);
	}
	
	public BlockingQueueExecutorService(int size , Router router) {
		this.size = size;
		this.threadPool = Executors.newFixedThreadPool(size);
		if(router !=null) {
			this.router = router;
		}
		this.initExecutor();
	}
	/**
	 * 调用Callable 
	 * @param callable
	 * @param timeout 等待执行的时间
	 * @return
	 * @throws TimeoutException 
	 */
	public <V> V submit(Callable<V> callable , int timeout) throws TimeoutException {
		Task task = new Task(callable);
		this.queues.get(route(callable)).offer(task);
		/**
		 * 等待任务执行完成
		 */
		boolean r =	 task.lock(timeout);
		/**
		 * 如果执行完成，返回结果
		 */
		if(r) {
			return (V)task.getResult();
		/**
		 * 如果是超时直接抛出异常
		 */
		} else {
			throw new TimeoutException();
		}
	}
	
	private int route(Object callable) {
		return this.router.route(callable, this.size);
	}
	
	
	/**
	 * 初始化线程
	 * 初始化队列
	 */
	private void initExecutor() {
		for(int i = 0 ; i < this.size; i++){
			ArrayBlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(100);
			this.queues.add(queue);
			this.threadPool.submit(new BlockingQueueWorker(queue));
			
		}
	}
	
	public void shutdown() {
		this.threadPool.shutdown();
	}
	
	public static class BlockingQueueWorker implements Runnable {

		private  ArrayBlockingQueue<Task> queue;
		
		public BlockingQueueWorker(ArrayBlockingQueue<Task> queue) {
			this.queue = queue;
		}
		
		@Override
		public void run() {
			while(true) {
				Task task;
				try {
					task = queue.take();
					 task.runSafe();
					 /**
					  * 执行任务后释放锁
					  */
					 task.releaseLock();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static class Task {
		
		private Object task;
		private CountDownLatch latch;
		private Object result;
		
		public Object getResult() {
			return result;
		}

		public void setResult(Object result) {
			this.result = result;
		}

		public Task(Object task) {
			this.task = task;
			this.latch = new CountDownLatch(1);
		}
		
		public Object getTask() {
			return task;
		}
		public void setTask(Object task) {
			this.task = task;
		}
		public CountDownLatch getLatch() {
			return latch;
		}
		public void setLatch(CountDownLatch latch) {
			this.latch = latch;
		}
		/**
		 * 释放锁，等待
		 * @param timeout
		 */
		public boolean lock(int timeout) {
			try {
				return this.latch.await(timeout, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		/**
		 * 释放锁
		 */
		public void releaseLock() {
			this.latch.countDown();
		}
		/**
		 * 调用任务
		 */
		public void runSafe() {
			/**
			 * 捕获异常保证可以执行完成
			 */
			try {
				if(task instanceof Runnable) {
					((Runnable)(task)).run();
				} else if (task instanceof Callable) {
					this.result = ((Callable)(task)).call();
				}
			} catch (Exception e) {
				 e.printStackTrace();
			}
		}
	}
	
	public static class TimeoutException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	public static interface Router {
		/**
		 * 
		 * @param task 提交的任务
		 * @param size 队列的数量
		 * @return 选择进入队列的下标
		 */
		public int route(Object task , int size);
	}
	
	public static class DefaultRouter implements Router {

		@Override
		public int route(Object task, int size) {
			return task.hashCode() % size;
		}
		
	}
	
}
