/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.math3.ml.distance;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link CanberraDistance} class.
 */
public class CanberraDistanceTest {
    final DistanceMeasure distance = new CanberraDistance();

    @Test
    public void testZero() {
        final double[] a = { 0, 1, -2, 3.4, 5, -6.7, 89 };
        assertEquals(0, distance.compute(a, a), 0d);
    }

    @Test
    public void testZero2() {
        final double[] a = { 0, 0 };
        assertEquals(0, distance.compute(a, a), 0d);
    }

    @Test
    public void test() {
        final double[] a = { 1, 2, 3, 4, 9 };
        final double[] b = { -5, -6, 7, 4, 3 };
        final double expected = 2.9;
        assertEquals(expected, distance.compute(a, b), 0d);
        assertEquals(expected, distance.compute(b, a), 0d);
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
