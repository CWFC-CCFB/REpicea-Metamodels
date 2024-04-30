/*
 * This file is part of the repicea-metamodels library.
 *
 * Copyright (C) 2021-24 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed with the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * Please see the license at http://www.gnu.org/copyleft/lesser.html.
 */
package repicea.simulation.metamodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import repicea.math.Matrix;
import repicea.simulation.metamodel.ParametersMapUtilities.InputParametersMapKey;
import repicea.stats.data.StatisticalDataException;
import repicea.stats.distributions.GaussianDistribution;

/**
 * A modified implementation of the Chapman-Richards derivative model.<p>
 * This model is meant to work with stem density.
 * @author Mathieu Fortin - April 2024
 */
class ModifiedChapmanRichardsDerivativeModelImplementation extends AbstractModelImplementation {

	protected ModifiedChapmanRichardsDerivativeModelImplementation(String outputType, MetaModel model, LinkedHashMap<String, Object>[] startingValues) throws StatisticalDataException {
		super(outputType, model, startingValues);
	}

	@Override
	double getPrediction(double ageYr, double timeSinceBeginning, double r1, Matrix parameters) {
		return ModifiedChapmanRichardsDerivativeModelImplementation.computePredictions(
				parameters == null ? getParameters() : parameters, 
				ageYr, 
				timeSinceBeginning, 
				r1);
	}
	
	static double computePredictions(Matrix parms, double ageYr, double timeSinceBeginning, double r1) {
		double b1 = parms.getValueAt(0, 0);
		double b2 = parms.getValueAt(1, 0);
		double b3 = parms.getValueAt(2, 0);
		double pred = (b1 + r1) * Math.exp(-b2 * ageYr) * (1 - Math.exp(-b3 * ageYr));
		return pred;
	}

	@Override
	public GaussianDistribution getStartingParmEst(double coefVar) {
		fixedEffectsParameterIndices = new ArrayList<Integer>();
		fixedEffectsParameterIndices.add(0);
		fixedEffectsParameterIndices.add(1);
		fixedEffectsParameterIndices.add(2);

		indexCorrelationParameter = 3;
		indexResidualErrorVariance = 4;

		int lastIndex = !isVarianceErrorTermAvailable ? 
				indexResidualErrorVariance + 1: 
					indexResidualErrorVariance;

		Matrix parmEst = new Matrix(lastIndex,1);
		setFixedEffectStartingValuesFromParametersMap(parmEst);

		Matrix varianceDiag = new Matrix(parmEst.m_iRows,1);
		for (int i = 0; i < varianceDiag.m_iRows; i++) {
			varianceDiag.setValueAt(i, 0, Math.pow(parmEst.getValueAt(i, 0) * coefVar, 2d));
		}
		
		GaussianDistribution gd = new GaussianDistribution(parmEst, varianceDiag.matrixDiagonal());

		return gd;
	}

	@Override
	Matrix getFirstDerivative(double ageYr, double timeSinceBeginning, double r1) {
		return computeDerivatives(getParameters(), ageYr, timeSinceBeginning, r1);
	}

	static Matrix computeDerivatives(Matrix parms, double ageYr, double timeSinceBeginning, double r1) {
		double b1 = parms.getValueAt(0, 0);
		double b2 = parms.getValueAt(1, 0);
		double b3 = parms.getValueAt(2, 0);
		
		double exp1 = Math.exp(-b2 * ageYr);
		double exp2 = Math.exp(-b3 * ageYr);
		
		Matrix derivatives = new Matrix(3,1);
		derivatives.setValueAt(0, 0, exp1 * (1 - exp2));
		derivatives.setValueAt(1, 0, (b1 + r1) * exp1 * (1 - exp2) * -ageYr);
		derivatives.setValueAt(2, 0, (b1 + r1) * exp1 * exp2 * ageYr);
		return derivatives;
	}
	
	
	@Override
	public boolean isInterceptModel() {return false;}

	@Override
	public List<String> getEffectList() {
		return Arrays.asList(new String[] {"b1","b2","b3"});
	}

	@Override
	List<String> getParameterNames() {
		return Arrays.asList(isVarianceErrorTermAvailable ? 				
				new String[] {"b1", "b2", "b3", AbstractModelImplementation.CORRELATION_PARM} :
					new String[] {"b1", "b2", "b3", AbstractModelImplementation.CORRELATION_PARM, AbstractModelImplementation.RESIDUAL_VARIANCE});
	}

	@Override
	public String getModelDefinition() {
		return "y ~ b1*exp(-b2*t)*(1-exp(-b3*t))";
	}

	@SuppressWarnings("unchecked")
	@Override
	LinkedHashMap<String, Object>[] getDefaultParameters() {
		 LinkedHashMap<String, Object>[] inputMap = new LinkedHashMap[5];
		 LinkedHashMap<String, Object> oMap = new LinkedHashMap<String, Object>();
		 oMap.put(InputParametersMapKey.Parameter.name(), "b1");
		 oMap.put(InputParametersMapKey.StartingValue.name(), 5000 + "");
		 oMap.put(InputParametersMapKey.Distribution.name(), "Uniform");
		 oMap.put(InputParametersMapKey.DistParms.name(), new String[]{"0", "10000"});
		 inputMap[0] = oMap;
		 oMap = new LinkedHashMap<String, Object>();
		 oMap.put(InputParametersMapKey.Parameter.name(), "b2");
		 oMap.put(InputParametersMapKey.StartingValue.name(), 0.005 + "");
		 oMap.put(InputParametersMapKey.Distribution.name(), "Uniform");
		 oMap.put(InputParametersMapKey.DistParms.name(), new String[]{"0.0001", "0.01"});
		 inputMap[1] = oMap;
		 oMap = new LinkedHashMap<String, Object>();
		 oMap.put(InputParametersMapKey.Parameter.name(), "b3");
		 oMap.put(InputParametersMapKey.StartingValue.name(), 0.2 + "");
		 oMap.put(InputParametersMapKey.Distribution.name(), "Uniform");
		 oMap.put(InputParametersMapKey.DistParms.name(), new String[]{"0.001", "0.5"});
		 inputMap[2] = oMap;
		 oMap = new LinkedHashMap<String, Object>();
		 oMap.put(InputParametersMapKey.Parameter.name(), AbstractModelImplementation.CORRELATION_PARM);
		 oMap.put(InputParametersMapKey.StartingValue.name(), 0.92 + "");
		 oMap.put(InputParametersMapKey.Distribution.name(), "Uniform");
		 oMap.put(InputParametersMapKey.DistParms.name(), new String[]{"0.80", "0.995"});
		 inputMap[3] = oMap;
		 oMap = new LinkedHashMap<String, Object>();
		 oMap.put(InputParametersMapKey.Parameter.name(), AbstractModelImplementation.RESIDUAL_VARIANCE);
		 oMap.put(InputParametersMapKey.StartingValue.name(), 2500 + "");
		 oMap.put(InputParametersMapKey.Distribution.name(), "Uniform");
		 oMap.put(InputParametersMapKey.DistParms.name(), new String[]{"0", "5000"});
		 inputMap[4] = oMap;
		 return inputMap;
	}

}
