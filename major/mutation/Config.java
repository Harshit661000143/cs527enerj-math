/*
 * Generated driver for Major v2.x
 */

// This package name is required by Major! 
package major.mutation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class name is required by Major!
 */
public class Config{
	public static boolean USE_INFECTION=true;
	public static boolean USE_PROPAGATION=true;
	public static boolean USE_PARTITIONING=false;

	/*
     * The mutant identifier:
     * 
     * __M_NO <  0 -> Run original version
     *
     * __M_NO == 0 -> Run original version and gather coverage information
     *
     * __M_NO >  0 -> Execute mutant with the corresponding id
     */
	public static int __M_NO = -1;
	
	// Temporary solution to avoid multiple evaluations of expressions with side effects
	// TODO: Use local, temporary variables
	public static int __TMP_INDEX;
 
    // Initial size for HashMaps
	public static final int INIT_CAPACITY=131072;
	
	// Set of all covered mutants (weakly killed + weakly live)
	public static Set<Integer> allCoveredMutants = new HashSet<Integer>(INIT_CAPACITY);

	// Set of all strongly killable mutants (weakly killed)
    public static Set<Integer> allKillableMutants = new HashSet<Integer>(INIT_CAPACITY);
    
    // Set of all already killed mutants (exception during evaluation)
    public static Set<Integer> allKilledMutants = new TreeSet<Integer>();
    
	// The mutation mapping: mutantId -> opcode
    public static java.util.Map<Integer,Integer> mapMutId2Opcode = new HashMap<Integer, Integer>(INIT_CAPACITY);
    
	// Mapping from expression Id to Array of mutation Ids for this expression
    public static java.util.Map<Integer,Integer[]> mapExprId2Mutants = new HashMap<Integer, Integer[]>(INIT_CAPACITY);
    
	// Mapping from expression Id to corresponding expression object
    private static Map<Integer, Expression> mapExprId2Expression = new HashMap<Integer, Expression>(INIT_CAPACITY);

    // Groups of mutants leading to equivalent results                 
    private static Map<Integer, Set<Integer>> killableGroups = new HashMap<Integer, Set<Integer>>();
        
/***************************************************************************************
 *                                                                  
 * Data structures for storing mutants and expression nodes
 *            
 **************************************************************************************/
    	/*
    	 * Mutant representation, which stores:
    	 *  - Id
    	 *  - opcode
    	 *  - value
    	 */
    	private static final class Mutant implements Comparable<Mutant>{
        	final int mutId;
        	final int op;
        	Object value;
        	
        	public Mutant(int mutId, int op){
        		this.mutId=mutId;
        		this.op=op;
        	}
        	
        	public double getValue(double orig){
        		return ((Number)value).doubleValue(); 	
        	}    	
        	public float getValue(float orig){
        		return ((Number)value).floatValue(); 	
        	}
        	public long getValue(long orig){
        		return ((Number)value).longValue(); 	
        	}
        	public int getValue(int orig){
        		return ((Number)value).intValue(); 	
        	}
        	public short getValue(short orig){
        		return ((Number)value).shortValue(); 	
        	}    	
        	public byte getValue(byte orig){
        		return ((Number)value).byteValue(); 	
        	}
        	public char getValue(char orig){
        		return ((Character)value).charValue(); 	
        	}    	    	
        	public boolean getValue(boolean orig){
        		return ((Boolean)value).booleanValue(); 	
        	}    	
        	public Object getValue(Object orig){
        		return value; 	
        	}
        	public String getValue(String orig){
        		return (String)value; 	
        	}
        	    	
        	public void setValue(Object value){
        		this.value=value;
        	}
        	
        	@Override
        	public String toString() {
        		return "Mutant["+mutId+": "+op+", "+value+"]";
        	}
        	    	
        	@Override
        	public int compareTo(Mutant m){
        		return mutId - m.mutId;
        	}
        }
        
    	/*
    	 * Representation of an expression node with:
    	 * - unique expression ID
    	 * - a list of subexpressions
    	 * - parent node reference
    	 * - set of all generated mutants
    	 * - set of live mutants
    	 * - set of killable mutants (temporary)
    	 */
    	private static final class Expression{
            final int exprId;
            Expression parent=null;
            List<Expression> subExpressions = null;
            // Set of all generated mutants for this expression
            Set<Mutant> allMutants;
            // Set of all live mutants for this expression
            Set<Mutant> liveMutants;
            // Set of killable mutants for this expression (for a certain execution)
            Set<Mutant> killableMutants;

            Expression(int exprId, List<Expression> subExprs){
                this.exprId=exprId;
                this.subExpressions=subExprs;
                // init set of all mutants (if any)
                if(mapExprId2Mutants.containsKey(exprId)){
                	allMutants = new TreeSet<Mutant>();
                	for(int mutId : mapExprId2Mutants.get(exprId)){
                		allMutants.add(new Mutant(mutId, mapMutId2Opcode.get(mutId)));
                	}
                	liveMutants = new TreeSet<Mutant>(allMutants);
                }
                for(Expression child : subExprs){
                	child.parent=this;
                }
                // Always init the killable set since it is used to propagate mutants
                killableMutants = new TreeSet<Mutant>();
            }
            
            /*
             * Recursively clear the set of killable mutants for this tree
             * and update set of live mutants
             */
            void resetMutants(){
            	killableMutants.clear();
            	if(liveMutants!=null){
            		Iterator<Mutant> iter = liveMutants.iterator();
            		while(iter.hasNext()){
            			int mutId = iter.next().mutId;
            			if(allKillableMutants.contains(mutId) || allKilledMutants.contains(mutId)){
            				iter.remove();
            			}
            		}
            	}
            	for(Expression child : subExpressions){
            		child.resetMutants();
            	}
            }
            
            /*
             * Reset live mutants to all generated mutants
             */
            void resetLiveMutants(){
            	if(allMutants!= null){
            		liveMutants=new TreeSet<Mutant>(allMutants);
            	}
            }
            
