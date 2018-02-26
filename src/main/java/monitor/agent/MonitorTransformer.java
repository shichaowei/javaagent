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
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class MonitorTransformer implements ClassFileTransformer {

	final static String prefix = "\nlong startTime = System.currentTimeMillis();\n";
	final static String postfix = "\nlong endTime = System.currentTimeMillis();\n";
	static Boolean CachingExecutorFlag=true;
	static Boolean ExecutorFlag=true;
	final static List<String> methodList = new ArrayList<String>();
	static {
//		methodList.add("monitor.agent.MyTest.sayHello");
		methodList.add("monitor.agent.MyTest.sayHello2");
//		methodList.add("monitor.agent.MyTest.sayHello3");
		// methodList.add("com.alibaba.druid.util.JdbcUtils.executeQuery");
		// methodList.add("com.alibaba.druid.util.JdbcUtils.executeUpdate");
//		methodList.add("org.apache.ibatis.executor.BaseExecutor.query");
//		methodList.add("org.apache.ibatis.executor.BaseExecutor.update");
		methodList.add("org.apache.ibatis.executor.CachingExecutor.query");
		methodList.add("org.apache.ibatis.executor.CachingExecutor.update");
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
//		 System.out.println("classname is "+className);
		if (className.startsWith("monitor/agent") || className.startsWith("com/neo/mapper")
				|| (className.startsWith("org/apache/ibatis") && (!className.contains("sun")))) {
			// 将‘/’替换为‘.’m比如monitor/agent/Mytest替换为monitor.agent.Mytest
			className = className.replace("/", ".");
			CtClass ctclass = null;
			try {
				// 用于取得字节码类，必须在当前的classpath中，使用全称 ,这 部分是关于javassist的知识
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
						classPool.importPackage("java.util.Date");
						classPool.importPackage("java.text.DateFormat");
						classPool.importPackage("org.apache.ibatis.mapping.MappedStatement");
						classPool.importPackage("org.apache.ibatis.mapping.ParameterMapping");
						classPool.importPackage("org.apache.ibatis.session.Configuration");
						classPool.importPackage("org.apache.ibatis.mapping.BoundSql");
//						 System.out.println("classname*** is"+ctclass.getName()+"****method is:"+method);
						// 获取方法名
						String methodName = method.substring(method.lastIndexOf('.') + 1, method.length());
						// 得到这方法实例
						CtMethod ctmethod = ctclass.getDeclaredMethod(methodName);
						String[] paramNames = getMethodParamNames(ctmethod);
						//增加一个方法
						if(CachingExecutorFlag &&ctclass.getName().equals("org.apache.ibatis.executor.CachingExecutor")){
							CtMethod m=CtNewMethod.make("public  String getParameterValue(Object obj) {String value = null;if (obj instanceof String) {value = \"'\" + obj.toString() + \"'\";} else if (obj instanceof java.util.Date) {java.text.DateFormat formatter = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.DEFAULT, java.text.DateFormat.DEFAULT, java.util.Locale.CHINA);value = \"'\" + formatter.format(new java.util.Date()) + \"'\";} else {if (obj != null) {value = obj.toString();} else {value = \"\";}}return value;};",
														ctclass);
							ctclass.addMethod(m);
							CtMethod m1=CtNewMethod.make(
									"public  String showSql(org.apache.ibatis.session.Configuration configuration, org.apache.ibatis.mapping.BoundSql boundSql)"
									+ " {"
									+ "Object parameterObject = boundSql.getParameterObject();"
									+ "String sql = boundSql.getSql();"
									+ "if (boundSql.getParameterMappings().size() > 0 && parameterObject != null) { "
									+ "org.apache.ibatis.type.TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();"
									+ "if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {"
//									+" System.out.println(sql);"
									+ "sql = sql.replaceFirst(\"[?]\", getParameterValue(parameterObject));"
									+ "}"
//									+ "}"
									+ "else {"
									+ "org.apache.ibatis.reflection.MetaObject metaObject = configuration.newMetaObject(parameterObject);"
									+ "for (int i=0;i<boundSql.getParameterMappings().size();i++) {"
									+ 	"org.apache.ibatis.mapping.ParameterMapping parameterMapping=(org.apache.ibatis.mapping.ParameterMapping)boundSql.getParameterMappings().get(i);"
									+ 	"String propertyName = parameterMapping.getProperty();"
									+ 	"if (metaObject.hasGetter(propertyName)) {"
									+ 		"Object obj = metaObject.getValue(propertyName);"
									+ 		"sql = sql.replaceFirst(\"[?]\", getParameterValue(obj));"
									+ 	"}"
									+ "else if (boundSql.hasAdditionalParameter(propertyName)) {"
									+ "Object obj = boundSql.getAdditionalParameter(propertyName);"
									+ "sql = sql.replaceFirst(\"[?]\", getParameterValue(obj));"
									+ "}"
									+ "}"
									+ "}"
									+ "}"
									+ "return sql;}"
									,ctclass);
							ctclass.addMethod(m1);
							CachingExecutorFlag=false;
						}
						/*if(ExecutorFlag &&ctclass.getName().equals("org.apache.ibatis.executor.BaseExecutor")){
							CtMethod m=CtNewMethod.make("public  String getParameterValue(Object obj) {String value = null;if (obj instanceof String) {value = \"'\" + obj.toString() + \"'\";} else if (obj instanceof java.util.Date) {java.text.DateFormat formatter = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.DEFAULT, java.text.DateFormat.DEFAULT, java.util.Locale.CHINA);value = \"'\" + formatter.format(new java.util.Date()) + \"'\";} else {if (obj != null) {value = obj.toString();} else {value = \"\";}}return value;};",
									ctclass);
							ctclass.addMethod(m);
							CtMethod m1=CtNewMethod.make(
									"public  String showSql(org.apache.ibatis.session.Configuration configuration, org.apache.ibatis.mapping.BoundSql boundSql)"
									+ " {"
									+ "Object parameterObject = boundSql.getParameterObject();"
									+ "String sql = boundSql.getSql();"
									+ "if (boundSql.getParameterMappings().size() > 0 && parameterObject != null) { "
									+ "org.apache.ibatis.type.TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();"
									+ "if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {"
//									+" System.out.println(sql);"
									+ "sql = sql.replaceFirst(\"[?]\", getParameterValue(parameterObject));"
									+ "}"
//									+ "}"
									+ "else {"
									+ "org.apache.ibatis.reflection.MetaObject metaObject = configuration.newMetaObject(parameterObject);"
									+ "for (int i=0;i<boundSql.getParameterMappings().size();i++ ) {"
									+ "org.apache.ibatis.mapping.ParameterMapping parameterMapping=(org.apache.ibatis.mapping.ParameterMapping)boundSql.getParameterMappings().get(i);"
									+ "String propertyName = parameterMapping.getProperty();"
									+ "if (metaObject.hasGetter(propertyName)) {"
									+ "Object obj = metaObject.getValue(propertyName);"
									+ "sql = sql.replaceFirst(\"[?]\", getParameterValue(obj));"
									+ "}"
									+ "else if (boundSql.hasAdditionalParameter(propertyName)) {"
									+ "Object obj = boundSql.getAdditionalParameter(propertyName);"
									+ "sql = sql.replaceFirst(\"[?]\", getParameterValue(obj));"
									+ "}"
									+ "}"
									+ "}"
									+ "}"
									+ "return sql;}"
									,ctclass);
							ctclass.addMethod(m1);
							ExecutorFlag=false;
						}*/

//						ctmethod.insertAt (26, "System.out.println(\"第一个入参传参的值为:\"+$1);");
						for(int i=1;i<paramNames.length+1;i++){
							if(paramNames[i-1].equals("ms")){
								String var = String.valueOf(i)+"****"+ctmethod.getName()+"****"+ctclass.getName();
								System.out.println("javaagent content:"+String.valueOf(i)+"****"+ctmethod.getName()+"****"+ctclass.getName());
								ctmethod.insertBefore(""
										+ "Object parameter = null;if ($2 !=null) { parameter = $2;}"
										+ "org.apache.ibatis.mapping.BoundSql boundSql=$1.getBoundSql(parameter);"
										+ "org.apache.ibatis.session.Configuration configuration = $1.getConfiguration();"
										+ "Object obj = boundSql.getParameterObject();"
										+ "String value=getParameterValue(obj);"
										+ "String sqltemp=showSql(configuration, boundSql);"
										+ "System.out.println(\"Interceptor is:\"+sqltemp);"
										+ "org.apache.http.impl.client.CloseableHttpClient httpCilent = org.apache.http.impl.client.HttpClients.createDefault();"
										+ "org.apache.http.client.methods.HttpGet httpGet = new org.apache.http.client.methods.HttpGet(\"http://192.168.16.18:8090/sqlprocess?sql=\"+java.net.URLEncoder.encode(sqltemp));"
										+ "httpCilent.execute(httpGet);"
//										+ "System.out.println(boundSql.getSql().toString());"
										);
								break;
								/*ctmethod.insertBefore("Object parameter = null;if ($2 !=null) { parameter = $2;}"
										+ "org.apache.ibatis.mapping.BoundSql boundSql=$1.getBoundSql(parameter);"
//										+ "org.apache.ibatis.session.Configuration configuration=$1.getConfiguration();"
										+ "Object parameterObject=boundSql.getParameterObject();"
										+ "List<org.apache.ibatis.mapping.ParameterMapping>parameterMappings=boundSql.getParameterMappings();"
										+ "String sql=boundSql.getSql().replaceAll(\"[\\s]+\",\" \");"
										+ "if(parameterMappings.size()>0&&parameterObject!=null)"
										+ "{TypeHandlerRegistry typeHandlerRegistry=configuration.getTypeHandlerRegistry();if(typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())){sql=sql.replaceFirst(\"\\?\",getParameterValue(parameterObject));}"
										+ "else{org.apache.ibatis.reflection.MetaObject metaObject=configuration.newMetaObject(parameterObject);for(org.apache.ibatis.mapping.ParameterMapping parameterMapping:parameterMappings){String propertyName=parameterMapping.getProperty();if(metaObject.hasGetter(propertyName)){Object obj=metaObject.getValue(propertyName);sql=sql.replaceFirst(\"\\?\",getParameterValue(obj));}else if(boundSql.hasAdditionalParameter(propertyName)){Object obj=boundSql.getAdditionalParameter(propertyName);sql=sql.replaceFirst(\"\\?\",getParameterValue(obj));System.out.println(sql);}}}}");
						*/	}
//							ctmethod.insertBefore(String.format("if($%d!=null){System.out.println(\"%s第%d个入参%s传参的值为:\"+org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString($%d));}",i,method,i,paramNames[i-1],i));
//							ctmethod.insertBefore(String.format("System.out.println(\"第%d入参参数名称为:%s\");",i,paramNames[i-1]));
//							ctmethod.insertBefore(String.format("System.out.println(\"%s第%d个入参%s传参的值为:\"+($%d).toString());",method,i,paramNames[i-1],i));
//							ctmethod.insertBefore(String.format("System.out.println(\"%s第%d个入参%s传参的值为:\");",method,i,paramNames[i-1]));
						}
						/*// 新定义一个方法叫做比如sayHello$impl
						 *
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
