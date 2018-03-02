package snippet;


public class Snippet {

	public static void main(String[] args) {
		try {
			httpGet("http://www.baidu.com");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void httpGet(String url) throws Exception{

		org.apache.http.impl.client.CloseableHttpClient httpCilent = org.apache.http.impl.client.HttpClients.createDefault();//Creates CloseableHttpClient instance with default configuration.
		org.apache.http.client.methods.HttpGet httpGet = new org.apache.http.client.methods.HttpGet("http://192.168.16.18:8090/sqlprocess?sql="+java.net.URLEncoder.encode("select id, userName, passWord, user_sex as userSex, nick_name as nickName"
				+ " from users where 1=1  and userName = 'aa' and user_sex = 'MAN' order by id desc limit 0,3"));
		httpCilent.execute(httpGet);
		try {
			httpCilent.execute(httpGet);
		} catch (java.io.IOException e) {
		    e.printStackTrace();
		}finally {
		    try {
		        httpCilent.close();//释放资源
		    } catch (java.io.IOException e) {
		        e.printStackTrace();
		    }
		}
	    }


	 public static String sendPost(String url, String param) {
	        java.io.PrintWriter out = null;
	        java.io.BufferedReader in = null;
	        String result = "";
	        try {
	            java.net.URL realUrl = new java.net.URL(url);
	            // 打开和URL之间的连接
	            java.net.URLConnection conn = realUrl.openConnection();
	            // 设置通用的请求属性
	            conn.setRequestProperty("accept", "*/*");
	            conn.setRequestProperty("connection", "Keep-Alive");
	            conn.setRequestProperty("user-agent",
	                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
	            // 发送POST请求必须设置如下两行
	            conn.setDoOutput(true);
	            conn.setDoInput(true);
	            // 获取URLConnection对象对应的输出流
	            out = new java.io.PrintWriter(conn.getOutputStream());
	            // 发送请求参数
	            out.print(param);
	            // flush输出流的缓冲
	            out.flush();
	            // 定义BufferedReader输入流来读取URL的响应
	            in = new java.io.BufferedReader(
	                    new java.io.InputStreamReader(conn.getInputStream(),"utf-8"));
	            String line;
	            while ((line = in.readLine()) != null) {
	                result += line;
	            }
	        } catch (Exception e) {
	            System.out.println("发送 POST 请求出现异常！"+e);
	            e.printStackTrace();
	        }
	        //使用finally块来关闭输出流、输入流
	        finally{
	            try{
	                if(out!=null){
	                    out.close();
	                }
	                if(in!=null){
	                    in.close();
	                }
	            }
	            catch(java.io.IOException ex){
	                ex.printStackTrace();
	            }
	        }
	        return result;
	    }

}

