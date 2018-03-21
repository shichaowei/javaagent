package monitor.agent;


import java.lang.instrument.Instrumentation;

/**
 * TODO Comment of MyAgent
 * @author yongkang.qiyk
 *
 */
public class MyAgent {

    public static void premain(String agentArgs, Instrumentation inst){
        System.out.println("premain-1."+agentArgs);
//        inst.addTransformer(new MybatisTransformer(agentArgs));
//        inst.addTransformer(new SleepTransformer(agentArgs));
        inst.addTransformer(new MonitorTransformer(agentArgs));


    }

}
