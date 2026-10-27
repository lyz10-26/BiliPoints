package cn.pi.bilipoints;
import java.util.*;
/** A group shares a destination/credit category and must run in sequence. */
public final class DailyTaskPlan {
 public final LinkedHashMap<String,ArrayList<TaskItem>> groups=new LinkedHashMap<>();
 public final ArrayList<String> codes=new ArrayList<>();
 public DailyTaskPlan(Collection<TaskItem> tasks){
  HashSet<String> seen=new HashSet<>();
  for(TaskItem t:tasks){if(t.purchase()||t.watch()||t.state==3||!seen.add(t.code))continue;
   codes.add(t.code);String key=t.browse()?t.browseKind():"other";
   if(!groups.containsKey(key))groups.put(key,new ArrayList<TaskItem>());groups.get(key).add(t);
  }
 }
}
