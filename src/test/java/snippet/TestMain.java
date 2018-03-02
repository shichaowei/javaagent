package snippet;

import java.io.IOException;

public class TestMain {
	static String businessJdbcUrl = "jdbc:mysql://10.200.130.103:3306/test?useUnicode=true&characterEncoding=utf-8";
	static String businessJdbcName = "fdtest";
	static String businessJdbcPassword = "Mysqltest@123098";
	static String httpserver = "192.168.16.18:8090";

	public static void main(String[] args) throws IOException {
		String temp=
				"String geturl=\"http://httpserver/sqlprocess?sql=java.net.URLEncoder.encode(sql)&businessJdbcUrl=java.net.URLEncod er.encode(businessJdbcUrl)&businessJdbcName=java.net.URLEncoder.encode(businessJdbcName)&businessJdbcPassword=java.net.URLEncoder.encode(businessJdbcPassword);";

			System.out.println(String.format(temp, args));





//		String var="businessJdbcUrl=mysql://10.200.130.103:3306/test?useUnicode=true&characterEncoding=utf-8$$businessJdbcName=root$$businessJdbcPassword=Mysqltest@123098$$httpserver=192.168.16.18:8090";
//		String[] temp= var.split("\\$\\$");
//		System.out.println(ToStringBuilder.reflectionToString(temp));
//		for (String element : temp) {
//			String key=element.substring(0, element.indexOf("="));
//			String value=element.substring(element.indexOf("=")+1).trim();
//			try {
//				System.out.println(key+"***"+value);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}

//		System.out.println("SELECT * FROM users WHERE id = ?".replaceFirst("[?]", "123"));
//		// TODO Auto-generated method stub
//		File file= new File("E:/a.txt");
//		BufferedReader reader = new BufferedReader(new FileReader(file));
//		String temp=null;
//		int line = 1;
//        // 一次读入一行，直到读入null为文件结束
//        while ((temp = reader.readLine()) != null) {
//            // 显示行号
//            System.out.print( temp.trim());
//            line++;
//        }
//        reader.close();


	}

}
