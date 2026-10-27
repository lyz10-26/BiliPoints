package cn.pi.bilipoints;
import java.util.*;import java.util.concurrent.*;
/** Runs independent groups; callers keep dependent operations inside each work item. */
public final class TaskBatch {
 public interface Work {void run()throws Exception;}
 public static void run(List<Work> work,int parallelism,Runnable abort)throws Exception{
  if(work.isEmpty())return;if(parallelism<1)throw new IllegalArgumentException("parallelism");
  ExecutorService pool=Executors.newFixedThreadPool(Math.min(parallelism,work.size()));
  CompletionService<Void> finished=new ExecutorCompletionService<>(pool);boolean complete=false;
  try{
   for(Work w:work)finished.submit(()->{w.run();return null;});
   for(int n=0;n<work.size();n++)try{finished.take().get();}catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw new RuntimeException(cause);}
   complete=true;
  }finally{
   if(!complete)abort.run();pool.shutdownNow();boolean interrupted=false;
   // Do not release the account/session lock while an old request is still running.
   while(!pool.isTerminated())try{pool.awaitTermination(250,TimeUnit.MILLISECONDS);}catch(InterruptedException e){interrupted=true;}
   if(interrupted)Thread.currentThread().interrupt();
  }
 }
}