            @Override
            public String toString(){
            	StringBuffer buf = new StringBuffer(64);
            	buf.append("Expression[").append(exprId).append(":");
            	for(Expression sub : subExpressions){
            		buf.append(" ").append(sub.exprId);
            	}
            	buf.append("]");
            	return buf.toString();
            }        
       }  
    	
/***************************************************************************************
 *                                                                  
 * Methods for the mutation analysis back-end
 *            
 **************************************************************************************/
	/**
	 * Initialize mappings and expression trees
	 * 
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	public static boolean initMappings(File fMutMap, File fExprMap){
        try{
            BufferedReader br = new BufferedReader(new FileReader(fMutMap));
            String str;
            int prevExprId=-1;
            List<Integer> tmp = new ArrayList<Integer>(8);
            while((str=br.readLine())!=null){
            	// exclude comments
                if(str.matches("#.*")) continue;
                if(str.matches("\\s*")) continue;
                String values[]=str.split(":");
                int exprId = Integer.parseInt(values[0]);
                int mutId  = Integer.parseInt(values[1]);
                int mutOp  = Integer.parseInt(values[2]);
                mapMutId2Opcode.put(mutId, mutOp);
                if(prevExprId!=exprId){
                	// if this is not the first entry
                	if(prevExprId!=-1){
                		mapExprId2Mutants.put(prevExprId, tmp.toArray(new Integer[0]));
		        		tmp.clear();
		        	}
		        	prevExprId=exprId;
                }
            	tmp.add(mutId);
            }
            br.close();
            // add last array of mutants to exprMap
            mapExprId2Mutants.put(prevExprId, tmp.toArray(new Integer[0]));            
            System.out.println("MAJOR: "+mapMutId2Opcode.size()+" mutants mapped");
            
            br = new BufferedReader(new FileReader(fExprMap));
            while((str=br.readLine())!=null){
                /// exclude comments
                if(str.matches("#.*")) continue;
                if(str.matches("\\s*")) continue;
                String values[]=str.split(":");
                assert(values.length>=1);
                int exprId = Integer.parseInt(values[0]);
                
                // read IDs of subexpressions and add them to a list (we have to preserve the order)
            	List<Expression> subs = new ArrayList<Expression>(values.length-1);
            	for(int i=1; i<values.length; ++i){
            		subs.add(mapExprId2Expression.get(Integer.parseInt(values[i])));
            	}
            	// build current expression and add it to map of all expressions
            	Expression expr = new Expression(exprId, subs);
            	mapExprId2Expression.put(exprId, expr);                
            }
            br.close();
            System.out.println("MAJOR: "+mapExprId2Expression.size()+" expressions mapped");
            
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
	                                                                 
    /**
     * Reset the coverage and killable information
     */
    public static void reset(){                                
        allCoveredMutants.clear();                              
        allKillableMutants.clear();
        allKilledMutants.clear();
        killableGroups.clear();
        for(Expression expr : mapExprId2Expression.values()){
        	expr.resetLiveMutants();
        }
    }                                                                  
    
    /**
     * Get number of generated mutants
     */
    public static int getNumberGenerated(){   
    	return mapMutId2Opcode.size();    	                                                    
    }
    
    /**
     * Get list of all covered mutants
     */
    public static List<Integer> getCoverageList(){   
    	return new ArrayList<Integer>(allCoveredMutants);    	                                                    
    }
    
    /**
     * Get list of all killable mutants (weakly killed)
     */
    public static List<Integer> getKillableList(){   
    	return new ArrayList<Integer>(allKillableMutants);    	
    }
    
    /**                                                                
     * Get a map of groups of killable mutants                         
     *                                                                 
     */                                                                
    public static Map<Integer, Set<Integer>> getGroupedKillableMap() { 
        Map<Integer, Set<Integer>> groups = new LinkedHashMap<Integer, Set<Integer>>();
        Set<Integer> coveredMutants = new HashSet<Integer>();          
        for (Integer mutantId : killableGroups.keySet()) {             
            if (coveredMutants.contains(mutantId))                     
                continue;                                              
                                                                       
            Set<Integer> group = killableGroups.get(mutantId);         
            groups.put(mutantId, group);                               
            coveredMutants.addAll(group);                              
        }
        // add all remaining killable mutants as independent groups
        for(Integer mutId : allKillableMutants){
        	if(!coveredMutants.contains(mutId)){
        		groups.put(mutId, Collections.singleton(mutId));
        	}
        }
        return groups;                                                 
    }
    
    /**
     * Get list of all killed mutants (due to an exception)
     */
    public static List<Integer> getKilledList(){   
    	return new ArrayList<Integer>(allKilledMutants);    	
    }
    	  
/***************************************************************************************
 *                                                                  
 * Instrumentation methods called by mutated class files
 *            
 **************************************************************************************/
    /**
     * Coverage method for major version 1.x
     */
	public static boolean COVERED(int from, int to){
		for(int i=from; i<=to; ++i){
			allCoveredMutants.add(i);
		}
		// as required by conditional mutation
		return false;
	}		

	/**
	 *  Coverage methods for major version 2.x
	 */
	private static void coverExpression(int exprId){
		// check whether expression has mutants
		if(!mapExprId2Mutants.containsKey(exprId)) return;
		
	    for(int mutId : mapExprId2Mutants.get(exprId)){
	    	// the expression is covered but the mutants are not necessarily killable
	    	allCoveredMutants.add(mutId);
	    }
	}
	public static void coverMutant(int mutId){
		allCoveredMutants.add(mutId);
		// method called for STD operator -> always regard these mutants as killable
		if(!USE_INFECTION) return;
		allKillableMutants.add(mutId);
	}
	
	/**                                                                
     * Partition a set of mutants into groups based on the result      
     */                                                                
    private static Collection<Set<Integer>> getGroupsFromMutantResults(Set<Mutant> mutants) {
        Map<Object, Set<Integer>> resultGroups = new HashMap<Object, Set<Integer>>();
        for (Mutant mut : mutants) {                                   
            Object result = mut.value;                                 
            if (!resultGroups.containsKey(result))                     
                resultGroups.put(result, new HashSet<Integer>());      
                                                                       
            resultGroups.get(result).add(mut.mutId);                   
        }                                                              
                                                                       
        return resultGroups.values();                                  
    }                                                                  
                                                                       
    /**                                                                
     * Initialize mapping with the first grouping we found             
     */                                                                
    private static void initKillableGroups(Collection<Set<Integer>> newGroups) {
        for (Set<Integer> group : newGroups) {                         
            for (Integer mutantId : group) {                           
                killableGroups.put(mutantId, new HashSet<Integer>(group));
            }                                                          
        }                                                              
    } 
    
