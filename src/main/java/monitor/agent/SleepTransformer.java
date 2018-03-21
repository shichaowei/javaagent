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

public class SleepTransformer implements ClassFileTransformer {

	final static List<String> methodList = new ArrayList<String>();
	final static List<String> classList = new ArrayList<String>();
	static long sleeptime = 5000;
	static String sleeplocation = "after";

	/**
	 * -javaagent:/root/javaagent/javaagentsrc-0.0.1-SNAPSHOT-jar-with-dependencies.jar="
	 * classname=com.fengdai.qa.c,com.fengdai.qa.d##
	 * methodname=com.fengdai.qa.c.m1,com.fengdai.qa.d.m1##sleeptime=5000##sleeplocation=before"
	 *
	 * @param agentArgs
	 */
	public SleepTransformer(String agentArgs) {
		String[] args = agentArgs.split("\\#\\#");
		System.out.println(ToStringBuilder.reflectionToString(args));
		for (String element : args) {
			String key = element.substring(0, element.indexOf("="));
			String value = element.substring(element.indexOf("=") + 1).trim();
			System.out.println(key + "***" + value);
			switch (key) {
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
			case "sleeplocation":
				sleeplocation = value;
				break;
			case "sleeptime":
				sleeptime = Long.valueOf(value);
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
				System.out.println(methodvar);
				System.out.println("check classname:"+className);
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
					System.out.println(ctMethod.getName());
					if ("after".equals(sleeplocation)) {
						ctMethod.insertAfter("long sleeptime="+sleeptime+";try {Thread.sleep(sleeptime);} catch (InterruptedException e) {e.printStackTrace();}");
					} else {
						ctMethod.insertBefore("long sleeptime="+sleeptime+";try {Thread.sleep(sleeptime);} catch (InterruptedException e) {e.printStackTrace();}");
					}
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
						CtMethod ctmethod = ctclass.getDeclaredMethod(methodName);
						System.out.println(ctmethod.getName());
						if ("after".equals(sleeplocation)) {
							try {
								ctmethod.insertAfter("long sleeptime="+sleeptime+";try {Thread.sleep(sleeptime);} catch (InterruptedException e) {e.printStackTrace();}");
							} catch (CannotCompileException e) {
								e.printStackTrace();
							}
						} else {
							try {
								ctmethod.insertBefore("long sleeptime="+sleeptime+";try {Thread.sleep(sleeptime);} catch (InterruptedException e) {e.printStackTrace();}");
							} catch (CannotCompileException e) {
								e.printStackTrace();
							}
						}
					} catch (NotFoundException e) {
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
