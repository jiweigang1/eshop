package com.eshop;

import java.util.concurrent.Callable;

import org.junit.Test;

import com.italking.eshop.util.thread.BlockingQueueExecutorService;
import com.italking.eshop.util.thread.BlockingQueueExecutorService.Router;
import com.italking.eshop.util.thread.BlockingQueueExecutorService.TimeoutException;

import junit.framework.TestCase;

public class TestService  extends TestCase{
	
	@Test
	public  void test1() {
		BlockingQueueExecutorService service = new BlockingQueueExecutorService(10);
		try {
		boolean result = service.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					System.out.println("call");
					return true;
				}
			}, 5);
			
		 System.out.println("执行完成：" + result);
		} catch (TimeoutException e) {
			e.printStackTrace();
			System.out.println("执行超时");
		}
	}
	/**
	 * 测试执行超时
	 */
	@Test
	public  void test2() {
		BlockingQueueExecutorService service = new BlockingQueueExecutorService(10);
		try {
		boolean result = service.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Thread.sleep(6000);
					System.out.println("call");
					return true;
				}
			}, 5);
			
		 System.out.println("执行完成：" + result);
		} catch (TimeoutException e) {
			e.printStackTrace();
			System.out.println("执行超时");
		}
	}
	
	/**
	 * 测试多个线程
	 */
	
	public static void main(String[] args) {
		final BlockingQueueExecutorService service = new BlockingQueueExecutorService(2 ,new TRouter());
		final long start = System.currentTimeMillis();
		for(int i = 0 ; i < 4 ; i++) {
			final int t = i;
			new Thread() {
				@Override
				public void run() {
					for(int j=0 ; j < 5; j++) {
						try {
							 int  in   = j;
							 int  type = t;
							 int  result =	service.submit(new Request(type , in), 10);
							 	System.out.println("执行完成：" + type + " : " + result);
							} catch (TimeoutException e) {
								System.out.println("执行超时");
							}
					}
					
					System.out.println("结束：" + (System.currentTimeMillis() -  start));
				}
				
			}.start();
		}
	}
	
	
	public static class Request implements Callable<Integer> {

		private int type ;
		private int index; 
		
		public Request(int type, int index) {
			super();
			this.type = type;
			this.index = index;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		@Override
		public Integer call() throws Exception {
			//System.out.println("type = " + type + " index = " + index);
			Thread.sleep(1000);
			return this.index;
		}
	}
	
	public static class TRouter implements Router {
		@Override
		public int route(Object task, int size) {
			Request request = (Request)task;
			int index = request.getType() % size;
			System.out.println(index);
			return index;
		}
		
	}
	

}