    /**                                                                
     * Merge a new partitioning with the existing partitioning, splitting groups
     * as necessary                                                    
     */                                                                
    private static void mergeKillableGroups(Collection<Set<Integer>> newGroups) {
    	for (Integer mutantId : killableGroups.keySet().toArray(new Integer[0])) {             
            for (Set<Integer> group : newGroups) {                     
                if (group.contains(mutantId)) {                        
                    killableGroups.get(mutantId).retainAll(group);     
                }else{
                	// TODO: We need to tune the performance here!
                	initKillableGroups(newGroups);
                }
            }                                                          
        }                                                            
    }                                                                  
                                                                       
    /**                                                                
     * Analyze a set of mutant results, and current update partitioning
     */                                                                
    public static void updateKillableGroups(Set<Mutant> mutants) {   
    	if(!USE_PARTITIONING) return;  
        Collection<Set<Integer>> groups = getGroupsFromMutantResults(mutants);
        if (killableGroups.isEmpty())                                  
            initKillableGroups(groups);                                
        else                                                           
            mergeKillableGroups(groups);                               
    } 

    /**
     * This method is called for each toplevel expression.
     * It stores all propagated killable mutants in the
     * corresponding set and calls the resetMutants method 
     * on this toplevel node.
     * 
     */
	private static void handleToplevel(int exprId){
		Expression toplevel = mapExprId2Expression.get(exprId);
		for(Mutant mut : toplevel.killableMutants){
			allKillableMutants.add(mut.mutId);
		}
		if(!USE_PARTITIONING) {
			toplevel.resetMutants();
			return;
		}
		if(toplevel.killableMutants.size() > 0){
			updateKillableGroups(toplevel.killableMutants);
		}
		toplevel.resetMutants();
    }
	
