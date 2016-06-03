/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldRef;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.ValueUtil;
import org.jpmml.sklearn.ClassDictUtil;
import org.jpmml.sklearn.FeatureMapper;
import org.jpmml.sklearn.MatrixUtil;
import sklearn.Transformer;

public class PCA extends Transformer {

	public PCA(String module, String name){
		super(module, name);
	}

	@Override
	public List<Feature> encodeFeatures(String id, List<Feature> inputFeatures, FeatureMapper featureMapper){
		int[] shape = getComponentsShape();

		int numberOfComponents = shape[0];
		int numberOfFeatures = shape[1];

		if(inputFeatures.size() != numberOfFeatures){
			throw new IllegalArgumentException();
		}

		List<? extends Number> components = getComponents();
		List<? extends Number> mean = getMean();

		Boolean whiten = getWhiten();

		List<? extends Number> explainedVariance = (whiten ? getExplainedVariance() : null);

		List<Feature> features = new ArrayList<>();

		for(int i = 0; i < numberOfComponents; i++){
			List<? extends Number> component = MatrixUtil.getRow(components, numberOfComponents, numberOfFeatures, i);

			Apply apply = new Apply("sum");

			for(int j = 0; j < numberOfFeatures; j++){
				Feature inputFeature = inputFeatures.get(j);

				// "($name[i] - mean[i]) * component[i]"
				Expression expression = new FieldRef(inputFeature.getName());

				Number meanValue = mean.get(j);
				if(!ValueUtil.isZero(meanValue)){
					expression = PMMLUtil.createApply("-", expression, PMMLUtil.createConstant(meanValue));
				}

				Number componentValue = component.get(j);
				if(!ValueUtil.isOne(componentValue)){
					expression = PMMLUtil.createApply("*", expression, PMMLUtil.createConstant(componentValue));
				}

				apply.addExpressions(expression);
			}

			if(whiten){
				Number explainedVarianceValue = explainedVariance.get(i);

				if(!ValueUtil.isOne(explainedVarianceValue)){
					apply = PMMLUtil.createApply("/", apply, PMMLUtil.createConstant(Math.sqrt(ValueUtil.asDouble(explainedVarianceValue))));
				}
			}

			DerivedField derivedField = featureMapper.createDerivedField(createName(id, i), apply);

			features.add(new ContinuousFeature(derivedField));
		}

		return features;
	}

	@Override
	protected String name(){
		return "pca";
	}

	public Boolean getWhiten(){
		return (Boolean)get("whiten");
	}

	public List<? extends Number> getComponents(){
		return (List)ClassDictUtil.getArray(this, "components_");
	}

	public List<? extends Number> getExplainedVariance(){
		return (List)ClassDictUtil.getArray(this, "explained_variance_");
	}

	public List<? extends Number> getMean(){
		return (List)ClassDictUtil.getArray(this, "mean_");
	}

	private int[] getComponentsShape(){
		return ClassDictUtil.getShape(this, "components_", 2);
	}
}