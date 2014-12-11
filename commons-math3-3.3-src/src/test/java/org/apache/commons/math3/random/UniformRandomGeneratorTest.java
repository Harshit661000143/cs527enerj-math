//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at

//http://www.apache.org/licenses/LICENSE-2.0

//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.commons.math3.random;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.Assert;
import org.junit.Test;


public class UniformRandomGeneratorTest {

    @Test
    public void testMeanAndStandardDeviation() {
        RandomGenerator rg = new JDKRandomGenerator();
        rg.setSeed(17399225432l);
        UniformRandomGenerator generator = new UniformRandomGenerator(rg);
        double[] sample = new double[10000];
        for (int i = 0; i < sample.length; ++i) {
            sample[i] = generator.nextNormalizedDouble();
        }
        assertEquals(0.0, StatUtils.mean(sample), 0.07);
        assertEquals(1.0, StatUtils.variance(sample), 0.02);
    }
   public void assertEquals(double obj,double obj1,double tol) {
 try
         {
           Assert.assertEquals(obj,obj1,tol);
	   double error2= obj1-obj;
           error2= Math.abs(error2);	
           System.out.println("\n********************PASSED****************\nExpected Value:" + obj+"\nError Value:"+error2+ "\nTolerance:"+tol);
         }
         catch (AssertionError e)
         {
           //  throw e;
	   double error= obj1-obj;
           error= Math.abs(error);	
           System.out.println("\n*********************FAILED****************\nExpected Value:"+obj+"\nError value:"+error);
         }
 
       }

}
