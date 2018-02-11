package com.lzy.javaagent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class MonitorTransformer implements ClassFileTransformer {

    final static String prefix ="\nlong startTime = System.currentTimeMillis();\n";
    final static String postfix ="\nlong endTime = System.currentTimeMillis();\n";
    final static List<String>methodList =new ArrayList<String>();
    static{
        methodList.add("monitor.agent.TestJVM.say");
//        methodList.add("monitor.agent.MyTest.sayHello2");
    }

    /* (non-Javadoc)
     * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,byte[] classfileBuffer)
            throws IllegalClassFormatException {
        //先判断下现在加载的class的包路径是不是需要监控的类，通过instrumentation进来的class路径用‘/’分割
        if(className.startsWith("monitor/agent")){
            //将‘/’替换为‘.’m比如monitor/agent/Mytest替换为monitor.agent.Mytest
            className = className.replace("/",".");
            CtClass ctclass = null;
            try {
                //用于取得字节码类，必须在当前的classpath中，使用全称 ,这部分是关于javassist的知识
                ctclass = ClassPool.getDefault().get(className);
            //循环一下，看看哪些方法需要加时间监测
            for(String method :methodList){
                if (method.startsWith(className)){
                         //获取方法名
                        String methodName = method.substring(method.lastIndexOf('.')+1, method.length());
                        String outputStr ="\nSystem.out.println(\"this method "+methodName+" cost:\" +(endTime - startTime) +\"ms.\");";
                        //得到这方法实例
                        CtMethod ctmethod = ctclass.getDeclaredMethod(methodName);
                        //新定义一个方法叫做比如sayHello$impl
                        String newMethodName = methodName +"$impl";
                     //原来的方法改个名字
                        ctmethod.setName(newMethodName);

                      //创建新的方法，复制原来的方法 ，名字为原来的名字
                        CtMethod newMethod = CtNewMethod.copy(ctmethod, methodName, ctclass,null);
                        //构建新的方法体
                        StringBuilder bodyStr =new StringBuilder();
                        bodyStr.append("{");
                        bodyStr.append(prefix);
                        //调用原有代码，类似于method();($$)表示所有的参数
                        bodyStr.append(newMethodName +"($$);\n");

                        bodyStr.append(postfix);
                        bodyStr.append(outputStr);

                        bodyStr.append("}");
                        //替换新方法
                        newMethod.setBody(bodyStr.toString());
                        //增加新方法
                        ctclass.addMethod(newMethod);
                }
            }
                return ctclass.toBytecode();
            } catch (IOException e) {
                //TODO Auto-generated catch block
                e.printStackTrace();
            } catch (CannotCompileException e) {
                //TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NotFoundException e) {
                //TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

}
