package com.lzy.javaagent;

import java.lang.instrument.Instrumentation;

public class MyAgent {

	 public static void premain(String agentArgs, Instrumentation inst){
	        System.out.println("premain-1."+agentArgs);
	        inst.addTransformer(new MonitorTransformer());
	    }
}
