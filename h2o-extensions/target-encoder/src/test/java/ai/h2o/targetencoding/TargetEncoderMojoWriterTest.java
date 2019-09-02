package ai.h2o.targetencoding;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import water.Scope;
import water.TestUtil;
import water.fvec.Frame;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TargetEncoderMojoWriterTest extends TestUtil {

  @BeforeClass
  public static void stall() { stall_till_cloudsize(1); }

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void writeModelToZipFile() throws Exception{

    TargetEncoderModel targetEncoderModel = null;
    String fileNameForMojo = "test_mojo_te.zip";
    try {
      Scope.enter();
      Frame trainFrame = parse_test_file("./smalldata/gbm_test/titanic.csv");
      Scope.track(trainFrame);
      TargetEncoderModel.TargetEncoderParameters p = new TargetEncoderModel.TargetEncoderParameters();
      String responseColumnName = "survived";

      asFactor(trainFrame, responseColumnName);

      p._blending = false;
      p._response_column = responseColumnName;
      p._ignored_columns = ignoredColumns(trainFrame, "home.dest", "embarked", p._response_column);
      p.setTrain(trainFrame._key);

      TargetEncoderBuilder builder = new TargetEncoderBuilder(p);

      builder.trainModel().get(); // Waiting for training to be finished
      targetEncoderModel = builder.getTargetEncoderModel(); // TODO change the way of how we getting model after PUBDEV-6670. We should be able to get it from DKV with .trainModel().get()
      Scope.track_generic(targetEncoderModel);
      File mojoFile = folder.newFile(fileNameForMojo);
      
      try (FileOutputStream modelOutput = new FileOutputStream(mojoFile)){
        assertEquals(0, mojoFile.length());

        targetEncoderModel.getMojo().writeTo(modelOutput);
        assertTrue(mojoFile.length() > 0);
      }
    } finally {
      if(targetEncoderModel != null) TargetEncoderFrameHelper.encodingMapCleanUp(targetEncoderModel._output._target_encoding_map);
      Scope.exit();
    }
  }

}
