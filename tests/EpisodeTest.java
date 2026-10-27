import cn.pi.bilipoints.*;import org.json.*;
public class EpisodeTest {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  long now=1800000000000L;JSONObject r=new JSONObject("{\"result\":{\"title\":\"凡人修仙传\",\"episodes\":[{\"id\":1,\"title\":\"196\",\"section_type\":0,\"pub_time\":1790000000},{\"id\":2,\"title\":\"197\",\"long_title\":\"慕兰之战21\",\"section_type\":0,\"pub_time\":1791000000},{\"id\":3,\"title\":\"198\",\"badge\":\"预告\",\"section_type\":1,\"pub_time\":1792000000},{\"id\":4,\"title\":\"199\",\"section_type\":0,\"pub_time\":1900000000},{\"id\":5,\"title\":\"200\",\"section_type\":0,\"is_view_hide\":true}]}}");
  EpisodeChoice e=EpisodeChoice.latest(r,now);ok(e.id==2&&e.title.equals("197"),"select published main episode, not later trailer, future or hidden episode");ok(e.url().equals("https://www.bilibili.com/bangumi/play/ep2")&&e.appUrl().equals("bilibili://bangumi/play/ep2"),"canonical official playback target");
  try{EpisodeChoice.latest(new JSONObject("{\"result\":{\"title\":\"别的剧\",\"episodes\":[]}}"),now);throw new AssertionError("wrong series");}catch(JSONException expected){}
  TaskItem watch=new TaskItem(new JSONObject("{\"task_code\":\"ogvwatchnew\",\"title\":\"观看剧集内容\"}"),"");ok(watch.watch()&&!watch.browse(),"watch task uses actual player, not hidden browse clock");
  JSONArray after=new JSONArray("[{\"order_no\":\"watch\",\"point\":40,\"remark\":\"观看剧集内容\"}]");ok(PointEvidence.credited(new JSONArray(),after,watch)==40,"watch credit still requires new matching ledger evidence");
  if(args.length>0){JSONObject live=new JSONObject(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0])),"UTF-8"));EpisodeChoice actual=EpisodeChoice.latest(live,System.currentTimeMillis());ok(actual.id==live.getJSONObject("result").getJSONObject("new_ep").getLong("id"),"public official API sample agrees on current main episode");System.out.println("Official sample: "+actual.label()+" · "+actual.url());}
  System.out.println("PASS: latest-main filtering, future/trailer exclusion, player URL, watch task identification and credit evidence");
 }
}
