/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.hive.kll;

import java.util.ArrayList;
import java.util.List;

final class Util {

  static float[] objectsToPrimitives(final Float[] array) {
    final float[] result = new float[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  static double[] objectsToPrimitives(final Double[] array) {
    final double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  static List<Float> primitivesToList(final float[] array) {
    final List<Float> result = new ArrayList<>(array.length);
    for (float item: array) { result.add(item); }
    return result;
  }

  static List<Double> primitivesToList(final double[] array) {
    final List<Double> result = new ArrayList<>(array.length);
    for (double item: array) { result.add(item); }
    return result;
  }

}
