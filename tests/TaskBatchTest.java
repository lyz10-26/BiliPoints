import cn.pi.bilipoints.*;import org.json.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class TaskBatchTest {
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
 static TaskItem task(String code,String title,int state)throws Exception{return new TaskItem(new JSONObject().put("task_code",code).put("title",title).put("state",state),"");}
 public static void main(String[] args)throws Exception{
  TaskItem anime=task("animatetab","浏览追番频道",1),film=task("filmtab","浏览影视频道",1),mall=task("vipmallview","浏览会员购页面",1),alias=task("vipmallviewnew","浏览会员购页面",1),dress=task("dressview","浏览装扮商城主页",1);
  DailyTaskPlan plan=new DailyTaskPlan(Arrays.asList(anime,film,mall,alias,dress,anime,task("ogvwatchnew","观看剧集内容",0),task("buy","购买商品",0),task("finished","已完成",3)));
  ok(plan.codes.size()==5&&plan.groups.size()==4,"exclude watch, purchases, completed and duplicate codes");
  ok(plan.groups.get("vipmallview").size()==2,"same credit/destination aliases run within one ordered group");
  CountDownLatch firstWave=new CountDownLatch(3),release=new CountDownLatch(1),fourth=new CountDownLatch(1);AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger(),started=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
  List<TaskBatch.Work> work=new ArrayList<>();
  for(int i=0;i<4;i++)work.add(()->{int n=active.incrementAndGet();maximum.updateAndGet(v->Math.max(v,n));int launch=started.incrementAndGet();try{if(launch<=3){firstWave.countDown();ok(release.await(3,TimeUnit.SECONDS),"test gate timeout");}else fourth.countDown();}finally{active.decrementAndGet();}});
  Thread coordinator=new Thread(()->{try{TaskBatch.run(work,3,()->{});}catch(Throwable e){failure.set(e);}});coordinator.start();
  ok(firstWave.await(3,TimeUnit.SECONDS),"three independent jobs must actually overlap");ok(started.get()==3&&fourth.getCount()==1,"fourth job waits for a free slot");release.countDown();coordinator.join(4000);
  ok(!coordinator.isAlive()&&failure.get()==null&&maximum.get()==3&&started.get()==4&&active.get()==0,"all jobs finish with bounded parallelism");
  CountDownLatch waiting=new CountDownLatch(1);AtomicBoolean aborted=new AtomicBoolean(),exited=new AtomicBoolean();
  try{TaskBatch.run(Arrays.asList(()->{waiting.countDown();try{new CountDownLatch(1).await();}finally{exited.set(true);}},()->{ok(waiting.await(3,TimeUnit.SECONDS),"peer started");throw new Exception("login expired");}),2,()->aborted.set(true));throw new AssertionError("failure not propagated");}catch(Exception e){ok(e.getMessage().equals("login expired"),"original fatal reason propagated");}
  ok(aborted.get()&&exited.get(),"fatal failure aborts peers and joins them before returning");
  CountDownLatch cancellationStarted=new CountDownLatch(1);AtomicBoolean cancelledPeerExited=new AtomicBoolean();AtomicReference<Throwable> cancellation=new AtomicReference<>();
  Thread cancelCoordinator=new Thread(()->{try{TaskBatch.run(Arrays.asList(()->{cancellationStarted.countDown();try{new CountDownLatch(1).await();}finally{cancelledPeerExited.set(true);}}),3,()->{});}catch(Throwable e){cancellation.set(e);}});
  cancelCoordinator.start();ok(cancellationStarted.await(3,TimeUnit.SECONDS),"cancellable work started");cancelCoordinator.interrupt();cancelCoordinator.join(4000);
  ok(!cancelCoordinator.isAlive()&&cancelledPeerExited.get()&&cancellation.get() instanceof InterruptedException,"stop interrupts and joins all workers");
  TaskProgress progress=new TaskProgress(plan.codes);ok(progress.update(anime.code,1)==26,"one of five completed tasks advances a fifth");ok(progress.update(anime.code,.2)==26,"late callbacks never lower progress");List<TaskBatch.Work> updates=new ArrayList<>();for(String code:plan.codes)updates.add(()->{for(int n=0;n<=100;n++)progress.update(code,n/100.0);});TaskBatch.run(updates,3,()->{});ok(progress.update(anime.code,1)==90,"parallel progress reaches final-sync boundary");
  JSONArray ledger=new JSONArray("[{\"order_no\":\"a\",\"point\":10,\"remark\":\"浏览追番频道\"},{\"order_no\":\"f\",\"point\":10,\"remark\":\"浏览影视频道\"},{\"order_no\":\"m\",\"point\":10,\"remark\":\"浏览会员购页面\"}]");
  for(TaskItem t:Arrays.asList(anime,film,mall))ok(PointEvidence.credited(new JSONArray(),ledger,t)==10,"parallel ledger must not credit another category");
  System.out.println("PASS: real worker overlap, concurrency cap, task exclusions/grouping, fatal/stop cleanup, aggregate progress and credit isolation");
 }
}
