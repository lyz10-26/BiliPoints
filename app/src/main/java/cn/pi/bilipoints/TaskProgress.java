package cn.pi.bilipoints;
import java.util.*;
/** Per-task monotonic progress prevents one concurrent task from overwriting another. */
public final class TaskProgress {
 private final Map<String,Double> fractions=new LinkedHashMap<>();
 public TaskProgress(){}
 public TaskProgress(Collection<String> codes){for(String code:codes)fractions.put(code,0.0);}
 public synchronized int update(String code,double fraction){
  double bounded=Math.max(0,Math.min(1,fraction));Double prior=fractions.get(code);fractions.put(code,Math.max(prior==null?0:prior,bounded));double sum=0;for(double value:fractions.values())sum+=value;
  return 10+(int)(80*sum/Math.max(1,fractions.size()));
 }
}
