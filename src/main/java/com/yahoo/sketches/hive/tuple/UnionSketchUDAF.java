/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hive.tuple;

import static com.yahoo.sketches.Util.DEFAULT_NOMINAL_ENTRIES;

import java.util.Arrays;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFParameterInfo;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;

import com.yahoo.sketches.memory.NativeMemory;
import com.yahoo.sketches.tuple.Sketches;
import com.yahoo.sketches.tuple.Summary;
import com.yahoo.sketches.tuple.SummaryFactory;

/**
 * Generic implementation to be sub-classed with a particular type of Summary
 */
public abstract class UnionSketchUDAF extends AbstractGenericUDAFResolver {

  @Override
  public GenericUDAFEvaluator getEvaluator(final GenericUDAFParameterInfo info) throws SemanticException {
    final ObjectInspector[] inspectors = info.getParameterObjectInspectors();

    if (inspectors.length < 1) {
      throw new UDFArgumentException("Expected at least 1 argument");
    }
    if (inspectors.length > 2) {
      throw new UDFArgumentTypeException(inspectors.length - 1, "Expected no more than 2 arguments");
    }

    ObjectInspectorValidator.validateGivenPrimitiveCategory(inspectors[0], 0, PrimitiveCategory.BINARY);

    // number of nominal entries
    if (inspectors.length > 1) {
      ObjectInspectorValidator.validateIntegralParameter(inspectors[1], 1);
    }

    return createEvaluator();
  }

  /**
   * This is needed because a concrete UDAF is going to have its own concrete evaluator static inner class.
   * @return an instance of evaluator
   */
  public abstract GenericUDAFEvaluator createEvaluator();

  public static abstract class UnionSketchEvaluator<S extends Summary> extends SketchEvaluator<S> {

    private PrimitiveObjectInspector sketchInspector_;

    public UnionSketchEvaluator(final SummaryFactory<S> summaryFactory) {
      super(summaryFactory);
    }

    @Override
    public ObjectInspector init(final Mode mode, final ObjectInspector[] inspectors) throws HiveException {
      super.init(mode, inspectors);
      if (mode == Mode.PARTIAL1 || mode == Mode.COMPLETE) {
        // input is original data
        sketchInspector_ = (PrimitiveObjectInspector) inspectors[0];
        if (inspectors.length > 1) {
          numNominalEntriesInspector_ = (PrimitiveObjectInspector) inspectors[1];
        }
      } else {
        // input for PARTIAL2 and FINAL is the output from PARTIAL1
        intermediateInspector_ = (StructObjectInspector) inspectors[0];
      }

      if (mode == Mode.PARTIAL1 || mode == Mode.PARTIAL2) {
        // intermediate results need to include the the nominal number of entries
        return ObjectInspectorFactory.getStandardStructObjectInspector(
          Arrays.asList(NUM_NOMINAL_ENTRIES_FIELD, SKETCH_FIELD),
          Arrays.asList(
            PrimitiveObjectInspectorFactory.getPrimitiveWritableObjectInspector(PrimitiveCategory.INT),
            PrimitiveObjectInspectorFactory.getPrimitiveWritableObjectInspector(PrimitiveCategory.BINARY)
          )
        );
      } else {
        // final results include just the sketch
        return PrimitiveObjectInspectorFactory.getPrimitiveWritableObjectInspector(PrimitiveCategory.BINARY);
      }
    }

    @Override
    public void iterate(final @SuppressWarnings("deprecation") AggregationBuffer buf, final Object[] data) throws HiveException {
      if (data[0] == null) return;
      @SuppressWarnings("unchecked")
      final UnionState<S> state = (UnionState<S>) buf;
      if (!state.isInitialized()) {
        initializeState(state, data);
      }
      final byte[] serializedSketch = (byte[]) sketchInspector_.getPrimitiveJavaObject(data[0]);
      if (serializedSketch == null) return;
      state.update(Sketches.heapifySketch(new NativeMemory(serializedSketch)));
    }

    private void initializeState(final UnionState<S> state, final Object[] data) {
      int numNominalEntries = DEFAULT_NOMINAL_ENTRIES;
      if (numNominalEntriesInspector_ != null) {
        numNominalEntries = PrimitiveObjectInspectorUtils.getInt(data[1], numNominalEntriesInspector_);
      } 
      state.init(numNominalEntries, summaryFactory_);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AggregationBuffer getNewAggregationBuffer() throws HiveException {
      return new UnionState<S>();
    }

  }

}