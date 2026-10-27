package cn.pi.bilipoints;
import org.json.*;import java.net.*;import java.io.*;import java.util.Base64;
/** Public GitHub metadata with an independent API fallback, never Bilibili credentials. */
public final class AppUpdates {
 public static final String REPO="https://github.com/lyz10-26/BiliPoints";
 public static final String INDEX="https://raw.githubusercontent.com/lyz10-26/BiliPoints/main/latest.json";
 public static final String API_INDEX="https://api.github.com/repos/lyz10-26/BiliPoints/contents/latest.json?ref=main";
 public final int code;public final String name,apk;
 public interface Source {String read(String url)throws Exception;}
 public AppUpdates(JSONObject j)throws Exception{
  code=j.getInt("version_code");name=j.getString("version_name");apk=j.getString("apk_url");
  if(code<=0||name.isEmpty()||!apk.matches("https://github\\.com/lyz10-26/BiliPoints/raw/refs/heads/main/BiliPoints-[A-Za-z0-9.\\-]+\\.apk"))throw new IOException("更新数据或下载地址不合法");
 }
 public static AppUpdates parse(String text)throws Exception{
  JSONObject j=new JSONObject(text);
  if(!j.has("version_code")){
   if(!"base64".equals(j.optString("encoding")))throw new JSONException("仓库更新数据格式不受支持");
   String encoded=j.getString("content").replaceAll("\\s","");
   if(encoded.length()>24000)throw new IOException("更新数据过大");
   j=new JSONObject(new String(Base64.getDecoder().decode(encoded),"UTF-8"));
  }
  return new AppUpdates(j);
 }
 public static AppUpdates fetch()throws Exception{return fetch(url->read(url));}
 public static AppUpdates fetch(Source source)throws Exception{
  Exception first;
  try{return parse(source.read(INDEX));}catch(Exception e){first=e;}
  try{return parse(source.read(API_INDEX));}catch(Exception e){throw new IOException("主更新源："+describe(first)+"\n备用更新源："+describe(e)+"\n可打开仓库手动下载。",e);}
 }
 static String read(String url)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
  c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","BiliPoints-Update");
  try{
   int status=c.getResponseCode();
   if(status!=200)throw new IOException(status==403?"HTTP 403：访问被拒绝或API限流":status==404?"HTTP 404：仓库更新文件不存在":status==429?"HTTP 429：请求过于频繁":status>=300&&status<400?"HTTP "+status+"：更新源发生重定向":"HTTP "+status+"：更新源响应异常");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(out.size()+n>32768)throw new IOException("更新数据过大");out.write(b,0,n);}}
   return new String(out.toByteArray(),"UTF-8");
  }finally{c.disconnect();}
 }
 public static String describe(Exception e){
  if(e instanceof UnknownHostException)return "域名解析失败，请检查网络或DNS";
  if(e instanceof SocketTimeoutException)return "连接或读取超时";
  if(e instanceof javax.net.ssl.SSLException)return "HTTPS连接或证书验证失败";
  if(e instanceof ConnectException)return "无法连接更新服务器";
  if(e instanceof JSONException||e instanceof IllegalArgumentException)return "更新文件格式错误";
  return e.getMessage()==null?"更新检查异常":e.getMessage();
 }
}
