package hex.schemas;
import ai.h2o.targetencoding.TargetEncoder;
import ai.h2o.targetencoding.TargetEncoderBuilder;
import ai.h2o.targetencoding.TargetEncoderModel;
import water.api.API;
import water.api.schemas3.ModelParametersSchemaV3;

import java.util.List;

public class TargetEncoderV3 extends ModelBuilderSchema<TargetEncoderBuilder, TargetEncoderV3, TargetEncoderV3.TargetEncoderParametersV3> {
  public static class TargetEncoderParametersV3 extends ModelParametersSchemaV3<TargetEncoderModel.TargetEncoderParameters, TargetEncoderParametersV3> {
    
    @API(help = "Blending enabled/disabled")
    public boolean blending;

    @API(help = "Inflection point. Used for blending (if enabled). Blending is to be enabled separately using the 'blending' parameter.")
    public double k;

    @API(help = "Smooothing. Used for blending (if enabled). Blending is to be enabled separately using the 'blending' parameter.")
    public double f;

    @API(help = "Data leakage handling strategy. Default to None.", values = {"None", "KFold", "LeaveOneOut"})
    public TargetEncoder.DataLeakageHandlingStrategy data_leakage_handling;
  
    @Override
    public String[] fields() {
      final List<String> params = extractDeclaredApiParameters(getClass());
      params.add("model_id");
      params.add("ignored_columns");
      params.add("training_frame");
      params.add("fold_column");
  
      return params.toArray(new String[0]);
    }

    @Override
    public TargetEncoderParametersV3 fillFromImpl(TargetEncoderModel.TargetEncoderParameters impl) {
      return fillFromImpl(impl, new String[0]);
    }

    @Override
    protected TargetEncoderParametersV3 fillFromImpl(TargetEncoderModel.TargetEncoderParameters impl, String[] fieldsToSkip) {
      final TargetEncoderParametersV3 teParamsV3 = super.fillFromImpl(impl, fieldsToSkip);
      if (impl._blending_parameters != null) {
        teParamsV3.f = impl._blending_parameters.getF();
        teParamsV3.k = impl._blending_parameters.getK();
      }
      return teParamsV3;
    }
  }
}
