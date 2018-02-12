package monitor.agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class MonitorTransformer implements ClassFileTransformer {

	final static String prefix = "\nlong startTime = System.currentTimeMillis();\n";
	final static String postfix = "\nlong endTime = System.currentTimeMillis();\n";
	final static List<String> methodList = new ArrayList<String>();
	static {
//		methodList.add("monitor.agent.MyTest.sayHello");
		methodList.add("monitor.agent.MyTest.sayHello2");
		// methodList.add("com.alibaba.druid.util.JdbcUtils.executeQuery");
		// methodList.add("com.alibaba.druid.util.JdbcUtils.executeUpdate");
		methodList.add("org.apache.ibatis.executor.BaseExecutor.query");
		methodList.add("org.apache.ibatis.executor.CachingExecutor.query");
		methodList.add("org.apache.ibatis.session.defaults.DefaultSqlSession.selectList");
		methodList.add("com.neo.mapper.MybatisInterceptor.showSql");
	}

	/**
	 *
	 * <p>
	 * 获取方法参数名称
	 * </p>
	 *
	 * @param cm
	 * @return
	 */
	protected static String[] getMethodParamNames(CtMethod cm) {
		CtClass cc = cm.getDeclaringClass();
		MethodInfo methodInfo = cm.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
		if (attr == null) {
			System.err.println("attr is null");
		}

		HashMap<Integer, String> result = null;
		String[] paramNames = null;
		try {
			paramNames = new String[cm.getParameterTypes().length];
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		TreeMap<Integer, String> sortMap = new TreeMap<Integer, String>();
		for (int i = 0; i < attr.tableLength(); i++)
		    sortMap.put(attr.index(i), attr.variableName(i));
		int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
		paramNames = Arrays.copyOfRange(sortMap.values().toArray(new String[0]), pos, paramNames.length + pos);
		/*for(Entry<Integer, String> entry:sortMap.entrySet()){
            System.out.println("方法入参的slot是"+entry.getKey()+"方法的入参是 :"+entry.getValue());
       }*/

		/*int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
		for (int i = 0; i < paramNames.length; i++) {
			System.out.println(i);
			paramNames[i] = attr.variableName(i + pos);
			System.out.println("方法的入参是 :"+paramNames[i]);
		}*/
		for (String var : paramNames) {
//			System.out.println("方法的入参是 :"+var);
		}
		return paramNames;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader,
	 * java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		// 先判断下现在加载的class的包路径是不是需要监控的类，通过instrumentation进来的class路径用‘/’分割
		// System.out.println("classname is "+className);
		if (className.startsWith("monitor/agent") || className.startsWith("com/neo/mapper")
				|| (className.startsWith("org/apache/ibatis") && (!className.contains("sun")))) {
			// 将‘/’替换为‘.’m比如monitor/agent/Mytest替换为monitor.agent.Mytest
			className = className.replace("/", ".");
			CtClass ctclass = null;
			try {
				// 用于取得字节码类，必须在当前的classpath中，使用全称 ,这部分是关于javassist的知识
				// http://www.bijishequ.com/detail/49694?p=
				ClassPool classPool = ClassPool.getDefault();
				classPool.insertClassPath(new ClassClassPath(this.getClass()));
				classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
				ctclass = classPool.get(className);
				// for(CtMethod var: ctclass.getMethods()){
				// System.out.println("classname is"+className+"****method is:"+var.getName());
				// System.out.println(var.getMethodInfo());
				// System.out.println(var.getParameterTypes());
				// }
				// 循环一下，看看哪些方法需要加时间监测
				for (String method : methodList) {
					if (method.startsWith(className)) {
						// System.out.println("classname*** is"+className+"****method is:"+method);
						// 获取方法名
						String methodName = method.substring(method.lastIndexOf('.') + 1, method.length());
						String outputStr = "\nSystem.out.println(\"this method " + methodName + "----$1---"
								+ " cost:\" +(endTime - startTime) +\"ms.\");";
						// 得到这方法实例
						CtMethod ctmethod = ctclass.getDeclaredMethod(methodName);
						String[] paramNames = getMethodParamNames(ctmethod);
//						ctmethod.insertAt (26, "System.out.println(\"第一个入参传参的值为:\"+$1);");
						for(int i=1;i<paramNames.length+1;i++)
							ctmethod.insertBefore(String.format("System.out.println(\"%s第一个入参传参的值为:\"+$%d);",method,i));

						/*// 新定义一个方法叫做比如sayHello$impl
						String newMethodName = methodName + "$impl";
						// 原来的方法改个名字
						ctmethod.setName(newMethodName);
						String type = ctmethod.getReturnType().getName();

						// 创建新的方法，复制原来的方法 ，名字为原来的名字
						CtMethod newMethod = CtNewMethod.copy(ctmethod, methodName, ctclass, null);
						System.out.println(newMethod.getMethodInfo().toString());
						// 构建新的方法体
						StringBuilder bodyStr = new StringBuilder();
						bodyStr.append("{");
						bodyStr.append(prefix);
						// 调用原有代码，类似于method();($$)表示所有的参数
						if (!"void".equals(type)) {
							bodyStr.append(type).append(" result = ");
						}
						bodyStr.append(newMethodName + "($$);\n");

						bodyStr.append("System.out.println(\"*****\"+result);");
						bodyStr.append(postfix);
						bodyStr.append(outputStr);
						if (!"void".equals(type)) {
							bodyStr.append("return result;\n");
						}
						bodyStr.append("}");
						System.out.println(bodyStr.toString());
						// 替换新方法
						newMethod.setBody(bodyStr.toString());
						// 增加新方法
						ctclass.addMethod(newMethod);*/
					}
				}
				return ctclass.toBytecode();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CannotCompileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

}