	/**
     * This method is called for each expression that 
     * cannot further be propagated (e.g., method calls).
     * It stops the propagation of killable mutants for
     * every subexpression of this node and calls the
     * resetMutants method for this node.
     * 
     * TODO: Should we just call handleToplevel for each subexpression?
     *       If not check for redundant mutants here
     */
	private static void handleCall(int exprId){
		Expression expr = mapExprId2Expression.get(exprId);
        // Check for mutants of child expressions
		for(Expression subExpr : expr.subExpressions){
			if(subExpr!=null){
				for(Mutant mut : subExpr.killableMutants){
					// conservative: 
					// add all killable mutants of sub expression to the 
					// result set of killable mutants and stop propagation
					allKillableMutants.add(mut.mutId);
		        }
		        if(!USE_PARTITIONING) {
					continue;
				}
				if(subExpr.killableMutants.size() > 0){
					updateKillableGroups(subExpr.killableMutants);
				}
			}
        }
		expr.resetMutants();
		
		// check whether the expression itself has mutants
 		if(!mapExprId2Mutants.containsKey(exprId)) return;
		for(int mutId : mapExprId2Mutants.get(exprId)){
			// conservative:
			// regard all mutants as being killable, e.g., due to side effects
			coverMutant(mutId);
		}
	}


// All toplevel methods -> called for outermost expressions
    public static boolean TOPLEVEL_EXPR_Z(int exprId, boolean arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static byte TOPLEVEL_EXPR_B(int exprId, byte arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static char TOPLEVEL_EXPR_C(int exprId, char arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static double TOPLEVEL_EXPR_D(int exprId, double arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static float TOPLEVEL_EXPR_F(int exprId, float arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static int TOPLEVEL_EXPR_I(int exprId, int arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static java.lang.Object TOPLEVEL_EXPR_L(int exprId, java.lang.Object arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static java.lang.String TOPLEVEL_EXPR_L(int exprId, java.lang.String arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static long TOPLEVEL_EXPR_J(int exprId, long arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }
    public static short TOPLEVEL_EXPR_S(int exprId, short arg){
    	if(USE_PROPAGATION){;
			handleToplevel(exprId);
		}		
		return arg;
    }

// All call methods -> called for expressions with "unpropagateable" subexpressions, e.g., method calls
    public static boolean CALL_Z(int exprId, boolean arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static byte CALL_B(int exprId, byte arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static char CALL_C(int exprId, char arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static double CALL_D(int exprId, double arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static float CALL_F(int exprId, float arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static int CALL_I(int exprId, int arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static java.lang.Object CALL_L(int exprId, java.lang.Object arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static java.lang.String CALL_L(int exprId, java.lang.String arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static long CALL_J(int exprId, long arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }
    public static short CALL_S(int exprId, short arg){
    	if(USE_PROPAGATION) {
			handleCall(exprId);
		}		
		return arg;
    }

// All array methods -> called for array access to propagate its value and check for exceptions
    public static int ARRAY_Z(int exprId, boolean[] array, int index, boolean writeAccess){
    	boolean orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	boolean mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_B(int exprId, byte[] array, int index, boolean writeAccess){
    	byte orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	byte mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_C(int exprId, char[] array, int index, boolean writeAccess){
    	char orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	char mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_D(int exprId, double[] array, int index, boolean writeAccess){
    	double orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	double mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_F(int exprId, float[] array, int index, boolean writeAccess){
    	float orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	float mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_I(int exprId, int[] array, int index, boolean writeAccess){
    	int orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	int mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_L(int exprId, java.lang.Object[] array, int index, boolean writeAccess){
    	java.lang.Object orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	java.lang.Object mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_L(int exprId, java.lang.String[] array, int index, boolean writeAccess){
    	java.lang.String orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	java.lang.String mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_J(int exprId, long[] array, int index, boolean writeAccess){
    	long orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	long mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }
    public static int ARRAY_S(int exprId, short[] array, int index, boolean writeAccess){
    	short orig = array[index];
    	
    	if(!(USE_INFECTION && USE_PROPAGATION)) return index;
        
		Expression expr = mapExprId2Expression.get(exprId);

		// array access always has one index child node
		assert(expr.subExpressions.size()==1);
		Expression indexExpr = expr.subExpressions.get(0);
		
        // Check for mutants of index child expressions
		for(Mutant m : indexExpr.killableMutants){
        	int mutValue = m.getValue(index);
        	
        	short mutResult;
        	try{
        		mutResult = array[mutValue];
        	}catch(Exception e){
        		// this mutant will be caught due to an exception 
        		// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
        		allKilledMutants.add(m.mutId);
        		continue;
        	}
        
        	// TODO: Use equals for String and Wrapper types
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from index child
        		expr.killableMutants.add(m);
        	}else if(writeAccess){
        		// same value stored at different array location
        		// conservative: mutant is killable
        		m.setValue(mutResult);	        		
        		expr.killableMutants.add(m);
        	}
        }
        // stop propagation for arrays 
		// TODO: propagate array value
        handleToplevel(exprId);
                   
        return index;
    }

// All expression methods -> called for all expressions to identify covered and weakly killed mutants
    public static boolean EXPR_JJ_Z(int exprId, int op, long lhs, long rhs){
        coverExpression(exprId);

        boolean orig = EVAL_JJ_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_JJ_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	long mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	boolean mutResult = EVAL_JJ_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	long mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_JJ_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_SE_JJ_Z(int exprId, int op, long lhs, long rhs){
        coverExpression(exprId);

        boolean orig = EVAL_JJ_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_JJ_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	long mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	boolean mutResult = EVAL_JJ_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	long mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_JJ_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_Z_Z(int exprId, int op, boolean arg){
        coverExpression(exprId);

        boolean orig = EVAL_Z_Z(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	boolean mutResult = EVAL_Z_Z(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	boolean mutValue = m.getValue(arg);
        	boolean mutResult = EVAL_Z_Z(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static double EXPR_D_D(int exprId, int op, double arg){
        coverExpression(exprId);

        double orig = EVAL_D_D(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	double mutResult = EVAL_D_D(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	double mutValue = m.getValue(arg);
        	double mutResult = EVAL_D_D(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static int EXPR_IJ_I(int exprId, int op, int lhs, long rhs){
        coverExpression(exprId);

        int orig = EVAL_IJ_I(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	int mutResult;
	        	try{
	        		mutResult = EVAL_IJ_I(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	int mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	int mutResult = EVAL_IJ_I(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	long mutValue = m.getValue(rhs);
        	int mutResult;
        	try{
        		mutResult = EVAL_IJ_I(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static int EXPR_SE_IJ_I(int exprId, int op, int lhs, long rhs){
        coverExpression(exprId);

        int orig = EVAL_IJ_I(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	int mutResult;
	        	try{
	        		mutResult = EVAL_IJ_I(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	int mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	int mutResult = EVAL_IJ_I(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	long mutValue = m.getValue(rhs);
        	int mutResult;
        	try{
        		mutResult = EVAL_IJ_I(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static double EXPR_DD_D(int exprId, int op, double lhs, double rhs){
        coverExpression(exprId);

        double orig = EVAL_DD_D(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	double mutResult;
	        	try{
	        		mutResult = EVAL_DD_D(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	double mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	double mutResult = EVAL_DD_D(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	double mutValue = m.getValue(rhs);
        	double mutResult;
        	try{
        		mutResult = EVAL_DD_D(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static double EXPR_SE_DD_D(int exprId, int op, double lhs, double rhs){
        coverExpression(exprId);

        double orig = EVAL_DD_D(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	double mutResult;
	        	try{
	        		mutResult = EVAL_DD_D(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	double mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	double mutResult = EVAL_DD_D(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	double mutValue = m.getValue(rhs);
        	double mutResult;
        	try{
        		mutResult = EVAL_DD_D(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static long EXPR_JJ_J(int exprId, int op, long lhs, long rhs){
        coverExpression(exprId);

        long orig = EVAL_JJ_J(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	long mutResult;
	        	try{
	        		mutResult = EVAL_JJ_J(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	long mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	long mutResult = EVAL_JJ_J(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	long mutValue = m.getValue(rhs);
        	long mutResult;
        	try{
        		mutResult = EVAL_JJ_J(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static long EXPR_SE_JJ_J(int exprId, int op, long lhs, long rhs){
        coverExpression(exprId);

        long orig = EVAL_JJ_J(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	long mutResult;
	        	try{
	        		mutResult = EVAL_JJ_J(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	long mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	long mutResult = EVAL_JJ_J(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	long mutValue = m.getValue(rhs);
        	long mutResult;
        	try{
        		mutResult = EVAL_JJ_J(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static float EXPR_F_F(int exprId, int op, float arg){
        coverExpression(exprId);

        float orig = EVAL_F_F(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	float mutResult = EVAL_F_F(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	float mutValue = m.getValue(arg);
        	float mutResult = EVAL_F_F(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static long EXPR_J_J(int exprId, int op, long arg){
        coverExpression(exprId);

        long orig = EVAL_J_J(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	long mutResult = EVAL_J_J(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	long mutValue = m.getValue(arg);
        	long mutResult = EVAL_J_J(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_DD_Z(int exprId, int op, double lhs, double rhs){
        coverExpression(exprId);

        boolean orig = EVAL_DD_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_DD_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	double mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	boolean mutResult = EVAL_DD_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	double mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_DD_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_SE_DD_Z(int exprId, int op, double lhs, double rhs){
        coverExpression(exprId);

        boolean orig = EVAL_DD_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_DD_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	double mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	boolean mutResult = EVAL_DD_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	double mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_DD_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_FF_Z(int exprId, int op, float lhs, float rhs){
        coverExpression(exprId);

        boolean orig = EVAL_FF_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_FF_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	float mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	boolean mutResult = EVAL_FF_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	float mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_FF_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_SE_FF_Z(int exprId, int op, float lhs, float rhs){
        coverExpression(exprId);

        boolean orig = EVAL_FF_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_FF_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	float mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	boolean mutResult = EVAL_FF_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	float mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_FF_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_II_Z(int exprId, int op, int lhs, int rhs){
        coverExpression(exprId);

        boolean orig = EVAL_II_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_II_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	int mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	boolean mutResult = EVAL_II_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	int mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_II_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_SE_II_Z(int exprId, int op, int lhs, int rhs){
        coverExpression(exprId);

        boolean orig = EVAL_II_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_II_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	int mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	boolean mutResult = EVAL_II_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	int mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_II_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static long EXPR_JI_J(int exprId, int op, long lhs, int rhs){
        coverExpression(exprId);

        long orig = EVAL_JI_J(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	long mutResult;
	        	try{
	        		mutResult = EVAL_JI_J(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	long mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	long mutResult = EVAL_JI_J(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	int mutValue = m.getValue(rhs);
        	long mutResult;
        	try{
        		mutResult = EVAL_JI_J(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static long EXPR_SE_JI_J(int exprId, int op, long lhs, int rhs){
        coverExpression(exprId);

        long orig = EVAL_JI_J(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	long mutResult;
	        	try{
	        		mutResult = EVAL_JI_J(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	long mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	long mutResult = EVAL_JI_J(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	int mutValue = m.getValue(rhs);
        	long mutResult;
        	try{
        		mutResult = EVAL_JI_J(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static float EXPR_FF_F(int exprId, int op, float lhs, float rhs){
        coverExpression(exprId);

        float orig = EVAL_FF_F(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	float mutResult;
	        	try{
	        		mutResult = EVAL_FF_F(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	float mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	float mutResult = EVAL_FF_F(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	float mutValue = m.getValue(rhs);
        	float mutResult;
        	try{
        		mutResult = EVAL_FF_F(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static float EXPR_SE_FF_F(int exprId, int op, float lhs, float rhs){
        coverExpression(exprId);

        float orig = EVAL_FF_F(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	float mutResult;
	        	try{
	        		mutResult = EVAL_FF_F(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	float mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	float mutResult = EVAL_FF_F(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	float mutValue = m.getValue(rhs);
        	float mutResult;
        	try{
        		mutResult = EVAL_FF_F(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static char EXPR_C_C(int exprId, int op, char arg){
        coverExpression(exprId);

        char orig = EVAL_C_C(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	char mutResult = EVAL_C_C(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	char mutValue = m.getValue(arg);
        	char mutResult = EVAL_C_C(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static short EXPR_S_S(int exprId, int op, short arg){
        coverExpression(exprId);

        short orig = EVAL_S_S(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	short mutResult = EVAL_S_S(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	short mutValue = m.getValue(arg);
        	short mutResult = EVAL_S_S(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static int EXPR_I_I(int exprId, int op, int arg){
        coverExpression(exprId);

        int orig = EVAL_I_I(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	int mutResult = EVAL_I_I(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	int mutValue = m.getValue(arg);
        	int mutResult = EVAL_I_I(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_LL_Z(int exprId, int op, java.lang.Object lhs, java.lang.Object rhs){
        coverExpression(exprId);

        boolean orig = EVAL_LL_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_LL_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	java.lang.Object mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	boolean mutResult = EVAL_LL_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	java.lang.Object mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_LL_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_SE_LL_Z(int exprId, int op, java.lang.Object lhs, java.lang.Object rhs){
        coverExpression(exprId);

        boolean orig = EVAL_LL_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_LL_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	java.lang.Object mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	boolean mutResult = EVAL_LL_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	java.lang.Object mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_LL_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static int EXPR_II_I(int exprId, int op, int lhs, int rhs){
        coverExpression(exprId);

        int orig = EVAL_II_I(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	int mutResult;
	        	try{
	        		mutResult = EVAL_II_I(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	int mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	int mutResult = EVAL_II_I(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	int mutValue = m.getValue(rhs);
        	int mutResult;
        	try{
        		mutResult = EVAL_II_I(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static int EXPR_SE_II_I(int exprId, int op, int lhs, int rhs){
        coverExpression(exprId);

        int orig = EVAL_II_I(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	int mutResult;
	        	try{
	        		mutResult = EVAL_II_I(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	int mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	int mutResult = EVAL_II_I(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	int mutValue = m.getValue(rhs);
        	int mutResult;
        	try{
        		mutResult = EVAL_II_I(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static java.lang.String EXPR_L_L(int exprId, int op, java.lang.String arg){
        coverExpression(exprId);

        java.lang.String orig = EVAL_L_L(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	java.lang.String mutResult = EVAL_L_L(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	java.lang.String mutValue = m.getValue(arg);
        	java.lang.String mutResult = EVAL_L_L(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static java.lang.Object EXPR_L_L(int exprId, int op, java.lang.Object arg){
        coverExpression(exprId);

        java.lang.Object orig = EVAL_L_L(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	java.lang.Object mutResult = EVAL_L_L(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	java.lang.Object mutValue = m.getValue(arg);
        	java.lang.Object mutResult = EVAL_L_L(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static byte EXPR_B_B(int exprId, int op, byte arg){
        coverExpression(exprId);

        byte orig = EVAL_B_B(op, arg);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	byte mutResult = EVAL_B_B(m.op, arg);
	        	if(mutResult!=orig){
	        		// Set expression value and save in killable set
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// check for mutated literals that do not have a subexpression
		if(expr.subExpressions.size()==0){
			return orig;
		}
		
		// unary operators always have one sub expression
		assert(expr.subExpressions.size()==1);
		Expression argExpr = expr.subExpressions.get(0);

        // Check for mutants of arg child expressions
        for(Mutant m : argExpr.killableMutants){
        	byte mutValue = m.getValue(arg);
        	byte mutResult = EVAL_B_B(op, mutValue);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from arg child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_ZZ_Z(int exprId, int op, boolean lhs, boolean rhs){
        coverExpression(exprId);

        boolean orig = EVAL_ZZ_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_ZZ_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// Clone mutant, set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	boolean mutValue = m.getValue(lhs);
        	// no need to check for div by zero exception
        	boolean mutResult = EVAL_ZZ_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);	        		
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
    	for(Mutant m : rhsExpr.killableMutants){
        	boolean mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_ZZ_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	    		allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }
    public static boolean EXPR_SE_ZZ_Z(int exprId, int op, boolean lhs, boolean rhs){
        coverExpression(exprId);

        boolean orig = EVAL_ZZ_Z(op, lhs, rhs);
        
        if(!USE_INFECTION) return orig;
        
		Expression expr = mapExprId2Expression.get(exprId);

		if(expr.liveMutants!=null){
	        // Add all killable mutants for this expr (if mutated)
			Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	switch(m.op){
	        		case -2: // true
	        		case -3: // false
	        		case -4: // rhs
	        		case -5: // lhs
	        			// this expression contains side effects 
	        			// -> mark these mutants killable and do not propagate
		        		allKillableMutants.add(m.mutId);
		        		iter.remove();
		        		continue;
	        	}
	        	
	        	boolean mutResult;
	        	try{
	        		mutResult = EVAL_ZZ_Z(m.op, lhs, rhs);
	        	}catch(ArithmeticException e){
	        		// this mutant will be caught due to an exception 
	        		// -> mark it killable and already killed, and do not propagate
	        		allKillableMutants.add(m.mutId);
	        		allKilledMutants.add(m.mutId);
	        		iter.remove();
	        		continue;
	        	}
	        	if(mutResult!=orig){
	        		// set expression value, and save in killable map
	        		m.setValue(mutResult);
	        		expr.killableMutants.add(m);
	        	}
	        }
		}
		
		if(!USE_PROPAGATION){
			handleToplevel(exprId);
			return orig;
		}
		
		// binary expressions always have a lhs and rhs node
		assert(expr.subExpressions.size()==2);
		Expression lhsExpr = expr.subExpressions.get(0);
		Expression rhsExpr = expr.subExpressions.get(1);

        // Check for mutants of lhs child expressions
        for(Mutant m : lhsExpr.killableMutants){
        	boolean mutValue = m.getValue(lhs);
        	// no need to check for div by zero
        	boolean mutResult = EVAL_ZZ_Z(op, mutValue, rhs);
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from lhs child
        		expr.killableMutants.add(m);
        	}
        }
        
        // Check for mutants of rhs child expressions
        for(Mutant m : rhsExpr.killableMutants){
        	boolean mutValue = m.getValue(rhs);
        	boolean mutResult;
        	try{
        		mutResult = EVAL_ZZ_Z(op, lhs, mutValue);
	        }catch(ArithmeticException e){
	    		// this mutant will be caught due to an exception 
	        	// -> mark it killable and already killed, and do not propagate
        		allKillableMutants.add(m.mutId);
	        	allKilledMutants.add(m.mutId);
	    		continue;
	    	}
        	if(mutResult!=orig){
        		m.setValue(mutResult);
        		// propagate killable mutants from rhs child
        		expr.killableMutants.add(m);
        	}
        }
                
        return orig;
    }

// All eval methods -> called to determine expression values
    public static boolean EVAL_JJ_Z(int op, long lhs, long rhs){
        switch(op){
            case 75931: return lhs < rhs;
            case 75933: return lhs > rhs;
            case 75934: return lhs <= rhs;
            case 75932: return lhs >= rhs;
            case 75929: return lhs == rhs;
            case 75930: return lhs != rhs;
            case -2: return true;
            case -3: return false;
            default: throw new RuntimeException("Unknown opcode(EVAL_JJ_Z): "+op);
        }
    }
    public static boolean EVAL_Z_Z(int op, boolean arg){
        switch(op){
            case 257: return ! arg;
            case -15: return arg;
            case -2: return true;
            case -3: return false;
            default: throw new RuntimeException("Unknown opcode(EVAL_Z_Z): "+op);
        }
    }
    public static double EVAL_D_D(int op, double arg){
        switch(op){
            case 0: return + arg;
            case 119: return - arg;
            case 99: return ++ arg;
            case 103: return -- arg;
            case -11: return -arg;
            case -12: return 1;
            case -13: return -1;
            case -14: return 0;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_D_D): "+op);
        }
    }
    public static int EVAL_IJ_I(int op, int lhs, long rhs){
        switch(op){
            case 270: return lhs << rhs;
            case 272: return lhs >> rhs;
            case 274: return lhs >>> rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_IJ_I): "+op);
        }
    }
    public static double EVAL_DD_D(int op, double lhs, double rhs){
        switch(op){
            case 99: return lhs + rhs;
            case 103: return lhs - rhs;
            case 107: return lhs * rhs;
            case 111: return lhs / rhs;
            case 115: return lhs % rhs;
            case -4: return lhs;
            case -5: return rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_DD_D): "+op);
        }
    }
    public static long EVAL_JJ_J(int op, long lhs, long rhs){
        switch(op){
            case 97: return lhs + rhs;
            case 101: return lhs - rhs;
            case 105: return lhs * rhs;
            case 109: return lhs / rhs;
            case 113: return lhs % rhs;
            case 127: return lhs & rhs;
            case 129: return lhs | rhs;
            case 131: return lhs ^ rhs;
            case 271: return lhs << rhs;
            case 273: return lhs >> rhs;
            case 275: return lhs >>> rhs;
            case -4: return lhs;
            case -5: return rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_JJ_J): "+op);
        }
    }
    public static float EVAL_F_F(int op, float arg){
        switch(op){
            case 0: return + arg;
            case 118: return - arg;
            case 98: return ++ arg;
            case 102: return -- arg;
            case -11: return -arg;
            case -12: return 1;
            case -13: return -1;
            case -14: return 0;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_F_F): "+op);
        }
    }
    public static long EVAL_J_J(int op, long arg){
        switch(op){
            case 0: return + arg;
            case 117: return - arg;
            case 131: return ~ arg;
            case 97: return ++ arg;
            case 101: return -- arg;
            case -11: return -arg;
            case -12: return 1;
            case -13: return -1;
            case -14: return 0;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_J_J): "+op);
        }
    }
    public static boolean EVAL_DD_Z(int op, double lhs, double rhs){
        switch(op){
            case 77979: return lhs < rhs;
            case 77469: return lhs > rhs;
            case 77982: return lhs <= rhs;
            case 77468: return lhs >= rhs;
            case 77465: return lhs == rhs;
            case 77466: return lhs != rhs;
            case -2: return true;
            case -3: return false;
            default: throw new RuntimeException("Unknown opcode(EVAL_DD_Z): "+op);
        }
    }
    public static boolean EVAL_FF_Z(int op, float lhs, float rhs){
        switch(op){
            case 76955: return lhs < rhs;
            case 76445: return lhs > rhs;
            case 76958: return lhs <= rhs;
            case 76444: return lhs >= rhs;
            case 76441: return lhs == rhs;
            case 76442: return lhs != rhs;
            case -2: return true;
            case -3: return false;
            default: throw new RuntimeException("Unknown opcode(EVAL_FF_Z): "+op);
        }
    }
    public static boolean EVAL_II_Z(int op, int lhs, int rhs){
        switch(op){
            case 161: return lhs < rhs;
            case 163: return lhs > rhs;
            case 164: return lhs <= rhs;
            case 162: return lhs >= rhs;
            case 159: return lhs == rhs;
            case 160: return lhs != rhs;
            case -2: return true;
            case -3: return false;
            default: throw new RuntimeException("Unknown opcode(EVAL_II_Z): "+op);
        }
    }
    public static long EVAL_JI_J(int op, long lhs, int rhs){
        switch(op){
            case 121: return lhs << rhs;
            case 123: return lhs >> rhs;
            case 125: return lhs >>> rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_JI_J): "+op);
        }
    }
    public static float EVAL_FF_F(int op, float lhs, float rhs){
        switch(op){
            case 98: return lhs + rhs;
            case 102: return lhs - rhs;
            case 106: return lhs * rhs;
            case 110: return lhs / rhs;
            case 114: return lhs % rhs;
            case -4: return lhs;
            case -5: return rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_FF_F): "+op);
        }
    }
    public static char EVAL_C_C(int op, char arg){
        switch(op){
            case 96: return ++ arg;
            case 100: return -- arg;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_C_C): "+op);
        }
    }
    public static short EVAL_S_S(int op, short arg){
        switch(op){
            case 96: return ++ arg;
            case 100: return -- arg;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_S_S): "+op);
        }
    }
    public static int EVAL_I_I(int op, int arg){
        switch(op){
            case 0: return + arg;
            case 116: return - arg;
            case 130: return ~ arg;
            case 96: return ++ arg;
            case 100: return -- arg;
            case -11: return -arg;
            case -12: return 1;
            case -13: return -1;
            case -14: return 0;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_I_I): "+op);
        }
    }
    public static boolean EVAL_LL_Z(int op, java.lang.Object lhs, java.lang.Object rhs){
        switch(op){
            case 165: return lhs == rhs;
            case 166: return lhs != rhs;
            case -2: return true;
            case -3: return false;
            default: throw new RuntimeException("Unknown opcode(EVAL_LL_Z): "+op);
        }
    }
    public static int EVAL_II_I(int op, int lhs, int rhs){
        switch(op){
            case 96: return lhs + rhs;
            case 100: return lhs - rhs;
            case 104: return lhs * rhs;
            case 108: return lhs / rhs;
            case 112: return lhs % rhs;
            case 126: return lhs & rhs;
            case 128: return lhs | rhs;
            case 130: return lhs ^ rhs;
            case 120: return lhs << rhs;
            case 122: return lhs >> rhs;
            case 124: return lhs >>> rhs;
            case -4: return lhs;
            case -5: return rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_II_I): "+op);
        }
    }
    public static java.lang.String EVAL_L_L(int op, java.lang.String arg){
        switch(op){
            case -6: return null;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_L_L): "+op);
        }
    }
    public static java.lang.Object EVAL_L_L(int op, java.lang.Object arg){
        switch(op){
            case -6: return null;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_L_L): "+op);
        }
    }
    public static byte EVAL_B_B(int op, byte arg){
        switch(op){
            case 96: return ++ arg;
            case 100: return -- arg;
            case -15: return arg;
            default: throw new RuntimeException("Unknown opcode(EVAL_B_B): "+op);
        }
    }
    public static boolean EVAL_ZZ_Z(int op, boolean lhs, boolean rhs){
        switch(op){
            case 126: return lhs & rhs;
            case 128: return lhs | rhs;
            case 130: return lhs ^ rhs;
            case 159: return lhs == rhs;
            case 160: return lhs != rhs;
            case 258: return lhs && rhs;
            case 259: return lhs || rhs;
            case -2: return true;
            case -3: return false;
            case -4: return lhs;
            case -5: return rhs;
            default: throw new RuntimeException("Unknown opcode(EVAL_ZZ_Z): "+op);
        }
    }

// Lazy methods -> called for short circuit operators
    public static boolean LAZY_LHS(int exprId, int op, boolean lhs){
    	// Check whether rhs will be executed
    	switch(op){
    		case 258:
    			// (true) && ... -> rhs will be executed    			
    			if(lhs) return lhs;
    			break;
    		case 259:
    			// (false) || ... -> rhs will be executed
    			if(!lhs) return lhs;
    			break;
    		default: throw new RuntimeException("Invalid opcode(LAZY_LHS): "+op);
    	}
    	
    	// rhs will not be executed -> coverExpression and evaluate as much mutants as possbile    	
    	coverExpression(exprId);
    	
    	if(!USE_INFECTION) return lhs;
    	
    	Expression expr = mapExprId2Expression.get(exprId);
    	
    	if(expr.liveMutants!=null){
			// Add all killable mutants for this expr (if mutated)
	        for(Mutant m : expr.liveMutants){
	        	int mutOp = mapMutId2Opcode.get(m.mutId);
	        	// &&
		    	if(op==258){
			    	switch(mutOp){
			    		case 126: // lhs & rhs;
			    		case 128: // lhs | rhs;
			    		case 130: // lhs ^ rhs;
			    		case 159: // lhs == rhs;
			    		case 160: // lhs != rhs;
			    		case 259: // lhs || rhs;
			    		case -5:  // rhs;
			    			m.setValue(!lhs);
			    			expr.killableMutants.add(m);
			    			continue; // consider these mutants as covered and killable since we cannot evaluate them		    			
			    		case -2:  // true;
			    		case -3:  // false;
			    		case -4:  // lhs;
			    			break;
			    		default:
			    			throw new RuntimeException("Unknown opcode(LAZY_LHS): "+mutOp);
			    	}
		    	}
		    	// ||
		    	else if(op==259){
		    		switch(mutOp){
			    		case 126: // lhs & rhs;
			    		case 128: // lhs | rhs;
			    		case 130: // lhs ^ rhs;
			    		case 159: // lhs == rhs;
			    		case 160: // lhs != rhs;
			    		case 258: // lhs && rhs;
			    		case -5:  // rhs;
			    			m.setValue(!lhs);
			    			expr.killableMutants.add(m);
			    			continue; // consider these mutants as covered and killable since we cannot evaluate them   			
			    		case -2:  // true;
			    		case -3:  // false;
			    		case -4:  // lhs;
			    			break;
			    		default:
			    			throw new RuntimeException("Unknown opcode(LAZY_LHS): "+mutOp);
		    		}
		    	}else throw new RuntimeException("Invalid opcode(LAZY_LHS): "+op);
        		boolean mutValue = EVAL_ZZ_Z(mutOp, lhs, false);
        		if(mutValue!=lhs){
	    			m.setValue(mutValue);
	    			expr.killableMutants.add(m);
        		}
            }
            
            if(!USE_PROPAGATION){
				handleToplevel(exprId);
				return lhs;
			}
            
	        // binary expressions always have a lhs and rhs node
			assert(expr.subExpressions.size()==2);
			Expression lhsExpr = expr.subExpressions.get(0);
			
	        // Check for mutants of lhs child expressions
	        for(Mutant m : lhsExpr.killableMutants){
	        	boolean mutValue = m.getValue(lhs);
	        	if(mutValue!=lhs){
	        		expr.killableMutants.add(m);
	        	}
	        }
    	}
           
		return lhs;
    }
    
    public static boolean LAZY_SE_LHS(int exprId, int op, boolean lhs){
    	// Check whether rhs will be executed
    	switch(op){
    		case 258:
    			// (true) && ... -> rhs will be executed    			
    			if(lhs) return lhs;
    			break;
    		case 259:
    			// (false) || ... -> rhs will be executed
    			if(!lhs) return lhs;
    			break;
    		default: throw new RuntimeException("Invalid opcode(LAZY_SE_LHS): "+op);
    	}
    	
    	// rhs will not be executed -> coverExpression and evaluate as much mutants as possbile    	
    	coverExpression(exprId);
    	
    	if(!USE_INFECTION) return lhs;
    	
    	Expression expr = mapExprId2Expression.get(exprId);
    	
    	if(expr.liveMutants!=null){
			// Add all killable mutants for this expr (if mutated)
    		Iterator<Mutant> iter = expr.liveMutants.iterator();
	        while(iter.hasNext()){
	        	Mutant m = iter.next();
	        	int mutOp = mapMutId2Opcode.get(m.mutId);
	        	// &&
		    	if(op==258){
			    	switch(mutOp){
			    		case 126: // lhs & rhs;
			    		case 128: // lhs | rhs;
			    		case 130: // lhs ^ rhs;
			    		case 159: // lhs == rhs;
			    		case 160: // lhs != rhs;
			    		case 259: // lhs || rhs;
			    			m.setValue(!lhs);
			    			// consider these mutants as covered and killable since we cannot evaluate them
			    			expr.killableMutants.add(m);
			    			continue; 		    			
			    		case -2:  // true;
			    		case -3:  // false;
			    		case -4:  // lhs;
			    		case -5:  // rhs;
		        			// this expression contains side effects 
			    			// -> mark these mutants killable and do not propagate
			        		allKillableMutants.add(m.mutId);
			        		iter.remove();
			        		continue;
			    		default:
			    			throw new RuntimeException("Unknown opcode(LAZY_SE_LHS): "+mutOp);
			    	}
		    	}
		    	// ||
		    	else if(op==259){
		    		switch(mutOp){
			    		case 126: // lhs & rhs;
			    		case 128: // lhs | rhs;
			    		case 130: // lhs ^ rhs;
			    		case 159: // lhs == rhs;
			    		case 160: // lhs != rhs;
			    		case 258: // lhs && rhs;
			    			m.setValue(!lhs);
			    			// consider these mutants as covered and killable since we cannot evaluate them
			    			expr.killableMutants.add(m);
			    			continue;    			
			    		case -2:  // true;
			    		case -3:  // false;
			    		case -4:  // lhs;
			    		case -5:  // rhs;
		        			// this expression contains side effects 
			    			// -> mark these mutants killable and do not propagate
			        		allKillableMutants.add(m.mutId);
			        		iter.remove();
			        		continue;

			    		default:
			    			throw new RuntimeException("Unknown opcode(LAZY_SE_LHS): "+mutOp);
		    		}
		    	}else throw new RuntimeException("Invalid opcode(LAZY_SE_LHS): "+op);
            }
            
            if(!USE_PROPAGATION){
				handleToplevel(exprId);
				return lhs;
			}
    		
    		// binary expressions always have a lhs and rhs node
			assert(expr.subExpressions.size()==2);
			Expression lhsExpr = expr.subExpressions.get(0);
	        
			// Check for mutants of lhs child expressions
	        for(Mutant m : lhsExpr.killableMutants){
	        	boolean mutValue = m.getValue(lhs);
	        	if(mutValue==lhs){
	        		
	        	}else{
	        		expr.killableMutants.add(m);
	        	}
	        }
    	}
           
		return lhs;
    }
    
    public static boolean LAZY_RHS(int exprId, int op, boolean rhs){
    	boolean lhs;
    	switch(op){
			case 258:
				// lhs && rhs ... -> lhs was true    			
				lhs = true;
				break;
			case 259:
				// lhs || rhs ... -> lhs was false
				lhs = false;
				break;
			default: throw new RuntimeException("Invalid opcode(LAZY_RHS): "+op);
    	}
    	// evaluate mutants
        boolean result = EXPR_ZZ_Z(exprId, op, lhs, rhs);
        if(result!=rhs) throw new RuntimeException("Unexpected result (LAZY_RHS): "+result+" != "+rhs);
                
        return rhs;
    }
    
    public static boolean LAZY_SE_RHS(int exprId, int op, boolean rhs){
    	boolean lhs;
    	switch(op){
			case 258:
				// lhs && rhs ... -> lhs was true    			
				lhs = true;
				break;
			case 259:
				// lhs || rhs ... -> lhs was false
				lhs = false;
				break;
			default: throw new RuntimeException("Invalid opcode(LAZY_SE_RHS): "+op);
    	}
    	// evaluate mutants
        boolean result = EXPR_SE_ZZ_Z(exprId, op, lhs, rhs);
        if(result!=rhs) throw new RuntimeException("Unexpected result (LAZY_SE_RHS): "+result+" != "+rhs);
                
        return rhs;
    }

}
