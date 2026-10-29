import cn.pi.bilipoints.*;import org.json.*;
public class BrowseTest {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] x)throws Exception{
  TaskItem mall=new TaskItem(new JSONObject("{\"task_code\":\"mall_task_v3\",\"title\":\"浏览会员购页面10秒\",\"state\":1}"),"");ok(mall.browse()&&mall.browseKind().equals("vipmallview"),"new task code must still show timer by known task title");
  TaskItem dress=new TaskItem(new JSONObject("{\"task_code\":\"dress_view_2026\",\"title\":\"浏览装扮商城主页\",\"state\":1}"),"");ok(dress.browse()&&dress.browseSeconds()==0,"dress browse opens without a dwell countdown");ok(mall.browseSeconds()==12,"mall browsing retains its dwell requirement");
  TaskItem buy=new TaskItem(new JSONObject("{\"task_code\":\"vipmallbuy\",\"title\":\"购买商品\"}"),"");ok(!buy.browse(),"purchases never timed or automated");
  BrowseClock c=new BrowseClock(12,0);ok(c.seconds()==12,"initial timer is immediately available");c.update(10000,false);ok(c.seconds()==12,"page loading does not count");c.update(13000,true);ok(c.seconds()==9,"foreground browsing counts");c.update(33000,false);ok(c.seconds()==9,"off page error or login pause");c.anchor(50000);c.update(52000,true);ok(c.seconds()==7,"background time excluded after resume");c.update(59000,true);ok(c.seconds()==0,"finishes after actual foreground duration");c.update(69000,true);ok(c.seconds()==0,"never negative");
  System.out.println("PASS: changed task codes, purchase exclusion, visible initial timer, page loading/background/error pause and completion");
 }
}
