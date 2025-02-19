package com.nkt.proj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.xmlpull.v1.XmlPullParserException;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IdentityRef;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;

//dump the call graph from FlowDroid
public class ActivityMap {

	static List applicationClasses;

	static boolean isDescendantOf(SootClass child, SootClass cls) {
		boolean ret = false;
		while (true) {
			if ("java.lang.Object".equals(child.toString()))
				break;

			if (child.getSuperclass().toString().equals(cls.toString()) || child.toString().equals(cls.toString())) {
				ret = true;
				break;
			}
			child = child.getSuperclass();

		}
		return ret;
	}

	public static void main(String[] args) throws XmlPullParserException {
		SetupApplication app = new SetupApplication(args[1], args[0]);
		applicationClasses = new ArrayList<>();

		final long beforeRun = System.nanoTime();
		// app.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
		app.constructCallgraph();

		for (SootClass cls : Scene.v().getApplicationClasses()) {

			if (cls.declaresMethodByName("onCreate") || cls.declaresMethodByName("onStart")
					|| cls.declaresMethodByName("onStop")) {
				applicationClasses.add(cls);
				System.err.println("-->" + cls.toString());
				System.err.println(isDescendantOf(cls, cls));
			}
		}

		CallGraph appCallGraph = Scene.v().getCallGraph();
		System.out.println("" + appCallGraph.size());

		System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");

		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_dir(Collections.singletonList(args[0]));
		Options.v().set_android_jars(args[1]);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().setPhaseOption("cg.spark", "on");

		Scene.v().loadNecessaryClasses();

		SootMethod entryPoint = app.getDummyMainMethod();
		Options.v().set_main_class(entryPoint.getSignature());
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		// System.out.println(entryPoint.getActiveBody());
		
		printCG(appCallGraph);
	}
	
	
	public static HashSet<Value> getConstants(Value v, Stmt s, Body b)
	{
		HashSet<Value> res = new HashSet();
		
		if(v instanceof Local)
		{
			List<Local> params = b.getParameterLocals();

		LocalDefs lds = LocalDefs.Factory.newLocalDefs(b);
		List<Unit> l = lds.getDefsOfAt((Local) v,s);
		
		for( int i = 0; i< l.get(l.size() - 1).getUseBoxes().size(); i++)
			res.addAll(getConstants(l.get(l.size() - 1).getUseBoxes().get(i).getValue(), (Stmt) l.get(l.size() - 1), b));
		}
		else
		{	
			System.out.println(v.getClass());
			res.add(v);
		}
		
		return res;
	}

	public static void printCG(CallGraph cg) {
        Iterator<Edge> edgeItr = cg.iterator();
        List<String> edgeList = new ArrayList<>();

        while(edgeItr.hasNext()){
            Edge edge = edgeItr.next();

            SootMethod srcMethod = edge.getSrc().method();
            String srcMethodDeclaration = srcMethod.getDeclaringClass().toString() + "." +
                    srcMethod.getName() +
                    srcMethod.getParameterTypes().toString()
                            .replace('[', '(').replace(']', ')');

            SootMethod tgtMethod = edge.getTgt().method();
            String tgtMethodDeclaration = tgtMethod.getDeclaringClass().toString() + "." +
                    tgtMethod.getName() +
                    tgtMethod.getParameterTypes().toString()
                            .replace('[', '(').replace(']', ')');
            
            
            //System.out.println(tgtMethod.getActiveBody().toString());
            if(tgtMethodDeclaration.contains("startActivit"))
            {
            	
            	SootClass cls = srcMethod.getDeclaringClass();
            	if(srcMethod.getDeclaringClass().isInnerClass())
            		cls = cls.getOuterClass();

            
            JimpleBody body = (JimpleBody) srcMethod.retrieveActiveBody();
            Chain units = body.getUnits();
			//important to use snapshotIterator here
			for(Iterator<Unit> stmts = units.snapshotIterator(); stmts.hasNext();) {
			     Stmt stmt = (Stmt)stmts.next();

			     if(stmt.containsInvokeExpr() && stmt.getInvokeExpr().getMethodRef().name().contains("startActi"))
			     {
			    	 			    	 
			    	 String str = stmt.getInvokeExpr().getArg(0).toString();
			    	 

							if(stmt.getInvokeExpr().getArg(0).getType().toString().contains("Intent"))
							 {	
								
								if(stmt.getInvokeExpr().getArg(0) instanceof Local)
								{
																	
								LocalDefs lds = LocalDefs.Factory.newLocalDefs(body);
								List<Unit> l = lds.getDefsOfAt((Local) stmt.getInvokeExpr().getArg(0),stmt);
								
								List<UnitValueBoxPair> luvbp = new ArrayList<UnitValueBoxPair>();
								for(int i = 0; i<l.size(); i++)
									luvbp.addAll(new SimpleLocalUses(body,lds).getUsesOf(l.get(i)));
									
								Stmt st;
								for(UnitValueBoxPair up: luvbp)
								{
									Stmt s = (Stmt) up.getUnit();
									if(s.containsInvokeExpr() && s.getInvokeExpr().getMethodRef().name().contains("init"))
									{
										int n = s.getInvokeExpr().getArgCount();
										
										HashSet<Value> res = getConstants(s.getInvokeExpr().getArg(n-1),s,body);
										
										Iterator<Value> itr = res.iterator();
										for(; itr.hasNext();)
										{
											Value v = itr.next();
											
											if(v instanceof IdentityRef)
											{
												System.out.println(v.toString() + "is a parameter");
											}
										}
										
											str = res.toString();
										}
									}
								}
							
							 }
							

			           // edgeList.add(cls.toString() + " -----> " + stmt.getInvokeExpr().getArg(1));
							System.err.println(cls.toString() + " -----> " + str);
			     }
            
			}
           }
        
        //System.out.println(applicationCallGraph.size());
        for (String edgeStr : edgeList){
        		
     //       System.err.println(edgeStr);
        }
    }

	}
}

