package generated.vdm;

import java.lang.Exception;
import org.overture.interpreter.values.BooleanValue;
import org.overture.interpreter.values.IntegerValue;
import org.overture.interpreter.values.SeqValue;
import org.overture.interpreter.values.Value;
import org.overture.interpreter.values.VoidValue;
import org.vdm.overture.VDMTypesHelper;
import org.vdm.test.Test;

public class VDMTest extends Test {
  public Value a(BooleanValue c, IntegerValue e, SeqValue test) throws Exception {
    super.a(VDMTypesHelper.getJavaValueFromVDMValue(c, "boolean"), VDMTypesHelper.getJavaValueFromVDMValue(e, "int"), VDMTypesHelper.getJavaValueFromVDMValue(test, "java.util.List<java.lang.Integer>"));
    return new VoidValue();
  }
}
