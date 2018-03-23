package monitor.agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class MonitorTransformer implements ClassFileTransformer {

	final static List<String> methodList = new ArrayList<String>();
	final static List<String> classList = new ArrayList<String>();
	static String httpserver = "10.200.141.37:8080";

	/**
	 * -javaagent:/root/javaagent/javaagentsrc-0.0.1-SNAPSHOT-jar-with-dependencies.jar="
	 * classname=com.fengdai.qa.c,com.fengdai.qa.d##
	 * methodname=com.fengdai.qa.c.m1,com.fengdai.qa.d.m1"
	 *
	 * @param agentArgs
	 */
	public MonitorTransformer(String agentArgs) {
		String[] args = agentArgs.split("\\#\\#");
		System.out.println(ToStringBuilder.reflectionToString(args));
		for (String element : args) {
			String key = element.substring(0, element.indexOf("="));
			String value = element.substring(element.indexOf("=") + 1).trim();
			System.out.println(key + "***" + value);
			switch (key) {

			case "httpserver":
				httpserver=value;
				break;
			case "classname":
				for (String classvar : value.split(",")) {
					classList.add(classvar);
				}
				break;
			case "methodname":
				for (String methodvar : value.split(",")) {
					methodList.add(methodvar);
				}
				break;
			default:
				break;
			}
		}
	}

	private boolean checkMethod(String className) {

		className = className.replace("/", ".");
		for (String methodvar : methodList) {
			if (methodvar.startsWith(className)) {
//				System.out.println(methodvar);
//				System.out.println("check classname:"+className);
				return true;
			}
		}
		return false;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		className = className.replace("/", ".");
		CtClass ctclass = null;
		// 用于取得字节码类，必须在当前的classpath中，使用全称 ,这 部分是关于javassist的知识
		// http://www.bijishequ.com/detail/49694?p=
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
		classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));


		if (classList.contains(className)) {
			try {
				ctclass = classPool.get(className);
			} catch (NotFoundException e) {
//				e.printStackTrace();
			}
			CtMethod[] ctMethods = ctclass.getDeclaredMethods();
			for (CtMethod ctMethod : ctMethods) {
				try {
					ctMethod.insertBefore("long var =new java.util.Date().getTime();");
					ctMethod.insertAfter("long time=new java.util.Date().getTime()-var;System.out.println(time);");
				} catch (CannotCompileException e) {
					e.printStackTrace();
				}
			}
		}

		if (checkMethod(className)) {
			try {
				//这玩意速度启动很慢，先判断在用
				ctclass = classPool.get(className);
			} catch (NotFoundException e) {
//				e.printStackTrace();
			}
			for (String method : methodList) {
				if (method.startsWith(className)) {
					// 获取方法名
					String methodName = method.substring(method.lastIndexOf('.') + 1, method.length());
					// 得到这方法实例
					System.out.println("fangfa:"+methodName);
					try {
						String outputStr ="\nSystem.out.println(\"this method "+className+":"+methodName+" cost:\" + (endTime-startTime) +\"ms.\");";
						String httpsrc=
								 "org.apache.http.impl.client.CloseableHttpClient httpCilent = org.apache.http.impl.client.HttpClients.createDefault();\n"
								+ "try {"
								+ "String httpserver=\""+httpserver+"\";"
								+ "String classname=\""+className+"\";"
								+ "String methodname=\""+methodName+"\";"
								+ "exectime= endTime-startTime;"
								+ "String geturl=\"http://\"+httpserver+\"/monitorprocess?classname=\"+java.net.URLEncoder.encode(classname)+\"&methodname=\"+java.net.URLEncoder.encode(methodname)+\"&exectime=\"+exectime;"
//								+ "System.out.println(geturl);"
								+ "org.apache.http.client.methods.HttpGet httpGet = new org.apache.http.client.methods.HttpGet(geturl);"
								+ "httpCilent.execute(httpGet);"
								+ "}catch(Exception e){"
								+ "System.out.println(\"javaagent is exception\");"
								+ "e.printStackTrace();"
								+ "}finally {"
								//此处需要加上close 否则资源不释放 状态closewait
								+ "httpCilent.close();"
								+ "}";


						CtMethod ctmethod = ctclass.getDeclaredMethod(methodName);
						ctmethod.addLocalVariable("startTime", ctclass.longType);
						ctmethod.addLocalVariable("endTime", ctclass.longType);
						ctmethod.addLocalVariable("exectime", ctclass.longType);
						ctmethod.insertBefore("\n startTime = System.currentTimeMillis();\n");
						ctmethod.insertAfter("\n endTime = System.currentTimeMillis();\n"+httpsrc+outputStr);
						/*String prefix ="\nlong startTime = System.currentTimeMillis();\n";
						String postfix ="\nlong endTime = System.currentTimeMillis();\n";
						String outputStr ="\nSystem.out.println(\"this method "+methodName+" cost:\" +(endTime - startTime) +\"ms.\");";
						String newMethodName = methodName +"$impl";
						ctmethod.setName(newMethodName);

						CtMethod newMethod = CtNewMethod.copy(ctmethod, methodName, ctclass,null);
						StringBuilder bodyStr =new StringBuilder();
						bodyStr.append("{");
						bodyStr.append(prefix);
						bodyStr.append(newMethodName +"($$);\n");
						bodyStr.append(postfix);
						bodyStr.append(outputStr);
						bodyStr.append("}");
						newMethod.setBody(bodyStr.toString());
						ctclass.addMethod(newMethod);*/



					} catch (NotFoundException | CannotCompileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}


		}

		try {
			return ctclass.toBytecode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CannotCompileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
